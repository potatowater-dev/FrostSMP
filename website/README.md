# Frost FFA Website

Minecraft-themed landing page for **frostffa.xyz**, deployable with Firebase Hosting + Stripe rank store.

**DNS split:**
- `frostffa.xyz` → Minecraft server
- `play.frostffa.xyz` → this website (Firebase Hosting)

## Local preview

```bash
cd website
npx -y firebase-tools@latest emulators:start --only hosting
```

Open http://localhost:5000

## Deploy hosting

```bash
cd website
npx -y firebase-tools@latest deploy --only hosting:frostffa
```

## Stripe rank store

Ranks: **Ice $1**, **Snow $3**, **Frost $5**, **Subzero $10**

Checkout uses **Firebase Cloud Functions** + **Stripe Checkout** (secret key never touches the browser).

### One-time setup

1. **Firebase Blaze plan** (pay-as-you-go) — required for Cloud Functions  
   [Upgrade project](https://console.firebase.google.com/project/andventure-map/usage/details)

2. **Stripe account** → [dashboard.stripe.com](https://dashboard.stripe.com)  
   Copy **Secret key** (`sk_live_...` or `sk_test_...` for testing)

3. **Set Firebase secrets** (from `website/`):

```bash
firebase functions:secrets:set STRIPE_SECRET_KEY
firebase functions:secrets:set STRIPE_WEBHOOK_SECRET
```

Optional Discord purchase alerts:

```bash
firebase functions:secrets:set DISCORD_WEBHOOK_URL
# or set as env param when deploying
```

4. **Install & deploy functions**:

```bash
cd website/functions
npm install
cd ..
firebase deploy --only functions
```

5. **Stripe webhook** (Dashboard → Developers → Webhooks → Add endpoint):

| Field | Value |
|--------|--------|
| URL | `https://play.frostffa.xyz/api/stripe-webhook` |
| Events | `checkout.session.completed` |

Copy the **Signing secret** (`whsec_...`) into `STRIPE_WEBHOOK_SECRET`.

6. **Redeploy hosting** (includes API rewrites):

```bash
firebase deploy --only hosting:frostffa,functions
```

### After purchase

The webhook logs `{ username, rank, luckperms_group }`. Grant the matching LuckPerms group manually, or wire a Discord bot / plugin later.

Suggested LuckPerms groups: `ice`, `snow`, `frost`, `subzero`.

### Auto-grant in-game (FrostStore)

1. Create a **Firestore database** (Native mode) in Firebase Console if you don’t have one:  
   https://console.firebase.google.com/project/andventure-map/firestore
2. Finish the **Stripe webhook** (`checkout.session.completed`) so purchases are queued.
3. Install `froststore/build/libs/FrostStore-1.0.0.jar` + LuckPerms.
4. In `plugins/FrostStore/config.yml` set:
   - `api-key` = value from `website/functions/.store_api_key_for_server.txt` (same as Firebase `STORE_API_KEY`)
   - `api-base-url: "https://frostffa.web.app"`
5. Create LP groups `ice` / `snow` / `frost` / `subzero`.
6. Restart. FrostStore polls every few seconds and runs `lp user {user} parent set {group}`.

### Test mode

Use Stripe **test keys** and test card `4242 4242 4242 4242`. Switch to live keys before going public.

## Custom domain (play.frostffa.xyz)

Firebase Console → Hosting → **frostffa** → Add custom domain → `play.frostffa.xyz`

Namecheap DNS:

| Host | Type | Value |
|------|------|--------|
| `play` | CNAME | `frostffa.web.app` |

Keep existing `@` A + SRV records for Minecraft.

## Links

- Server IP: `frostffa.xyz`
- Discord: https://discord.gg/playfrost
