package app.vitalmorph.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrainerProfileEntity::class,
        MonsterGenerationEntity::class,
        LegacyStatsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class VitaMorphDatabase : RoomDatabase() {
    abstract fun trainerProfileDao(): TrainerProfileDao
    abstract fun monsterGenerationDao(): MonsterGenerationDao
    abstract fun legacyStatsDao(): LegacyStatsDao

    companion object {
        private const val DATABASE_NAME = "vitalmorph.db"

        @Volatile
        private var instance: VitaMorphDatabase? = null

        fun getInstance(context: Context): VitaMorphDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VitaMorphDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}
