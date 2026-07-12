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
    version = 3,
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

        /** v3: 系譜表示用に世代へ最終形態・大会順位・継承内容の列を追加。既存行はデフォルト値。 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `monster_generation` ADD COLUMN `finalFormId` TEXT")
                db.execSQL("ALTER TABLE `monster_generation` ADD COLUMN `finalPlacement` INTEGER")
                db.execSQL("ALTER TABLE `monster_generation` ADD COLUMN `awardedHp` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `monster_generation` ADD COLUMN `awardedAttack` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `monster_generation` ADD COLUMN `awardedDefense` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `monster_generation` ADD COLUMN `awardedSpeed` INTEGER NOT NULL DEFAULT 0")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
