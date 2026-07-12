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
interface LegacyStatsDao {
    @Query("SELECT * FROM legacy_stats WHERE id = ${LegacyStatsEntity.SINGLETON_ID}")
    suspend fun get(): LegacyStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LegacyStatsEntity)

    @Query("DELETE FROM legacy_stats")
    suspend fun clear()
}
