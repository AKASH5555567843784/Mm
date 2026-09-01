package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SassinessLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mm_assistant_settings")

class MMSettingsDataStore(private val context: Context) {

    private val dataStore = context.settingsDataStore

    val sassinessLevel: Flow<SassinessLevel> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val levelId = preferences[KEY_SASSINESS_LEVEL] ?: SassinessLevel.SASSY.id
            SassinessLevel.fromId(levelId)
        }

    val isBatteryAdaptiveWakeWordEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_BATTERY_ADAPTIVE] ?: true
        }

    val isInteractionCacheEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_INTERACTION_CACHE] ?: true
        }

    suspend fun setSassinessLevel(level: SassinessLevel) {
        dataStore.edit { preferences ->
            preferences[KEY_SASSINESS_LEVEL] = level.id
        }
    }

    suspend fun setBatteryAdaptiveWakeWord(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BATTERY_ADAPTIVE] = enabled
        }
    }

    suspend fun setInteractionCacheEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_INTERACTION_CACHE] = enabled
        }
    }

    companion object {
        val KEY_SASSINESS_LEVEL = stringPreferencesKey("key_sassiness_level")
        val KEY_BATTERY_ADAPTIVE = booleanPreferencesKey("key_battery_adaptive_wake_word")
        val KEY_INTERACTION_CACHE = booleanPreferencesKey("key_interaction_cache_enabled")

        @Volatile
        private var INSTANCE: MMSettingsDataStore? = null

        fun getInstance(context: Context): MMSettingsDataStore {
            return INSTANCE ?: synchronized(this) {
                val instance = MMSettingsDataStore(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
