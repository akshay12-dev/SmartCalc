package com.allcalc.app

import android.app.Application
import android.content.Context
import com.allcalc.app.data.currency.CurrencyRepository
import com.allcalc.app.data.local.AppDatabase
import com.allcalc.app.data.local.HistoryRepository
import com.allcalc.app.data.preferences.PreferencesRepository
import com.allcalc.app.domain.calculator.ExpressionEngine
import com.allcalc.app.domain.converter.ConversionEngine
import com.allcalc.app.domain.scanner.MathNormalizer
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class AllCalcApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(filesDir, "crash.txt").writeText(
                    "Thread: ${thread?.name}\n$sw"
                )
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: AppDatabase = AppDatabase.build(appContext)
    val historyRepository: HistoryRepository = HistoryRepository(database.historyDao())
    val preferences: PreferencesRepository = PreferencesRepository(appContext)
    val expressionEngine: ExpressionEngine = ExpressionEngine()
    val conversionEngine: ConversionEngine = ConversionEngine
    val mathNormalizer: MathNormalizer = MathNormalizer()
    val currencyRepository: CurrencyRepository = CurrencyRepository(preferences)
}