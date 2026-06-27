/* ============ TechPulseSoft — interactions ============ */
(function () {
  "use strict";

  /* Loader */
  window.addEventListener("load", function () {
    var loader = document.getElementById("loader");
    if (loader) setTimeout(function () { loader.classList.add("hide"); }, 350);
  });

  /* Theme: respect saved choice, then system preference */
  var root = document.documentElement;
  var saved = localStorage.getItem("tps-theme");
  if (saved) {
    root.setAttribute("data-theme", saved);
  } else if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
    root.setAttribute("data-theme", "dark");
  }
  var themeToggle = document.getElementById("themeToggle");
  if (themeToggle) {
    themeToggle.addEventListener("click", function () {
      var next = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
      root.setAttribute("data-theme", next);
      localStorage.setItem("tps-theme", next);
    });
  }

  /* Mobile menu */
  var menuToggle = document.getElementById("menuToggle");
  var navLinks = document.getElementById("navLinks");
  if (menuToggle && navLinks) {
    menuToggle.addEventListener("click", function () {
      var open = navLinks.classList.toggle("open");
      menuToggle.setAttribute("aria-expanded", String(open));
    });
    navLinks.addEventListener("click", function (e) {
      if (e.target.tagName === "A") {
        navLinks.classList.remove("open");
        menuToggle.setAttribute("aria-expanded", "false");
      }
    });
  }

  /* Navbar shadow + back-to-top on scroll */
  var navbar = document.getElementById("navbar");
  var toTop = document.getElementById("toTop");
  function onScroll() {
    var y = window.scrollY;
    if (navbar) navbar.classList.toggle("scrolled", y > 8);
    if (toTop) toTop.classList.toggle("show", y > 600);
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();
  if (toTop) toTop.addEventListener("click", function () { window.scrollTo({ top: 0, behavior: "smooth" }); });

  /* Scroll reveal */
  var reveals = document.querySelectorAll(".reveal");
  if ("IntersectionObserver" in window) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add("in");
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12, rootMargin: "0px 0px -40px 0px" });
    reveals.forEach(function (el) { io.observe(el); });
  } else {
    reveals.forEach(function (el) { el.classList.add("in"); });
  }

  /* Animated counters */
  var statsWrap = document.getElementById("stats");
  var counted = false;
  function runCounters() {
    if (counted) return;
    counted = true;
    document.querySelectorAll(".num[data-count]").forEach(function (el) {
      var target = parseInt(el.getAttribute("data-count"), 10);
      var start = 0;
      var dur = 1400;
      var t0 = performance.now();
      function tick(now) {
        var p = Math.min((now - t0) / dur, 1);
        var eased = 1 - Math.pow(1 - p, 3);
        el.textContent = Math.floor(start + (target - start) * eased);
        if (p < 1) requestAnimationFrame(tick);
        else el.textContent = target;
      }
      requestAnimationFrame(tick);
    });
  }
  if (statsWrap && "IntersectionObserver" in window) {
    var co = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) { if (e.isIntersecting) { runCounters(); co.disconnect(); } });
    }, { threshold: 0.4 });
    co.observe(statsWrap);
  } else {
    runCounters();
  }

  /* Chatbot stub */
  var chatToggle = document.getElementById("chatToggle");
  var chatPanel = document.getElementById("chatPanel");
  var chatClose = document.getElementById("chatClose");
  function toggleChat(show) {
    if (!chatPanel) return;
    chatPanel.hidden = show === undefined ? !chatPanel.hidden : !show;
  }
  if (chatToggle) chatToggle.addEventListener("click", function () { toggleChat(); });
  if (chatClose) chatClose.addEventListener("click", function () { toggleChat(false); });

  /* Cookie consent */
  var cookie = document.getElementById("cookie");
  var cookieOk = document.getElementById("cookieOk");
  if (cookie && !localStorage.getItem("tps-cookie")) {
    cookie.hidden = false;
  }
  if (cookieOk) cookieOk.addEventListener("click", function () {
    localStorage.setItem("tps-cookie", "1");
    if (cookie) cookie.hidden = true;
  });

  /* Footer year safeguard (kept dynamic for accuracy) */
  var copy = document.querySelector(".copyright");
  if (copy) copy.textContent = "© " + new Date().getFullYear() + " TechPulseSoft. All Rights Reserved.";
})();
