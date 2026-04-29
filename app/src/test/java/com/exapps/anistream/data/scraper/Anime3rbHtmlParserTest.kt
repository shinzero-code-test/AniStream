package com.exapps.anistream.data.scraper

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Anime3rbHtmlParserTest {
    private val parser = Anime3rbHtmlParser()

    @Test
    fun parseHomeReadsCurrentAnime3rbCards() {
        val feed = parser.parseHome(loadSample(HOME_HTML))

        assertTrue(feed.featuredEpisodes.isNotEmpty())
        assertTrue(feed.latestEpisodes.isNotEmpty())
        assertTrue(feed.latestTitles.isNotEmpty())
        assertEquals("one-piece", feed.featuredEpisodes.first().titleSlug)
        assertTrue(feed.latestEpisodes.first().episodeNumber > 0)
        assertTrue(feed.latestTitles.first().slug.isNotBlank())
    }

    @Test
    fun parseSearchReadsCurrentTitleCards() {
        val page = parser.parseSearch(loadSample(TITLE_LIST_HTML), page = 1)

        assertTrue(page.items.size >= 1)
        assertEquals("jujutsu-kaisen", page.items.first().slug)
        assertEquals("Jujutsu Kaisen", page.items.first().title)
        assertEquals("8.52", page.items.first().rating)
    }

    @Test
    fun parseCatalogDetectsLivewirePagination() {
        val page = parser.parseCatalog(loadSample(TITLE_LIST_HTML), page = 1)

        assertEquals(1, page.items.size)
        assertTrue(page.hasNextPage)
        assertTrue(page.items.first().posterUrl.startsWith("https://images.anime3rb.com/"))
    }

    @Test
    fun parseDetailsReadsMetadataAndEpisodes() {
        val details = parser.parseDetails(loadSample(DETAILS_HTML), "jujutsu-kaisen")

        assertEquals("Jujutsu Kaisen", details.title)
        assertEquals("منتهي", details.status)
        assertEquals("9.18", details.score)
        assertEquals(989, details.ratingCount)
        assertEquals("2020-10-03", details.publishedAt)
        assertEquals(24, details.episodeCount)
        assertTrue(details.synopsis.contains("يوجي إيتادوري"))
        assertTrue(details.genres.contains("أكشن"))
        assertEquals(2, details.episodes.size)
        assertEquals(1, details.episodes.first().episodeNumber)
    }

    @Test
    fun parseEpisodeReadsLivewireAndDownloadData() {
        val stream = parser.parseEpisodePage(loadSample(EPISODE_HTML), "jujutsu-kaisen", 1)

        assertEquals("Jujutsu Kaisen", stream.animeTitle)
        assertEquals("ريومين سوكونا", stream.episodeTitle)
        assertNotNull(stream.csrfToken)
        assertNotNull(stream.livewireSnapshot)
        assertEquals("66137468a5d62", stream.selectedServerId)
        assertTrue(stream.playbackUrl.orEmpty().startsWith("https://video.vid3rb.com/player/"))
        assertFalse(stream.downloadLinks.isEmpty())
        assertEquals(2, stream.nextEpisodeNumber)
    }

    private fun loadSample(html: String) = Jsoup.parse(
        html,
        "https://anime3rb.com/",
    )

    private companion object {
        const val HOME_HTML = """
            <html><body>
                <section>
                    <h2>الأنميات المثبتة</h2>
                    <ul class="glide__slides">
                        <li class="glide__slide">
                            <a href="https://anime3rb.com/episode/one-piece/1159" class="video-card">
                                <div class="poster"><img src="https://images.anime3rb.com/455312/164e8802b3cc4e.jpg" alt="بوستر One Piece"></div>
                                <h3 class="title-name">One Piece</h3>
                                <p class="number">الحلقة 1159</p>
                            </a>
                        </li>
                    </ul>
                </section>
                <section>
                    <h3>أحدث الحلقات</h3>
                    <div id="videos">
                        <a href="https://anime3rb.com/episode/jujutsu-kaisen/1" class="video-card">
                            <div class="poster"><img src="https://images.anime3rb.com/295921/164f0f91dcbd19.jpg" alt="بوستر Jujutsu Kaisen"></div>
                            <h3 class="title-name">Jujutsu Kaisen</h3>
                            <p class="number">الحلقة 1</p>
                        </a>
                    </div>
                </section>
                <section>
                    <h3>آخر الأنميات المضافة</h3>
                    <ul class="glide__slides">
                        <li class="glide__slide"><div class="title-card">
                            <a href="https://anime3rb.com/titles/jujutsu-kaisen" class="btn"><img src="https://images.anime3rb.com/295921/164f0f91dcbd19.jpg"><h2 class="title-name">Jujutsu Kaisen</h2></a>
                            <a href="https://anime3rb.com/titles/jujutsu-kaisen" class="details">
                                <div class="genres"><span>أكشن</span></div>
                                <span class="badge">خريف 2020</span><span class="badge">8.52</span><span class="badge">24 حلقات</span>
                            </a>
                        </div></li>
                    </ul>
                </section>
            </body></html>
        """

        const val TITLE_LIST_HTML = """
            <html><body>
                <div class="titles-list">
                    <div class="title-card">
                        <a href="https://anime3rb.com/titles/jujutsu-kaisen" class="btn"><img src="https://images.anime3rb.com/295921/164f0f91dcbd19.jpg"><h2 class="title-name">Jujutsu Kaisen</h2></a>
                        <a href="https://anime3rb.com/titles/jujutsu-kaisen" class="details">
                            <div class="genres"><span>أكشن</span><span>شونين</span></div>
                            <span class="badge">خريف 2020</span><span class="badge">8.52</span><span class="badge">24 حلقات</span>
                            <p class="synopsis">طالب الثانوية يوجي إيتادوري.</p>
                        </a>
                    </div>
                </div>
                <nav role="navigation" aria-label="Pagination Navigation">
                    <button type="button" wire:click="nextPage('page')" rel="next" aria-label="التالي">التالي</button>
                </nav>
            </body></html>
        """

        const val DETAILS_HTML = """
            <html><body>
                <meta property="og:image" content="https://images.anime3rb.com/295921/164f0f91dcbd19.jpg">
                <section><img src="https://images.anime3rb.com/295921/164f0f91dcbd19.jpg" alt="بوستر Jujutsu Kaisen"></section>
                <div><div>
                    <h1 class="text-2xl"><span dir="ltr">Jujutsu Kaisen</span> <span>( مسلسل )</span></h1>
                    <div class="rounded-lg border"><p class="font-light text-sm">التقييم</p><p class="text-lg">9.18</p></div>
                    <div class="rounded-lg border"><p class="font-light text-sm">الحلقات</p><p class="text-lg">24</p></div>
                    <div class="rounded-lg border"><p class="font-light text-sm">التصنيف العمري</p><p class="text-lg">عنف و ألفاظ خارجة R - 17+</p></div>
                    <div x-data="{summary: false}">
                        <div><p>يقضي طالب الثانوية يوجي إيتادوري أيامه بلا مبالاة.</p></div>
                        <div><p>طالب الثانوية يوجي إيتادوري يعيش بين نادي الغيبيات.</p></div>
                    </div>
                    <a href="https://anime3rb.com/genre/action">أكشن</a>
                    <div class="flex flex-col"><label>أسماء أخرى :</label><div><h2>JJK</h2></div></div>
                </div></div>
                <table class="leading-loose">
                    <tr><td>الحالة:</td><td>منتهي</td></tr>
                    <tr><td>إصدار:</td><td>خريف 2020</td></tr>
                </table>
                <script type="application/ld+json">{"@type":"TVSeries","datePublished":"2020-10-03T00:00:00+00:00","aggregateRating":{"@type":"AggregateRating","ratingValue":9.18,"ratingCount":989}}</script>
                <div class="video-list">
                    <a href="https://anime3rb.com/episode/jujutsu-kaisen/1"><img src="https://images.anime3rb.com/295921/164f0f91dcbd19.jpg"><span class="rounded">23:59</span><div class="video-data"><span>الحلقة 1</span><p>ريومين سوكونا</p></div></a>
                    <a href="https://anime3rb.com/episode/jujutsu-kaisen/2"><img src="https://images.anime3rb.com/295921/164f0f91dcbd19.jpg"><span class="rounded">23:59</span><div class="video-data"><span>الحلقة 2</span><p>من أجل نفسي</p></div></a>
                </div>
            </body></html>
        """

        const val EPISODE_HTML = """
            <html><head>
                <meta name="csrf-token" content="JYQBUutiHWRDZljslELuCH799Iw03LuaqHeV54OY">
                <meta property="og:image" content="https://video.vid3rb.com/video/9bc16fc9-d7b3-4840-9921-7453d4c1f9f4">
            </head><body>
                <div wire:snapshot="{&quot;data&quot;:{&quot;title_slug&quot;:&quot;jujutsu-kaisen&quot;,&quot;video_number&quot;:&quot;1&quot;,&quot;views&quot;:212288,&quot;video_url&quot;:&quot;https:\/\/video.vid3rb.com\/player\/9bc16f8b-35b6-4b91-90e2-5d477b093e58?token=e06dc9d3965141e827cb17c4e101e83ae29d4cab93216d7dd60a12d5da9f770f&amp;expires=1777250207&quot;,&quot;video_source&quot;:&quot;66137468a5d62&quot;},&quot;memo&quot;:{&quot;id&quot;:&quot;YypUorkGVBQeSUP13HeK&quot;},&quot;checksum&quot;:&quot;5458a460&quot;}" wire:id="YypUorkGVBQeSUP13HeK">
                    <iframe title="Jujutsu Kaisen الحلقة 1" src="https://video.vid3rb.com/player/9bc16f8b-35b6-4b91-90e2-5d477b093e58?token=e06dc9d3965141e827cb17c4e101e83ae29d4cab93216d7dd60a12d5da9f770f&amp;expires=1777250207"></iframe>
                    <h1><a href="https://anime3rb.com/titles/jujutsu-kaisen">Jujutsu Kaisen الحلقة 1</a></h1>
                    <h2 class="inline text-lg font-light">ريومين سوكونا</h2>
                    <button data-video-source="66137468a5d62"><span title="ترجمة Crunchyroll">ترجمة Crunchyroll</span></button>
                    <a href="https://anime3rb.com/download/9bc16fce-7e0d-4f69-b4dc-8f6cabe2e7e7?expires=1777243007">تحميل مباشر [287.94 ميغابايت]</a>
                    <a href="https://anime3rb.com/titles/jujutsu-kaisen/download">تحميل مجمع</a>
                </div>
                <div class="video-list">
                    <a href="https://anime3rb.com/episode/jujutsu-kaisen/1" class="active"><img src="https://images.anime3rb.com/295921/164f0f91dcbd19.jpg"><div class="video-data"><span>الحلقة 1</span><p>ريومين سوكونا</p></div></a>
                    <a href="https://anime3rb.com/episode/jujutsu-kaisen/2"><img src="https://images.anime3rb.com/295921/164f0f91dcbd19.jpg"><div class="video-data"><span>الحلقة 2</span><p>من أجل نفسي</p></div></a>
                </div>
            </body></html>
        """
    }
}
