# TechPulseSoft Website — Free Hosting & Domain Setup Guide

This is a **static website** (HTML/CSS/JS, no build step) for **https://techpulsesoft.com**.
You can host it **100% free** on **GitHub Pages** and point your **GoDaddy** domain to it.

> Total cost: **$0** (you already own the domain). GitHub Pages gives free hosting **and** free HTTPS.

---

## What's in this folder

```
techpulsesoft-website/
├── index.html        # The full single-page website
├── styles.css        # Premium styling, dark mode, animations
├── main.js           # Theme toggle, scroll reveal, counters, chat, cookie banner
├── CNAME             # Custom domain for GitHub Pages (techpulsesoft.com)
├── assets/favicon.svg
└── SETUP_GUIDE.md    # ← you are here
```

Deployment is automated by `.github/workflows/deploy-website.yml`.

---

## Step 1 — Preview locally (optional)

```bash
cd techpulsesoft-website
python3 -m http.server 8000
# open http://localhost:8000
```

---

## Step 2 — Get the code onto `main`

This branch is `claude/professional-website-setup-mabnd3`. Open a Pull Request and merge it into `main`
(the deploy workflow runs on pushes to `main`).

---

## Step 3 — Enable GitHub Pages (one-time)

1. Go to your repo on GitHub → **Settings** → **Pages**.
2. Under **Build and deployment → Source**, choose **GitHub Actions**.
3. That's it. Every push to `main` that touches `techpulsesoft-website/` now redeploys automatically.

Your site first goes live at: **https://rahulbandgar.github.io/digital/**
(the custom domain in Step 4 replaces this.)

---

## Step 4 — Connect your GoDaddy domain (techpulsesoft.com)

> ⚠️ Note: your message said "echpulsesoft.com" — the correct domain from your brief is
> **techpulsesoft.com**. The `CNAME` file is set to that. Change it if your real domain differs.

### 4a. Add DNS records in GoDaddy

GoDaddy → **My Products** → your domain → **DNS** → **Manage DNS**. Add these records:

**Apex domain (techpulsesoft.com) — four A records:**

| Type | Name | Value           | TTL   |
|------|------|-----------------|-------|
| A    | @    | 185.199.108.153 | 1 hr  |
| A    | @    | 185.199.109.153 | 1 hr  |
| A    | @    | 185.199.110.153 | 1 hr  |
| A    | @    | 185.199.111.153 | 1 hr  |

**(Recommended) IPv6 — four AAAA records:**

| Type | Name | Value                  |
|------|------|------------------------|
| AAAA | @    | 2606:50c0:8000::153    |
| AAAA | @    | 2606:50c0:8001::153    |
| AAAA | @    | 2606:50c0:8002::153    |
| AAAA | @    | 2606:50c0:8003::153    |

**www subdomain — one CNAME:**

| Type  | Name | Value                   |
|-------|------|-------------------------|
| CNAME | www  | rahulbandgar.github.io  |

> Delete any GoDaddy "Parked"/forwarding A record on `@` first, or it will conflict.
> GoDaddy doesn't allow CNAME on the apex `@`, which is why we use A/AAAA records for the root.

### 4b. Set the custom domain in GitHub

1. Repo → **Settings** → **Pages** → **Custom domain** → enter `techpulsesoft.com` → **Save**.
   (The `CNAME` file already does this, but setting it in the UI triggers verification.)
2. Wait for the DNS check to pass (minutes to a few hours — DNS can take up to 48h).
3. Tick **Enforce HTTPS** once the certificate is issued.

Verify propagation anytime at https://dnschecker.org (search the A record for techpulsesoft.com).

---

## Step 5 — Make the contact form actually send email (free)

The form posts to **Formspree** (free tier: 50 submissions/month).

1. Create a free account at https://formspree.io.
2. New form → copy your endpoint, e.g. `https://formspree.io/f/abcd1234`.
3. In `index.html`, replace `YOUR_FORM_ID`:
   ```html
   <form class="contact-form" action="https://formspree.io/f/abcd1234" method="POST">
   ```
4. Commit & push. Submissions now arrive in your email.

**Free alternatives:** Web3Forms, Getform, or a Google Form embed.

---

## Optional premium add-ons (all free tiers)

| Feature              | How to add                                                                 |
|----------------------|----------------------------------------------------------------------------|
| **Calendly booking** | Replace `#contact` links on the "Book Consultation" buttons with your Calendly URL, or embed their widget. |
| **Google Analytics** | Create a GA4 property, paste the gtag snippet before `</head>` in `index.html`. |
| **reCAPTCHA**        | Formspree includes spam filtering; for reCAPTCHA v3 add Google's site key + script. |
| **Live AI chatbot**  | The "Talk to TechPulseSoft" widget is a stub. Wire it to Tawk.to (free) or your own API. |
| **WhatsApp button**  | Edit the `wa.me/10000000000` link in `index.html` to your real number. |

---

## Updating the site later

Edit the files → commit to `main` → GitHub Actions redeploys in ~1 minute. No servers, no bills.

## Before you go live — replace placeholders

Search `index.html` for and update: `hello@techpulsesoft.com`, the phone `+1 (000) 000-0000`,
office address, `wa.me/10000000000`, `YOUR_FORM_ID`, and the LinkedIn/GitHub URLs.
