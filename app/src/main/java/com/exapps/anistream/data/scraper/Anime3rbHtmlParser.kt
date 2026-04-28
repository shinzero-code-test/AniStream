package com.exapps.anistream.data.scraper

import com.exapps.anistream.domain.model.AnimeCard
import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.model.DownloadLink
import com.exapps.anistream.domain.model.EpisodeCard
import com.exapps.anistream.domain.model.EpisodeItem
import com.exapps.anistream.domain.model.EpisodeStream
import com.exapps.anistream.domain.model.HomeFeed
import com.exapps.anistream.domain.model.PaginatedTitles
import com.exapps.anistream.domain.model.StreamType
import com.exapps.anistream.domain.model.VideoSource
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
        return PaginatedTitles(
            items = document.select("div.titles-list div.title-card").map(::parseTitleCard).distinctBy { it.slug },
            currentPage = page,
            hasNextPage = document.selectFirst("nav[aria-label=Pagination Navigation] button[rel=next]") != null,
        )
    }

    fun parseSearch(document: Document, page: Int): PaginatedTitles {
        val items = document.select("div.titles-list div.title-card").map(::parseTitleCard)
        return PaginatedTitles(
            items = items.distinctBy { it.slug },
            currentPage = page,
            hasNextPage = document.selectFirst("nav[aria-label=Pagination Navigation] button[rel=next]") != null,
        )
    }

    fun parseDetails(document: Document, slug: String): AnimeDetails {
        val titleHeader = document.selectFirst("h1.text-2xl")
        val title = titleHeader?.selectFirst("span[dir=ltr]")?.text().orEmpty()
        val typeLabel = titleHeader
            ?.select("span")
            ?.getOrNull(1)
            ?.text()
            ?.removePrefix("(")
            ?.removeSuffix(")")
            ?.trim()

        val posterUrl = document.selectFirst("section img[alt^=بوستر]")?.bestImageUrl().orEmpty()
        val contentRoot = titleHeader?.parents()?.firstOrNull { parent ->
            parent.selectFirst("div[x-data*=summary]") != null
        } ?: document

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

        val alternateTitles = run {
            val labels = contentRoot.select("label")
            val targetLabel = labels.firstOrNull { it.text().contains("أسماء أخرى") }
            targetLabel?.nextElementSibling()?.select("h2")?.eachText().orEmpty()
        }

        val episodes = document
            .select("div.video-list a[href*=/episode/]")
            .map(::parseEpisodeItem)
            .distinctBy { it.episodeUrl }

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
            score = metrics.metricValue("التقييم"),
            episodeCount = metrics.metricValue("الحلقات")?.toIntOrNull(),
            synopsis = fullSynopsis,
            summary = summary,
            alternateTitles = alternateTitles,
            genres = contentRoot.select("a[href*=/genre/]").map { it.text().trim() }.distinct(),
            episodes = episodes,
        )
    }

    fun parseEpisodePage(document: Document, titleSlug: String, episodeNumber: Int): EpisodeStream {
        val titleLink = document.selectFirst("h1 a[href*=/titles/]")
        val animeTitle = titleLink
            ?.text()
            ?.substringBefore("الحلقة")
            ?.trim()
            .orEmpty()
        val posterUrl = document.selectFirst("div.video-list img[alt^=بوستر]")?.bestImageUrl()
            ?: document.selectFirst("meta[property=og:image]")?.attr("content")
            ?: ""

        val snapshot = document.allElements.firstOrNull { element ->
            element.hasAttr("wire:snapshot") && element.attr("wire:snapshot").contains("video_url")
        }?.attr("wire:snapshot").orEmpty()

        val decodedSnapshot = Parser.unescapeEntities(snapshot, false)
        val directPlayerUrl = decodedSnapshot.livewireValue("video_url") ?: document.selectFirst("iframe")?.absUrl("src")
        val activeSourceId = decodedSnapshot.livewireValue("video_source")
        val iframeUrl = document.selectFirst("iframe")?.absUrl("src")
        val episodeTitle = document.selectFirst("h2.inline.text-lg.font-light")?.text()?.trim()

        val availableSources = document.select("button[data-video-source]").mapIndexed { index, button ->
            val label = button.selectFirst("span[title]")?.text()?.trim()
                ?: button.text().trim()
            VideoSource(
                id = button.attr("data-video-source").ifBlank { "source-$index" },
                label = label,
                url = directPlayerUrl.orEmpty(),
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
        }

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
        )
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

    private fun parseVideoCard(anchor: Element): EpisodeCard {
        val url = anchor.absUrl("href")
        val segments = url.substringAfter("https://anime3rb.com").trim('/').split("/")
        val titleSlug = segments.getOrNull(1).orEmpty()
        val episodeNumber = segments.getOrNull(2)?.toIntOrNull() ?: anchor.selectFirst("p.number")?.text()?.extractNumber()?.toIntOrNull() ?: 0
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
        return Regex("\"$field\":\"(.*?)\"")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\/", "/")
            ?.replace("&amp;", "&")
    }
}
