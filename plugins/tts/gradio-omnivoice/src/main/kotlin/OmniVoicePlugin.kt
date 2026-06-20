package io.github.ireaderorg.plugins.gradioomnivoice

import ireader.plugin.api.*

/**
 * OmniVoice Plugin - Multilingual TTS with voice cloning.
 * 
 * OmniVoice supports:
 * - 100+ languages
 * - Voice cloning from reference audio
 * - Fine-grained control over generation parameters
 * 
 * Uses the k2-fsa/OmniVoice Hugging Face space.
 */
class OmniVoicePlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-omnivoice",
        name = "OmniVoice (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "OmniVoice - Multilingual TTS with voice cloning. Supports 100+ languages.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://k2-fsa-omnivoice.hf.space",
            "gradio.apiName" to "/_design_fn",
            "gradio.apiType" to "GRADIO_API_CALL",
            "gradio.audioOutputIndex" to "0",
            "gradio.languages" to "en,zh,ja,ko,fr,de,es,pt,ru,ar,hi,bn,pa,ta,te,ml,kn,gu,mr,or,as,ne,si,th,lo,my,km,tl,vi,id,ms,sw,am,so,ha,yo,ig,zu,xh,af,st,tn,ts,ss,ve,nr",
            "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"lang","choices":["Auto","English","Chinese","Japanese","Korean","French","German","Spanish","Portuguese","Russian","Arabic","Hindi","Bengali","Punjabi","Tamil","Telugu","Malayalam","Kannada","Gujarati","Marathi","Oriya","Assamese","Nepali","Sinhala","Thai","Lao","Burmese","Khmer","Tagalog","Vietnamese","Indonesian","Malay","Swahili","Amharic","Somali","Hausa","Yoruba","Igbo","Zulu","Xhosa","Afrikaans","Sotho","Tswana","Tsonga","Venda","Ndebele"],"default":"Auto"},{"type":"float","name":"ns","default":32,"min":1,"max":100},{"type":"float","name":"gs","default":2.0,"min":0.1,"max":10.0},{"type":"boolean","name":"dn","default":true},{"type":"float","name":"sp","default":1.0,"min":0.1,"max":2.0},{"type":"float","name":"du","default":3,"min":0.5,"max":30},{"type":"boolean","name":"pp","default":true},{"type":"boolean","name":"po","default":true}]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "OmniVoice plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
