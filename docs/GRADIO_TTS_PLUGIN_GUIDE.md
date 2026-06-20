# Gradio TTS Plugin Development Guide

This document provides a comprehensive guide for creating Gradio-based TTS engine plugins for IReader.

## Overview

Gradio TTS plugins allow IReader to use any Hugging Face Space or self-hosted Gradio endpoint for text-to-speech synthesis. The plugin system handles:

- API endpoint auto-detection
- Parameter mapping
- Audio format handling
- SSE streaming for modern Gradio 4.x APIs

## Plugin Structure

A Gradio TTS plugin consists of:

```
plugins/tts/gradio-your-tts/
├── build.gradle.kts          # Plugin configuration
├── src/main/kotlin/
│   └── YourTTSPlugin.kt      # Plugin implementation
└── src/main/resources/
    └── gradio-config.json    # Gradio API configuration
```

## Step 1: Create the Plugin Class

```kotlin
package io.github.ireaderorg.plugins.gradioyourtts

import ireader.plugin.api.*

class YourTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-your-tts",
        name = "Your TTS (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Description of your TTS engine",
        author = PluginAuthor(
            name = "Your Name",
            website = "https://github.com/yourname"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://your-space.hf.space",
            "gradio.apiName" to "/your_endpoint",
            "gradio.apiType" to "AUTO",
            "gradio.audioOutputIndex" to "0",
            "gradio.languages" to "en,zh",
            "gradio.params" to """[...]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Your TTS plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
```

## Step 2: Configure build.gradle.kts

```kotlin
plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-your-tts")
    name.set("Your TTS (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Description of your TTS engine")
    author.set("Your Name")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradioyourtts.YourTTSPlugin")
    tags.set(listOf("tts", "gradio", "your-tts"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://your-space.hf.space",
        "gradio.apiName" to "/your_endpoint",
        "gradio.apiType" to "AUTO",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en,zh",
        "gradio.params" to """[...]"""
    ))
}
```

## Step 3: Define API Parameters

The `gradio.params` JSON array defines the parameters your TTS API expects. Each parameter has:

- `type`: Parameter type (see supported types below)
- `name`: Parameter name (must match the Gradio API)
- `default`: Default value (optional)
- `min`/`max`: For numeric types (optional)

### Supported Parameter Types

| Type | Description | Example | JSON Output |
|------|-------------|---------|-------------|
| `text` | Text input (receives synthesis text) | `{"type":"text","name":"text_input"}` | `"Hello world"` |
| `string` | String parameter with default | `{"type":"string","name":"voice","default":"default_voice"}` | `"default_voice"` |
| `float` | Floating point number | `{"type":"float","name":"speed","default":1.0,"min":0.5,"max":2.0}` | `1.0` |
| `number` | Number (alias for float) | `{"type":"number","name":"cfg","default":2.0,"min":1.0,"max":3.0}` | `2.0` |
| `boolean` | Boolean value | `{"type":"boolean","name":"normalize","default":false}` | `false` |
| `audio` | Audio file input (optional) | `{"type":"audio","name":"reference_audio"}` | `null` |
| `choice` | Choice from options | `{"type":"choice","name":"voice","choices":["voice1","voice2"],"default":"voice1"}` | `"voice1"` |

### Important Notes on Parameter Types

1. **Text Input (`"type":"text"`)**: Only ONE parameter should have this type. It receives the text to synthesize.

2. **Boolean (`"type":"boolean"`)**: Must use `"type":"boolean"`, not `"type":"string"`. Boolean values are sent as JSON booleans (`true`/`false`), not strings.

3. **Audio (`"type":"audio"`)**: Optional audio parameters are sent as `null` when empty.

4. **Number (`"type":"number"`)**: Use for numeric parameters. Sent as JSON numbers.

### Example: VoxCPM API Parameters

```json
[
    {"type":"text","name":"text_input"},
    {"type":"string","name":"control_instruction","default":"A young girl with a soft, sweet voice."},
    {"type":"audio","name":"reference_wav_path_input"},
    {"type":"boolean","name":"use_prompt_text","default":false},
    {"type":"string","name":"prompt_text_input","default":""},
    {"type":"number","name":"cfg_value_input","default":2.0,"min":1.0,"max":3.0},
    {"type":"boolean","name":"do_normalize","default":false},
    {"type":"boolean","name":"denoise","default":false}
]
```

This produces the request body:
```json
{
    "data": [
        "Text to synthesize",
        "A young girl with a soft, sweet voice.",
        null,
        false,
        "",
        2.0,
        false,
        false
    ]
}
```

## Step 4: API Type Configuration

The `gradio.apiType` determines which API endpoint pattern to use:

| API Type | Endpoint Pattern | Description |
|----------|------------------|-------------|
| `AUTO` | Try all | Auto-detects the correct endpoint (recommended) |
| `GRADIO_API_CALL` | `/gradio_api/call/{fn_name}` | Modern Gradio 4.x with SSE streaming |
| `CALL` | `/call/{fn_name}` | Older Gradio 4.x with SSE streaming |
| `API_PREDICT` | `/api/predict` | Legacy Gradio 3.x |
| `RUN` | `/run/{fn_name}` | Run endpoint |
| `QUEUE` | `/queue/join` | Queue-based for long-running tasks |

**Recommendation**: Use `AUTO` unless you know the specific API type.

## Step 5: Finding API Information

To find the correct parameters for a Gradio Space:

1. Visit `https://your-space.hf.space/gradio_api/info`
2. Look at the `named_endpoints` section
3. Find your endpoint (e.g., `/generate`)
4. Note the parameters and their types

Example response:
```json
{
    "named_endpoints": {
        "/generate": {
            "parameters": [
                {"parameter_name": "text_input", "type": {"type": "string"}},
                {"parameter_name": "control_instruction", "type": {"type": "string"}},
                {"parameter_name": "reference_wav_path_input", "type": {"type": "filepath"}},
                {"parameter_name": "use_prompt_text", "type": {"type": "bool"}},
                {"parameter_name": "cfg_value_input", "type": {"type": "number"}}
            ]
        }
    }
}
```

## Common Issues and Solutions

### Issue 1: Text Duplicated in Request Body

**Symptom**: The synthesis text appears multiple times in the request body.

**Cause**: Multiple parameters have `"type":"text"`.

**Solution**: Only ONE parameter should have `"type":"text"`. All other text-like parameters should use `"type":"string"`.

### Issue 2: Boolean Values Sent as Strings

**Symptom**: Boolean values appear as `"false"` (string) instead of `false` (boolean) in the request body.

**Cause**: Using `"type":"string"` instead of `"type":"boolean"` for boolean parameters.

**Solution**: Always use `"type":"boolean"` for boolean parameters.

### Issue 3: Missing Parameters in Request

**Symptom**: The API returns an error because required parameters are missing.

**Cause**: Not all API parameters are defined in `gradio.params`.

**Solution**: Include ALL parameters that the API expects, even optional ones. Use appropriate default values.

### Issue 4: API Returns `event: error, data: null`

**Symptom**: The SSE response contains an error.

**Causes**:
- Incorrect number of parameters
- Wrong parameter types
- Missing required parameters
- Malformed request body

**Solution**: 
1. Check the API info endpoint for correct parameters
2. Verify parameter types match the API expectations
3. Ensure all required parameters are included

## Testing Your Plugin

1. Build the plugin:
   ```bash
   ./gradlew :plugins:tts:gradio-your-tts:assemble
   ```

2. Install the plugin in IReader

3. Test the API directly using curl:
   ```bash
   # Get event ID
   EVENT_ID=$(curl -s -X POST "https://your-space.hf.space/gradio_api/call/endpoint" \
     -H "Content-Type: application/json" \
     -d '{"data": ["Hello", "default", null, false]}' | \
     python3 -c "import sys,json; print(json.load(sys.stdin)['event_id'])")
   
   # Get result
   curl -s "https://your-space.hf.space/gradio_api/call/endpoint/$EVENT_ID"
   ```

4. Check the IReader logs for debugging information

## Complete Example: Simple TTS Plugin

For a simple TTS API with just text and speed parameters:

```kotlin
// Plugin class
class SimpleTTSPlugin : Plugin {
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-simple-tts",
        name = "Simple TTS (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Simple TTS with speed control",
        author = PluginAuthor(name = "Your Name"),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://your-space.hf.space",
            "gradio.apiName" to "/synthesize",
            "gradio.apiType" to "AUTO",
            "gradio.audioOutputIndex" to "0",
            "gradio.languages" to "en",
            "gradio.params" to """[{"type":"text","name":"text"},{"type":"float","name":"speed","default":1.0,"min":0.5,"max":2.0}]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        context.log(LogLevel.INFO, "Simple TTS plugin initialized")
    }
    
    override fun cleanup() {}
}
```

## Reference: Existing Plugins

Look at existing plugins for reference:
- `gradio-voxcpm-tts` - Complex with 8 parameters including audio, boolean, number (WORKING)

**Note:** Other plugins (bark, edge, fish-speech, openvoice, parler, persian-*, silero, style-tts-2, tortoise, xtts-v2) were removed due to broken/unavailable Hugging Face Spaces. When creating new plugins, verify the target space is active and responding.

**Important:** The package name in the Kotlin file MUST match the mainClass in build.gradle.kts. For example:
- File: `package io.github.ireaderorg.plugins.gradioqwen3tts`
- build.gradle: `mainClass.set("io.github.ireaderorg.plugins.gradioqwen3tts.Qwen3TTSPlugin")`

## Gradio API Response Formats

### Modern Gradio 4.x (SSE Streaming)

The API returns an event ID, then you poll for the result:

```
POST /gradio_api/call/endpoint
→ {"event_id": "abc123"}

GET /gradio_api/call/endpoint/abc123
→ event: complete
→ data: [{"path": "/tmp/...", "url": "https://.../file=...", ...}]
```

The audio URL is in the `url` field of the first item in the `data` array.

### Legacy Gradio 3.x

```
POST /api/predict
→ {"data": [{"name": "audio.wav", "data": "data:audio/wav;base64,..."}]}
```

The audio is base64-encoded in the `data` field.
