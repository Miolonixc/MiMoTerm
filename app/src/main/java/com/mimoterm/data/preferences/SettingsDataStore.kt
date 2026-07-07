package com.mimoterm.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val TERMINAL_FONT_SIZE = intPreferencesKey("terminal_font_size")
        val TERMINAL_THEME = stringPreferencesKey("terminal_theme")
        val FTP_HOST = stringPreferencesKey("ftp_host")
        val FTP_PORT = intPreferencesKey("ftp_port")
        val FTP_USERNAME = stringPreferencesKey("ftp_username")
        val FTP_PASSWORD = stringPreferencesKey("ftp_password")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val apiBaseUrl: Flow<String> = context.dataStore.data.map { it[API_BASE_URL] ?: "https://api.mimo.xiaomi.com" }
    val terminalFontSize: Flow<Int> = context.dataStore.data.map { it[TERMINAL_FONT_SIZE] ?: 14 }
    val terminalTheme: Flow<String> = context.dataStore.data.map { it[TERMINAL_THEME] ?: "dark" }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { it[API_BASE_URL] = url }
    }

    suspend fun setTerminalFontSize(size: Int) {
        context.dataStore.edit { it[TERMINAL_FONT_SIZE] = size }
    }
}
