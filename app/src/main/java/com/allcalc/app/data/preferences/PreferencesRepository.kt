package com.allcalc.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private object PreferenceKeys {
    val THEME = intPreferencesKey("theme")
    val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
    val ANGLE_MODE = intPreferencesKey("angle_mode")
    val HAPTICS = booleanPreferencesKey("haptics")
    val SOUND = booleanPreferencesKey("sound")
    val AUTO_CURRENCY = booleanPreferencesKey("auto_currency")
    val CURRENCY_RATES = stringPreferencesKey("currency_rates_json")
    val CURRENCY_UPDATED_AT = longPreferencesKey("currency_updated_at")
    val CURRENCY_SAMPLE = booleanPreferencesKey("currency_sample")
}

data class CurrencyCache(
    val rates: Map<String, Double>,
    val updatedAt: Long,
    val isSample: Boolean
)

class PreferencesRepository(private val context: Context) {

    private val store = context.dataStore

    val theme: Flow<Int> = store.data.map { it[PreferenceKeys.THEME] ?: 0 }
    val decimalPrecision: Flow<Int> = store.data.map { it[PreferenceKeys.DECIMAL_PRECISION] ?: 4 }
    val angleMode: Flow<Int> = store.data.map { it[PreferenceKeys.ANGLE_MODE] ?: 0 }
    val haptics: Flow<Boolean> = store.data.map { it[PreferenceKeys.HAPTICS] ?: true }
    val sound: Flow<Boolean> = store.data.map { it[PreferenceKeys.SOUND] ?: false }
    val autoCurrency: Flow<Boolean> = store.data.map { it[PreferenceKeys.AUTO_CURRENCY] ?: true }

    val currencyCache: Flow<CurrencyCache?> = store.data.map { prefs ->
        val json = prefs[PreferenceKeys.CURRENCY_RATES] ?: return@map null
        val updatedAt = prefs[PreferenceKeys.CURRENCY_UPDATED_AT] ?: 0L
        val isSample = prefs[PreferenceKeys.CURRENCY_SAMPLE] ?: false
        val rates = try {
            parseRateJson(json)
        } catch (e: Exception) {
            return@map null
        }
        CurrencyCache(rates, updatedAt, isSample)
    }

    suspend fun setTheme(value: Int) = store.edit { it[PreferenceKeys.THEME] = value }
    suspend fun setDecimalPrecision(value: Int) = store.edit { it[PreferenceKeys.DECIMAL_PRECISION] = value }
    suspend fun setAngleMode(value: Int) = store.edit { it[PreferenceKeys.ANGLE_MODE] = value }
    suspend fun setHaptics(value: Boolean) = store.edit { it[PreferenceKeys.HAPTICS] = value }
    suspend fun setSound(value: Boolean) = store.edit { it[PreferenceKeys.SOUND] = value }
    suspend fun setAutoCurrency(value: Boolean) = store.edit { it[PreferenceKeys.AUTO_CURRENCY] = value }

    suspend fun saveCurrencyCache(cache: CurrencyCache) = store.edit { prefs ->
        prefs[PreferenceKeys.CURRENCY_RATES] = buildRateJson(cache.rates)
        prefs[PreferenceKeys.CURRENCY_UPDATED_AT] = cache.updatedAt
        prefs[PreferenceKeys.CURRENCY_SAMPLE] = cache.isSample
    }

    private fun parseRateJson(json: String): Map<String, Double> {
        val obj = JSONObject(json)
        val out = HashMap<String, Double>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            out[key] = obj.getDouble(key)
        }
        return out
    }

    private fun buildRateJson(rates: Map<String, Double>): String {
        val obj = JSONObject()
        rates.forEach { (key, value) -> obj.put(key, value) }
        return obj.toString()
    }
}