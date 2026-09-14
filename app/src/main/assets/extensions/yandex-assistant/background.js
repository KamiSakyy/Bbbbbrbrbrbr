/* eslint-disable */
/**
 * Фоновая логика встроенного расширения:
 *  1) блокировка рекламы/трекеров через webRequest (это и есть основной
 *     источник экономии трафика — реклама не скачивается вообще);
 *  2) приём настроек из приложения через нативный порт GeckoView.
 */
const YB_DEFAULTS = { darkMode: true, blockAds: true };

const ybState = Object.assign({}, YB_DEFAULTS);
let ybBlocked = 0;

function ybHostMatches(host, entry) {
  return host === entry || host.endsWith("." + entry);
}

function ybIsBlocked(host) {
  if (!host) return false;
  const h = host.toLowerCase();
  for (let i = 0; i < YB_BLOCKLIST.length; i++) {
    const entry = YB_BLOCKLIST[i];
    if (entry.indexOf("/") >= 0) continue; // блокируем только домены — это безопасно
    if (ybHostMatches(h, entry)) return true;
  }
  return false;
}

async function ybLoadConfig() {
  try {
    const stored = await browser.storage.local.get(YB_DEFAULTS);
    ybState.darkMode = stored.darkMode !== false;
    ybState.blockAds = stored.blockAds !== false;
  } catch (e) {
    /* значения по умолчанию уже включены */
  }
}

function ybConnectNative() {
  try {
    const port = browser.runtime.connectNative("browser");
    port.onMessage.addListener(function (message) {
      if (!message || message.type !== "config") return;
      if (typeof message.darkMode === "boolean") ybState.darkMode = message.darkMode;
      if (typeof message.blockAds === "boolean") ybState.blockAds = message.blockAds;
      try {
        browser.storage.local.set({ darkMode: ybState.darkMode, blockAds: ybState.blockAds });
      } catch (e) {}
    });
  } catch (e) {
    /* без нативного порта работают настройки по умолчанию */
  }
}

ybLoadConfig().then(function () {
  ybConnectNative();

  browser.webRequest.onBeforeRequest.addListener(
    function (details) {
      if (!ybState.blockAds) return {};
      let host = "";
      try {
        host = new URL(details.url).hostname;
      } catch (e) {
        return {};
      }
      if (ybIsBlocked(host)) {
        ybBlocked++;
        return { cancel: true };
      }
      return {};
    },
    {
      urls: ["<all_urls>"],
      types: [
        "script",
        "image",
        "xmlhttprequest",
        "sub_frame",
        "stylesheet",
        "font",
        "media",
        "object",
        "other"
      ]
    },
    ["blocking"]
  );
});
