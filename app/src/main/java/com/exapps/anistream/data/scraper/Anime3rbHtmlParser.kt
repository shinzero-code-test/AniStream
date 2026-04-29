package com.exapps.anistream.data.scraper

import com.exapps.anistream.domain.model.AnimeCard
import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.model.DownloadLink
import com.exapps.anistream.domain.model.EpisodeCard
import com.exapps.anistream.domain.model.EpisodeItem
import com.exapps.anistream.domain.model.EpisodeStream
import com.exapps.anistream.domain.model.ExternalLink
import com.exapps.anistream.domain.model.HomeFeed
import com.exapps.anistream.domain.model.PaginatedTitles
import com.exapps.anistream.domain.model.StreamType
import com.exapps.anistream.domain.model.TrailerItem
import com.exapps.anistream.domain.model.VideoSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Anime3rbHtmlParser @Inject constructor() {

    fun parseHome(document: Document): HomeFeed {
        val sections = document.select("section")

        val featuredSection = sections.firstOrNull { section ->
            section.select("h2, h3").any { it.text().contains("الأنميات المثبتة") }
        }
        val latestEpisodesSection = sections.firstOrNull { section ->
            section.select("h2, h3").any { it.text().contains("أحدث الحلقات") }
        }
        val latestTitlesSection = sections.firstOrNull { section ->
            section.select("h2, h3").any { it.text().contains("آخر الأنميات المضافة") }
        }

        val featuredEpisodes = featuredSection
            ?.select("ul.glide__slides > li:not(.glide__slide--clone) a.video-card")
            ?.map(::parseVideoCard)
            .orEmpty()

        val latestEpisodes = latestEpisodesSection
            ?.select("#videos a.video-card")
            ?.map(::parseVideoCard)
            .orEmpty()

        val latestTitles = latestTitlesSection
            ?.select("ul.glide__slides > li:not(.glide__slide--clone) div.title-card")
            ?.map(::parseTitleCard)
            .orEmpty()

        return HomeFeed(
            featuredEpisodes = featuredEpisodes.distinctBy { it.episodeUrl },
            latestEpisodes = latestEpisodes.distinctBy { it.episodeUrl },
            latestTitles = latestTitles.distinctBy { it.slug },
        )
    }

    fun parseCatalog(document: Document, page: Int): PaginatedTitles {
        val titleCards = document.select("div.titles-list div.title-card")
            .map(::parseTitleCard)
        val items = titleCards.ifEmpty { parseStructuredDataTitleList(document) }

        return PaginatedTitles(
            items = items.distinctBy { it.slug },
            currentPage = page,
            hasNextPage = document.hasNextPage(),
        )
    }

    fun parseSearch(document: Document, page: Int): PaginatedTitles {
        val titleCards = document.select("div.titles-list div.title-card")
            .map(::parseTitleCard)
        val items = titleCards
            .ifEmpty { parseStructuredDataTitleList(document) }
            .ifEmpty { document.select("a.simple-title-card[href*=/titles/]").map(::parseSimpleTitleCard) }

        return PaginatedTitles(
            items = items.distinctBy { it.slug },
            currentPage = page,
            hasNextPage = document.hasNextPage(),
        )
    }

    fun parseDetails(document: Document, slug: String): AnimeDetails {
        val structuredTitle = document.structuredDataObjects()
            .firstOrNull { it.typeName() in TITLE_SCHEMA_TYPES }
        val aggregateRating = structuredTitle?.get("aggregateRating")?.jsonObjectOrNull()
        val titleHeader = document.selectFirst("h1.text-2xl")
        val title = titleHeader?.selectFirst("span[dir=ltr]")?.text()?.trim()
            ?: structuredTitle?.stringValue("name")?.cleanSiteSuffix()
            ?: document.selectFirst("meta[property=og:title]")?.attr("content")?.cleanSiteSuffix()
            ?: ""
        val typeLabel = titleHeader
            ?.select("span")
            ?.getOrNull(1)
            ?.text()
            ?.removePrefix("(")
            ?.removeSuffix(")")
            ?.trim()

        val posterUrl = document.selectFirst("section img[alt^=بوستر]")?.bestImageUrl()
            ?: structuredTitle?.stringValue("image")
            ?: document.selectFirst("meta[property=og:image]")?.attr("content")
            ?: ""
        val contentRoot = titleHeader?.parent()?.parent() ?: document
        val detailsTable = document.select("table.leading-loose").firstOrNull()
        val metrics = document.select("div.rounded-lg.border")

        val synopsisContainer = contentRoot.selectFirst("div[x-data*=summary]")
        val fullSynopsis = synopsisContainer
            ?.children()
            ?.getOrNull(0)
            ?.select("p")
            ?.eachText()
            ?.filter { it.isNotBlank() }
            ?.joinToString("\n\n")
            .orEmpty()
        val summary = synopsisContainer
            ?.children()
            ?.getOrNull(1)
            ?.select("p")
            ?.eachText()
            ?.filter { it.isNotBlank() }
            ?.joinToString("\n\n")

        val alternateTitles = contentRoot
            .findLabeledBlock("أسماء أخرى")
            ?.select("h2")
            ?.eachText()
            .orEmpty()
            .ifEmpty { structuredTitle?.stringListValue("alternateName").orEmpty() }

        val externalLinks = contentRoot
            .findLabeledBlock("المصادر")
            ?.select("a.btn.btn-md.btn-light")
            ?.mapNotNull { link ->
                val url = link.absUrl("href")
                if (url.isBlank()) return@mapNotNull null
                ExternalLink(label = link.text().trim(), url = url)
            }
            .orEmpty()

        val trailers = parseTrailers(document)

        val episodes = document
            .select("div.video-list a[href*=/episode/]")
            .map(::parseEpisodeItem)
            .distinctBy { it.episodeUrl }

        val relatedSection = document.select("div.glide").firstOrNull { glide ->
            glide.select("h3.text-lg").any { it.text().contains("أنميات مشابهة مقترحة") }
        }
        val relatedTitles = relatedSection
            ?.select("ul.glide__slides > li div.title-card")
            ?.map(::parseTitleCard)
            ?.distinctBy { it.slug }
            .orEmpty()

        return AnimeDetails(
            slug = slug,
            title = title,
            typeLabel = typeLabel,
            posterUrl = posterUrl,
            status = detailsTable.valueForLabel("الحالة:"),
            releaseSeason = detailsTable.valueForLabel("إصدار:"),
            studio = detailsTable.valueForLabel("الاستديو:"),
            author = detailsTable.valueForLabel("المؤلف:"),
            ageRating = metrics.metricValue("التصنيف العمري"),
            score = metrics.metricValue("التقييم")
                ?: aggregateRating?.stringValue("ratingValue")
                ?: aggregateRating?.doubleValue("ratingValue")?.trimTrailingZero(),
            ratingCount = aggregateRating?.intValue("ratingCount"),
            publishedAt = structuredTitle?.stringValue("datePublished")?.substringBefore("T"),
            episodeCount = metrics.metricValue("الحلقات")?.toIntOrNull()
                ?: structuredTitle?.intValue("numberOfEpisodes")
                ?: episodes.size.takeIf { it > 0 },
            synopsis = fullSynopsis.ifBlank {
                structuredTitle?.stringValue("description")
                    ?: document.selectFirst("meta[name=description]")?.attr("content")
                    ?: ""
            },
            summary = summary,
            alternateTitles = alternateTitles,
            genres = contentRoot.select("a[href*=/genre/]").map { it.text().trim() }.distinct()
                .ifEmpty { structuredTitle?.stringListValue("genre").orEmpty() },
            episodes = episodes,
            trailers = trailers,
            externalLinks = externalLinks,
            relatedTitles = relatedTitles,
        )
    }

    fun parseEpisodePage(document: Document, titleSlug: String, episodeNumber: Int): EpisodeStream {
        val structuredVideos = document.structuredDataObjects()
            .filter { it.typeName() == "VideoObject" }
        val playerComponent = document.allElements.firstOrNull { element ->
            element.hasAttr("wire:snapshot") && element.attr("wire:snapshot").contains("video_url")
        }
        val titleLink = document.selectFirst("h1 a[href*=/titles/]")
        val animeTitle = titleLink
            ?.text()
            ?.substringBefore("الحلقة")
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: document.selectFirst("script[type=application/ld+json]")
                ?.data()
                ?.substringAfter("\"isPartOf\"", missingDelimiterValue = "")
                ?.substringAfter("\"name\":\"", missingDelimiterValue = "")
                ?.substringBefore(" - Anime3rb", missingDelimiterValue = "")
                ?.cleanSiteSuffix()
            .orEmpty()
        val posterUrl = document.selectFirst("div.video-list img[alt^=بوستر]")?.bestImageUrl()
            ?: document.selectFirst("meta[property=og:image]")?.attr("content")
            ?: structuredVideos.firstNotNullOfOrNull { it.stringValue("thumbnailUrl") }
            ?: ""

        val snapshot = playerComponent?.attr("wire:snapshot").orEmpty()

        val decodedSnapshot = Parser.unescapeEntities(snapshot, false)
        val directPlayerUrl = decodedSnapshot.livewireValue("video_url")
            ?: document.selectFirst("iframe")?.absUrl("src")
            ?: structuredVideos.firstNotNullOfOrNull { it.stringValue("embedUrl") }?.htmlDecoded()
        val activeSourceId = decodedSnapshot.livewireValue("video_source")
        val csrfToken = document.selectFirst("meta[name=csrf-token]")?.attr("content")
        val iframeUrl = document.selectFirst("iframe")?.absUrl("src") ?: directPlayerUrl
        val episodeTitle = document.selectFirst("h2.inline.text-lg.font-light")?.text()?.trim()
            ?: structuredVideos.firstNotNullOfOrNull { it.stringValue("description") }
                ?.substringAfter(" بعنوان ", missingDelimiterValue = "")
                ?.substringBefore(" - Anime3rb", missingDelimiterValue = "")
                ?.takeIf { it.isNotBlank() }
        val views = decodedSnapshot.livewireValue("views")?.toLongOrNull()
            ?: structuredVideos.firstNotNullOfOrNull { video ->
                video["interactionStatistic"]
                    ?.jsonObjectOrNull()
                    ?.intValue("userInteractionCount")
            }?.toLong()
        val batchDownloadUrl = document.selectFirst("a[href*=/titles/$titleSlug/download]")?.absUrl("href")

        val nextEpisodeNumber = document
            .selectFirst("div.video-list a.active")
            ?.nextElementSibling()
            ?.absUrl("href")
            ?.substringAfterLast("/")
            ?.toIntOrNull()

        val availableSources = document.select("button[data-video-source]").mapIndexed { index, button ->
            val label = button.selectFirst("span[title]")?.text()?.trim()
                ?: button.text().trim()
            VideoSource(
                id = button.attr("data-video-source").ifBlank { "source-$index" },
                label = label,
                url = directPlayerUrl.orEmpty(),
                type = StreamType.PLAYER_PAGE,
                serverId = button.attr("data-video-source").ifBlank { null },
            )
        }
        val structuredSources = structuredVideos.mapIndexedNotNull { index, video ->
            val url = video.stringValue("embedUrl")?.htmlDecoded() ?: return@mapIndexedNotNull null
            VideoSource(
                id = "structured-video-$index",
                label = video.stringValue("name")?.cleanSiteSuffix() ?: "Embedded player",
                url = url,
                type = StreamType.PLAYER_PAGE,
            )
        }

        val downloadLinks = document.select("a[href*=/download/]").mapNotNull { link ->
            val url = link.absUrl("href")
            if (url.isBlank()) return@mapNotNull null
            val quality = link.parent()?.selectFirst("label")?.text()?.trim().orEmpty()
            DownloadLink(
                qualityLabel = quality.ifBlank { link.text().trim() },
                url = url,
            )
        }.filterNot { it.url == batchDownloadUrl }

        return EpisodeStream(
            titleSlug = titleSlug,
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            refererUrl = document.location(),
            iframeUrl = iframeUrl,
            playbackUrl = directPlayerUrl,
            availableSources = when {
                availableSources.isNotEmpty() -> availableSources
                structuredSources.isNotEmpty() -> structuredSources
                !directPlayerUrl.isNullOrBlank() -> listOf(
                    VideoSource(
                        id = activeSourceId ?: "default",
                        label = "Embedded player",
                        url = directPlayerUrl,
                        type = StreamType.PLAYER_PAGE,
                    ),
                )

                else -> emptyList()
            },
            downloadLinks = downloadLinks,
            views = views,
            nextEpisodeNumber = nextEpisodeNumber,
            batchDownloadUrl = batchDownloadUrl,
            csrfToken = csrfToken,
            livewireComponentId = playerComponent?.attr("wire:id"),
            livewireSnapshot = decodedSnapshot.ifBlank { null },
            selectedServerId = activeSourceId,
        )
    }

    private fun parseTrailers(document: Document): List<TrailerItem> {
        val iframeTrailers = document.select("div#trailers iframe[data-src]").mapIndexed { index, iframe ->
            TrailerItem(
                title = "Trailer ${index + 1}",
                embedUrl = iframe.absUrl("data-src").ifBlank { iframe.attr("data-src") },
            )
        }.filter { it.embedUrl.isNotBlank() }

        if (iframeTrailers.isNotEmpty()) return iframeTrailers

        return Regex("https://www\\.youtube\\.com/embed/[^\"\\s]+")
            .findAll(document.outerHtml())
            .mapIndexed { index, matchResult ->
                TrailerItem(title = "Trailer ${index + 1}", embedUrl = matchResult.value)
            }
            .toList()
            .distinctBy { it.embedUrl }
    }

    private fun parseTitleCard(card: Element): AnimeCard {
        val posterAnchor = card.selectFirst("a[href*=/titles/]")
        val detailsAnchor = card.selectFirst("a.details[href*=/titles/]") ?: posterAnchor
        val url = detailsAnchor?.absUrl("href").orEmpty()
        val slug = url.substringAfter("/titles/", missingDelimiterValue = "").substringBefore("/")

        val badges = card.select("a.details span.badge").eachText()
        val rating = badges.firstOrNull { it.contains(Regex("\\d+\\.\\d+")) }
        val episodeCount = badges.firstOrNull { it.contains("حلق") }
        val seasonLabel = badges.firstOrNull { badge -> badge != rating && badge != episodeCount }

        return AnimeCard(
            slug = slug,
            title = card.selectFirst("h2.title-name")?.text()?.trim().orEmpty(),
            posterUrl = card.selectFirst("img")?.bestImageUrl().orEmpty(),
            detailsUrl = url,
            synopsis = card.selectFirst("p.synopsis")?.text()?.trim(),
            rating = rating?.extractNumber(),
            episodeCount = episodeCount?.extractNumber(),
            seasonLabel = seasonLabel,
            genres = card.select("div.genres span").eachText(),
        )
    }

    private fun parseSimpleTitleCard(anchor: Element): AnimeCard {
        val url = anchor.absUrl("href")
        val slug = url.substringAfter("/titles/", missingDelimiterValue = "").substringBefore("/")
        val badges = anchor.select("span.badge").eachText()
        val rating = badges.firstOrNull { it.contains(Regex("\\d+\\.\\d+")) }
        val episodeCount = badges.firstOrNull { it.contains("حلق") }
        val seasonLabel = badges.firstOrNull { badge -> badge != rating && badge != episodeCount }

        return AnimeCard(
            slug = slug,
            title = anchor.selectFirst("h4, h2.title-name")?.text()?.trim()
                ?: anchor.selectFirst("img")?.attr("alt")?.removePrefix("بوستر")?.trim()
                ?: slug,
            posterUrl = anchor.selectFirst("img")?.bestImageUrl().orEmpty(),
            detailsUrl = url,
            rating = rating?.extractNumber(),
            episodeCount = episodeCount?.extractNumber(),
            seasonLabel = seasonLabel,
        )
    }

    private fun parseStructuredDataTitleList(document: Document): List<AnimeCard> {
        return document.structuredDataObjects()
            .filter { it.typeName() == "ItemList" }
            .flatMap { itemList ->
                itemList["itemListElement"]?.jsonArrayOrNull().orEmpty().mapNotNull { entry ->
                    val item = entry.jsonObjectOrNull()?.get("item")?.jsonObjectOrNull()
                        ?: entry.jsonObjectOrNull()
                        ?: return@mapNotNull null
                    val url = item.stringValue("url").orEmpty()
                    val slug = url.substringAfter("/titles/", missingDelimiterValue = "").substringBefore("/")
                    if (slug.isBlank()) return@mapNotNull null

                    AnimeCard(
                        slug = slug,
                        title = item.stringValue("name")?.cleanSiteSuffix() ?: slug,
                        posterUrl = item.stringValue("image").orEmpty(),
                        detailsUrl = url,
                        synopsis = item.stringValue("description"),
                        episodeCount = item.intValue("numberOfEpisodes")?.toString(),
                        genres = item.stringListValue("genre"),
                    )
                }
            }
    }

    private fun parseVideoCard(anchor: Element): EpisodeCard {
        val url = anchor.absUrl("href")
        val segments = url.substringAfter("https://anime3rb.com").trim('/').split("/")
        val titleSlug = segments.getOrNull(1).orEmpty()
        val episodeNumber = segments.getOrNull(2)?.toIntOrNull()
            ?: anchor.selectFirst("p.number")?.text()?.extractNumber()?.toIntOrNull()
            ?: 0
        return EpisodeCard(
            titleSlug = titleSlug,
            episodeNumber = episodeNumber,
            animeTitle = anchor.selectFirst("h3.title-name")?.text()?.trim().orEmpty(),
            posterUrl = anchor.selectFirst("div.poster img")?.bestImageUrl().orEmpty(),
            episodeUrl = url,
            episodeLabel = anchor.selectFirst("p.number")?.text()?.trim().orEmpty(),
            episodeTitle = null,
        )
    }

    private fun parseEpisodeItem(anchor: Element): EpisodeItem {
        val url = anchor.absUrl("href")
        val segments = url.substringAfter("https://anime3rb.com").trim('/').split("/")
        return EpisodeItem(
            titleSlug = segments.getOrNull(1).orEmpty(),
            episodeNumber = segments.getOrNull(2)?.toIntOrNull() ?: anchor.text().extractNumber()?.toIntOrNull() ?: 0,
            title = anchor.selectFirst("div.video-data p")?.text()?.trim(),
            duration = anchor.selectFirst("span.rounded")?.text()?.trim(),
            posterUrl = anchor.selectFirst("img")?.bestImageUrl().orEmpty(),
            episodeUrl = url,
        )
    }

    private fun Element.findLabeledBlock(label: String): Element? {
        val target = select("label").firstOrNull { it.text().contains(label) } ?: return null
        return target.nextElementSibling()
    }

    private fun String.extractNumber(): String? = Regex("\\d+(?:\\.\\d+)?").find(this)?.value

    private fun Element.bestImageUrl(): String {
        return absUrl("data-src").ifBlank {
            absUrl("src").ifBlank {
                attr("data-src").ifBlank { attr("src") }
            }
        }
    }

    private fun List<Element>.metricValue(label: String): String? {
        return firstOrNull { metric ->
            metric.selectFirst("p.font-light.text-sm")?.text()?.trim() == label
        }?.selectFirst("p.text-lg")?.text()?.trim()
    }

    private fun Element?.valueForLabel(label: String): String? {
        return this
            ?.select("tr")
            ?.firstOrNull { row -> row.selectFirst("td")?.text()?.trim() == label }
            ?.select("td")
            ?.getOrNull(1)
            ?.text()
            ?.trim()
    }

    private fun String.livewireValue(field: String): String? {
        return Regex("\"${Regex.escape(field)}\"\\s*:\\s*(?:\"((?:\\\\.|[^\"])*)\"|([0-9]+))")
            .find(this)
            ?.let { match ->
                match.groupValues.getOrNull(1).orEmpty().ifBlank { match.groupValues.getOrNull(2).orEmpty() }
            }
            ?.replace("\\/", "/")
            ?.replace("\\\"", "\"")
            ?.replace("&amp;", "&")
    }

    private fun Document.hasNextPage(): Boolean {
        return selectFirst("nav[aria-label=Pagination Navigation] button[rel=next]") != null ||
            selectFirst("button[wire\\:click*=nextPage]:not([disabled])") != null
    }

    private fun Document.structuredDataObjects(): List<JsonObject> {
        return select("script[type=application/ld+json]").flatMap { script ->
            val raw = script.data().ifBlank { script.html() }.trim()
            runCatching { json.parseToJsonElement(raw).structuredObjects() }.getOrDefault(emptyList())
        }
    }

    private fun JsonElement.structuredObjects(): List<JsonObject> {
        return when (this) {
            is JsonArray -> flatMap { it.structuredObjects() }
            is JsonObject -> listOf(this) + (get("@graph")?.structuredObjects().orEmpty()) +
                (get("video")?.structuredObjects().orEmpty())
            else -> emptyList()
        }
    }

    private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonElement.jsonArrayOrNull(): JsonArray? = this as? JsonArray

    private fun JsonObject.typeName(): String? = stringValue("@type")

    private fun JsonObject.stringValue(key: String): String? {
        return this[key]?.jsonPrimitiveOrNull()?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.intValue(key: String): Int? {
        return this[key]?.jsonPrimitiveOrNull()?.intOrNull
    }

    private fun JsonObject.doubleValue(key: String): Double? {
        return this[key]?.jsonPrimitiveOrNull()?.contentOrNull?.toDoubleOrNull()
    }

    private fun JsonObject.stringListValue(key: String): List<String> {
        val element = this[key] ?: return emptyList()
        return when (element) {
            is JsonArray -> element.mapNotNull { it.jsonPrimitiveOrNull()?.contentOrNull?.takeIf(String::isNotBlank) }
            else -> listOfNotNull(element.jsonPrimitiveOrNull()?.contentOrNull?.takeIf(String::isNotBlank))
        }
    }

    private fun JsonElement.jsonPrimitiveOrNull() = runCatching { jsonPrimitive }.getOrNull()

    private fun String.cleanSiteSuffix(): String = substringBefore(" - Anime3rb").trim()

    private fun String.htmlDecoded(): String = replace("&amp;", "&")

    private fun Double.trimTrailingZero(): String {
        return if (this % 1.0 == 0.0) toInt().toString() else toString()
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val TITLE_SCHEMA_TYPES = setOf("TVSeries", "Movie", "CreativeWork", "Series")
    }
}
