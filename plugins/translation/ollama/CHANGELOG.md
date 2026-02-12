# Ollama Translation Plugin - Changelog

## Version 2.0.0 (2025-02-12)

### Major Updates - API Implementation

Updated to use the latest Ollama API specification from [https://github.com/ollama/ollama/blob/main/docs/api.md](https://github.com/ollama/ollama/blob/main/docs/api.md)

### New Features

1. **Dual API Endpoint Support**
   - Added support for both `/api/chat` and `/api/generate` endpoints
   - `/api/chat` (default): Better for conversational context, multi-turn interactions
   - `/api/generate`: Simpler single-turn completions
   - User can switch between endpoints via configuration

2. **Enhanced Model Support**
   - Updated model list with latest Ollama models:
     - `llama3.3`, `llama3.2`, `llama3.1` (new Llama 3 variants)
     - `phi4` (latest Phi model)
     - `qwen2.5` (latest Qwen model)
     - `deepseek-r1` (new DeepSeek reasoning model)
     - `smollm2` (lightweight model for resource-constrained devices)
   - Maintained backward compatibility with existing models

3. **Improved Configuration**
   - Added API endpoint selector in settings
   - Temperature and max_tokens now properly saved to preferences
   - Better configuration UI with more helpful descriptions

4. **Better Error Handling**
   - More specific error messages for common issues:
     - Connection failures
     - Model not found (404 errors)
     - Invalid response formats
   - Separate parsing for chat vs generate responses

5. **Enhanced Documentation**
   - Added API endpoint usage notes
   - Updated installation instructions
   - Added link to Ollama model library

### Technical Changes

#### API Response Parsing

**Chat API Response Format:**
```json
{
  "model": "mistral",
  "created_at": "2025-02-08T11:22:15.229839Z",
  "message": {
    "role": "assistant",
    "content": "translated text here"
  },
  "done": true,
  "done_reason": "stop"
}
```

**Generate API Response Format:**
```json
{
  "model": "mistral",
  "created_at": "2025-02-08T11:02:55.115275Z",
  "response": "translated text here",
  "done": true,
  "done_reason": "stop"
}
```

#### New Methods

- `translateWithChat()`: Handles `/api/chat` endpoint
- `translateWithGenerate()`: Handles `/api/generate` endpoint
- `parseChatResponse()`: Parses chat API response
- `parseGenerateResponse()`: Parses generate API response

### Breaking Changes

None - fully backward compatible with v1.0.0

### Migration Guide

No migration needed. Existing configurations will continue to work with the default `/api/chat` endpoint.

To use the new `/api/generate` endpoint:
1. Open plugin settings
2. Select "Generate (/api/generate)" from the API Endpoint dropdown
3. Save settings

### Recommendations

- **For most users**: Use `/api/chat` (default) for better translation quality
- **For simple translations**: `/api/generate` may be slightly faster
- **For resource-constrained devices**: Try `smollm2:135m` model (only 221 MB)
- **For best quality**: Use `llama3.3` or `deepseek-r1` models

### API Reference

Based on official Ollama API documentation:
- Chat endpoint: https://docs.ollama.com/api/chat
- Generate endpoint: https://docs.ollama.com/api/generate
- Model library: https://ollama.com/library

### Testing

Tested with:
- Ollama v0.1.x and v0.2.x
- Models: mistral, llama3.3, gemma2, qwen2.5, smollm2:135m
- Both chat and generate endpoints
- Various text lengths and languages

### Known Issues

None at this time.

### Future Enhancements

Potential features for future versions:
- Streaming support for real-time translation
- Model auto-detection from Ollama server
- Batch translation optimization
- Custom system prompts per language pair
- Translation memory/caching

---

## Version 1.0.0 (Initial Release)

- Basic Ollama integration
- Support for `/api/chat` endpoint
- Configurable server URL and model
- Temperature and max_tokens settings
