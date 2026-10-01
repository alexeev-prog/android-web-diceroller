package com.alexeevprog.diceroller;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_CHOOSER_REQUEST = 1001;
    private ValueCallback<Uri[]> filePathCallback;

    private static final String HTML_CONTENT = """
<!DOCTYPE html>
<html lang="ru" data-theme="dark">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#0e1016">
<title>Кубики — бросок дайсов</title>

<script>
(function () {
  try {
    var t = localStorage.getItem('dice-theme');
    if (t !== 'light' && t !== 'dark') {
      t = (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) ? 'light' : 'dark';
    }
    document.documentElement.setAttribute('data-theme', t);
  } catch (e) {
    document.documentElement.setAttribute('data-theme', 'dark');
  }
})();
</script>

<style>
  /* ---------- ТЕМЫ ---------- */
  html[data-theme="light"] {
    color-scheme: light;
    --bg: #eef1f7;
    --bg-grad: radial-gradient(1100px 520px at 50% -12%, #ffffff 0%, #eef1f7 62%);
    --surface: #ffffff;
    --surface-2: #f4f6fb;
    --border: #e3e8f1;
    --border-strong: #ccd5e4;
    --text: #141924;
    --muted: #6b7688;
    --accent: #5b5bd6;
    --accent-soft: rgba(91, 91, 214, .10);
    --die-bg: #ffffff;
    --die-border: #d6dEEC;
    --crit: #c2740a;
    --fail: #d33a3a;
    --shadow: 0 1px 2px rgba(20, 25, 40, .05), 0 10px 28px -10px rgba(20, 25, 40, .16);
    --shadow-sm: 0 1px 2px rgba(20, 25, 40, .06);
  }

  html[data-theme="dark"] {
    color-scheme: dark;
    --bg: #0e1016;
    --bg-grad: radial-gradient(1100px 520px at 50% -12%, #1a2030 0%, #0e1016 62%);
    --surface: #161a23;
    --surface-2: #1c212c;
    --border: #262c3a;
    --border-strong: #364056;
    --text: #e9edf5;
    --muted: #8b95a9;
    --accent: #7c7cf0;
    --accent-soft: rgba(124, 124, 240, .14);
    --die-bg: #1e2430;
    --die-border: #333c4e;
    --crit: #f5b942;
    --fail: #f87171;
    --shadow: 0 1px 2px rgba(0, 0, 0, .35), 0 14px 34px -14px rgba(0, 0, 0, .7);
    --shadow-sm: 0 1px 2px rgba(0, 0, 0, .4);
  }

  /* ---------- БАЗА ---------- */
  * { box-sizing: border-box; }

  html, body { height: 100%; }

  body {
    margin: 0;
    font-family: ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto,
                 "Helvetica Neue", Arial, "Noto Sans", sans-serif;
    background-color: var(--bg);
    background-image: var(--bg-grad);
    background-attachment: fixed;
    color: var(--text);
    -webkit-font-smoothing: antialiased;
    -webkit-tap-highlight-color: transparent;
    transition: background-color .3s ease, color .3s ease;
    overflow-x: hidden;
  }

  button, input { font: inherit; color: inherit; }

  button { -webkit-tap-highlight-color: transparent; }

  h1, h2 { letter-spacing: -.01em; }

  /* ---------- КОНТЕЙНЕР ---------- */
  .app {
    max-width: 880px;
    margin: 0 auto;
    padding: 14px 14px calc(40px + env(safe-area-inset-bottom, 0px));
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  /* ---------- ШАПКА ---------- */
  .topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 6px 4px 2px;
  }

  .brand { display: flex; align-items: center; gap: 12px; min-width: 0; }

  .brand__icon {
    width: 44px; height: 44px; flex: 0 0 44px;
    border-radius: 14px;
    display: grid; place-items: center;
    font-size: 22px;
    background: var(--accent-soft);
    border: 1px solid var(--border);
  }

  .brand h1 { margin: 0; font-size: 19px; font-weight: 750; }

  .brand__sub {
    margin: 2px 0 0;
    font-size: 12.5px;
    color: var(--muted);
    white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  }

  .theme-btn {
    width: 44px; height: 44px; flex: 0 0 44px;
    border-radius: 14px;
    border: 1px solid var(--border);
    background: var(--surface);
    color: var(--text);
    display: grid; place-items: center;
    cursor: pointer;
    box-shadow: var(--shadow-sm);
    transition: background .18s, border-color .18s, transform .1s;
  }
  .theme-btn:hover { border-color: var(--border-strong); }
  .theme-btn:active { transform: scale(.94); }
  .theme-btn svg { width: 20px; height: 20px; }

  .icon-sun { display: none; }
  .icon-moon { display: block; }
  html[data-theme="dark"] .icon-sun { display: block; }
  html[data-theme="dark"] .icon-moon { display: none; }

  /* ---------- КАРТОЧКИ ---------- */
  .card {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 18px;
    padding: 15px;
    box-shadow: var(--shadow);
    transition: background-color .3s, border-color .3s;
  }

  .card__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    flex-wrap: wrap;
    margin-bottom: 12px;
  }

  .card__head h2 {
    margin: 0;
    font-size: 12px;
    font-weight: 750;
    letter-spacing: .09em;
    text-transform: uppercase;
    color: var(--muted);
  }

  .ghost-btn {
    border: 1px solid var(--border);
    background: transparent;
    color: var(--muted);
    border-radius: 9px;
    padding: 6px 11px;
    font-size: 12.5px;
    font-weight: 650;
    cursor: pointer;
    transition: color .16s, border-color .16s, background .16s;
  }
  .ghost-btn:hover { color: var(--text); border-color: var(--border-strong); background: var(--surface-2); }
  .ghost-btn:active { transform: scale(.96); }

  /* ---------- СЕТКА КУБИКОВ ---------- */
  .dice-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
    gap: 9px;
  }

  .die-card {
    border: 1px solid var(--border);
    background: var(--surface-2);
    border-radius: 14px;
    padding: 9px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    transition: border-color .18s, background .18s;
  }

  .die-card.is-active {
    border-color: var(--accent);
    background: var(--accent-soft);
  }

  .die-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 6px;
  }

  .die-chip {
    font-size: 12.5px;
    font-weight: 800;
    letter-spacing: .01em;
    padding: 3px 8px;
    border-radius: 8px;
    background: var(--surface);
    border: 1px solid var(--border);
    color: var(--muted);
    transition: background .18s, color .18s, border-color .18s;
  }

  .die-card.is-active .die-chip {
    background: var(--accent);
    border-color: transparent;
    color: #fff;
  }

  .die-count {
    font-size: 17px;
    font-weight: 800;
    font-variant-numeric: tabular-nums;
    color: var(--muted);
    transition: color .18s;
  }
  .die-card.is-active .die-count { color: var(--text); }

  .die-card__actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 5px;
  }

  .step {
    height: 34px;
    border-radius: 9px;
    border: 1px solid var(--border);
    background: var(--surface);
    color: var(--text);
    font-size: 17px;
    font-weight: 700;
    line-height: 1;
    display: grid;
    place-items: center;
    cursor: pointer;
    user-select: none;
    transition: background .15s, border-color .15s, transform .08s, opacity .15s;
  }
  .step:hover:not(:disabled) { border-color: var(--border-strong); background: var(--surface-2); }
  .step:active:not(:disabled) { transform: scale(.93); }
  .step:disabled { opacity: .32; cursor: default; }

  .grid-footer {
    margin-top: 11px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    font-size: 12.5px;
    color: var(--muted);
    font-weight: 600;
  }
  .grid-footer b { color: var(--text); font-variant-numeric: tabular-nums; }

  /* ---------- МОДИФИКАТОР + БРОСОК ---------- */
  .roll-card {
    display: flex;
    align-items: flex-end;
    gap: 12px;
    flex-wrap: wrap;
  }

  .mod-wrap { display: flex; flex-direction: column; gap: 6px; flex: 0 0 auto; }

  .mod-wrap label {
    font-size: 11px;
    font-weight: 750;
    letter-spacing: .08em;
    text-transform: uppercase;
    color: var(--muted);
    padding-left: 2px;
  }

  .stepper {
    display: flex;
    align-items: center;
    gap: 3px;
    padding: 3px;
    background: var(--surface-2);
    border: 1px solid var(--border);
    border-radius: 13px;
  }

  .stepper input {
    width: 58px;
    border: 0;
    background: transparent;
    text-align: center;
    font-size: 16px;
    font-weight: 800;
    font-variant-numeric: tabular-nums;
    padding: 6px 0;
    outline: none;
    -moz-appearance: textfield;
    appearance: textfield;
    border-radius: 8px;
  }
  .stepper input::-webkit-outer-spin-button,
  .stepper input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
  .stepper input:focus-visible { background: var(--surface); box-shadow: 0 0 0 2px var(--accent-soft); }

  .step--sm { width: 36px; height: 36px; border-radius: 10px; }

  .roll-btn {
    flex: 1 1 210px;
    min-height: 54px;
    border: 0;
    border-radius: 15px;
    background-color: var(--accent);
    background-image: linear-gradient(180deg, rgba(255,255,255,.16), rgba(0,0,0,.14));
    color: #fff;
    font-size: 16px;
    font-weight: 800;
    letter-spacing: .01em;
    cursor: pointer;
    box-shadow: 0 10px 24px -12px var(--accent);
    transition: transform .1s, opacity .2s, filter .2s;
  }
  .roll-btn:hover:not(:disabled) { filter: brightness(1.06); }
  .roll-btn:active:not(:disabled) { transform: scale(.985); }
  .roll-btn:disabled { opacity: .62; cursor: default; }

  /* ---------- РЕЗУЛЬТАТ ---------- */
  .formula {
    font-size: 12px;
    font-weight: 700;
    color: var(--muted);
    font-variant-numeric: tabular-nums;
    text-align: right;
    word-break: break-word;
    max-width: 100%;
  }

  .empty-state {
    margin: 0;
    padding: 18px 8px;
    text-align: center;
    color: var(--muted);
    font-size: 13.5px;
  }

  .groups { display: flex; flex-direction: column; gap: 14px; }

  .group__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    font-size: 12px;
    font-weight: 750;
    letter-spacing: .04em;
    color: var(--muted);
    margin-bottom: 7px;
    font-variant-numeric: tabular-nums;
  }

  .group__sum { color: var(--text); }

  .dice-row {
    display: flex;
    flex-wrap: wrap;
    gap: 7px;
  }

  .die {
    width: 46px;
    height: 46px;
    flex: 0 0 46px;
    border-radius: 12px;
    display: grid;
    place-items: center;
    font-size: 17px;
    font-weight: 800;
    font-variant-numeric: tabular-nums;
    background: var(--die-bg);
    border: 1.5px solid var(--die-border);
    color: var(--text);
    box-shadow: var(--shadow-sm);
    transition: border-color .2s, color .2s, background .3s;
  }

  @keyframes diePop {
    0%   { transform: scale(1.24); }
    55%  { transform: scale(.94); }
    100% { transform: scale(1); }
  }
  .die.settled { animation: diePop .26s ease-out; }

  .die--crit { border-color: var(--crit); color: var(--crit); box-shadow: 0 0 0 3px rgba(245,185,66,.18); }
  .die--fail { border-color: var(--fail); color: var(--fail); box-shadow: 0 0 0 3px rgba(248,113,113,.16); }

  .totals {
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px dashed var(--border);
    display: flex;
    flex-direction: column;
    gap: 7px;
  }

  .total-row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 12px;
    font-size: 13.5px;
    color: var(--muted);
  }
  .total-row b { color: var(--text); font-weight: 750; font-variant-numeric: tabular-nums; }

  .total-main {
    margin-top: 5px;
    padding: 11px 15px;
    border-radius: 14px;
    background: var(--accent-soft);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .total-main span {
    font-size: 11.5px;
    font-weight: 800;
    letter-spacing: .1em;
    text-transform: uppercase;
    color: var(--muted);
  }

  .total-main b {
    font-size: 30px;
    font-weight: 850;
    letter-spacing: -.02em;
    font-variant-numeric: tabular-nums;
    transition: opacity .25s, filter .25s;
  }

  .total-main.is-pending b { opacity: .18; filter: blur(5px); }

  /* ---------- ИСТОРИЯ ---------- */
  .history {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 7px;
    max-height: 300px;
    overflow-y: auto;
    overscroll-behavior: contain;
  }

  .history li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 9px 12px;
    border-radius: 11px;
    background: var(--surface-2);
    border: 1px solid transparent;
    font-size: 13px;
    transition: border-color .16s;
  }
  .history li:hover { border-color: var(--border); }

  .history .h-formula {
    color: var(--muted);
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }

  .history .h-total {
    font-weight: 850;
    font-variant-numeric: tabular-nums;
    flex: 0 0 auto;
  }

  .history .empty-item {
    justify-content: center;
    color: var(--muted);
    font-size: 13px;
    background: transparent;
    padding: 14px 0;
  }

  /* ---------- ТОСТ ---------- */
  .toast {
    position: fixed;
    left: 50%;
    bottom: calc(22px + env(safe-area-inset-bottom, 0px));
    transform: translate(-50%, 18px);
    background: var(--text);
    color: var(--bg);
    padding: 10px 18px;
    border-radius: 999px;
    font-size: 13px;
    font-weight: 700;
    opacity: 0;
    pointer-events: none;
    transition: opacity .22s ease, transform .22s ease;
    z-index: 99;
    max-width: calc(100vw - 32px);
    text-align: center;
    box-shadow: 0 12px 30px -12px rgba(0,0,0,.6);
  }
  .toast.show { opacity: 1; transform: translate(-50%, 0); }

  /* ---------- АНИМАЦИИ ---------- */
  @keyframes shakeX {
    10%, 90% { transform: translateX(-2px); }
    20%, 80% { transform: translateX(4px); }
    30%, 50%, 70% { transform: translateX(-6px); }
    40%, 60% { transform: translateX(6px); }
  }
  .shake { animation: shakeX .5s cubic-bezier(.36,.07,.19,.97); }

  /* ---------- АДАПТИВ ---------- */
  @media (max-width: 480px) {
    .app { padding-left: 11px; padding-right: 11px; gap: 12px; }
    .card { padding: 13px; border-radius: 16px; }
    .dice-grid { grid-template-columns: repeat(auto-fill, minmax(84px, 1fr)); gap: 8px; }
    .die { width: 42px; height: 42px; flex-basis: 42px; font-size: 16px; border-radius: 11px; }
    .total-main b { font-size: 26px; }
    .brand__sub { display: none; }
  }

  @media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
      animation-duration: .001ms !important;
      animation-iteration-count: 1 !important;
      transition-duration: .001ms !important;
    }
  }
</style>
</head>

<body>
  <div class="app">

    <!-- ШАПКА -->
    <header class="topbar">
      <div class="brand">
        <div class="brand__icon" aria-hidden="true">🎲</div>
        <div>
          <h1>Кубики</h1>
          <p class="brand__sub">Бросок дайсов и подсчёт результата</p>
        </div>
      </div>

      <button class="theme-btn" id="themeBtn" type="button" aria-label="Переключить тему">
        <svg class="icon-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
        <svg class="icon-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="4.2"/>
          <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>
        </svg>
      </button>
    </header>

    <!-- НАБОР КУБИКОВ -->
    <section class="card">
      <div class="card__head">
        <h2>Набор кубиков</h2>
        <button class="ghost-btn" id="clearBtn" type="button">Сбросить</button>
      </div>
      <div class="dice-grid" id="diceGrid"></div>
      <div class="grid-footer">
        <span>Всего кубиков</span>
        <span><b id="totalDice">0 / 60</b></span>
      </div>
    </section>

    <!-- МОДИФИКАТОР И БРОСОК -->
    <section class="card roll-card">
      <div class="mod-wrap">
        <label for="modInput">Модификатор</label>
        <div class="stepper">
          <button type="button" class="step step--sm" data-mod="-1" aria-label="Уменьшить модификатор">−</button>
          <input id="modInput" type="number" value="0" step="1" inputmode="numeric" aria-label="Модификатор">
          <button type="button" class="step step--sm" data-mod="1" aria-label="Увеличить модификатор">+</button>
        </div>
      </div>

      <button class="roll-btn" id="rollBtn" type="button">Бросить кубики</button>
    </section>

    <!-- РЕЗУЛЬТАТ -->
    <section class="card" id="resultCard">
      <div class="card__head">
        <h2>Результат</h2>
        <span class="formula" id="formula"></span>
      </div>
      <div id="resultBody">
        <p class="empty-state">Выберите кубики и нажмите «Бросить кубики»</p>
      </div>
    </section>

    <!-- ИСТОРИЯ -->
    <section class="card">
      <div class="card__head">
        <h2>История бросков</h2>
        <button class="ghost-btn" id="clearHistory" type="button">Очистить</button>
      </div>
      <ul class="history" id="historyList"></ul>
    </section>

  </div>

  <div class="toast" id="toast" role="status" aria-live="polite"></div>

<script>
(function () {
  'use strict';

  /* ======================== КОНСТАНТЫ ======================== */
  var DICE = [4, 6, 8, 10, 12, 20, 100];
  var MAX_PER_TYPE = 20;
  var MAX_TOTAL = 60;
  var MAX_HISTORY = 30;
  var STORAGE_KEY = 'dice-app-state-v1';
  var THEME_KEY = 'dice-theme';

  /* ======================== СОСТОЯНИЕ ======================== */
  var state = {
    counts: {},
    modifier: 0,
    history: [],
    rolling: false,
    rollToken: 0,
    pendingRoll: null
  };
  DICE.forEach(function (s) { state.counts[s] = 0; });
  state.counts[6] = 1; // по умолчанию 1d6

  /* ======================== DOM ======================== */
  var els = {
    grid: document.getElementById('diceGrid'),
    totalDice: document.getElementById('totalDice'),
    modInput: document.getElementById('modInput'),
    rollBtn: document.getElementById('rollBtn'),
    clearBtn: document.getElementById('clearBtn'),
    resultBody: document.getElementById('resultBody'),
    formula: document.getElementById('formula'),
    historyList: document.getElementById('historyList'),
    clearHistory: document.getElementById('clearHistory'),
    themeBtn: document.getElementById('themeBtn'),
    toast: document.getElementById('toast')
  };

  /* ======================== УТИЛИТЫ ======================== */
  var cryptoArr = (window.crypto && typeof window.crypto.getRandomValues === 'function')
    ? new Uint32Array(1) : null;

  function randInt(min, max) {
    var range = max - min + 1;
    if (range <= 1) return min;

    if (cryptoArr) {
      var limit = Math.floor(4294967296 / range) * range;
      var x, guard = 0;
      do {
        window.crypto.getRandomValues(cryptoArr);
        x = cryptoArr[0];
        guard++;
      } while (x >= limit && guard < 100);
      return min + (x % range);
    }
    return min + Math.floor(Math.random() * range);
  }

  function clamp(v, min, max) {
    return v < min ? min : (v > max ? max : v);
  }

  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  var toastTimer = null;
  function toast(msg) {
    els.toast.textContent = msg;
    els.toast.classList.add('show');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      els.toast.classList.remove('show');
      toastTimer = null;
    }, 2200);
  }

  function shake(el) {
    el.classList.remove('shake');
    void el.offsetWidth; // reflow, чтобы анимация перезапустилась
    el.classList.add('shake');
    var onEnd = function () {
      el.classList.remove('shake');
      el.removeEventListener('animationend', onEnd);
    };
    el.addEventListener('animationend', onEnd);
  }

  function totalSelected() {
    var t = 0;
    for (var i = 0; i < DICE.length; i++) t += state.counts[DICE[i]];
    return t;
  }

  /* ======================== ХРАНИЛИЩЕ ======================== */
  function load() {
    var raw;
    try { raw = localStorage.getItem(STORAGE_KEY); } catch (e) { return; }
    if (!raw) return;

    var d;
    try { d = JSON.parse(raw); } catch (e) { return; }
    if (!d || typeof d !== 'object') return;

    if (d.counts && typeof d.counts === 'object') {
      DICE.forEach(function (s) {
        var v = Number(d.counts[s]);
        state.counts[s] = Number.isFinite(v) ? clamp(Math.round(v), 0, MAX_PER_TYPE) : 0;
      });
    }

    var m = Number(d.modifier);
    if (Number.isFinite(m)) state.modifier = clamp(Math.round(m), -99, 99);

    if (Array.isArray(d.history)) {
      state.history = d.history.slice(0, MAX_HISTORY).filter(function (h) {
        return h && typeof h.formula === 'string' && Number.isFinite(Number(h.total));
      }).map(function (h) {
        return {
          formula: h.formula.slice(0, 200),
          total: Number(h.total),
          time: Number(h.time) || Date.now()
        };
      });
    }
  }

  function save() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        counts: state.counts,
        modifier: state.modifier,
        history: state.history
      }));
    } catch (e) { /* приватный режим / переполнение — игнорируем */ }
  }

  /* ======================== ТЕМА ======================== */
  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', theme === 'dark' ? '#0e1016' : '#eef1f7');
    try { localStorage.setItem(THEME_KEY, theme); } catch (e) { /* ignore */ }
    els.themeBtn.setAttribute('aria-label',
      theme === 'dark' ? 'Включить светлую тему' : 'Включить тёмную тему');
  }

  els.themeBtn.addEventListener('click', function () {
    var cur = document.documentElement.getAttribute('data-theme');
    applyTheme(cur === 'dark' ? 'light' : 'dark');
  });

  /* ======================== СЕТКА КУБИКОВ ======================== */
  function buildGrid() {
    var html = '';
    DICE.forEach(function (sides) {
      html +=
        '<div class="die-card" data-sides="' + sides + '">' +
          '<div class="die-card__top">' +
            '<span class="die-chip">d' + sides + '</span>' +
            '<span class="die-count" data-count>0</span>' +
          '</div>' +
          '<div class="die-card__actions">' +
            '<button type="button" class="step" data-act="dec" aria-label="Убрать кубик d' + sides + '">−</button>' +
            '<button type="button" class="step" data-act="inc" aria-label="Добавить кубик d' + sides + '">+</button>' +
          '</div>' +
        '</div>';
    });
    els.grid.innerHTML = html;
  }

  function syncGrid() {
    var total = totalSelected();

    DICE.forEach(function (sides) {
      var card = els.grid.querySelector('.die-card[data-sides="' + sides + '"]');
      if (!card) return;

      var c = state.counts[sides];
      var countEl = card.querySelector('[data-count]');
      var decBtn = card.querySelector('[data-act="dec"]');
      var incBtn = card.querySelector('[data-act="inc"]');

      if (countEl) countEl.textContent = c;
      card.classList.toggle('is-active', c > 0);
      if (decBtn) decBtn.disabled = c <= 0;
      if (incBtn) incBtn.disabled = (c >= MAX_PER_TYPE) || (total >= MAX_TOTAL);
    });

    els.totalDice.textContent = total + ' / ' + MAX_TOTAL;
  }

  els.grid.addEventListener('click', function (e) {
    var target = e.target;
    if (!(target instanceof Element)) return;

    var btn = target.closest('button[data-act]');
    if (!btn) return;

    var card = btn.closest('.die-card');
    if (!card) return;

    var sides = Number(card.getAttribute('data-sides'));
    var act = btn.getAttribute('data-act');
    if (DICE.indexOf(sides) === -1) return;

    if (act === 'inc') {
      if (totalSelected() >= MAX_TOTAL) { toast('Максимум ' + MAX_TOTAL + ' кубиков'); return; }
      if (state.counts[sides] >= MAX_PER_TYPE) { toast('Максимум ' + MAX_PER_TYPE + ' кубиков d' + sides); return; }
      state.counts[sides]++;
    } else {
      if (state.counts[sides] <= 0) return;
      state.counts[sides]--;
    }

    syncGrid();
    save();
  });

  els.clearBtn.addEventListener('click', function () {
    DICE.forEach(function (s) { state.counts[s] = 0; });
    setModifier(0);
    syncGrid();
    save();
    toast('Набор сброшен');
  });

  /* ======================== МОДИФИКАТОР ======================== */
  function setModifier(v) {
    var n = Number(v);
    if (!Number.isFinite(n)) n = 0;
    n = clamp(Math.round(n), -99, 99);
    state.modifier = n;
    els.modInput.value = String(n);
    save();
  }

  Array.prototype.forEach.call(document.querySelectorAll('[data-mod]'), function (btn) {
    btn.addEventListener('click', function () {
      var delta = Number(btn.getAttribute('data-mod'));
      var current = parseInt(els.modInput.value, 10);
      if (!Number.isFinite(current)) current = state.modifier;
      setModifier(current + delta);
    });
  });

  els.modInput.addEventListener('input', function () {
    var v = parseInt(els.modInput.value, 10);
    if (Number.isFinite(v)) {
      state.modifier = clamp(v, -99, 99);
      save();
    }
  });

  els.modInput.addEventListener('blur', function () {
    var v = parseInt(els.modInput.value, 10);
    setModifier(Number.isFinite(v) ? v : 0);
  });

  els.modInput.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') {
      e.preventDefault();
      els.modInput.blur();
      roll();
    }
  });

  /* ======================== ФОРМУЛА ======================== */
  function buildFormula() {
    var parts = [];
    DICE.forEach(function (s) {
      if (state.counts[s] > 0) parts.push(state.counts[s] + 'd' + s);
    });
    var base = parts.join(' + ');
    if (state.modifier > 0) base += ' + ' + state.modifier;
    else if (state.modifier < 0) base += ' − ' + Math.abs(state.modifier);
    return base;
  }

  /* ======================== БРОСОК ======================== */
  function roll() {
    if (state.rolling) return;

    // Синхронизируем модификатор из поля ввода
    var mv = parseInt(els.modInput.value, 10);
    state.modifier = clamp(Number.isFinite(mv) ? mv : 0, -99, 99);
    els.modInput.value = String(state.modifier);

    if (totalSelected() <= 0) {
      toast('Добавьте хотя бы один кубик');
      shake(els.rollBtn);
      return;
    }

    // Генерируем результат
    var groups = [];
    var diceCount = 0;
    var diceSum = 0;

    DICE.forEach(function (sides) {
      var count = state.counts[sides];
      if (count <= 0) return;

      var values = [];
      var sum = 0;
      for (var i = 0; i < count; i++) {
        var v = randInt(1, sides);
        values.push(v);
        sum += v;
      }
      groups.push({ sides: sides, count: count, values: values, sum: sum });
      diceCount += count;
      diceSum += sum;
    });

    var rollData = {
      groups: groups,
      diceCount: diceCount,
      diceSum: diceSum,
      modifier: state.modifier,
      total: diceSum + state.modifier,
      formula: buildFormula(),
      time: Date.now()
    };

    var token = ++state.rollToken;
    state.rolling = true;
    state.pendingRoll = rollData;

    els.rollBtn.disabled = true;
    els.rollBtn.textContent = 'Бросаем…';

    renderRoll(rollData, true);
    startAnimation(rollData, token);
  }

  /* ======================== ОТРИСОВКА РЕЗУЛЬТАТА ======================== */
  function renderRoll(rollData, animate) {
    var html = '<div class="groups">';

    rollData.groups.forEach(function (g) {
      html +=
        '<div class="group">' +
          '<div class="group__head">' +
            '<span class="group__label">' + g.count + 'd' + g.sides + '</span>' +
            '<span class="group__sum">= ' + g.sum + '</span>' +
          '</div>' +
          '<div class="dice-row">';

      g.values.forEach(function (v) {
        html += '<div class="die" data-final="' + v + '">' + v + '</div>';
      });

      html += '</div></div>';
    });

    html += '</div>';

    var modStr = rollData.modifier > 0
      ? '+' + rollData.modifier
      : (rollData.modifier < 0 ? '−' + Math.abs(rollData.modifier) : '0');

    html +=
      '<div class="totals">' +
        '<div class="total-row">' +
          '<span>Сумма кубиков (' + rollData.diceCount + ' шт.)</span>' +
          '<b>' + rollData.diceSum + '</b>' +
        '</div>' +
        '<div class="total-row">' +
          '<span>Модификатор</span>' +
          '<b>' + modStr + '</b>' +
        '</div>' +
        '<div class="total-main' + (animate ? ' is-pending' : '') + '">' +
          '<span>Итог</span>' +
          '<b>' + rollData.total + '</b>' +
        '</div>' +
      '</div>';

    els.resultBody.innerHTML = html;
    els.formula.textContent = rollData.formula;

    if (!animate) {
      markCrits(rollData);
      return;
    }
  }

  function markCrits(rollData) {
    var flat = [];
    rollData.groups.forEach(function (g) {
      g.values.forEach(function (v) { flat.push({ sides: g.sides, value: v }); });
    });

    var nodes = els.resultBody.querySelectorAll('.die');
    for (var i = 0; i < nodes.length && i < flat.length; i++) {
      var info = flat[i];
      if (info.sides === 20 && info.value === 20) nodes[i].classList.add('die--crit');
      if (info.sides === 20 && info.value === 1) nodes[i].classList.add('die--fail');
    }
  }

  /* ======================== АНИМАЦИЯ ======================== */
  function startAnimation(rollData, token) {
    var flat = [];
    rollData.groups.forEach(function (g) {
      g.values.forEach(function (v) { flat.push({ sides: g.sides, value: v }); });
    });

    var nodes = els.resultBody.querySelectorAll('.die');
    var items = [];
    for (var i = 0; i < nodes.length; i++) {
      var info = flat[i] || { sides: 6, value: 1 };
      items.push({
        el: nodes[i],
        sides: info.sides,
        final: info.value,
        settled: false,
        crit: info.sides === 20 && info.value === 20,
        fail: info.sides === 20 && info.value === 1
      });
    }

    if (!items.length) { finishRoll(token); return; }

    var settleTimes = items.map(function (_, idx) {
      return Math.min(420 + idx * 45, 1200);
    });

    var t0 = performance.now();
    var rafId = 0;
    var stopped = false;

    function settleItem(it) {
      if (it.settled) return;
      it.settled = true;
      it.el.textContent = String(it.final);
      it.el.classList.add('settled');
      if (it.crit) it.el.classList.add('die--crit');
      if (it.fail) it.el.classList.add('die--fail');
    }

    function frame(now) {
      if (stopped || token !== state.rollToken) return;

      var elapsed = now - t0;
      var allDone = true;

      for (var i = 0; i < items.length; i++) {
        var it = items[i];
        if (elapsed >= settleTimes[i]) {
          settleItem(it);
        } else {
          allDone = false;
          it.el.textContent = String(randInt(1, it.sides));
        }
      }

      if (allDone) {
        rafId = 0;
        finishRoll(token);
      } else {
        rafId = requestAnimationFrame(frame);
      }
    }

    rafId = requestAnimationFrame(frame);

    // Страховка: если rAF приостановлен (скрытая вкладка и т.п.)
    setTimeout(function () {
      if (stopped || token !== state.rollToken || !state.rolling) return;
      stopped = true;
      if (rafId) cancelAnimationFrame(rafId);
      rafId = 0;
      items.forEach(settleItem);
      finishRoll(token);
    }, 2500);
  }

  function finishRoll(token) {
    if (token !== state.rollToken) return;

    state.rolling = false;
    els.rollBtn.disabled = false;
    els.rollBtn.textContent = 'Бросить кубики';

    var pending = els.resultBody.querySelector('.total-main.is-pending');
    if (pending) pending.classList.remove('is-pending');

    if (state.pendingRoll) {
      addHistory(state.pendingRoll);
      state.pendingRoll = null;
    }
  }

  /* ======================== ИСТОРИЯ ======================== */
  function addHistory(rollData) {
    state.history.unshift({
      formula: rollData.formula,
      total: rollData.total,
      time: rollData.time
    });
    if (state.history.length > MAX_HISTORY) {
      state.history.length = MAX_HISTORY;
    }
    renderHistory();
    save();
  }

  function renderHistory() {
    if (!state.history.length) {
      els.historyList.innerHTML = '<li class="empty-item">Пока пусто — бросьте кубики</li>';
      return;
    }

    var html = '';
    state.history.forEach(function (h) {
      html +=
        '<li>' +
          '<span class="h-formula">' + escapeHtml(h.formula) + '</span>' +
          '<span class="h-total">' + escapeHtml(h.total) + '</span>' +
        '</li>';
    });
    els.historyList.innerHTML = html;
  }

  els.clearHistory.addEventListener('click', function () {
    if (!state.history.length) { toast('История уже пуста'); return; }
    state.history = [];
    renderHistory();
    save();
    toast('История очищена');
  });

  /* ======================== КНОПКА БРОСКА ======================== */
  els.rollBtn.addEventListener('click', roll);

  /* ======================== ИНИЦИАЛИЗАЦИЯ ======================== */
  function init() {
    load();

    var theme = document.documentElement.getAttribute('data-theme');
    applyTheme(theme === 'light' ? 'light' : 'dark');

    buildGrid();
    syncGrid();
    els.modInput.value = String(state.modifier);
    renderHistory();

    els.rollBtn.textContent = 'Бросить кубики';
  }

  init();

})();
</script>
</body>
</html>
""";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createWebView();
    }

    private void createWebView() {
        WebView wv = new WebView(this);

        android.webkit.WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);

        try {
            java.io.File dir = getDir("webview", android.content.Context.MODE_PRIVATE);
            if (!dir.exists()) dir.mkdirs();
            s.setDatabasePath(dir.getAbsolutePath());
            android.webkit.WebStorage.getInstance().setQuotaForOrigin("file:///", 200L * 1024L * 1024L);
        } catch (Exception ignored) {}

        wv.addJavascriptInterface(new AndroidFileSaver(), "AndroidFileSaver");
        wv.addJavascriptInterface(new AndroidStorage(), "AndroidStorage");

        wv.setWebViewClient(new WebViewClient());

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                MainActivity.this.filePathCallback = callback;
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        wv.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } catch (Exception ignored) {}
        });

        wv.loadDataWithBaseURL(null, HTML_CONTENT, "text/html", "UTF-8", null);
        setContentView(wv);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{ data.getData() };
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    /**
     * Мост для сохранения файлов из JavaScript.
     * Принимает dataURL "data:mime;base64,..." и сохраняет в Downloads.
     */
    public class AndroidFileSaver {
        @JavascriptInterface
        public void saveBase64(final String dataUrl, final String suggestedName, final String mimetype) {
            new Thread(() -> {
                try {
                    int comma = dataUrl.indexOf(',');
                    if (comma < 0) {
                        showToast("Неверный формат данных");
                        return;
                    }
                    String b64 = dataUrl.substring(comma + 1);
                    byte[] bytes = Base64.decode(b64, Base64.DEFAULT);

                    String fileName = (suggestedName == null || suggestedName.isEmpty())
                        ? "download_" + System.currentTimeMillis()
                        : suggestedName;
                    String mime = (mimetype == null || mimetype.isEmpty())
                        ? "application/octet-stream"
                        : mimetype;

                    if (Build.VERSION.SDK_INT >= 29) {
                        // Android 10+ через MediaStore (Downloads)
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                        Uri uri = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri != null) {
                            OutputStream os = getContentResolver().openOutputStream(uri);
                            if (os != null) {
                                os.write(bytes);
                                os.close();
                                showToast("Сохранено в Downloads: " + fileName);
                                return;
                            }
                        }
                        showToast("Не удалось сохранить");
                    } else {
                        // Android 9 и ниже — напрямую в папку
                        File downloads = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                        if (!downloads.exists()) downloads.mkdirs();
                        File out = new File(downloads, fileName);
                        FileOutputStream fos = new FileOutputStream(out);
                        fos.write(bytes);
                        fos.close();
                        showToast("Сохранено: " + out.getAbsolutePath());
                    }
                } catch (Exception e) {
                    showToast("Ошибка: " + e.getMessage());
                }
            }).start();
        }
    }

    /**
     * Мост для постоянного хранения данных из JavaScript.
     * SharedPreferences — данные не стираются при закрытии приложения.
     */
    public class AndroidStorage {
        private SharedPreferences prefs;

        AndroidStorage() {
            prefs = getSharedPreferences("apkb_storage", Context.MODE_PRIVATE);
        }

        @JavascriptInterface
        public String get(String key) {
            try { return prefs.getString(key, null); } catch (Exception e) { return null; }
        }

        @JavascriptInterface
        public void set(String key, String value) {
            try { prefs.edit().putString(key, value).apply(); } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void remove(String key) {
            try { prefs.edit().remove(key).apply(); } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public String keys() {
            try {
                JSONArray arr = new JSONArray();
                for (String k : prefs.getAll().keySet()) arr.put(k);
                return arr.toString();
            } catch (Exception e) {
                return "[]";
            }
        }
    }

    private void showToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show());
    }
}
