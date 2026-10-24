(function () {
  "use strict";

  const storageKey = "clinguard-theme";
  const root = document.documentElement;
  const systemTheme = window.matchMedia(
    "(prefers-color-scheme: dark)"
  );

  let savedTheme = null;

  try {
    savedTheme = localStorage.getItem(storageKey);
  } catch (_) {}

  let manualSelection =
    savedTheme === "light" || savedTheme === "dark";

  function applyTheme(theme) {
    root.dataset.theme = theme;

    const button = document.getElementById("cg-theme-toggle");

    if (button) {
      const dark = theme === "dark";

      button.textContent = dark
        ? "☀ Modo claro"
        : "☾ Modo oscuro";

      button.setAttribute(
        "aria-label",
        dark ? "Cambiar a modo claro" : "Cambiar a modo oscuro"
      );

      button.setAttribute("aria-pressed", String(dark));
    }
  }

  applyTheme(
    manualSelection
      ? savedTheme
      : systemTheme.matches ? "dark" : "light"
  );

  function createButton() {
    if (document.getElementById("cg-theme-toggle")) {
      return;
    }

    const button = document.createElement("button");

    button.id = "cg-theme-toggle";
    button.className = "cg-theme-toggle";
    button.type = "button";

    button.addEventListener("click", function () {
      const theme =
        root.dataset.theme === "dark" ? "light" : "dark";

      manualSelection = true;
      applyTheme(theme);

      try {
        localStorage.setItem(storageKey, theme);
      } catch (_) {}
    });

    document.body.appendChild(button);
    applyTheme(root.dataset.theme);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", createButton, {
      once: true
    });
  } else {
    createButton();
  }

  systemTheme.addEventListener("change", function (event) {
    if (!manualSelection) {
      applyTheme(event.matches ? "dark" : "light");
    }
  });

  window.addEventListener("storage", function (event) {
    if (event.key !== storageKey) return;

    manualSelection =
      event.newValue === "light" || event.newValue === "dark";

    applyTheme(
      manualSelection
        ? event.newValue
        : systemTheme.matches ? "dark" : "light"
    );
  });
})();