(function () {
  const IP = "frostffa.xyz";

  // Copy IP
  const copyBtn = document.getElementById("copy-ip");
  const toast = document.getElementById("toast");

  function showToast(message) {
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add("show");
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => toast.classList.remove("show"), 2200);
  }

  async function copyIp() {
    try {
      await navigator.clipboard.writeText(IP);
      showToast("IP copied — paste in Minecraft!");
    } catch {
      const ta = document.createElement("textarea");
      ta.value = IP;
      ta.style.position = "fixed";
      ta.style.left = "-9999px";
      document.body.appendChild(ta);
      ta.select();
      document.execCommand("copy");
      document.body.removeChild(ta);
      showToast("IP copied!");
    }
  }

  copyBtn?.addEventListener("click", copyIp);

  // Mobile nav
  const toggle = document.querySelector(".menu-toggle");
  const nav = document.querySelector(".nav");
  toggle?.addEventListener("click", () => {
    const open = nav?.classList.toggle("open");
    toggle.setAttribute("aria-expanded", open ? "true" : "false");
  });

  nav?.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", () => nav.classList.remove("open"));
  });

  // Scroll reveal
  const revealEls = document.querySelectorAll(".reveal");
  if (revealEls.length && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -40px 0px" }
    );
    revealEls.forEach((el) => observer.observe(el));
  } else {
    revealEls.forEach((el) => el.classList.add("is-visible"));
  }

  // Snow particles
  const canvas = document.getElementById("snow");
  if (!canvas) return;

  const ctx = canvas.getContext("2d");
  let w = 0;
  let h = 0;
  const flakes = [];

  function resize() {
    w = canvas.width = window.innerWidth;
    h = canvas.height = window.innerHeight;
  }

  function spawnFlake(fromTop = true) {
    return {
      x: Math.random() * w,
      // Always enter from above the viewport (stagger so they don't rain as one line)
      y: fromTop ? -(Math.random() * h) - 4 : -4,
      r: Math.random() * 2.2 + 0.6,
      speed: Math.random() * 0.8 + 0.35,
      drift: Math.random() * 0.6 - 0.3,
      alpha: Math.random() * 0.45 + 0.15,
    };
  }

  window.addEventListener("resize", resize);
  resize();

  const count = Math.min(120, Math.floor(w / 12));
  for (let i = 0; i < count; i++) flakes.push(spawnFlake(true));

  function tick() {
    ctx.clearRect(0, 0, w, h);
    for (const f of flakes) {
      f.y += f.speed;
      f.x += f.drift;
      if (f.y > h + 4) {
        f.y = -4;
        f.x = Math.random() * w;
      }
      ctx.beginPath();
      ctx.arc(f.x, f.y, f.r, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(200, 235, 255, ${f.alpha})`;
      ctx.fill();
    }
    requestAnimationFrame(tick);
  }

  tick();
})();
