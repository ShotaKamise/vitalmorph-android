package app.vitalmorph.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrainerProfileEntity::class,
        MonsterGenerationEntity::class,
        LegacyStatsEntity::class,
        InteractionStateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class VitaMorphDatabase : RoomDatabase() {
    abstract fun trainerProfileDao(): TrainerProfileDao
    abstract fun monsterGenerationDao(): MonsterGenerationDao
    abstract fun legacyStatsDao(): LegacyStatsDao
    abstract fun interactionStateDao(): InteractionStateDao

    companion object {
        private const val DATABASE_NAME = "vitalmorph.db"

        /** v2: 交流状態(タッチ・会話の1日ごとの回数)テーブルを追加。既存データは変更しない。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `interaction_state` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`lastInteractionAt` INTEGER NOT NULL, " +
                        "`consecutiveTouches` INTEGER NOT NULL, " +
                        "`touchRewardCountToday` INTEGER NOT NULL, " +
                        "`conversationCountToday` INTEGER NOT NULL, " +
                        "`miniGameRewardCountToday` INTEGER NOT NULL, " +
                        "`lastDailyResetDate` TEXT, " +
                        "PRIMARY KEY(`id`))",
                )
            }
        }

        @Volatile
        private var instance: VitaMorphDatabase? = null

        fun getInstance(context: Context): VitaMorphDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VitaMorphDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
