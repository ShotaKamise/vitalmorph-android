package app.vitalmorph.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.vitalmorph.domain.InteractionState
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
    val finalFormId: String? = null,
    val finalPlacement: Int? = null,
    val awardedHp: Int = 0,
    val awardedAttack: Int = 0,
    val awardedDefense: Int = 0,
    val awardedSpeed: Int = 0,
) {
    fun toDomain() = MonsterGeneration(
        generationId = generationId,
        generationNumber = generationNumber,
        sex = MonsterSex.valueOf(sex),
        seasonStart = LocalDate.parse(seasonStart),
        seasonEnd = seasonEnd?.let(LocalDate::parse),
        mood = MonsterGeneration.clampMood(mood),
        bond = MonsterGeneration.clampBond(bond),
        finalFormId = finalFormId,
        finalPlacement = finalPlacement,
        awardedHp = awardedHp,
        awardedAttack = awardedAttack,
        awardedDefense = awardedDefense,
        awardedSpeed = awardedSpeed,
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
            finalFormId = generation.finalFormId,
            finalPlacement = generation.finalPlacement,
            awardedHp = generation.awardedHp,
            awardedAttack = generation.awardedAttack,
            awardedDefense = generation.awardedDefense,
            awardedSpeed = generation.awardedSpeed,
        )
    }
}

@Entity(tableName = "interaction_state")
data class InteractionStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastInteractionAt: Long,
    val consecutiveTouches: Int,
    val touchRewardCountToday: Int,
    val conversationCountToday: Int,
    val miniGameRewardCountToday: Int,
    val lastDailyResetDate: String?,
) {
    fun toDomain() = InteractionState(
        lastInteractionAt = lastInteractionAt,
        consecutiveTouches = consecutiveTouches,
        touchRewardCountToday = touchRewardCountToday,
        conversationCountToday = conversationCountToday,
        miniGameRewardCountToday = miniGameRewardCountToday,
        lastDailyResetDate = lastDailyResetDate?.let(LocalDate::parse),
    )

    companion object {
        const val SINGLETON_ID = 1

        fun fromDomain(state: InteractionState) = InteractionStateEntity(
            id = SINGLETON_ID,
            lastInteractionAt = state.lastInteractionAt,
            consecutiveTouches = state.consecutiveTouches,
            touchRewardCountToday = state.touchRewardCountToday,
            conversationCountToday = state.conversationCountToday,
            miniGameRewardCountToday = state.miniGameRewardCountToday,
            lastDailyResetDate = state.lastDailyResetDate?.toString(),
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
