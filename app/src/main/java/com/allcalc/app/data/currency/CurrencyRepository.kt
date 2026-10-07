package com.allcalc.app.data.currency

import com.allcalc.app.data.preferences.CurrencyCache
import com.allcalc.app.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class CurrencyInfo(val code: String, val name: String, val flag: String)

object Currencies {
    val list = listOf(
        CurrencyInfo("INR", "Indian Rupee", "🇮🇳"),
        CurrencyInfo("USD", "US Dollar", "🇺🇸"),
        CurrencyInfo("EUR", "Euro", "🇪🇺"),
        CurrencyInfo("GBP", "British Pound", "🇬🇧"),
        CurrencyInfo("JPY", "Japanese Yen", "🇯🇵"),
        CurrencyInfo("AED", "UAE Dirham", "🇦🇪"),
        CurrencyInfo("CAD", "Canadian Dollar", "🇨🇦"),
        CurrencyInfo("AUD", "Australian Dollar", "🇦🇺"),
        CurrencyInfo("SGD", "Singapore Dollar", "🇸🇬"),
        CurrencyInfo("CHF", "Swiss Franc", "🇨🇭"),
        CurrencyInfo("CNY", "Chinese Yuan", "🇨🇳"),
        CurrencyInfo("NZD", "New Zealand Dollar", "🇳🇿"),
        CurrencyInfo("SEK", "Swedish Krona", "🇸🇪"),
        CurrencyInfo("NOK", "Norwegian Krone", "🇳🇴"),
        CurrencyInfo("MYR", "Malaysian Ringgit", "🇲🇾"),
        CurrencyInfo("ZAR", "South African Rand", "🇿🇦"),
        CurrencyInfo("BRL", "Brazilian Real", "🇧🇷"),
        CurrencyInfo("MXN", "Mexican Peso", "🇲🇽"),
        CurrencyInfo("RUB", "Russian Ruble", "🇷🇺"),
        CurrencyInfo("TRY", "Turkish Lira", "🇹🇷")
    )

    fun byCode(code: String): CurrencyInfo =
        list.firstOrNull { it.code == code } ?: CurrencyInfo(code, code, "🏳️")
}

class CurrencyRepository(private val preferences: PreferencesRepository) {

    sealed interface FetchResult {
        data class Success(val cache: CurrencyCache) : FetchResult
        data class Failure(val message: String) : FetchResult
    }

    /**
     * A clearly-labelled offline seed used only before the first successful network fetch.
     * It is stamped as a sample and replaced by live API data, so it is never treated as
     * a permanent source of truth.
     */
    private val sampleRates = mapOf(
        "USD" to 1.0, "EUR" to 0.92, "GBP" to 0.79, "JPY" to 149.5, "INR" to 83.2,
        "AED" to 3.6725, "CAD" to 1.36, "AUD" to 1.53, "SGD" to 1.35, "CHF" to 0.88,
        "CNY" to 7.13, "NZD" to 1.64, "SEK" to 10.4, "NOK" to 10.55, "MYR" to 4.72,
        "ZAR" to 18.7, "BRL" to 4.95, "MXN" to 17.1, "RUB" to 91.0, "TRY" to 32.1
    )

    suspend fun loadCache(): CurrencyCache =
        preferences.currencyCache.first() ?: CurrencyCache(sampleRates, 0L, true)

    suspend fun save(cache: CurrencyCache) = preferences.saveCurrencyCache(cache)

    suspend fun refresh(): FetchResult {
        return try {
            val json = fetchRatesJson()
            val obj = JSONObject(json)
            if (obj.optString("result") != "success") {
                return FetchResult.Failure("The rates service rejected the request")
            }
            val ratesObject = obj.getJSONObject("rates")
            val rates = HashMap<String, Double>()
            val keys = ratesObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                rates[key] = ratesObject.getDouble(key)
            }
            var updatedAt = obj.optLong("time_last_update_unix") * 1000L
            if (updatedAt == 0L) updatedAt = System.currentTimeMillis()
            val cache = CurrencyCache(rates, updatedAt, false)
            save(cache)
            FetchResult.Success(cache)
        } catch (e: Exception) {
            FetchResult.Failure(e.message ?: "Unable to reach the rates service")
        }
    }

    fun convert(amount: Double, from: String, to: String, rates: Map<String, Double>): Double {
        if (from == to) return amount
        val fromRate = rates[from] ?: throw IOException("No rate available for $from")
        val toRate = rates[to] ?: throw IOException("No rate available for $to")
        val inUsd = amount / fromRate
        return inUsd * toRate
    }

    private fun fetchRatesJson(): String {
        val connection = URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}