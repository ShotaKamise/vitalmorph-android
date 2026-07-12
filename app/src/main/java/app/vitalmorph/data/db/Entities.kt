package app.vitalmorph.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.vitalmorph.domain.LegacyStats
import app.vitalmorph.domain.MonsterGeneration
import app.vitalmorph.domain.MonsterSex
import app.vitalmorph.domain.TrainerProfile
import java.time.LocalDate

@Entity(tableName = "trainer_profile")
data class TrainerProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain() = TrainerProfile(name = name, createdAt = createdAt, updatedAt = updatedAt)

    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(tableName = "monster_generation")
data class MonsterGenerationEntity(
    @PrimaryKey(autoGenerate = true) val generationId: Long = 0,
    val generationNumber: Int,
    val sex: String,
    val seasonStart: String,
    val seasonEnd: String?,
    val mood: Int,
    val bond: Int,
) {
    fun toDomain() = MonsterGeneration(
        generationId = generationId,
        generationNumber = generationNumber,
        sex = MonsterSex.valueOf(sex),
        seasonStart = LocalDate.parse(seasonStart),
        seasonEnd = seasonEnd?.let(LocalDate::parse),
        mood = MonsterGeneration.clampMood(mood),
        bond = MonsterGeneration.clampBond(bond),
    )

    companion object {
        fun fromDomain(generation: MonsterGeneration) = MonsterGenerationEntity(
            generationId = generation.generationId,
            generationNumber = generation.generationNumber,
            sex = generation.sex.name,
            seasonStart = generation.seasonStart.toString(),
            seasonEnd = generation.seasonEnd?.toString(),
            mood = MonsterGeneration.clampMood(generation.mood),
            bond = MonsterGeneration.clampBond(generation.bond),
        )
    }
}

@Entity(tableName = "legacy_stats")
data class LegacyStatsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val hpPoints: Int,
    val attackPoints: Int,
    val defensePoints: Int,
    val speedPoints: Int,
    val totalGenerations: Int,
) {
    fun toDomain() = LegacyStats(
        hpPoints = hpPoints,
        attackPoints = attackPoints,
        defensePoints = defensePoints,
        speedPoints = speedPoints,
        totalGenerations = totalGenerations,
    )

    companion object {
        const val SINGLETON_ID = 1

        fun fromDomain(stats: LegacyStats) = LegacyStatsEntity(
            id = SINGLETON_ID,
            hpPoints = stats.hpPoints,
            attackPoints = stats.attackPoints,
            defensePoints = stats.defensePoints,
            speedPoints = stats.speedPoints,
            totalGenerations = stats.totalGenerations,
        )
    }
}
