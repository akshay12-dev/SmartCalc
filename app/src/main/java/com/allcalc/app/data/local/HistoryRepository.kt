package com.allcalc.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

object HistoryTypes {
    const val CALC = "CALC"
    const val CONVERSION = "CONVERSION"
    const val SCAN = "SCAN"
}

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val category: String = "",
    val label: String,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(item: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE type = :type ORDER BY timestamp DESC LIMIT :limit")
    fun observeByType(type: String, limit: Int): Flow<List<HistoryEntity>>
}

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun build(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "allcalc.db"
                ).build().also { instance = it }
            }
    }
}

class HistoryRepository(private val dao: HistoryDao) {
    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()
    fun observeByType(type: String, limit: Int): Flow<List<HistoryEntity>> = dao.observeByType(type, limit)
    suspend fun add(item: HistoryEntity) = dao.insert(item)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun clear() = dao.clearAll()
}