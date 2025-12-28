# FlareSolverr Cloudflare Bypass Plugin (Windows)

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

- Windows 10+ (x64)
- ~350MB disk space

## Building

```bash
cd IReader-plugins
./gradlew :plugins:features:flaresolverr-bypass-windows:packagePlugin
```

## Size

~350MB (includes Chromium browser)
