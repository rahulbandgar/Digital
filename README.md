# TechPulseSoft Website

Source for **https://techpulsesoft.com** — a premium, responsive enterprise
software-consultancy website. Static HTML/CSS/JS (no build step), hosted **free**
on GitHub Pages.

## Structure

```
.
├── techpulsesoft-website/        # The website
│   ├── index.html
│   ├── styles.css
│   ├── main.js
│   ├── CNAME                     # Custom domain (techpulsesoft.com)
│   ├── assets/favicon.svg
│   └── SETUP_GUIDE.md            # Free hosting + GoDaddy DNS setup
└── .github/workflows/
    └── deploy-website.yml        # Auto-deploy to GitHub Pages
```

## Preview locally

```bash
cd techpulsesoft-website
python3 -m http.server 8000
# open http://localhost:8000
```

## Deploy

Pushes to `claude/professional-website-setup-mabnd3` that touch
`techpulsesoft-website/` trigger the GitHub Pages deploy.

**One-time setup:** Repo → **Settings → Pages → Source → GitHub Actions**.

Full hosting and custom-domain (GoDaddy) instructions are in
[`techpulsesoft-website/SETUP_GUIDE.md`](techpulsesoft-website/SETUP_GUIDE.md).
