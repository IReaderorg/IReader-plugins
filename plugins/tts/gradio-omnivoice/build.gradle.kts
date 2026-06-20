plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-omnivoice")
    name.set("OmniVoice (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("OmniVoice - Multilingual TTS with voice cloning. Supports 100+ languages.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradioomnivoice.OmniVoicePlugin")
    tags.set(listOf("tts", "gradio", "omnivoice", "voice-clone", "multilingual"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://k2-fsa-omnivoice.hf.space",
        "gradio.apiName" to "/_design_fn",
        "gradio.apiType" to "GRADIO_API_CALL",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en,zh,ja,ko,fr,de,es,pt,ru,ar,hi,bn,pa,ta,te,ml,kn,gu,mr,or,as,ne,si,th,lo,my,km,tl,vi,id,ms,tl,sw,am,so,ha,yo,ig,zu,xh,af,st,tn,ts,ss,ve,nr",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"lang","choices":["Auto","English","Chinese","Japanese","Korean","French","German","Spanish","Portuguese","Russian","Arabic","Hindi","Bengali","Punjabi","Tamil","Telugu","Malayalam","Kannada","Gujarati","Marathi","Oriya","Assamese","Nepali","Sinhala","Thai","Lao","Burmese","Khmer","Tagalog","Vietnamese","Indonesian","Malay","Swahili","Amharic","Somali","Hausa","Yoruba","Igbo","Zulu","Xhosa","Afrikaans","Sotho","Tswana","Tsonga","Venda","Ndebele"],"default":"Auto"},{"type":"float","name":"ns","default":32,"min":1,"max":100},{"type":"float","name":"gs","default":2.0,"min":0.1,"max":10.0},{"type":"boolean","name":"dn","default":true},{"type":"float","name":"sp","default":1.0,"min":0.1,"max":2.0},{"type":"float","name":"du","default":3,"min":0.5,"max":30},{"type":"boolean","name":"pp","default":true},{"type":"boolean","name":"po","default":true}]"""
    ))
}
