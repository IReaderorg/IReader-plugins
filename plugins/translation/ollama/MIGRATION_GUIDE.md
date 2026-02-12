# Migration Guide: Ollama Plugin v1.0 → v2.0

## Overview

Version 2.0 of the Ollama Translation Plugin is **fully backward compatible** with v1.0. No action is required for existing users.

## What's New in v2.0

### 1. Dual API Endpoint Support

You can now choose between two Ollama API endpoints:

- **Chat API** (`/api/chat`) - Default, recommended
  - Better context handling
  - More consistent translations
  - Same as v1.0 behavior

- **Generate API** (`/api/generate`) - New option
  - Simpler, faster
  - Good for quick translations
  - Lower memory usage

### 2. More Models

New models added:
- `llama3.3` - Latest Llama 3
- `llama3.2`, `llama3.1` - More Llama 3 variants
- `phi4` - Latest Microsoft Phi
- `qwen2.5` - Latest Qwen
- `deepseek-r1` - DeepSeek reasoning model
- `smollm2` - Lightweight (221 MB)

### 3. Better Configuration

- Settings now persist properly
- More helpful descriptions
- Test connection button
- Links to model library

## Migration Steps

### For Most Users: No Action Needed

Your existing configuration will continue to work:
- ✅ Server URL preserved
- ✅ Model selection preserved
- ✅ All settings preserved
- ✅ Same translation quality

### Optional: Try New Features

#### Switch to Generate Endpoint (Optional)

If you want faster translations:

1. Open IReader Settings
2. Go to Translation → Ollama Translation
3. Find "API Endpoint" setting
4. Select "Generate (/api/generate)"
5. Save

**When to use Generate:**
- Simple, short translations
- Speed is priority
- Limited system resources

**When to use Chat (default):**
- Long texts with context
- Quality is priority
- Multi-paragraph translations

#### Try New Models (Optional)

If you want to try newer models:

1. Pull the model first:
   ```bash
   ollama pull llama3.3
   # or
   ollama pull smollm2:135m  # For low-resource devices
   ```

2. In plugin settings:
   - Select new model from dropdown
   - Or enter custom model name

**Model Recommendations:**

| Your Priority | Recommended Model | Size |
|---------------|-------------------|------|
| Best Quality | `llama3.3` | 42 GB |
| Balanced | `mistral` (default) | 4.1 GB |
| Speed | `phi4` | 7.9 GB |
| Low Resources | `smollm2:135m` | 221 MB |

## Troubleshooting

### "Model not found" Error

**Cause:** New model not pulled yet

**Solution:**
```bash
ollama pull <model-name>
```

### Settings Not Saving

**Cause:** Old preferences format

**Solution:**
1. Note your current settings
2. Clear plugin data (if available)
3. Reconfigure plugin
4. Settings will now persist properly

### Translations Different Quality

**Cause:** Different endpoint or model

**Solution:**
- Check "API Endpoint" setting (should be "Chat" for best quality)
- Verify model is fully downloaded: `ollama list`
- Lower temperature to 0.1 for more consistent results

## Rollback (If Needed)

If you experience issues with v2.0:

1. **Keep using v1.0 behavior:**
   - Ensure "API Endpoint" is set to "Chat"
   - Use same model as before
   - All v1.0 functionality is preserved

2. **Report issues:**
   - Open issue on GitHub
   - Include error messages
   - Mention you're using v2.0

## Configuration Comparison

### v1.0 Configuration
```
Server URL: http://localhost:11434
Model: mistral
Temperature: 0.1 (hardcoded)
Max Tokens: 8192 (hardcoded)
Endpoint: /api/chat (only option)
```

### v2.0 Configuration
```
Server URL: http://localhost:11434
API Endpoint: Chat or Generate (new!)
Model: 15 options (was 9)
Temperature: 0.1 (now configurable & saved)
Max Tokens: 8192 (now configurable & saved)
```

## Feature Comparison

| Feature | v1.0 | v2.0 |
|---------|------|------|
| Chat API | ✅ | ✅ |
| Generate API | ❌ | ✅ |
| Model Count | 9 | 15 |
| Persistent Settings | Partial | Full |
| Error Messages | Basic | Detailed |
| Model Library Link | ❌ | ✅ |
| Test Connection | ❌ | ✅ |

## FAQ

**Q: Do I need to reconfigure anything?**
A: No, your existing configuration will work as-is.

**Q: Will my translations change?**
A: No, if you keep the same settings (Chat endpoint, same model).

**Q: Should I switch to Generate endpoint?**
A: Only if you want faster translations and don't mind slightly lower quality.

**Q: Do I need to update Ollama?**
A: No, v2.0 works with all Ollama versions that v1.0 worked with.

**Q: Can I use both endpoints?**
A: Yes, you can switch between them anytime in settings.

**Q: Will this break my existing translations?**
A: No, all existing functionality is preserved.

## Recommended Actions

### For All Users
1. ✅ Update to v2.0 (no configuration needed)
2. ✅ Continue using as before
3. ✅ Optionally explore new features

### For Power Users
1. ✅ Try Generate endpoint for speed comparison
2. ✅ Test new models (llama3.3, phi4, etc.)
3. ✅ Experiment with temperature settings
4. ✅ Use Test Connection feature

### For Low-Resource Devices
1. ✅ Try `smollm2:135m` model (only 221 MB!)
2. ✅ Use Generate endpoint
3. ✅ Lower max_tokens to 2048-4096

## Support

If you encounter any issues:

1. Check [README.md](README.md) for troubleshooting
2. Review [CHANGELOG.md](CHANGELOG.md) for detailed changes
3. Open issue on GitHub with:
   - Error message
   - Your configuration
   - Ollama version: `ollama --version`
   - Model used

## Summary

✅ **No migration needed** - v2.0 is fully backward compatible

✅ **Optional improvements** - Try new endpoints and models if desired

✅ **Same quality** - Default behavior unchanged

✅ **More options** - New features available when you want them

Enjoy the enhanced Ollama Translation Plugin!
