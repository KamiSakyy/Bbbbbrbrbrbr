/* eslint-disable */
/**
 * Принудительная тёмная тема для сайтов, у которых её нет.
 *
 * Логика: движок уже сообщает сайтам `prefers-color-scheme: dark`,
 * поэтому сайты со своей тёмной темой УЖЕ переключились на неё сами.
 * Если фон страницы всё равно светлый — значит своей тёмной темы нет,
 * и мы аккуратно инвертируем картинку, оставляя изображения и видео
 * в исходных цветах.
 */
(async function () {
  if (window.top !== window) return; // работаем только в основном фрейме

  let ybEnabled = true;
  try {
    const cfg = await browser.storage.local.get({ darkMode: true });
    ybEnabled = cfg.darkMode !== false;
  } catch (e) {
    /* по умолчанию включено */
  }
  if (!ybEnabled) return;

  const YB_CSS = [
    "html.yb-dark { background-color: #000 !important; }",
    "html.yb-dark { filter: invert(1) hue-rotate(180deg) !important; color-scheme: dark !important; }",
    "html.yb-dark img,",
    "html.yb-dark picture,",
    "html.yb-dark video,",
    "html.yb-dark canvas,",
    "html.yb-dark iframe,",
    "html.yb-dark svg,",
    "html.yb-dark [style*='background-image'] {",
    "  filter: invert(1) hue-rotate(180deg) !important;",
    "}"
  ].join("\n");

  function ybInjectStyle() {
    if (document.getElementById("yb-force-dark-style")) return;
    const style = document.createElement("style");
    style.id = "yb-force-dark-style";
    style.textContent = YB_CSS;
    const host = document.head || document.documentElement;
    if (host) host.appendChild(style);
  }

  function ybParseColor(value) {
    if (!value) return null;
    const match = value.match(/rgba?\(([^)]+)\)/);
    if (!match) return null;
    const parts = match[1].split(",").map(function (p) {
      return parseFloat(p.trim());
    });
    return {
      r: parts[0],
      g: parts[1],
      b: parts[2],
      a: parts.length > 3 ? parts[3] : 1
    };
  }

  function ybLuminance(color) {
    if (!color) return null;
    return (0.2126 * color.r + 0.7152 * color.g + 0.0722 * color.b) / 255;
  }

  function ybEffectiveBackground() {
    let el = document.body;
    while (el) {
      const bg = ybParseColor(getComputedStyle(el).backgroundColor);
      if (bg && bg.a > 0.5) return bg;
      el = el.parentElement;
    }
    const rootBg = ybParseColor(getComputedStyle(document.documentElement).backgroundColor);
    if (rootBg && rootBg.a > 0.5) return rootBg;
    return { r: 255, g: 255, b: 255, a: 1 };
  }

  function ybDeclaresDark() {
    const metas = document.querySelectorAll(
      'meta[name="color-scheme"], meta[name="supported-color-schemes"]'
    );
    for (let i = 0; i < metas.length; i++) {
      const content = (metas[i].getAttribute("content") || "").toLowerCase();
      if (content.indexOf("dark") >= 0) return true;
    }
    return false;
  }

  function ybApplyIfLight() {
    if (!ybEnabled) return;
    if (!document.documentElement) return;
    if (document.documentElement.classList.contains("yb-dark")) return;
    if (ybDeclaresDark()) return;
    const lum = ybLuminance(ybEffectiveBackground());
    if (lum !== null && lum > 0.55) {
      document.documentElement.classList.add("yb-dark");
    }
  }

  ybInjectStyle();
  document.addEventListener(
    "DOMContentLoaded",
    function () {
      setTimeout(ybApplyIfLight, 200);
    },
    { once: true }
  );
  setTimeout(ybApplyIfLight, 900);
  setTimeout(ybApplyIfLight, 2200);
})();
