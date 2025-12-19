package io.github.ireaderorg.plugins.tachiloader

import ireader.plugin.api.tachi.*
import java.lang.reflect.Method
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps a Tachi source using reflection.
 * Adapts the actual Tachiyomi source classes to our TachiCatalogueSource interface.
 */
class ReflectiveTachiSourceWrapper(
    override val underlyingSource: Any
) : TachiSourceWrapper {
    
    private val sourceClass = underlyingSource::class.java
    
    // Cache reflected methods
    private val idGetter: Method? = findGetter("getId")
    private val nameGetter: Method? = findGetter("getName")
    private val langGetter: Method? = findGetter("getLang")
    private val baseUrlGetter: Method? = findGetter("getBaseUrl")
    private val supportsLatestGetter: Method? = findGetter("getSupportsLatest")
    
    private val getPopularMangaMethod: Method? = findMethod("getPopularManga", Int::class.java)
    private val getSearchMangaMethod: Method? = findMethod("getSearchManga", Int::class.java, String::class.java, Any::class.java)
    private val getLatestUpdatesMethod: Method? = findMethod("getLatestUpdates", Int::class.java)
    private val getMangaDetailsMethod: Method? = findMethod("getMangaDetails", Any::class.java)
    private val getChapterListMethod: Method? = findMethod("getChapterList", Any::class.java)
    private val getPageListMethod: Method? = findMethod("getPageList", Any::class.java)
    private val getFilterListMethod: Method? = findMethod("getFilterList")
    
    override val id: Long
        get() = idGetter?.invoke(underlyingSource) as? Long ?: 0L
    
    override val name: String
        get() = nameGetter?.invoke(underlyingSource) as? String ?: "Unknown"
    
    override val lang: String
        get() = langGetter?.invoke(underlyingSource) as? String ?: ""
    
    override val supportsLatest: Boolean
        get() = supportsLatestGetter?.invoke(underlyingSource) as? Boolean ?: false
    
    override suspend fun getMangaDetails(manga: TachiManga): TachiManga {
        return withContext(Dispatchers.IO) {
            val method = getMangaDetailsMethod
                ?: throw UnsupportedOperationException("getMangaDetails not available")
            
            val tachiManga = createTachiManga(manga)
            val result = invokeSuspend(method, tachiManga)
            convertToTachiManga(result)
        }
    }
    
    override suspend fun getChapterList(manga: TachiManga): List<TachiChapter> {
        return withContext(Dispatchers.IO) {
            val method = getChapterListMethod
                ?: throw UnsupportedOperationException("getChapterList not available")
            
            val tachiManga = createTachiManga(manga)
            val result = invokeSuspend(method, tachiManga)
            
            @Suppress("UNCHECKED_CAST")
            (result as? List<Any>)?.map { convertToTachiChapter(it) } ?: emptyList()
        }
    }
    
    override suspend fun getPageList(chapter: TachiChapter): List<TachiPage> {
        return withContext(Dispatchers.IO) {
            val method = getPageListMethod
                ?: throw UnsupportedOperationException("getPageList not available")
            
            val tachiChapter = createTachiChapter(chapter)
            val result = invokeSuspend(method, tachiChapter)
            
            @Suppress("UNCHECKED_CAST")
            (result as? List<Any>)?.map { convertToTachiPage(it) } ?: emptyList()
        }
    }
    
    override suspend fun getPopularManga(page: Int): TachiMangasPage {
        return withContext(Dispatchers.IO) {
            val method = getPopularMangaMethod
                ?: throw UnsupportedOperationException("getPopularManga not available")
            
            val result = invokeSuspend(method, page)
            convertToMangasPage(result)
        }
    }
    
    override suspend fun getSearchManga(page: Int, query: String, filters: TachiFilterList): TachiMangasPage {
        return withContext(Dispatchers.IO) {
            val method = getSearchMangaMethod
                ?: throw UnsupportedOperationException("getSearchManga not available")
            
            val tachiFilters = createFilterList(filters)
            val result = invokeSuspend(method, page, query, tachiFilters)
            convertToMangasPage(result)
        }
    }
    
    override suspend fun getLatestUpdates(page: Int): TachiMangasPage {
        return withContext(Dispatchers.IO) {
            val method = getLatestUpdatesMethod
                ?: throw UnsupportedOperationException("getLatestUpdates not available")
            
            val result = invokeSuspend(method, page)
            convertToMangasPage(result)
        }
    }
    
    override fun getFilterList(): TachiFilterList {
        val method = getFilterListMethod ?: return TachiFilterList()
        val result = method.invoke(underlyingSource)
        return convertToTachiFilterList(result)
    }
    
    // Helper methods
    
    private fun findGetter(name: String): Method? {
        return try {
            sourceClass.getMethod(name)
        } catch (e: NoSuchMethodException) {
            null
        }
    }
    
    private fun findMethod(name: String, vararg paramTypes: Class<*>): Method? {
        return try {
            // Try exact match first
            sourceClass.getMethod(name, *paramTypes)
        } catch (e: NoSuchMethodException) {
            // Try finding by name only
            sourceClass.methods.find { it.name == name }
        }
    }
    
    private suspend fun invokeSuspend(method: Method, vararg args: Any?): Any? {
        // Handle both suspend and regular methods
        return try {
            val result = method.invoke(underlyingSource, *args)
            
            // If result is a Continuation-based suspend function result, await it
            if (result != null && result::class.java.name.contains("Continuation")) {
                // This is a coroutine, need to handle differently
                result
            } else {
                result
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to invoke ${method.name}", e)
        }
    }
    
    // Conversion methods
    
    private fun createTachiManga(manga: TachiManga): Any {
        // Create SManga instance using reflection
        val smangaClass = sourceClass.classLoader?.loadClass("eu.kanade.tachiyomi.source.model.SManga")
        val createMethod = smangaClass?.getMethod("create")
        val instance = createMethod?.invoke(null) ?: throw RuntimeException("Cannot create SManga")
        
        // Set properties
        setProperty(instance, "url", manga.url)
        setProperty(instance, "title", manga.title)
        setProperty(instance, "artist", manga.artist)
        setProperty(instance, "author", manga.author)
        setProperty(instance, "description", manga.description)
        setProperty(instance, "genre", manga.genre)
        setProperty(instance, "status", manga.status)
        setProperty(instance, "thumbnail_url", manga.thumbnailUrl)
        setProperty(instance, "initialized", manga.initialized)
        
        return instance
    }
    
    private fun createTachiChapter(chapter: TachiChapter): Any {
        val schapterClass = sourceClass.classLoader?.loadClass("eu.kanade.tachiyomi.source.model.SChapter")
        val createMethod = schapterClass?.getMethod("create")
        val instance = createMethod?.invoke(null) ?: throw RuntimeException("Cannot create SChapter")
        
        setProperty(instance, "url", chapter.url)
        setProperty(instance, "name", chapter.name)
        setProperty(instance, "date_upload", chapter.dateUpload)
        setProperty(instance, "chapter_number", chapter.chapterNumber)
        setProperty(instance, "scanlator", chapter.scanlator)
        
        return instance
    }
    
    private fun createFilterList(filters: TachiFilterList): Any {
        val filterListClass = sourceClass.classLoader?.loadClass("eu.kanade.tachiyomi.source.model.FilterList")
        val constructor = filterListClass?.getConstructor(List::class.java)
        return constructor?.newInstance(emptyList<Any>()) ?: throw RuntimeException("Cannot create FilterList")
    }
    
    private fun convertToTachiManga(obj: Any?): TachiManga {
        if (obj == null) return TachiManga(url = "", title = "")
        
        return TachiManga(
            url = getProperty(obj, "url") as? String ?: "",
            title = getProperty(obj, "title") as? String ?: "",
            artist = getProperty(obj, "artist") as? String,
            author = getProperty(obj, "author") as? String,
            description = getProperty(obj, "description") as? String,
            genre = getProperty(obj, "genre") as? String,
            status = getProperty(obj, "status") as? Int ?: 0,
            thumbnailUrl = getProperty(obj, "thumbnail_url") as? String,
            initialized = getProperty(obj, "initialized") as? Boolean ?: false
        )
    }
    
    private fun convertToTachiChapter(obj: Any): TachiChapter {
        return TachiChapter(
            url = getProperty(obj, "url") as? String ?: "",
            name = getProperty(obj, "name") as? String ?: "",
            dateUpload = getProperty(obj, "date_upload") as? Long ?: 0L,
            chapterNumber = getProperty(obj, "chapter_number") as? Float ?: -1f,
            scanlator = getProperty(obj, "scanlator") as? String
        )
    }
    
    private fun convertToTachiPage(obj: Any): TachiPage {
        return TachiPage(
            index = getProperty(obj, "index") as? Int ?: 0,
            url = getProperty(obj, "url") as? String ?: "",
            imageUrl = getProperty(obj, "imageUrl") as? String
        )
    }
    
    private fun convertToMangasPage(obj: Any?): TachiMangasPage {
        if (obj == null) return TachiMangasPage(emptyList(), false)
        
        @Suppress("UNCHECKED_CAST")
        val mangas = getProperty(obj, "mangas") as? List<Any> ?: emptyList()
        val hasNextPage = getProperty(obj, "hasNextPage") as? Boolean ?: false
        
        return TachiMangasPage(
            mangas = mangas.map { convertToTachiManga(it) },
            hasNextPage = hasNextPage
        )
    }
    
    private fun convertToTachiFilterList(obj: Any?): TachiFilterList {
        // Simplified - just return empty for now
        // Full implementation would convert each filter type
        return TachiFilterList()
    }
    
    private fun setProperty(obj: Any, name: String, value: Any?) {
        try {
            val setter = obj::class.java.getMethod("set${name.replaceFirstChar { it.uppercase() }}", value?.javaClass ?: Any::class.java)
            setter.invoke(obj, value)
        } catch (e: NoSuchMethodException) {
            // Try field access
            try {
                val field = obj::class.java.getDeclaredField(name)
                field.isAccessible = true
                field.set(obj, value)
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }
    
    private fun getProperty(obj: Any, name: String): Any? {
        return try {
            val getter = obj::class.java.getMethod("get${name.replaceFirstChar { it.uppercase() }}")
            getter.invoke(obj)
        } catch (e: NoSuchMethodException) {
            try {
                val field = obj::class.java.getDeclaredField(name)
                field.isAccessible = true
                field.get(obj)
            } catch (e2: Exception) {
                null
            }
        }
    }
}
