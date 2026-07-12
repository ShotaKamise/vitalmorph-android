package app.vitalmorph.data

import app.vitalmorph.data.db.DiscoveredFormEntity
import app.vitalmorph.data.db.InteractionStateEntity
import app.vitalmorph.data.db.LegacyStatsEntity
import app.vitalmorph.data.db.MonsterGenerationEntity
import app.vitalmorph.data.db.TrainerProfileEntity
import app.vitalmorph.data.db.VitaMorphDatabase
import app.vitalmorph.domain.GenerationPlan
import app.vitalmorph.domain.GenerationPlanner
import app.vitalmorph.domain.InteractionState
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
     * シーズン完了時に現在世代を閉じ、系譜用の記録(最終形態・大会順位・継承内容)を残す。
     * 次の世代は、次回の [ensureCurrentGeneration] が新しいseasonStartを検出して
     * ランダムな性別で孵化する(機嫌・絆は初期値に戻る)。
     */
    suspend fun closeCurrentGeneration(
        endDate: LocalDate,
        finalFormId: String? = null,
        finalPlacement: Int? = null,
        awardedHp: Int = 0,
        awardedAttack: Int = 0,
        awardedDefense: Int = 0,
        awardedSpeed: Int = 0,
    ) {
        val dao = database.monsterGenerationDao()
        dao.current()?.let { open ->
            dao.update(
                open.copy(
                    seasonEnd = endDate.toString(),
                    finalFormId = finalFormId ?: open.finalFormId,
                    finalPlacement = finalPlacement ?: open.finalPlacement,
                    awardedHp = awardedHp,
                    awardedAttack = awardedAttack,
                    awardedDefense = awardedDefense,
                    awardedSpeed = awardedSpeed,
                ),
            )
        }
    }

    /** すべての世代(進行中を含む)を世代番号順で返す。系譜画面用。 */
    suspend fun allGenerations(): List<MonsterGeneration> =
        database.monsterGenerationDao().all().map { it.toDomain() }

    /**
     * 世代の機嫌・絆を保存する。同じ世代のみ更新し、性別や開始日は変更しない。
     */
    suspend fun updateMoodBond(generation: MonsterGeneration) {
        database.monsterGenerationDao().update(MonsterGenerationEntity.fromDomain(generation))
    }

    suspend fun interactionState(): InteractionState =
        database.interactionStateDao().get()?.toDomain() ?: InteractionState()

    suspend fun saveInteractionState(state: InteractionState) {
        database.interactionStateDao().upsert(InteractionStateEntity.fromDomain(state))
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

    /** 図鑑(v0.11)で発見済みのフォームID一覧を返す。 */
    suspend fun discoveredFormIds(): Set<String> =
        database.discoveredFormDao().all().map { it.formId }.toSet()

    /**
     * フォームの発見を記録する。既に発見済みのIDはIGNORE挿入で初回記録を残す。
     * firstSeenAtは現在時刻。空リストは何もしない。
     */
    suspend fun recordDiscoveries(formIds: List<String>, generationNumber: Int) {
        if (formIds.isEmpty()) return
        val timestamp = now()
        val entities = formIds.distinct().map { id ->
            DiscoveredFormEntity(formId = id, firstSeenAt = timestamp, generationNumber = generationNumber)
        }
        database.discoveredFormDao().insertAll(entities)
    }

    /** すべてのRoomデータを削除する。SharedPreferencesの初期化とあわせて使う。 */
    suspend fun clearAll() {
        database.trainerProfileDao().clear()
        database.monsterGenerationDao().clear()
        database.legacyStatsDao().clear()
        database.interactionStateDao().clear()
        database.discoveredFormDao().clear()
    }

    private suspend fun insert(generation: MonsterGeneration): MonsterGeneration {
        val id = database.monsterGenerationDao().insert(MonsterGenerationEntity.fromDomain(generation))
        return generation.copy(generationId = id)
    }
}
