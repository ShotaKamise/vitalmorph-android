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
        FoodEntryEntity::class,
        CustomFoodEntity::class,
        FoodFavoriteEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class VitaMorphDatabase : RoomDatabase() {
    abstract fun trainerProfileDao(): TrainerProfileDao
    abstract fun monsterGenerationDao(): MonsterGenerationDao
    abstract fun legacyStatsDao(): LegacyStatsDao
    abstract fun interactionStateDao(): InteractionStateDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun customFoodDao(): CustomFoodDao
    abstract fun foodFavoriteDao(): FoodFavoriteDao

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

        /** v4: 食事管理MVP用のテーブルを追加。既存テーブルは変更しない。 */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `food_entry` (" +
                        "`entryId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`date` TEXT NOT NULL, `mealSlot` TEXT NOT NULL, `foodName` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, `amountUnit` TEXT NOT NULL, " +
                        "`calories` REAL NOT NULL, `proteinGrams` REAL NOT NULL, " +
                        "`fatGrams` REAL NOT NULL, `carbsGrams` REAL NOT NULL, " +
                        "`clientRecordId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_food` (" +
                        "`foodId` TEXT PRIMARY KEY NOT NULL, `name` TEXT NOT NULL, " +
                        "`standardAmount` REAL NOT NULL, `amountUnit` TEXT NOT NULL, " +
                        "`calories` REAL NOT NULL, `proteinGrams` REAL NOT NULL, " +
                        "`fatGrams` REAL NOT NULL, `carbsGrams` REAL NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `food_favorite` (`foodId` TEXT PRIMARY KEY NOT NULL)",
                )
            }
        }

        /** v5: 食事記録へ代表的なビタミン・ミネラル(VC/Ca/Fe)の列を追加。既存行は0。 */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `food_entry` ADD COLUMN `vitaminCMg` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `food_entry` ADD COLUMN `calciumMg` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `food_entry` ADD COLUMN `ironMg` REAL NOT NULL DEFAULT 0")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
