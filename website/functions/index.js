/** @typedef {{ name: string, price: number, description: string, group: string }} Rank */

/** @type {Record<string, Rank>} */
const RANKS = {
  ice: {
    name: "Ice",
    price: 100,
    description: "Ice rank — starter supporter perks on Frost FFA.",
    group: "ice",
  },
  snow: {
    name: "Snow",
    price: 300,
    description: "Snow rank — extra kit perks and chat flair.",
    group: "snow",
  },
  frost: {
    name: "Frost",
    price: 500,
    description: "Frost rank — premium arena perks.",
    group: "frost",
  },
  subzero: {
    name: "Subzero",
    price: 1000,
    description: "Subzero rank — top tier supporter package.",
    group: "subzero",
  },
};

const MINECRAFT_USERNAME = /^[a-zA-Z0-9_]{3,16}$/;

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret, defineString } = require("firebase-functions/params");
const admin = require("firebase-admin");
const Stripe = require("stripe");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

const stripeSecret = defineSecret("STRIPE_SECRET_KEY");
const webhookSecret = defineSecret("STRIPE_WEBHOOK_SECRET");
const storeApiKey = defineSecret("STORE_API_KEY");
const discordWebhookUrl = defineString("DISCORD_WEBHOOK_URL", { default: "" });

function siteOrigin(req) {
  const configured = process.env.SITE_ORIGIN;
  if (configured) {
    return configured.replace(/\/$/, "");
  }
  const host = req.get("x-forwarded-host") || req.get("host");
  const proto = req.get("x-forwarded-proto") || "https";
  if (host) {
    return `${proto}://${host}`;
  }
  return "https://frostffa.web.app";
}

function validateUsername(raw) {
  if (typeof raw !== "string") {
    return null;
  }
  const trimmed = raw.trim();
  if (!MINECRAFT_USERNAME.test(trimmed)) {
    return null;
  }
  return trimmed;
}

function authorizeStore(req) {
  const expected = storeApiKey.value().trim();
  if (!expected) {
    return false;
  }
  const header = req.get("authorization") || "";
  const token = header.toLowerCase().startsWith("bearer ")
    ? header.slice(7).trim()
    : String(req.get("x-api-key") || "").trim();
  return token.length > 0 && token === expected;
}

exports.createCheckoutSession = onRequest(
  {
    secrets: [stripeSecret],
    cors: [/frostffa\.web\.app$/, /frostffa\.xyz$/, /localhost/],
    maxInstances: 10,
  },
  async (req, res) => {
    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed" });
      return;
    }

    try {
      const rankId = String(req.body?.rank || "").toLowerCase();
      const rank = RANKS[rankId];
      if (!rank) {
        res.status(400).json({ error: "Unknown rank" });
        return;
      }

      const username = validateUsername(req.body?.username);
      if (!username) {
        res.status(400).json({ error: "Invalid Minecraft username (3–16 chars, letters/numbers/_)" });
        return;
      }

      const stripe = new Stripe(stripeSecret.value().trim());
      const origin = siteOrigin(req);

      const session = await stripe.checkout.sessions.create({
        mode: "payment",
        payment_method_types: ["card"],
        line_items: [
          {
            quantity: 1,
            price_data: {
              currency: "usd",
              unit_amount: rank.price,
              product_data: {
                name: `${rank.name} Rank — Frost FFA`,
                description: rank.description,
              },
            },
          },
        ],
        metadata: {
          rank_id: rankId,
          rank_name: rank.name,
          luckperms_group: rank.group,
          minecraft_username: username,
        },
        success_url: `${origin}/success?session_id={CHECKOUT_SESSION_ID}`,
        cancel_url: `${origin}/cancel`,
        client_reference_id: username,
      });

      res.status(200).json({ url: session.url });
    } catch (error) {
      console.error("createCheckoutSession failed", error);
      res.status(500).json({ error: "Could not start checkout" });
    }
  }
);

exports.stripeWebhook = onRequest(
  {
    secrets: [stripeSecret, webhookSecret],
    maxInstances: 5,
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method not allowed");
      return;
    }

    const stripe = new Stripe(stripeSecret.value().trim());
    const signature = req.headers["stripe-signature"];
    let event;

    try {
      event = stripe.webhooks.constructEvent(
        req.rawBody,
        signature,
        webhookSecret.value().trim()
      );
    } catch (error) {
      console.error("Webhook signature verification failed", error.message);
      res.status(400).send(`Webhook Error: ${error.message}`);
      return;
    }

    if (event.type === "checkout.session.completed") {
      const session = event.data.object;
      const username = session.metadata?.minecraft_username || session.client_reference_id || "unknown";
      const rankName = session.metadata?.rank_name || session.metadata?.rank_id || "unknown";
      const group = session.metadata?.luckperms_group || String(rankName).toLowerCase();
      const rankId = session.metadata?.rank_id || group;

      console.log(
        JSON.stringify({
          type: "rank_purchase",
          username,
          rank: rankName,
          group,
          sessionId: session.id,
          amountTotal: session.amount_total,
        })
      );

      try {
        await db.collection("rank_purchases").doc(session.id).set(
          {
            username,
            rank: rankName,
            rank_id: rankId,
            group,
            status: "pending",
            amount_total: session.amount_total || 0,
            created_at: admin.firestore.FieldValue.serverTimestamp(),
            updated_at: admin.firestore.FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
      } catch (firestoreError) {
        console.error("Failed to queue purchase", firestoreError);
      }

      const hook = discordWebhookUrl.value();
      if (hook) {
        try {
          await fetch(hook, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              content: null,
              embeds: [
                {
                  title: "New rank purchase",
                  color: 0x4eeaff,
                  fields: [
                    { name: "Player", value: username, inline: true },
                    { name: "Rank", value: rankName, inline: true },
                    { name: "LuckPerms group", value: group, inline: true },
                    { name: "Session", value: session.id, inline: false },
                  ],
                },
              ],
            }),
          });
        } catch (discordError) {
          console.error("Discord webhook failed", discordError);
        }
      }
    }

    res.status(200).json({ received: true });
  }
);

exports.pendingPurchases = onRequest(
  {
    secrets: [storeApiKey],
    cors: false,
    maxInstances: 5,
  },
  async (req, res) => {
    if (req.method !== "GET") {
      res.status(405).json({ error: "Method not allowed" });
      return;
    }
    if (!authorizeStore(req)) {
      res.status(401).json({ error: "Unauthorized" });
      return;
    }

    try {
      const snap = await db
        .collection("rank_purchases")
        .where("status", "==", "pending")
        .limit(25)
        .get();

      const purchases = snap.docs.map((doc) => {
        const data = doc.data() || {};
        return {
          id: doc.id,
          username: data.username || "",
          group: data.group || "",
          rank: data.rank || data.group || "",
        };
      });

      res.status(200).json({ purchases });
    } catch (error) {
      console.error("pendingPurchases failed", error);
      res.status(500).json({ error: "Failed to list purchases" });
    }
  }
);

exports.claimPurchase = onRequest(
  {
    secrets: [storeApiKey],
    cors: false,
    maxInstances: 5,
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed" });
      return;
    }
    if (!authorizeStore(req)) {
      res.status(401).json({ error: "Unauthorized" });
      return;
    }

    const id = String(req.body?.id || "").trim();
    if (!id) {
      res.status(400).json({ error: "Missing id" });
      return;
    }

    try {
      await db.collection("rank_purchases").doc(id).set(
        {
          status: "fulfilled",
          fulfilled_at: admin.firestore.FieldValue.serverTimestamp(),
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      res.status(200).json({ ok: true });
    } catch (error) {
      console.error("claimPurchase failed", error);
      res.status(500).json({ error: "Failed to claim purchase" });
    }
  }
);

/** Recover paid Stripe checkouts that missed the webhook (auth: STORE_API_KEY). */
exports.syncPaidCheckouts = onRequest(
  {
    secrets: [stripeSecret, storeApiKey],
    cors: false,
    invoker: "public",
    maxInstances: 2,
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed" });
      return;
    }
    if (!authorizeStore(req)) {
      res.status(401).json({ error: "Unauthorized" });
      return;
    }

    try {
      const stripe = new Stripe(stripeSecret.value().trim());
      const limit = Math.min(25, Math.max(1, Number(req.body?.limit) || 10));
      const sessions = await stripe.checkout.sessions.list({ limit, status: "complete" });
      const queued = [];
      const skipped = [];

      for (const session of sessions.data) {
        if (session.payment_status !== "paid") {
          skipped.push({ id: session.id, reason: "not_paid" });
          continue;
        }

        const existing = await db.collection("rank_purchases").doc(session.id).get();
        if (existing.exists && existing.data()?.status === "fulfilled") {
          skipped.push({ id: session.id, reason: "already_fulfilled" });
          continue;
        }

        const username =
          session.metadata?.minecraft_username || session.client_reference_id || "unknown";
        const rankName = session.metadata?.rank_name || session.metadata?.rank_id || "unknown";
        const group = session.metadata?.luckperms_group || String(rankName).toLowerCase();
        const rankId = session.metadata?.rank_id || group;

        await db.collection("rank_purchases").doc(session.id).set(
          {
            username,
            rank: rankName,
            rank_id: rankId,
            group,
            status: "pending",
            amount_total: session.amount_total || 0,
            created_at: admin.firestore.FieldValue.serverTimestamp(),
            updated_at: admin.firestore.FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
        queued.push({ id: session.id, username, group });
      }

      res.status(200).json({ queued, skipped });
    } catch (error) {
      console.error("syncPaidCheckouts failed", error);
      res.status(500).json({ error: "Failed to sync checkouts" });
    }
  }
);
