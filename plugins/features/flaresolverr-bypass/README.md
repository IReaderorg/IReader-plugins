# FlareSolverr Bypass Plugin

A lightweight Cloudflare bypass plugin that downloads FlareSolverr binaries on-demand from the official GitHub releases.

## Features

- **On-demand download**: FlareSolverr binaries are downloaded only when needed, keeping the plugin size minimal (~10KB vs 400-600MB)
- **Cross-platform**: Supports Windows, Linux, and macOS (Intel & Apple Silicon)
- **Auto-start**: Automatically starts FlareSolverr server when bypass is needed
- **Progress tracking**: Shows download progress in the UI

## How it works

1. When a Cloudflare challenge is detected, the plugin checks if FlareSolverr is installed
2. If not installed, it prompts the user to download (or auto-downloads based on settings)
3. Downloads the appropriate binary from [FlareSolverr GitHub Releases](https://github.com/FlareSolverr/FlareSolverr/releases)
4. Extracts and runs FlareSolverr locally
5. Uses FlareSolverr to solve the Cloudflare challenge

## Supported Platforms

| Platform | Architecture | Download Size |
|----------|-------------|---------------|
| Windows | x64 | ~600 MB |
| Linux | x64 | ~450 MB |
| macOS | x64 (Intel) | ~400 MB |
| macOS | arm64 (Apple Silicon) | ~400 MB |

## Requirements

- Internet connection for initial download
- ~1 GB free disk space
- For Linux/macOS: `tar` command available (usually pre-installed)

## Configuration

The plugin stores downloaded files in:
- Windows: `%LOCALAPPDATA%\IReader\plugins\flaresolverr\`
- macOS: `~/Library/Application Support/IReader/plugins/flaresolverr/`
- Linux: `~/.local/share/IReader/plugins/flaresolverr/`

## Version

- Plugin version: 2.0.0
- FlareSolverr version: v3.3.21

## License

MIT License - Same as IReader
