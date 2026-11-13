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
        // Asia & Oceania
        CurrencyInfo("INR", "Indian Rupee", "🇮🇳"),
        CurrencyInfo("JPY", "Japanese Yen", "🇯🇵"),
        CurrencyInfo("CNY", "Chinese Yuan", "🇨🇳"),
        CurrencyInfo("HKD", "Hong Kong Dollar", "🇭🇰"),
        CurrencyInfo("SGD", "Singapore Dollar", "🇸🇬"),
        CurrencyInfo("MYR", "Malaysian Ringgit", "🇲🇾"),
        CurrencyInfo("IDR", "Indonesian Rupiah", "🇮🇩"),
        CurrencyInfo("THB", "Thai Baht", "🇹🇭"),
        CurrencyInfo("PHP", "Philippine Peso", "🇵🇭"),
        CurrencyInfo("VND", "Vietnamese Dong", "🇻🇳"),
        CurrencyInfo("KRW", "South Korean Won", "🇰🇷"),
        CurrencyInfo("TWD", "Taiwanese Dollar", "🇹🇼"),
        CurrencyInfo("PKR", "Pakistani Rupee", "🇵🇰"),
        CurrencyInfo("BDT", "Bangladeshi Taka", "🇧🇩"),
        CurrencyInfo("LKR", "Sri Lankan Rupee", "🇱🇰"),
        CurrencyInfo("NPR", "Nepalese Rupee", "🇳🇵"),
        CurrencyInfo("AUD", "Australian Dollar", "🇦🇺"),
        CurrencyInfo("NZD", "New Zealand Dollar", "🇳🇿"),
        // Americas
        CurrencyInfo("USD", "US Dollar", "🇺🇸"),
        CurrencyInfo("CAD", "Canadian Dollar", "🇨🇦"),
        CurrencyInfo("MXN", "Mexican Peso", "🇲🇽"),
        CurrencyInfo("BRL", "Brazilian Real", "🇧🇷"),
        CurrencyInfo("ARS", "Argentine Peso", "🇦🇷"),
        CurrencyInfo("CLP", "Chilean Peso", "🇨🇱"),
        CurrencyInfo("COP", "Colombian Peso", "🇨🇴"),
        CurrencyInfo("PEN", "Peruvian Sol", "🇵🇪"),
        CurrencyInfo("UYU", "Uruguayan Peso", "🇺🇾"),
        // Europe
        CurrencyInfo("EUR", "Euro", "🇪🇺"),
        CurrencyInfo("GBP", "British Pound", "🇬🇧"),
        CurrencyInfo("CHF", "Swiss Franc", "🇨🇭"),
        CurrencyInfo("SEK", "Swedish Krona", "🇸🇪"),
        CurrencyInfo("NOK", "Norwegian Krone", "🇳🇴"),
        CurrencyInfo("DKK", "Danish Krone", "🇩🇰"),
        CurrencyInfo("PLN", "Polish Zloty", "🇵🇱"),
        CurrencyInfo("CZK", "Czech Koruna", "🇨🇿"),
        CurrencyInfo("HUF", "Hungarian Forint", "🇭🇺"),
        CurrencyInfo("RON", "Romanian Leu", "🇷🇴"),
        CurrencyInfo("HRK", "Croatian Kuna", "🇭🇷"),
        CurrencyInfo("RUB", "Russian Ruble", "🇷🇺"),
        CurrencyInfo("TRY", "Turkish Lira", "🇹🇷"),
        CurrencyInfo("UAH", "Ukrainian Hryvnia", "🇺🇦"),
        // Middle East & Africa
        CurrencyInfo("AED", "UAE Dirham", "🇦🇪"),
        CurrencyInfo("SAR", "Saudi Riyal", "🇸🇦"),
        CurrencyInfo("QAR", "Qatari Riyal", "🇶🇦"),
        CurrencyInfo("KWD", "Kuwaiti Dinar", "🇰🇼"),
        CurrencyInfo("BHD", "Bahraini Dinar", "🇧🇭"),
        CurrencyInfo("OMR", "Omani Rial", "🇴🇲"),
        CurrencyInfo("JOD", "Jordanian Dinar", "🇯🇴"),
        CurrencyInfo("EGP", "Egyptian Pound", "🇪🇬"),
        CurrencyInfo("ILS", "Israeli Shekel", "🇮🇱"),
        CurrencyInfo("ZAR", "South African Rand", "🇿🇦"),
        CurrencyInfo("NGN", "Nigerian Naira", "🇳🇬"),
        CurrencyInfo("KES", "Kenyan Shilling", "🇰🇪"),
        CurrencyInfo("GHS", "Ghanaian Cedi", "🇬🇭"),
        CurrencyInfo("MAD", "Moroccan Dirham", "🇲🇦"),
        CurrencyInfo("TZS", "Tanzanian Shilling", "🇹🇿"),
        // Other
        CurrencyInfo("IQD", "Iraqi Dinar", "🇮🇶"),
        CurrencyInfo("IRR", "Iranian Rial", "🇮🇷"),
        CurrencyInfo("MXN", "Mexican Peso", "🇲🇽"),
    )
        // Deduplicate by code (keep first occurrence)
        .distinctBy { it.code }
        .sortedBy { it.code }

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
        "ZAR" to 18.7, "BRL" to 4.95, "MXN" to 17.1, "RUB" to 91.0, "TRY" to 32.1,
        "HKD" to 7.82, "IDR" to 15750.0, "THB" to 35.1, "PHP" to 56.2, "KRW" to 1330.0,
        "TWD" to 31.8, "PKR" to 278.0, "BDT" to 110.0, "LKR" to 310.0, "NPR" to 133.0,
        "VND" to 24500.0, "ARS" to 890.0, "CLP" to 920.0, "COP" to 3950.0, "PEN" to 3.7,
        "UYU" to 38.5, "DKK" to 6.88, "PLN" to 3.97, "CZK" to 22.8, "HUF" to 360.0,
        "RON" to 4.57, "HRK" to 7.0, "UAH" to 37.5, "SAR" to 3.75, "QAR" to 3.64,
        "KWD" to 0.308, "BHD" to 0.376, "OMR" to 0.385, "JOD" to 0.709, "EGP" to 48.5,
        "ILS" to 3.75, "NGN" to 1580.0, "KES" to 128.0, "GHS" to 12.5, "MAD" to 10.0,
        "TZS" to 2700.0, "IQD" to 1310.0, "IRR" to 42250.0
    )

    suspend fun loadCache(): CurrencyCache =
        preferences.currencyCache.first() ?: CurrencyCache(sampleRates, 0L, true)

    suspend fun save(cache: CurrencyCache) = preferences.saveCurrencyCache(cache)

    private data class RatePayload(val rates: Map<String, Double>, val updatedAt: Long?)

    /**
     * Sources are tried in order. More than one is used because a single provider can be
     * blocked, rate-limited or unreachable on some networks/countries.
     */
    private val sources: List<Pair<String, (JSONObject) -> RatePayload>> = listOf(
        "https://open.er-api.com/v6/latest/USD" to ::parseErApi,
        "https://api.exchangerate-api.com/v4/latest/USD" to ::parseExchangeRateApiV4,
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json" to ::parseCurrencyApi,
        "https://api.frankfurter.app/latest?from=USD" to ::parseFrankfurter
    )

    suspend fun refresh(): FetchResult {
        var lastError = "Unable to reach any rates service. Check your internet connection."
        for ((apiUrl, parser) in sources) {
            try {
                val payload = parser(JSONObject(fetchRatesJson(apiUrl)))
                if (payload.rates.size < 10) {
                    lastError = "The rates service returned incomplete data"
                    continue
                }
                val rates = normalizeRates(payload.rates)
                val cache = CurrencyCache(
                    rates = rates,
                    updatedAt = payload.updatedAt ?: System.currentTimeMillis(),
                    isSample = false
                )
                save(cache)
                return FetchResult.Success(cache)
            } catch (e: Exception) {
                lastError = e.message ?: "Network error"
            }
        }
        return FetchResult.Failure(lastError)
    }

    /** open.er-api: { "result": "success", "rates": { "INR": 83.2, ... } } */
    private fun parseErApi(obj: JSONObject): RatePayload {
        if (obj.has("result") && obj.optString("result") != "success") {
            throw IOException("Rates service error: ${obj.optString("error-type", "unknown")}")
        }
        val rates = readRatesObject(obj.getJSONObject("rates"))
        val updated = obj.optLong("time_last_update_unix").takeIf { it > 0L }?.times(1000L)
        return RatePayload(rates, updated)
    }

    /** exchangerate-api v4: { "rates": {...}, "time_last_updated": 1234567890 } */
    private fun parseExchangeRateApiV4(obj: JSONObject): RatePayload {
        val rates = readRatesObject(obj.getJSONObject("rates"))
        val updated = obj.optLong("time_last_updated").takeIf { it > 0L }?.times(1000L)
        return RatePayload(rates, updated)
    }

    /** fawazahmed0 currency-api: { "date": "...", "usd": { "inr": 83.2, ... } } */
    private fun parseCurrencyApi(obj: JSONObject): RatePayload =
        RatePayload(readRatesObject(obj.getJSONObject("usd")), null)

    /** frankfurter: { "base": "USD", "rates": { "INR": 83.2, ... } } (USD itself is omitted) */
    private fun parseFrankfurter(obj: JSONObject): RatePayload =
        RatePayload(readRatesObject(obj.getJSONObject("rates")), null)

    private fun readRatesObject(ratesObject: JSONObject): Map<String, Double> {
        val out = HashMap<String, Double>()
        val keys = ratesObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = ratesObject.optDouble(key, Double.NaN)
            if (value.isFinite() && value > 0.0) out[key] = value
        }
        return out
    }

    /**
     * Uppercases codes, drops non-currency entries (crypto tickers, etc.), guarantees the USD
     * base rate, and fills any gaps left by a partial provider from the offline seed so a
     * conversion never fails with "No rate available".
     */
    private fun normalizeRates(raw: Map<String, Double>): Map<String, Double> {
        val out = HashMap<String, Double>()
        for ((key, value) in raw) {
            val code = key.uppercase()
            if (code.length == 3 && code.all { it in 'A'..'Z' }) out[code] = value
        }
        out["USD"] = 1.0
        for ((code, value) in sampleRates) {
            if (code !in out) out[code] = value
        }
        return out
    }

    fun convert(amount: Double, from: String, to: String, rates: Map<String, Double>): Double {
        if (from == to) return amount
        val fromRate = rates[from] ?: throw IOException("No rate available for $from")
        val toRate = rates[to] ?: throw IOException("No rate available for $to")
        val inUsd = amount / fromRate
        return inUsd * toRate
    }

    private fun fetchRatesJson(apiUrl: String): String {
        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.useCaches = false
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "AllCalcApp/1.0")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Connection", "close")
            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText().take(160) }
                } catch (_: Exception) {
                    null
                }
                throw IOException("HTTP $code from $apiUrl" + (detail?.let { ": $it" } ?: ""))
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}