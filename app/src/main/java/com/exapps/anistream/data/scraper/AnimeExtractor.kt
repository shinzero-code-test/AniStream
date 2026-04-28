package com.exapps.anistream.data.scraper

import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.model.CatalogFilters
import com.exapps.anistream.domain.model.EpisodeStream
import com.exapps.anistream.domain.model.HomeFeed
import com.exapps.anistream.domain.model.PaginatedTitles

interface AnimeExtractor {
    suspend fun getHomeFeed(): HomeFeed
    suspend fun getCatalog(page: Int = 1, filters: CatalogFilters = CatalogFilters()): PaginatedTitles
    suspend fun search(query: String, page: Int = 1, filters: CatalogFilters = CatalogFilters()): PaginatedTitles
    suspend fun getAnimeDetails(slug: String): AnimeDetails
    suspend fun getEpisodeStream(titleSlug: String, episodeNumber: Int): EpisodeStream
}
