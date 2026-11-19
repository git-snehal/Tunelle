package com.tunelle.data

import com.tunelle.data.model.BrowseType
import com.tunelle.data.model.SearchFilter
import com.tunelle.data.model.SearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * The languages a listener can say they listen to, on the welcome's first step.
 * Each finds its artists through YouTube Music's own artist search, with a couple
 * of phrasings, since no single query covers a whole language's music.
 */
enum class MusicLanguage(val label: String, val native: String, internal val queries: List<String>) {
    HINDI("Hindi", "हिन्दी", listOf("Bollywood singer", "Hindi songs artist")),
    ENGLISH("English", "English", listOf("Ed Sheeran Taylor Swift", "Billboard pop")),
    PUNJABI("Punjabi", "ਪੰਜਾਬੀ", listOf("Punjabi singer", "Punjabi songs artist")),
    TAMIL("Tamil", "தமிழ்", listOf("Tamil singer", "Tamil music composer")),
    TELUGU("Telugu", "తెలుగు", listOf("Telugu singer", "Telugu music composer")),
    BENGALI("Bengali", "বাংলা", listOf("Bengali singer", "Bangla band")),
    MARATHI("Marathi", "मराठी", listOf("Marathi singer", "Marathi songs artist")),
    KANNADA("Kannada", "ಕನ್ನಡ", listOf("Kannada singer", "Kannada music composer")),
    MALAYALAM("Malayalam", "മലയാളം", listOf("Malayalam singer", "Malayalam music composer")),
    GUJARATI("Gujarati", "ગુજરાતી", listOf("Gujarati singer", "Gujarati garba artist")),
    BHOJPURI("Bhojpuri", "भोजपुरी", listOf("Bhojpuri singer", "Bhojpuri songs artist")),
    HARYANVI("Haryanvi", "हरियाणवी", listOf("Haryanvi singer", "Haryanvi songs artist")),
    URDU("Urdu", "اردو", listOf("Pakistani singer", "Coke Studio Pakistan artist")),
    SPANISH("Spanish", "Español", listOf("cantante latino", "reggaeton artist")),
    KOREAN("Korean", "한국어", listOf("K-pop group", "Korean singer")),
    JAPANESE("Japanese", "日本語", listOf("J-pop singer", "anime song artist")),
    ARABIC("Arabic", "العربية", listOf("Arabic singer", "مطرب عربي")),
    FRENCH("French", "Français", listOf("chanteur français", "French pop artist")),
    PORTUGUESE("Portuguese", "Português", listOf("cantor brasileiro", "sertanejo artist")),
    INDONESIAN("Indonesian", "Bahasa Indonesia", listOf("penyanyi Indonesia", "Indonesian band")),
    ;

    companion object {
        private val NOT_ARTISTS = Regex(
            "\\b(singer|songs?|music|records|official|lyrics?|status|hits|jukebox|topic|channel|studios?|films?|tv|cantante|cantor|penyanyi)\\b",
            RegexOption.IGNORE_CASE,
        )

        fun of(name: String): MusicLanguage? = entries.firstOrNull { it.name == name }

        /**
         * Artists who sing in [languages], the first language's first, then taking
         * one from each in turn so a mix of languages shows a mix of artists.
         */
        suspend fun artists(languages: List<MusicLanguage>, perLanguage: Int = 15): List<ExploreFeed.Artist> = coroutineScope {
            val lists = languages.map { lang ->
                async {
                    lang.queries.map { q ->
                        async {
                            runCatching {
                                MusicRepository.search(q, SearchFilter.ARTISTS)
                                    .mapNotNull { (it as? SearchResult.Browse)?.item?.takeIf { b -> b.type == BrowseType.ARTIST && b.browseId.startsWith("UC") } }
                                    // A search for "Punjabi singer" also finds channels named after the
                                    // phrase itself, and labels; neither is an artist to follow.
                                    .filterNot { b -> NOT_ARTISTS.containsMatchIn(b.title) }
                                    .map { ExploreFeed.Artist(it.browseId, it.title, it.thumbnailUrl) }
                            }.getOrDefault(emptyList())
                        }
                    }.awaitAll().let { ExploreFeed.weave(it) }.distinctBy { it.browseId }.take(perLanguage)
                }
            }.awaitAll()
            ExploreFeed.weave(lists).distinctBy { it.browseId }
        }
    }
}
