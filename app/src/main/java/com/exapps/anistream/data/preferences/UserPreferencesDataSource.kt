package com.exapps.anistream.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.exapps.anistream.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "anistream_preferences")

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.preferencesDataStore

    val preferences: Flow<UserPreferences> = store.data.map { prefs ->
        UserPreferences(
            autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            preferSummary = prefs[Keys.PREFER_SUMMARY] ?: false,
            cinemaMode = prefs[Keys.CINEMA_MODE] ?: false,
        )
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        store.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setPreferSummary(enabled: Boolean) {
        store.edit { it[Keys.PREFER_SUMMARY] = enabled }
    }

    suspend fun setCinemaMode(enabled: Boolean) {
        store.edit { it[Keys.CINEMA_MODE] = enabled }
    }

    private object Keys {
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val PREFER_SUMMARY = booleanPreferencesKey("prefer_summary")
        val CINEMA_MODE = booleanPreferencesKey("cinema_mode")
    }
}
