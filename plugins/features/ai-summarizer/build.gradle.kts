plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.ai-summarizer")
    name.set("AI Summarizer")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("AI-powered text summarization for novels. Supports chapter summaries, book overviews, 'previously on' recaps, and key point extraction using OpenAI or Claude APIs.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT))
    mainClass.set("io.github.ireaderorg.plugins.aisummarizer.AISummarizerPluginImpl")
    tags.set(listOf("ai", "summarizer", "summary", "recap", "openai", "claude", "gpt", "reading"))
}
