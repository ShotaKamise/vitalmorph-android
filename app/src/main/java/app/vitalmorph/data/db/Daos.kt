package app.vitalmorph.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TrainerProfileDao {
    @Query("SELECT * FROM trainer_profile WHERE id = ${TrainerProfileEntity.SINGLETON_ID}")
    suspend fun get(): TrainerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TrainerProfileEntity)

    @Query("DELETE FROM trainer_profile")
    suspend fun clear()
}

@Dao
interface MonsterGenerationDao {
    @Query("SELECT * FROM monster_generation WHERE seasonEnd IS NULL ORDER BY generationNumber DESC LIMIT 1")
    suspend fun current(): MonsterGenerationEntity?

    @Query("SELECT * FROM monster_generation ORDER BY generationNumber ASC")
    suspend fun all(): List<MonsterGenerationEntity>

    @Query("SELECT COUNT(*) FROM monster_generation")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: MonsterGenerationEntity): Long

    @Update
    suspend fun update(entity: MonsterGenerationEntity)

    @Query("DELETE FROM monster_generation")
    suspend fun clear()
}

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entry WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, createdAt ASC")
    suspend fun between(startDate: String, endDate: String): List<FoodEntryEntity>

    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY createdAt ASC")
    suspend fun byDate(date: String): List<FoodEntryEntity>

    @Query("SELECT * FROM food_entry ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<FoodEntryEntity>

    @Insert
    suspend fun insert(entity: FoodEntryEntity): Long

    @Query("DELETE FROM food_entry WHERE entryId = :entryId")
    suspend fun delete(entryId: Long)

    @Query("DELETE FROM food_entry")
    suspend fun clear()
}

@Dao
interface CustomFoodDao {
    @Query("SELECT * FROM custom_food ORDER BY createdAt DESC")
    suspend fun all(): List<CustomFoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomFoodEntity)

    @Query("DELETE FROM custom_food")
    suspend fun clear()
}

@Dao
interface FoodFavoriteDao {
    @Query("SELECT foodId FROM food_favorite")
    suspend fun all(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: FoodFavoriteEntity)

    @Query("DELETE FROM food_favorite WHERE foodId = :foodId")
    suspend fun remove(foodId: String)

    @Query("DELETE FROM food_favorite")
    suspend fun clear()
}

@Dao
interface InteractionStateDao {
    @Query("SELECT * FROM interaction_state WHERE id = ${InteractionStateEntity.SINGLETON_ID}")
    suspend fun get(): InteractionStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InteractionStateEntity)

    @Query("DELETE FROM interaction_state")
    suspend fun clear()
}

@Dao
interface DiscoveredFormDao {
    @Query("SELECT * FROM discovered_form")
    suspend fun all(): List<DiscoveredFormEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<DiscoveredFormEntity>)

    @Query("DELETE FROM discovered_form")
    suspend fun clear()
}

@Dao
interface LegacyStatsDao {
    @Query("SELECT * FROM legacy_stats WHERE id = ${LegacyStatsEntity.SINGLETON_ID}")
    suspend fun get(): LegacyStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LegacyStatsEntity)

    @Query("DELETE FROM legacy_stats")
    suspend fun clear()
}
