# FlareSolverr Cloudflare Bypass Plugin (Linux)

Cloudflare bypass for IReader Desktop using bundled FlareSolverr.

## Features

- Zero configuration - FlareSolverr bundled with plugin
- Auto-starts when Cloudflare challenge detected
- Auto-stops when IReader closes

## Supported Challenges

| Type | Supported |
|------|-----------|
| JavaScript Challenge | ✅ |
| Managed Challenge | ✅ |
| Turnstile | ✅ |
| CAPTCHA | ❌ |

## Requirements

- Linux x64 (Ubuntu 20.04+, or equivalent)
- ~250MB disk space

## Building

```bash
cd IReader-plugins
./gradlew :plugins:features:flaresolverr-bypass-linux:packagePlugin
```

## Size

~250MB (includes Chromium browser)
