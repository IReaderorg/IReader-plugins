package io.github.ireaderorg.plugins.dictionary

import ireader.plugin.api.*

/**
 * Dictionary Plugin - Look up word definitions while reading.
 * Demonstrates the Feature plugin API.
 */
class DictionaryPlugin : FeaturePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.dictionary",
        name = "Dictionary Lookup",
        version = "1.0.0",
        versionCode = 1,
        description = "Look up word definitions while reading",
        author = PluginAuthor(name = "IReader Team"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.READER_CONTEXT, PluginPermission.NETWORK),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    override fun initialize(context: PluginContext) {
        // Initialize dictionary resources
    }
    
    override fun cleanup() {
        // Release resources
    }
    
    override fun getMenuItems(): List<PluginMenuItem> {
        return listOf(
            PluginMenuItem(
                id = "lookup",
                label = "Look up word",
                icon = "dictionary",
                order = 0
            ),
            PluginMenuItem(
                id = "add_to_vocabulary",
                label = "Add to vocabulary",
                icon = "bookmark",
                order = 1
            )
        )
    }
    
    override fun getScreens(): List<PluginScreen> {
        // In a real implementation, this would return Composable screens
        return emptyList()
    }
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        // When user selects text, offer to look it up
        return context.selectedText?.let { word ->
            if (word.split(" ").size <= 3) {
                // Single word or short phrase - offer lookup
                PluginAction.Navigate("dictionary/lookup?word=$word")
            } else {
                null
            }
        }
    }
    
    override fun getPreferencesScreen(): PluginScreen? {
        // Return preferences screen for configuring dictionary sources
        return null
    }
}
