/* TF FB Reels & Post Maker — Native Android WebView Bridge.
   Exposes a Capacitor-compatible plugin surface backed by Kotlin @JavascriptInterface (window.TFNative).
   Enables CORS-free HTTP fetching, native Photo Picker, MediaStore file saving, native Share sheet,
   background video export foreground service, and the native In-App Newspaper Browser. */
(function () {
  'use strict';

  var N = window.TFNative;
  if (!N) return;

  var nextId = 1;
  var pending = Object.create(null);
  var listeners = Object.create(null);

  function addListener(scope, event, fn) {
    var key = scope + ':' + event;
    if (!listeners[key]) listeners[key] = [];
    listeners[key].push(fn);
    return {
      remove: function () {
        var arr = listeners[key];
        if (!arr) return;
        var idx = arr.indexOf(fn);
        if (idx >= 0) arr.splice(idx, 1);
      }
    };
  }

  window.__tfEmit = function (scope, event, payload) {
    var key = scope + ':' + event;
    var arr = listeners[key];
    if (!arr || !arr.length) return;
    var copy = arr.slice();
    for (var i = 0; i < copy.length; i++) {
      try { copy[i](payload); } catch (e) { console.error('tfEmit', key, e); }
    }
  };

  window.__tfResolve = function (id, ok, payload) {
    var entry = pending[id];
    if (!entry) return;
    delete pending[id];
    if (ok) entry.res(payload);
    else entry.rej(new Error(typeof payload === 'string' ? payload : (payload && payload.message) || 'Native error'));
  };

  function callNative(method, argsObj) {
    return new Promise(function (res, rej) {
      var id = String(nextId++);
      pending[id] = { res: res, rej: rej };
      try {
        N[method](id, JSON.stringify(argsObj || {}));
      } catch (e) {
        delete pending[id];
        rej(e);
      }
    });
  }

  var CapacitorHttp = {
    request: function (opts) {
      return callNative('httpRequest', opts || {});
    },
    get: function (opts) {
      var o = Object.assign({}, opts || {}, { method: 'GET' });
      return callNative('httpRequest', o);
    },
    post: function (opts) {
      var o = Object.assign({}, opts || {}, { method: 'POST' });
      return callNative('httpRequest', o);
    }
  };

  var TFPicker = {
    pickImage: function () {
      return callNative('pickImage', {});
    }
  };

  var TFWork = {
    start: function (opts) {
      return callNative('workStart', opts || {});
    },
    update: function (opts) {
      return callNative('workUpdate', opts || {});
    },
    stop: function (opts) {
      return callNative('workStop', opts || {});
    }
  };

  var InAppBrowser = {
    openWebView: function (opts) {
      return callNative('iabOpen', opts || {});
    },
    close: function () {
      return callNative('iabClose', {});
    },
    executeScript: function (opts) {
      return callNative('iabExecute', opts || {});
    },
    addListener: function (event, cb) {
      return Promise.resolve(addListener('InAppBrowser', event, cb));
    }
  };

  var Filesystem = {
    writeFile: function (opts) {
      return callNative('writeFile', opts || {});
    }
  };

  var Share = {
    share: function (opts) {
      return callNative('share', opts || {});
    }
  };

  var App = {
    getInfo: function () {
      return callNative('getAppInfo', {});
    },
    exitApp: function () {
      try { N.exitApp(); } catch (e) {}
      return Promise.resolve();
    },
    addListener: function (event, cb) {
      return Promise.resolve(addListener('App', event, cb));
    }
  };

  var Plugins = {
    CapacitorHttp: CapacitorHttp,
    TFPicker: TFPicker,
    TFWork: TFWork,
    InAppBrowser: InAppBrowser,
    Filesystem: Filesystem,
    Share: Share,
    App: App
  };

  window.Capacitor = {
    isNativePlatform: function () { return true; },
    getPlatform: function () { return 'android'; },
    isPluginAvailable: function (name) { return !!Plugins[name]; },
    registerPlugin: function (name) { return Plugins[name] || null; },
    Plugins: Plugins
  };

  /* ---------- Toast helper ---------- */
  function say(msg) {
    try { if (typeof window.toast === 'function') { window.toast(msg); return; } } catch (e) {}
    if (!document.body) return;
    var d = document.createElement('div');
    d.textContent = msg;
    d.style.cssText = 'position:fixed;left:50%;bottom:90px;transform:translateX(-50%);background:rgba(20,24,40,.92);color:#fff;padding:10px 16px;border-radius:12px;font:14px/1.4 sans-serif;z-index:2147483647;max-width:86vw;text-align:center';
    document.body.appendChild(d);
    setTimeout(function () { d.remove(); }, 2200);
  }

  function blobToB64(blob) {
    return new Promise(function (res, rej) {
      var r = new FileReader();
      r.onload = function () { res(String(r.result).split(',')[1] || ''); };
      r.onerror = rej;
      r.readAsDataURL(blob);
    });
  }

  function cleanName(n) {
    return String(n || 'file').replace(/[\\/:*?"<>|]+/g, '_').slice(0, 120);
  }

  function stamp(n) {
    var i = n.lastIndexOf('.');
    var t = new Date().toISOString().replace(/\D/g, '').slice(2, 14);
    return i > 0 ? n.slice(0, i) + '_' + t + n.slice(i) : n + '_' + t;
  }

  async function toCache(blob, name) {
    var r = await Filesystem.writeFile({
      path: 'share/' + cleanName(name),
      data: await blobToB64(blob),
      mime: blob.type || '',
      directory: 'CACHE',
      recursive: true
    });
    return r.uri;
  }

  /* ---------- Share (replaces navigator.share) ---------- */
  async function nativeShare(data) {
    data = data || {};
    var files = [];
    for (var i = 0; i < (data.files || []).length; i++) {
      var f = data.files[i];
      files.push(await toCache(f, f.name || ('file' + i)));
    }
    var opts = { dialogTitle: 'শেয়ার করুন' };
    if (data.title) opts.title = data.title;
    if (data.text) opts.text = data.text;
    if (data.url) opts.url = data.url;
    if (files.length) opts.files = files;
    try {
      await Share.share(opts);
    } catch (e) {
      var m = String((e && e.message) || e);
      if (/cancel/i.test(m)) throw new DOMException('Share canceled', 'AbortError');
      throw e;
    }
  }
  try {
    Object.defineProperty(Navigator.prototype, 'share', { configurable: true, writable: true, value: nativeShare });
    Object.defineProperty(Navigator.prototype, 'canShare', { configurable: true, writable: true, value: function (d) { return !!d; } });
  } catch (e) {}

  /* ---------- Save files (replaces <a download>) ---------- */
  var FOLDER = 'TF FB Reels and Post Maker';
  async function saveBlob(blob, name) {
    name = stamp(cleanName(name));
    var media = /^(image|video)\//.test(blob.type || '') || /\.(png|jpe?g|webp|gif|mp4|webm)$/i.test(name);
    var path = (media ? 'Pictures/' : 'Download/') + FOLDER + '/' + name;
    try {
      await Filesystem.writeFile({
        path: path,
        data: await blobToB64(blob),
        mime: blob.type || '',
        directory: 'EXTERNAL_STORAGE',
        recursive: true
      });
      say('সেভ হয়েছে: ' + path);
    } catch (e) {
      try {
        await Share.share({ title: name, files: [await toCache(blob, name)], dialogTitle: 'ফাইল সেভ / শেয়ার' });
      } catch (e2) {
        if (!/cancel/i.test(String(e2 && e2.message))) say('সেভ করা যায়নি');
      }
    }
  }

  async function handleDownload(a) {
    try {
      var blob = await (await fetch(a.href)).blob();
      await saveBlob(blob, a.getAttribute('download') || 'file');
    } catch (e) {
      say('সেভ করা যায়নি');
    }
  }

  function isFileLink(a) {
    return a && a.hasAttribute && a.hasAttribute('download') && /^(blob:|data:)/.test(a.href || '');
  }

  var origClick = HTMLAnchorElement.prototype.click;
  HTMLAnchorElement.prototype.click = function () {
    if (isFileLink(this)) { handleDownload(this); return; }
    return origClick.apply(this, arguments);
  };
  document.addEventListener('click', function (ev) {
    var a = ev.target && ev.target.closest && ev.target.closest('a[download]');
    if (isFileLink(a)) { ev.preventDefault(); handleDownload(a); }
  }, true);

  /* ---------- Disable service worker inside bundled WebView ---------- */
  try {
    if (navigator.serviceWorker) {
      navigator.serviceWorker.register = function () {
        return Promise.resolve({ unregister: function () { return Promise.resolve(true); } });
      };
    }
  } catch (e) {}

  /* ---------- App version helper ---------- */
  window.TFAppVersion = async function () {
    try {
      var info = await App.getInfo();
      return { build: parseInt(info.build, 10) || 1, version: info.version || '1.0.1' };
    } catch (e) {
      return { build: 1, version: '1.0.1' };
    }
  };

  /* ---------- Hardware back button handler ---------- */
  var lastBack = 0;
  App.addListener('backButton', function (e) {
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', keyCode: 27, bubbles: true }));
    if (e && e.canGoBack) {
      history.back();
      return;
    }
    var now = Date.now();
    if (now - lastBack < 2000) {
      App.exitApp();
      return;
    }
    lastBack = now;
    say('বের হতে আবার Back চাপুন');
  });

  document.documentElement.classList.add('tf-android-app');
})();
