package com.exapps.anistream.di

import android.content.Context
import androidx.room.Room
import com.exapps.anistream.core.common.DefaultDispatcherProvider
import com.exapps.anistream.core.common.DispatcherProvider
import com.exapps.anistream.core.network.BrowserHeadersInterceptor
import com.exapps.anistream.core.network.CloudflareChallengeInterceptor
import com.exapps.anistream.core.network.InMemoryCookieJar
import com.exapps.anistream.core.network.InMemoryWebSessionStore
import com.exapps.anistream.core.network.MutableCookieStore
import com.exapps.anistream.core.network.WebSessionStore
import com.exapps.anistream.data.local.AppDatabase
import com.exapps.anistream.data.local.HistoryDao
import com.exapps.anistream.data.local.WatchlistDao
import com.exapps.anistream.data.repository.AnimeRepositoryImpl
import com.exapps.anistream.data.scraper.Anime3rbExtractor
import com.exapps.anistream.data.scraper.AnimeExtractor
import com.exapps.anistream.domain.repository.AnimeRepository
import com.exapps.anistream.core.webview.CloudflareChallengeSolver
import com.exapps.anistream.core.webview.CloudflareWebViewInterceptor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindCloudflareSolver(impl: CloudflareWebViewInterceptor): CloudflareChallengeSolver

    @Binds
    @Singleton
    abstract fun bindWebSessionStore(impl: InMemoryWebSessionStore): WebSessionStore

    @Binds
    @Singleton
    abstract fun bindAnimeExtractor(impl: Anime3rbExtractor): AnimeExtractor

    @Binds
    @Singleton
    abstract fun bindAnimeRepository(impl: AnimeRepositoryImpl): AnimeRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCookieJar(): InMemoryCookieJar = InMemoryCookieJar()

    @Provides
    @Singleton
    fun provideMutableCookieStore(cookieJar: InMemoryCookieJar): MutableCookieStore = cookieJar

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: InMemoryCookieJar,
        browserHeadersInterceptor: BrowserHeadersInterceptor,
        challengeInterceptor: CloudflareChallengeInterceptor,
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(browserHeadersInterceptor)
            .addInterceptor(challengeInterceptor)
            .addInterceptor(loggingInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "anistream.db",
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideWatchlistDao(database: AppDatabase): WatchlistDao = database.watchlistDao()

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao = database.historyDao()
}
