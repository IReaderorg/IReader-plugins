# IReader Plugins - Project Overview

## What is This Project?

**IReader-plugins** is the official plugin development kit and repository for IReader, a cross-platform ebook/novel reader application. This project enables developers to extend IReader's functionality through a modular plugin system.

## Why Does This Project Exist?

### The Problem
Traditional ebook readers are monolithic applications where adding new features requires modifying the core codebase. This creates several issues:
- Users can't customize their experience
- New features require app updates
- Third-party contributions are difficult
- Features bloat the main application

### The Solution
A plugin architecture that:
- Allows modular feature addition
- Enables community contributions
- Supports monetization for developers
- Keeps the core app lightweight
- Provides security through sandboxing

## What Can Plugins Do?

### 1. Themes
Change the visual appearance of IReader:
- Color schemes (light/dark/custom)
- Typography (fonts, sizes, spacing)
- Backgrounds and textures
- Accessibility themes (high contrast)

### 2. Text-to-Speech (TTS)
Add voice reading capabilities:
- Cloud TTS services (Google, Amazon, Azure)
- Local TTS engines
- Custom voice models
- Multi-language support

### 3. Translation
Enable reading in different languages:
- Real-time translation
- Offline translation
- Dictionary integration
- Language learning tools

### 4. Features
Add custom functionality:
- Dictionary lookup
- Note-taking and annotations
- Reading statistics
- Social sharing
- Bookmarks with tags
- Custom gestures

### 5. AI Capabilities
Intelligent text processing:
- Chapter/book summarization
- Character analysis and tracking
- Q&A about book content
- Content recommendations
- Sentiment analysis

## How Does It Work?

### Architecture

```
┌────────────────────────────────────────────────────────┐
│                    IReader App                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Plugin Manager                       │  │
│  │  • Loads plugins from .iplugin files             │  │
│  │  • Manages lifecycle (enable/disable)            │  │
│  │  • Enforces permissions and resource limits      │  │
│  └──────────────────────────────────────────────────┘  │
│                         │                               │
│                    Plugin API                           │
│                         │                               │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐        │
│  │Theme │ │ TTS  │ │Trans │ │Feat. │ │  AI  │        │
│  │Plugin│ │Plugin│ │Plugin│ │Plugin│ │Plugin│        │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘        │
└────────────────────────────────────────────────────────┘
```

### Plugin Lifecycle

1. **Discovery**: IReader scans for `.iplugin` files
2. **Loading**: Plugin manifest is read and validated
3. **Registration**: Plugin is added to registry
4. **Initialization**: `initialize()` called when enabled
5. **Execution**: Plugin methods called as needed
6. **Cleanup**: `cleanup()` called when disabled

### Security Model

Plugins run in a sandboxed environment:
- **Permission System**: Plugins declare required permissions
- **Resource Limits**: CPU, memory, and time limits enforced
- **Network Proxy**: All requests go through IReader's proxy
- **File Isolation**: Limited file system access

## Who Is This For?

### Plugin Developers
- Independent developers wanting to extend IReader
- Companies integrating their services (TTS, translation)
- Community members adding features they want

### IReader Users
- Readers wanting to customize their experience
- Users needing specific features (accessibility, languages)
- Power users wanting advanced functionality

## Project Components

### This Repository Contains:

| Component | Purpose |
|-----------|---------|
| `buildSrc/` | Gradle plugin for building IReader plugins |
| `annotations/` | KSP annotations for plugin metadata |
| `compiler/` | KSP processor for compile-time validation |
| `plugins/` | Example and community plugins |
| `docs/` | Documentation |

### Related Repositories:

| Repository | Purpose |
|------------|---------|
| `IReader` | Main application |
| `plugin-api` | Plugin interfaces (published as library) |

## Getting Involved

### As a Developer
1. Read the [Developer Guide](./DEVELOPER_GUIDE.md)
2. Check [Quick Reference](./QUICK_REFERENCE.md)
3. Look at example plugins in `plugins/`
4. Create your own plugin
5. Submit to the community repository

### As a User
1. Browse available plugins in IReader's Plugin Hub
2. Install plugins that interest you
3. Report issues or request features
4. Rate and review plugins

## Monetization

Developers can monetize their plugins:

| Model | Description |
|-------|-------------|
| Free | No cost, great for building reputation |
| Premium | One-time purchase |
| Freemium | Basic free, advanced features paid |
| Subscription | Recurring payment (for services) |

IReader handles payment processing and license validation.

## Roadmap

### Current Features
- ✅ Theme plugins
- ✅ TTS plugins
- ✅ Translation plugins
- ✅ Feature plugins
- ✅ AI plugins
- ✅ Plugin marketplace
- ✅ Monetization support

### Planned Features
- 🔄 Plugin composition (chaining)
- 🔄 Cross-plugin communication
- 🔄 Plugin analytics dashboard
- 🔄 Hot reload for development
- 📋 Plugin widgets
- 📋 Plugin shortcuts

## Support

- **Documentation**: This `docs/` folder
- **Issues**: GitHub Issues
- **Discord**: [IReader Community](https://discord.gg/ireader)
- **Email**: plugins@ireader.org

## License

This project is licensed under MPL 2.0. Plugins can use any compatible license.
