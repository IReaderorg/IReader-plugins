# Ollama Translation Plugin

Local AI translation using Ollama - run large language models locally for private, offline translation.

## Features

- ✅ **100% Private & Offline** - All translation happens on your device
- ✅ **No API Keys Required** - No cloud services, no subscriptions
- ✅ **Multiple Models** - Support for 15+ open-source LLMs
- ✅ **Dual API Support** - Both `/api/chat` and `/api/generate` endpoints
- ✅ **Context-Aware** - Maintains narrative tone and style
- ✅ **Customizable** - Adjust temperature, max tokens, and more

## Quick Start

### 1. Install Ollama

Download and install Ollama from [ollama.com](https://ollama.com)

**macOS/Linux:**
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

**Windows:**
Download the installer from [ollama.com/download](https://ollama.com/download)

### 2. Pull a Model

```bash
# Recommended for translation (balanced quality/speed)
ollama pull mistral

# Or try other models:
ollama pull llama3.3      # Best quality
ollama pull gemma2        # Good balance
ollama pull smollm2:135m  # Smallest (221 MB)
```

### 3. Start Ollama Server

```bash
ollama serve
```

The server will start at `http://localhost:11434`

### 4. Configure Plugin

In IReader:
1. Go to Settings → Translation
2. Select "Ollama Translation"
3. Configure:
   - **Server URL**: `http://localhost:11434` (default)
   - **API Endpoint**: Chat (recommended) or Generate
   - **Model**: Select from dropdown or enter custom model
   - **Temperature**: 0.1 (lower = more consistent)
   - **Max Tokens**: 8192 (adjust based on text length)

### 5. Test Connection

Click "Test Connection" button to verify Ollama is running and accessible.

## Supported Models

### Recommended for Translation

| Model | Size | Quality | Speed | Best For |
|-------|------|---------|-------|----------|
| `mistral` | 4.1 GB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | General translation |
| `llama3.3` | 42 GB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | Best quality |
| `gemma2` | 5.4 GB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Balanced |
| `qwen2.5` | 4.4 GB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Multilingual |
| `smollm2:135m` | 221 MB | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Low-resource devices |

### All Supported Models

- `mistral` - Mistral 7B (default)
- `llama3.3` - Meta Llama 3.3
- `llama3.2` - Meta Llama 3.2
- `llama3.1` - Meta Llama 3.1
- `llama2` - Meta Llama 2
- `gemma2` - Google Gemma 2
- `gemma` - Google Gemma
- `phi4` - Microsoft Phi-4
- `phi3` - Microsoft Phi-3
- `qwen2.5` - Alibaba Qwen 2.5
- `qwen2` - Alibaba Qwen 2
- `deepseek-r1` - DeepSeek R1 (reasoning model)
- `deepseek-coder` - DeepSeek Coder
- `codellama` - Meta Code Llama
- `smollm2` - HuggingFace SmolLM2

Browse all models: [ollama.com/library](https://ollama.com/library)

## API Endpoints

### Chat API (`/api/chat`) - Recommended

**Best for:**
- Conversational context
- Multi-turn interactions
- Better consistency across paragraphs

**Request format:**
```json
{
  "model": "mistral",
  "messages": [
    {"role": "system", "content": "You are a translator..."},
    {"role": "user", "content": "Translate this text..."}
  ],
  "stream": false
}
```

### Generate API (`/api/generate`)

**Best for:**
- Simple, single-turn completions
- Slightly faster responses
- Lower memory usage

**Request format:**
```json
{
  "model": "mistral",
  "prompt": "Translate this text...",
  "stream": false
}
```

## Configuration Options

### Server Settings

- **Server URL**: URL of your Ollama server
  - Default: `http://localhost:11434`
  - Remote: `http://192.168.1.100:11434` (if running on another machine)

- **API Endpoint**: Choose between Chat or Generate
  - Chat: Better quality, more context-aware
  - Generate: Simpler, slightly faster

### Model Settings

- **Model**: Select from preset models or enter custom
  - Preset: Choose from dropdown
  - Custom: Enter any model name (e.g., `mixtral:8x7b`)

### Advanced Settings

- **Temperature**: Controls randomness (0.0 - 1.0)
  - `0.1` (default): More consistent, literal translations
  - `0.5`: Balanced
  - `0.9`: More creative, varied translations

- **Max Tokens**: Maximum response length
  - `8192` (default): Good for most texts
  - `4096`: For shorter texts
  - `16384`: For very long texts (if model supports)

## Troubleshooting

### "Cannot connect to Ollama"

**Solution:**
1. Check if Ollama is running: `ollama list`
2. Start Ollama: `ollama serve`
3. Verify URL in plugin settings

### "Model not found"

**Solution:**
Pull the model first:
```bash
ollama pull mistral
```

### Translation is slow

**Solutions:**
1. Use a smaller model: `smollm2:135m`
2. Reduce max_tokens: `4096`
3. Switch to Generate endpoint
4. Check system resources (RAM, CPU)

### Translation quality is poor

**Solutions:**
1. Use a larger model: `llama3.3` or `gemma2`
2. Lower temperature: `0.1`
3. Use Chat endpoint instead of Generate
4. Ensure model is fully downloaded: `ollama list`

### Out of memory errors

**Solutions:**
1. Use smaller model: `smollm2:135m` (221 MB)
2. Close other applications
3. Reduce max_tokens
4. Use quantized models (e.g., `mistral:7b-q4_0`)

## Performance Tips

### For Best Quality
- Model: `llama3.3` or `deepseek-r1`
- Endpoint: Chat
- Temperature: `0.1`
- Max Tokens: `8192`

### For Best Speed
- Model: `smollm2:135m` or `phi3`
- Endpoint: Generate
- Temperature: `0.1`
- Max Tokens: `4096`

### For Low-Resource Devices
- Model: `smollm2:135m` (only 221 MB!)
- Endpoint: Generate
- Max Tokens: `2048`

## Remote Ollama Server

You can run Ollama on a more powerful machine and connect remotely:

### On Server Machine:
```bash
# Allow remote connections
OLLAMA_HOST=0.0.0.0:11434 ollama serve
```

### In Plugin Settings:
- Server URL: `http://192.168.1.100:11434` (replace with server IP)

## Privacy & Security

- ✅ All translation happens locally
- ✅ No data sent to cloud services
- ✅ No API keys or accounts required
- ✅ Works completely offline
- ✅ Full control over your data

## System Requirements

### Minimum (with smollm2:135m)
- RAM: 512 MB available
- Storage: 500 MB
- CPU: Any modern processor

### Recommended (with mistral)
- RAM: 8 GB
- Storage: 5 GB
- CPU: Multi-core processor
- GPU: Optional (speeds up inference)

### Optimal (with llama3.3)
- RAM: 16 GB+
- Storage: 50 GB
- CPU: High-performance multi-core
- GPU: NVIDIA GPU with 8GB+ VRAM

## Advanced Usage

### Custom Models

You can use any model from Ollama library:

```bash
# Pull a specific variant
ollama pull mixtral:8x7b

# Use in plugin
# Custom Model: mixtral:8x7b
```

### Model Variants

Models often have multiple variants:
- `mistral:latest` - Latest version
- `mistral:7b` - 7 billion parameters
- `mistral:7b-q4_0` - 4-bit quantized (smaller, faster)
- `mistral:7b-q8_0` - 8-bit quantized (balanced)

### Environment Variables

```bash
# Change default port
OLLAMA_HOST=0.0.0.0:8080 ollama serve

# Enable GPU
OLLAMA_GPU=1 ollama serve

# Set model directory
OLLAMA_MODELS=/path/to/models ollama serve
```

## FAQ

**Q: Do I need an internet connection?**
A: Only to download models initially. After that, everything works offline.

**Q: Can I use multiple models?**
A: Yes, pull multiple models and switch between them in settings.

**Q: How much disk space do I need?**
A: Depends on model:
- Small (smollm2): 221 MB
- Medium (mistral): 4.1 GB
- Large (llama3.3): 42 GB

**Q: Can I use this on mobile?**
A: Yes, on Android. iOS support depends on Ollama availability.

**Q: Is this faster than cloud translation?**
A: Depends on your hardware. With a good GPU, it can be very fast. Without GPU, cloud services may be faster.

**Q: Can I translate multiple languages?**
A: Yes, all models support multiple languages. Quality varies by language.

## Support

- **Ollama Documentation**: [docs.ollama.com](https://docs.ollama.com)
- **Model Library**: [ollama.com/library](https://ollama.com/library)
- **IReader Issues**: [github.com/IReaderorg/IReader/issues](https://github.com/IReaderorg/IReader/issues)

## License

This plugin is part of IReader and follows the same license.

Ollama is licensed under MIT License.

## Credits

- Built for [IReader](https://github.com/IReaderorg/IReader)
- Powered by [Ollama](https://ollama.com)
- Uses open-source LLMs from Meta, Google, Microsoft, Alibaba, and others
