(function () {
  const form = document.getElementById("store-form");
  const usernameInput = document.getElementById("mc-username");
  const toast = document.getElementById("toast");
  const buttons = document.querySelectorAll("[data-rank].btn-rank");

  if (!form || !usernameInput || buttons.length === 0) {
    return;
  }

  const USERNAME = /^[a-zA-Z0-9_]{3,16}$/;

  function showToast(message) {
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add("show");
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => toast.classList.remove("show"), 2800);
  }

  function setLoading(loading) {
    buttons.forEach((btn) => {
      btn.disabled = loading;
      if (loading) {
        btn.dataset.originalText = btn.textContent;
        btn.textContent = "Redirecting…";
      } else if (btn.dataset.originalText) {
        btn.textContent = btn.dataset.originalText;
      }
    });
  }

  async function startCheckout(rank) {
    const username = usernameInput.value.trim();
    if (!USERNAME.test(username)) {
      usernameInput.focus();
      showToast("Enter a valid Minecraft username (3–16 chars)");
      return;
    }

    setLoading(true);
    try {
      const response = await fetch("/api/create-checkout-session", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ rank, username }),
      });

      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.error || "Checkout failed");
      }
      if (!data.url) {
        throw new Error("No checkout URL returned");
      }
      window.location.href = data.url;
    } catch (error) {
      setLoading(false);
      showToast(error.message || "Could not start checkout");
    }
  }

  buttons.forEach((button) => {
    button.addEventListener("click", () => {
      startCheckout(button.dataset.rank);
    });
  });

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const first = document.querySelector(".btn-rank");
    if (first) {
      startCheckout(first.dataset.rank);
    }
  });
})();
