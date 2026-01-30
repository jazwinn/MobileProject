package com.jazwinn.fitnesstracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val ARE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("are_notifications_enabled")
        val IS_UNIT_METRIC = booleanPreferencesKey("is_unit_metric")
    }

    val isDarkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_MODE] ?: false // Default to light/system if not set, or handle system default logic in UI
    }

    val areNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ARE_NOTIFICATIONS_ENABLED] ?: true
    }

    val isUnitMetric: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_UNIT_METRIC] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARE_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setUnitMetric(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_UNIT_METRIC] = enabled
        }
    }
}
