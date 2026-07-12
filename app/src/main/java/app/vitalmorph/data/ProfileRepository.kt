package app.vitalmorph.data

import app.vitalmorph.data.db.LegacyStatsEntity
import app.vitalmorph.data.db.MonsterGenerationEntity
import app.vitalmorph.data.db.TrainerProfileEntity
import app.vitalmorph.data.db.VitaMorphDatabase
import app.vitalmorph.domain.GenerationPlan
import app.vitalmorph.domain.GenerationPlanner
import app.vitalmorph.domain.LegacyStats
import app.vitalmorph.domain.MonsterGeneration
import app.vitalmorph.domain.TrainerNameRules
import app.vitalmorph.domain.TrainerProfile
import java.time.LocalDate
import kotlin.random.Random

/**
 * トレーナープロフィール、世代(性別・機嫌・絆)、継承ポイントのRoom永続化。
 * 既存のSharedPreferences([GameStore])は変更せず、新規データのみここで扱う。
 */
class ProfileRepository(
    private val database: VitaMorphDatabase,
    private val random: Random = Random.Default,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun trainerProfile(): TrainerProfile? =
        database.trainerProfileDao().get()?.toDomain()

    /**
     * トレーナー名を保存する。呼び出し前に [TrainerNameRules.validate] で検証すること。
     */
    suspend fun setTrainerName(rawName: String): TrainerProfile {
        val name = TrainerNameRules.normalize(rawName)
        require(TrainerNameRules.validate(name) == null) { "invalid trainer name" }
        val dao = database.trainerProfileDao()
        val timestamp = now()
        val existing = dao.get()
        val entity = TrainerProfileEntity(
            name = name,
            createdAt = existing?.createdAt ?: timestamp,
            updatedAt = timestamp,
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    /**
     * 保存済みゲーム状態に対応する現在世代を返す。存在しなければ作成して永続化する。
     * - 既存SharedPreferencesユーザーの初回起動では、seasonStart+固定ソルトから決定的に性別を決める。
     * - 以降の孵化では約50%のランダムで決め、その結果を保存する。
     * - 同じ世代で性別が変わることはない。
     */
    suspend fun ensureCurrentGeneration(stored: StoredGameState): MonsterGeneration {
        val dao = database.monsterGenerationDao()
        val plan = GenerationPlanner.plan(
            currentGeneration = dao.current()?.toDomain(),
            hasAnyGenerationHistory = dao.count() > 0,
            seasonStart = stored.seasonStart,
            completedSeasons = stored.trainerProgress.completedSeasons,
            random = random,
        )
        return when (plan) {
            is GenerationPlan.Keep -> plan.generation
            is GenerationPlan.Create -> insert(plan.create)
            is GenerationPlan.CloseAndCreate -> {
                dao.update(MonsterGenerationEntity.fromDomain(plan.toClose))
                insert(plan.create)
            }
        }
    }

    /**
     * シーズン完了時に現在世代を閉じる。
     * 次の世代は、次回の [ensureCurrentGeneration] が新しいseasonStartを検出して
     * ランダムな性別で孵化する。
     */
    suspend fun closeCurrentGeneration(endDate: LocalDate) {
        val dao = database.monsterGenerationDao()
        dao.current()?.let { open ->
            dao.update(open.copy(seasonEnd = endDate.toString()))
        }
    }

    suspend fun legacyStats(): LegacyStats =
        database.legacyStatsDao().get()?.toDomain() ?: LegacyStats()

    suspend fun saveLegacyStats(stats: LegacyStats) {
        // 継承ポイントは決して減らさない(DATA_MODEL.mdの規約)。
        val current = legacyStats()
        val safe = LegacyStats(
            hpPoints = maxOf(current.hpPoints, stats.hpPoints).coerceAtMost(LegacyStats.MAX_POINTS_PER_STAT),
            attackPoints = maxOf(current.attackPoints, stats.attackPoints).coerceAtMost(LegacyStats.MAX_POINTS_PER_STAT),
            defensePoints = maxOf(current.defensePoints, stats.defensePoints).coerceAtMost(LegacyStats.MAX_POINTS_PER_STAT),
            speedPoints = maxOf(current.speedPoints, stats.speedPoints).coerceAtMost(LegacyStats.MAX_POINTS_PER_STAT),
            totalGenerations = maxOf(current.totalGenerations, stats.totalGenerations),
        )
        database.legacyStatsDao().upsert(LegacyStatsEntity.fromDomain(safe))
    }

    /** すべてのRoomデータを削除する。SharedPreferencesの初期化とあわせて使う。 */
    suspend fun clearAll() {
        database.trainerProfileDao().clear()
        database.monsterGenerationDao().clear()
        database.legacyStatsDao().clear()
    }

    private suspend fun insert(generation: MonsterGeneration): MonsterGeneration {
        val id = database.monsterGenerationDao().insert(MonsterGenerationEntity.fromDomain(generation))
        return generation.copy(generationId = id)
    }
}
