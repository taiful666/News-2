package com.taiful.tfnewscraft.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.taiful.tfnewscraft.data.model.LiveFeedMetadata
import com.taiful.tfnewscraft.data.model.NewsArticle
import com.taiful.tfnewscraft.data.model.NewspaperSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.zip.GZIPInputStream

data class LiveFeedBundle(
    val articles: List<NewsArticle>,
    val metadata: LiveFeedMetadata
)

class NewsRepository {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

        private const val GITHUB_LIVE_NEWS_RAW =
            "https://raw.githubusercontent.com/taiful666/TFCRFBPOSTMAKERAIMOBILE/live-news/news.json"
        private const val GITHUB_LIVE_NEWS_CDN =
            "https://cdn.jsdelivr.net/gh/taiful666/TFCRFBPOSTMAKERAIMOBILE@live-news/news.json"

        val NEWSPAPERS: List<NewspaperSource> = listOf(
            NewspaperSource("all", "সব পত্রিকা", "All Papers", "", "", "সব", 0xFFC81E2BL,
                listOf("https://www.prothomalo.com/feed/", "https://feeds.bbci.co.uk/bengali/rss.xml", "https://news.google.com/rss?hl=bn&gl=BD&ceid=BD:bn")),
            // --- দৈনিক পত্রিকা (19) ---
            NewspaperSource("prothomalo", "প্রথম আলো", "Prothom Alo", "prothomalo.com", "https://www.prothomalo.com/", "পত্রিকা", 0xFFD71920L, listOf("https://www.prothomalo.com/feed/")),
            NewspaperSource("kalerkantho", "কালের কণ্ঠ", "Kaler Kantho", "kalerkantho.com", "https://www.kalerkantho.com/", "পত্রিকা", 0xFF0F766EL, listOf("https://www.kalerkantho.com/rss.xml")),
            NewspaperSource("jugantor", "যুগান্তর", "Jugantor", "jugantor.com", "https://www.jugantor.com/", "পত্রিকা", 0xFF991B1BL, listOf("https://www.jugantor.com/feed/rss.xml")),
            NewspaperSource("samakal", "সমকাল", "Samakal", "samakal.com", "https://samakal.com/", "পত্রিকা", 0xFF0369A1L, listOf("https://samakal.com/feed")),
            NewspaperSource("ittefaq", "ইত্তেফাক", "Ittefaq", "ittefaq.com.bd", "https://www.ittefaq.com.bd/", "পত্রিকা", 0xFF1E3A8AL, listOf("https://www.ittefaq.com.bd/feed/")),
            NewspaperSource("bdpratidin", "বাংলাদেশ প্রতিদিন", "Bangladesh Pratidin", "bd-pratidin.com", "https://www.bd-pratidin.com/", "পত্রিকা", 0xFFBE123CL, listOf("https://www.bd-pratidin.com/rss.xml")),
            NewspaperSource("kalbela", "কালবেলা", "Kalbela", "kalbela.com", "https://www.kalbela.com/", "পত্রিকা", 0xFFB91C1CL, listOf("https://www.kalbela.com/rss.xml")),
            NewspaperSource("ajkerpatrika", "আজকের পত্রিকা", "Ajker Patrika", "ajkerpatrika.com", "https://www.ajkerpatrika.com/", "পত্রিকা", 0xFF6D28D9L, listOf("https://www.ajkerpatrika.com/feed")),
            NewspaperSource("deshrupantor", "দেশ রূপান্তর", "Desh Rupantor", "deshrupantor.com", "https://www.deshrupantor.com/", "পত্রিকা", 0xFF0E7490L, listOf("https://www.deshrupantor.com/rss.xml")),
            NewspaperSource("nayadiganta", "নয়া দিগন্ত", "Naya Diganta", "dailynayadiganta.com", "https://www.dailynayadiganta.com/", "পত্রিকা", 0xFF15803DL, listOf("https://www.dailynayadiganta.com/rss.xml")),
            NewspaperSource("manabzamin", "মানবজমিন", "Manab Zamin", "mzamin.com", "https://mzamin.com/", "পত্রিকা", 0xFF9F1239L, listOf("https://mzamin.com/rss.xml")),
            NewspaperSource("inqilab", "ইনকিলাব", "Inqilab", "dailyinqilab.com", "https://dailyinqilab.com/", "পত্রিকা", 0xFF047857L, listOf("https://dailyinqilab.com/rss.xml")),
            NewspaperSource("janakantha", "জনকণ্ঠ", "Janakantha", "dailyjanakantha.com", "https://www.dailyjanakantha.com/", "পত্রিকা", 0xFF1D4ED8L),
            NewspaperSource("bhorerkagoj", "ভোরের কাগজ", "Bhorer Kagoj", "bhorerkagoj.com", "https://www.bhorerkagoj.com/", "পত্রিকা", 0xFFC2410CL),
            NewspaperSource("bonikbarta", "বণিক বার্তা", "Bonik Barta", "bonikbarta.com", "https://bonikbarta.com/", "পত্রিকা", 0xFF374151L),
            NewspaperSource("dailystarbn", "ডেইলি স্টার বাংলা", "Daily Star Bangla", "bangla.thedailystar.net", "https://bangla.thedailystar.net/", "পত্রিকা", 0xFF0F172AL, listOf("https://bangla.thedailystar.net/rss.xml")),
            NewspaperSource("dailystar", "দ্য ডেইলি স্টার", "The Daily Star", "thedailystar.net", "https://www.thedailystar.net/", "ইংরেজি", 0xFF334155L, listOf("https://www.thedailystar.net/frontpage/rss.xml")),
            NewspaperSource("dhakatribune", "ঢাকা ট্রিবিউন", "Dhaka Tribune", "dhakatribune.com", "https://www.dhakatribune.com/", "ইংরেজি", 0xFF1E293BL),
            NewspaperSource("tbsnews", "দ্য বিজনেস স্ট্যান্ডার্ড", "TBS News", "tbsnews.net", "https://www.tbsnews.net/", "ইংরেজি", 0xFF0F766EL),
            // --- টিভি চ্যানেল (13) ---
            NewspaperSource("somoy", "সময় টিভি", "Somoy TV", "somoynews.tv", "https://www.somoynews.tv/", "টিভি", 0xFFDC2626L, listOf("https://www.somoynews.tv/rss.xml")),
            NewspaperSource("jamuna", "যমুনা টিভি", "Jamuna TV", "jamuna.tv", "https://jamuna.tv/", "টিভি", 0xFF1D4ED8L, listOf("https://jamuna.tv/feed")),
            NewspaperSource("channel24", "চ্যানেল ২৪", "Channel 24", "channel24bd.tv", "https://www.channel24bd.tv/", "টিভি", 0xFF0E7490L, listOf("https://www.channel24bd.tv/rss.xml")),
            NewspaperSource("ekattor", "একাত্তর টিভি", "Ekattor TV", "ekattor.tv", "https://ekattor.tv/", "টিভি", 0xFFB45309L, listOf("https://ekattor.tv/rss.xml")),
            NewspaperSource("rtv", "আরটিভি", "RTV", "rtvonline.com", "https://www.rtvonline.com/", "টিভি", 0xFF4338CAL, listOf("https://www.rtvonline.com/rss/rss.xml")),
            NewspaperSource("ntv", "এনটিভি", "NTV", "ntvbd.com", "https://www.ntvbd.com/", "টিভি", 0xFF15803DL, listOf("https://www.ntvbd.com/rss.xml")),
            NewspaperSource("channeli", "চ্যানেল আই", "Channel i", "channelionline.com", "https://www.channelionline.com/", "টিভি", 0xFF16A34AL, listOf("https://www.channelionline.com/feed/")),
            NewspaperSource("independent", "ইনডিপেনডেন্ট টিভি", "Independent TV", "independent24.com", "https://www.independent24.com/", "টিভি", 0xFF0284C7L),
            NewspaperSource("dbc", "ডিবিসি নিউজ", "DBC News", "dbcnews.tv", "https://dbcnews.tv/", "টিভি", 0xFF7C3AEDL),
            NewspaperSource("atnnews", "এটিএন নিউজ", "ATN News", "atnnewstv.com", "https://www.atnnewstv.com/", "টিভি", 0xFFEA580CL),
            NewspaperSource("news24", "নিউজ২৪", "News24", "news24bd.tv", "https://www.news24bd.tv/", "টিভি", 0xFFDC2626L),
            NewspaperSource("banglavision", "বাংলাভিশন", "Banglavision", "banglavision.tv", "https://www.banglavision.tv/", "টিভি", 0xFF2563EBL),
            NewspaperSource("ekhon", "এখন টিভি", "Ekhon TV", "ekhon.tv", "https://www.ekhon.tv/", "টিভি", 0xFF0D9488L),
            // --- অনলাইন ও আন্তর্জাতিক (7) ---
            NewspaperSource("bdnews24", "বিডিনিউজ২৪", "bdnews24", "bangla.bdnews24.com", "https://bangla.bdnews24.com/", "অনলাইন", 0xFFB91C1CL, listOf("https://bangla.bdnews24.com/feed")),
            NewspaperSource("dhakapost", "ঢাকা পোস্ট", "Dhaka Post", "dhakapost.com", "https://www.dhakapost.com/", "অনলাইন", 0xFF15803DL, listOf("https://www.dhakapost.com/rss/rss.xml")),
            NewspaperSource("jagonews", "জাগো নিউজ", "Jago News", "jagonews24.com", "https://www.jagonews24.com/", "অনলাইন", 0xFFC2410CL, listOf("https://www.jagonews24.com/rss/rss.xml")),
            NewspaperSource("banglatribune", "বাংলা ট্রিবিউন", "Bangla Tribune", "banglatribune.com", "https://www.banglatribune.com/", "অনলাইন", 0xFF7C2D12L, listOf("https://www.banglatribune.com/feed/")),
            NewspaperSource("banglanews24", "বাংলানিউজ২৪", "Banglanews24", "banglanews24.com", "https://www.banglanews24.com/", "অনলাইন", 0xFF0369A1L, listOf("https://www.banglanews24.com/rss/rss.xml")),
            NewspaperSource("risingbd", "রাইজিংবিডি", "Risingbd", "risingbd.com", "https://www.risingbd.com/", "অনলাইন", 0xFF9333EAL, listOf("https://www.risingbd.com/rss/rss.xml")),
            NewspaperSource("bbcbangla", "বিবিসি বাংলা", "BBC Bangla", "bbc.com/bengali", "https://www.bbc.com/bengali", "আন্তর্জাতিক", 0xFF991B1BL, listOf("https://feeds.bbci.co.uk/bengali/rss.xml"))
        )

        private val PAPER_BY_ID: Map<String, NewspaperSource> = NEWSPAPERS.associateBy { it.id }
    }

    /**
     * Fetches the live 10-minute GitHub Actions RSS aggregation JSON (`live-news/news.json`) first,
     * falling back to direct RSS / Google News Bangladesh if unavailable.
     */
    suspend fun fetchLiveNewsBundle(forceRefresh: Boolean = false): LiveFeedBundle =
        withContext(Dispatchers.IO) {
            val cacheBuster = if (forceRefresh) "?t=${System.currentTimeMillis()}" else "?t=${System.currentTimeMillis() / 60_000}"
            val githubJson = runCatching { httpGetText("$GITHUB_LIVE_NEWS_RAW$cacheBuster", 14_000) }
                .getOrElse {
                    runCatching { httpGetText("$GITHUB_LIVE_NEWS_CDN$cacheBuster", 14_000) }.getOrNull()
                }

            if (!githubJson.isNullOrBlank()) {
                val parsed = runCatching { parseGitHubLiveJson(githubJson) }.getOrNull()
                if (parsed != null && parsed.articles.isNotEmpty()) {
                    return@withContext parsed
                }
            }

            // Fallback: fetch directly from top RSS feeds
            val directItems = fetchDirectRss(NEWSPAPERS.first())
            LiveFeedBundle(
                articles = directItems,
                metadata = LiveFeedMetadata(
                    updatedIso = "",
                    updatedLabelBn = "সরাসরি আরএসএস ফিড",
                    isFromGitHubFeed = false,
                    totalArticles = directItems.size
                )
            )
        }

    suspend fun searchLiveNews(query: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://news.google.com/rss/search?q=$encoded&hl=bn&gl=BD&ceid=BD:bn"
        runCatching { parseRssFeed(url, NEWSPAPERS.first()) }
            .getOrDefault(emptyList())
            .distinctBy { it.title.trim() }
            .take(35)
    }

    private fun parseGitHubLiveJson(rawJson: String): LiveFeedBundle {
        val root = JSONObject(rawJson)
        val updatedIso = root.optString("updated")
        val sourcesObj = root.optJSONObject("sources") ?: return LiveFeedBundle(
            emptyList(),
            LiveFeedMetadata()
        )

        val allItems = mutableListOf<NewsArticle>()
        val keys = sourcesObj.keys()
        while (keys.hasNext()) {
            val sourceId = keys.next()
            val srcNode = sourcesObj.optJSONObject(sourceId) ?: continue
            val itemsArr = srcNode.optJSONArray("items") ?: continue
            val paperName = PAPER_BY_ID[sourceId]?.nameBn ?: if (sourceId == "google") "গুগল নিউজ" else sourceId

            for (i in 0 until itemsArr.length()) {
                val obj = itemsArr.optJSONObject(i) ?: continue
                val title = obj.optString("title").trim()
                val link = obj.optString("link").trim()
                if (title.length < 8 || link.isBlank()) continue
                val img = obj.optString("img").trim()
                val desc = obj.optString("desc").trim()
                val dateIso = obj.optString("date").trim()
                val gsrc = obj.optString("gsrc").trim()
                val displaySource = if (sourceId == "google" && gsrc.isNotBlank()) gsrc else paperName

                allItems.add(
                    NewsArticle(
                        id = link,
                        title = title,
                        summary = desc,
                        body = desc,
                        imageUrl = img,
                        sourceId = sourceId,
                        sourceName = displaySource,
                        sourceUrl = link,
                        publishedIso = dateIso,
                        publishedLabel = formatIsoRelativeBangla(dateIso)
                    )
                )
            }
        }

        // Sort newest first, prioritizing articles with real photos when timestamps tie
        val sorted = allItems
            .distinctBy { it.title.trim() }
            .sortedWith(
                compareByDescending<NewsArticle> { it.publishedIso }
                    .thenByDescending { it.imageUrl.isNotBlank() }
            )

        return LiveFeedBundle(
            articles = sorted,
            metadata = LiveFeedMetadata(
                updatedIso = updatedIso,
                updatedLabelBn = formatIsoRelativeBangla(updatedIso).ifBlank { "এইমাত্র আপডেট" },
                isFromGitHubFeed = true,
                totalArticles = sorted.size
            )
        )
    }

    private suspend fun fetchDirectRss(source: NewspaperSource): List<NewsArticle> = coroutineScope {
        val urls = source.feedUrls.ifEmpty {
            listOf("https://news.google.com/rss?hl=bn&gl=BD&ceid=BD:bn")
        }
        urls.map { feedUrl ->
            async { runCatching { parseRssFeed(feedUrl, source) }.getOrDefault(emptyList()) }
        }.awaitAll()
            .flatten()
            .distinctBy { it.title.trim() }
            .take(40)
    }

    private fun parseRssFeed(feedUrl: String, source: NewspaperSource): List<NewsArticle> {
        val xml = httpGetText(feedUrl, timeoutMs = 12_000)
        val doc = Jsoup.parse(xml, feedUrl, Parser.xmlParser())
        val items = doc.select("item, entry")
        val list = mutableListOf<NewsArticle>()

        for (el in items) {
            val rawTitle = el.selectFirst("title")?.text()?.trim().orEmpty()
            if (rawTitle.length < 8) continue

            val link = el.selectFirst("link")?.let { l ->
                l.attr("href").ifBlank { l.text() }
            }?.trim().orEmpty()

            val rawDesc = el.selectFirst("description, summary, content|encoded")?.text().orEmpty()
            val descDoc = Jsoup.parse(rawDesc)
            val cleanSummary = descDoc.text().trim()

            val imageUrl = extractFeedImage(el, descDoc)
            val pubDate = el.selectFirst("pubDate, updated, published")?.text()?.trim().orEmpty()
            val itemSource = el.selectFirst("source")?.text()?.trim().orEmpty()
                .ifBlank { resolveSourceName(link, source.nameBn) }

            val cleanedTitle = rawTitle.substringBeforeLast(" - ").ifBlank { rawTitle }

            list.add(
                NewsArticle(
                    id = link.ifBlank { UUID.randomUUID().toString() },
                    title = cleanedTitle,
                    summary = cleanSummary.take(400),
                    body = cleanSummary,
                    imageUrl = imageUrl,
                    sourceId = source.id,
                    sourceName = itemSource,
                    sourceUrl = link,
                    publishedLabel = pubDate.substringBefore(" GMT").substringBefore(" +").take(22)
                )
            )
        }
        return list
    }

    private fun extractFeedImage(el: org.jsoup.nodes.Element, descDoc: org.jsoup.nodes.Document): String {
        val mediaContent = el.selectFirst("media|content, media|thumbnail, enclosure")
        val attrUrl = mediaContent?.attr("url")?.trim().orEmpty()
        if (attrUrl.startsWith("http")) return attrUrl
        val imgInDesc = descDoc.selectFirst("img")?.attr("src")?.trim().orEmpty()
        if (imgInDesc.startsWith("http")) return imgInDesc
        return ""
    }

    /**
     * Extracts clean news content from any newspaper article URL or video link (YouTube/Facebook).
     */
    suspend fun fetchArticleFromUrl(url: String, titleHint: String = ""): NewsArticle =
        withContext(Dispatchers.IO) {
            val normalizedUrl = url.trim().let {
                if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it"
            }

            // Support YouTube video links via oEmbed for clean title & thumbnail
            if (normalizedUrl.contains("youtube.com", true) || normalizedUrl.contains("youtu.be", true)) {
                val yt = runCatching { fetchYouTubeOembed(normalizedUrl) }.getOrNull()
                if (yt != null) return@withContext yt
            }

            val html = httpGetText(normalizedUrl, timeoutMs = 15_000)
            val doc = Jsoup.parse(html, normalizedUrl)

            var title = ""
            var imageUrl = ""
            var description = ""
            var bodyText = ""
            var published = ""

            doc.select("script[type=application/ld+json]").forEach { script ->
                runCatching {
                    val raw = script.data().trim()
                    if (raw.startsWith("{")) {
                        extractFromJsonLd(JSONObject(raw)) { t, i, d, b, p ->
                            if (title.isBlank() && t.isNotBlank()) title = t
                            if (imageUrl.isBlank() && i.isNotBlank()) imageUrl = i
                            if (description.isBlank() && d.isNotBlank()) description = d
                            if (bodyText.isBlank() && b.isNotBlank()) bodyText = b
                            if (published.isBlank() && p.isNotBlank()) published = p
                        }
                    } else if (raw.startsWith("[")) {
                        val arr = JSONArray(raw)
                        for (idx in 0 until arr.length()) {
                            val obj = arr.optJSONObject(idx) ?: continue
                            extractFromJsonLd(obj) { t, i, d, b, p ->
                                if (title.isBlank() && t.isNotBlank()) title = t
                                if (imageUrl.isBlank() && i.isNotBlank()) imageUrl = i
                                if (description.isBlank() && d.isNotBlank()) description = d
                                if (bodyText.isBlank() && b.isNotBlank()) bodyText = b
                                if (published.isBlank() && p.isNotBlank()) published = p
                            }
                        }
                    }
                }
            }

            if (title.isBlank()) {
                title = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
                    ?: doc.selectFirst("h1")?.text()?.trim()
                    ?: doc.title().trim()
                    ?: titleHint
            }
            if (imageUrl.isBlank()) {
                imageUrl = doc.selectFirst("meta[property=og:image]")?.attr("abs:content")?.trim()
                    ?: doc.selectFirst("meta[name=twitter:image]")?.attr("abs:content")?.trim()
                    ?: doc.selectFirst("article img")?.attr("abs:src")?.trim()
                    ?: ""
            }
            if (description.isBlank()) {
                description = doc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
                    ?: doc.selectFirst("meta[name=description]")?.attr("content")?.trim()
                    ?: ""
            }

            if (bodyText.length < 180) {
                doc.select("script, style, nav, footer, header, aside, form, iframe, .ad, .advertisement").remove()
                val container = doc.selectFirst("article, main, [itemprop=articleBody], .story-content, .article-content") ?: doc.body()
                val paragraphs = container?.select("p")
                    ?.map { it.text().trim() }
                    ?.filter { it.length >= 25 }
                    ?.distinct()
                    .orEmpty()
                if (paragraphs.isNotEmpty()) {
                    bodyText = paragraphs.joinToString("\n\n")
                }
            }

            val siteName = doc.selectFirst("meta[property=og:site_name]")?.attr("content")?.trim()
                .orEmpty()
                .ifBlank { resolveSourceName(normalizedUrl, "") }

            NewsArticle(
                id = normalizedUrl,
                title = title.ifBlank { titleHint.ifBlank { "শিরোনামহীন সংবাদ" } },
                summary = description.ifBlank { bodyText.take(260) },
                body = bodyText.ifBlank { description },
                imageUrl = imageUrl,
                sourceName = siteName,
                sourceUrl = normalizedUrl,
                publishedIso = published,
                publishedLabel = formatIsoRelativeBangla(published).ifBlank { "সংগৃহীত খবর" }
            )
        }

    private fun fetchYouTubeOembed(videoUrl: String): NewsArticle {
        val oembedUrl = "https://www.youtube.com/oembed?url=${URLEncoder.encode(videoUrl, "UTF-8")}&format=json"
        val raw = httpGetText(oembedUrl, 10_000)
        val json = JSONObject(raw)
        val title = json.optString("title").trim()
        val author = json.optString("author_name").trim().ifBlank { "YouTube ভিডিও" }
        val thumb = json.optString("thumbnail_url").trim()
        return NewsArticle(
            id = videoUrl,
            title = title.ifBlank { "ভিডিও সংবাদ" },
            summary = "ভিডিও চ্যানেল: $author",
            body = "$title\n\nভিডিও সূত্র: $author ($videoUrl)",
            imageUrl = thumb,
            sourceName = author,
            sourceUrl = videoUrl,
            publishedLabel = "ভিডিও থেকে সংগৃহীত"
        )
    }

    private fun extractFromJsonLd(
        obj: JSONObject,
        onFound: (title: String, image: String, desc: String, body: String, date: String) -> Unit
    ) {
        val graph = obj.optJSONArray("@graph")
        if (graph != null) {
            for (i in 0 until graph.length()) {
                graph.optJSONObject(i)?.let { extractFromJsonLd(it, onFound) }
            }
        }
        val type = obj.optString("@type")
        if (type.contains("Article", true) || type.contains("Posting", true) || type.contains("Report", true) || type.contains("Video", true)) {
            val headline = obj.optString("headline").ifBlank { obj.optString("name") }
            val desc = obj.optString("description")
            val body = obj.optString("articleBody")
            val date = obj.optString("datePublished")
            val img = when (val rawImg = obj.opt("image") ?: obj.opt("thumbnailUrl")) {
                is String -> rawImg
                is JSONObject -> rawImg.optString("url")
                is JSONArray -> {
                    when (val first = rawImg.opt(0)) {
                        is String -> first
                        is JSONObject -> first.optString("url")
                        else -> ""
                    }
                }
                else -> ""
            }
            onFound(headline, img, desc, body, date)
        }
    }

    suspend fun loadBitmapFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank() || !url.startsWith("http", true)) return@withContext null
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
            }
            try {
                if (conn.responseCode !in 200..399) return@withContext null
                val bytes = conn.inputStream.use { it.readBytes() }
                decodeBoundedBitmap(bytes, maxDimension = 1440)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun decodeBoundedBitmap(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        )
    }

    private fun httpGetText(urlStr: String, timeoutMs: Int): String {
        var current = urlStr
        repeat(5) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Language", "bn-BD,bn;q=0.9,en;q=0.8")
                setRequestProperty("Accept-Encoding", "gzip")
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                val code = conn.responseCode
                if (code in setOf(301, 302, 303, 307, 308)) {
                    val loc = conn.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        current = URL(URL(current), loc).toString()
                        return@repeat
                    }
                }
                if (code !in 200..399) throw IllegalStateException("HTTP $code")
                val raw = conn.inputStream
                val stream = if (conn.contentEncoding.equals("gzip", true)) GZIPInputStream(raw) else raw
                val out = ByteArrayOutputStream()
                stream.use { input ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                    }
                }
                return out.toString(Charsets.UTF_8.name())
            } finally {
                conn.disconnect()
            }
        }
        throw IllegalStateException("Too many redirects")
    }

    private fun resolveSourceName(url: String, fallback: String): String {
        val host = runCatching { URL(url).host.lowercase().removePrefix("www.") }.getOrDefault("")
        return NEWSPAPERS.firstOrNull { it.domain.isNotBlank() && host.contains(it.domain.substringBefore("/")) }?.nameBn
            ?: when {
                host.contains("youtube") || host.contains("youtu.be") -> "YouTube"
                host.contains("facebook") || host.contains("fb.watch") -> "Facebook ভিডিও"
                fallback.isNotBlank() && fallback != "সব পত্রিকা" -> fallback
                host.isNotBlank() -> host
                else -> "সংবাদ ডেস্ক"
            }
    }

    private fun formatIsoRelativeBangla(iso: String): String {
        if (iso.isBlank()) return ""
        val millis = parseIsoMillis(iso) ?: return ""
        val diffMin = ((System.currentTimeMillis() - millis) / 60_000L).coerceAtLeast(0L)
        return when {
            diffMin < 2 -> "এইমাত্র"
            diffMin < 60 -> "${toBnDigits(diffMin)} মি. আগে"
            diffMin < 24 * 60 -> "${toBnDigits(diffMin / 60)} ঘন্টা আগে"
            else -> "${toBnDigits(diffMin / (24 * 60))} দিন আগে"
        }
    }

    private fun parseIsoMillis(iso: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        for (p in patterns) {
            runCatching {
                val sdf = SimpleDateFormat(p, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                sdf.parse(iso)?.time
            }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun toBnDigits(n: Long): String {
        val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return n.toString().map { if (it in '0'..'9') bn[it - '0'] else it }.joinToString("")
    }
}
