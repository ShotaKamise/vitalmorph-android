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
        DiscoveredFormEntity::class,
    ],
    version = 8,
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
    abstract fun discoveredFormDao(): DiscoveredFormDao

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

        /**
         * v6: 世代へ進化ルート適性(人型/動物)の列を追加。
         * 既存の進行中世代は旧仕様(第3週=人型)で進化していたため、HUMANOIDを既定にして
         * アップデートで姿が変わらないようにする。
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `monster_generation` ADD COLUMN `route` TEXT NOT NULL DEFAULT 'HUMANOID'")
            }
        }

        /**
         * v7: 世代へ性格(がんばりや/のんびり/クール/あまえんぼう/きまぐれ)の列を追加。
         * 既存の進行中世代はHARDWORKER(がんばりや)を既定にするため、シーズン途中で
         * アップデートしても会話トーンやタッチ許容が急に変わって見えないようにする。
         * 性格は能力差を持たない(会話トーンと連続タッチ許容回数のみに影響)。
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `monster_generation` ADD COLUMN `personality` TEXT NOT NULL DEFAULT 'HARDWORKER'")
            }
        }

        /**
         * v8: 図鑑用の発見済みフォーム表を追加(COMPLETION_PLAN T4)。既存テーブルは変更しない。
         * refreshで現在フォームと過去世代の最終形態を遡及取り込みするため、初回起動で自動的に埋まる。
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `discovered_form` (" +
                        "`formId` TEXT PRIMARY KEY NOT NULL, " +
                        "`firstSeenAt` INTEGER NOT NULL, " +
                        "`generationNumber` INTEGER NOT NULL)",
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                    .also { instance = it }
            }
    }
}
