package app.vitalmorph.domain

import java.time.LocalDate
import kotlin.random.Random

/**
 * 孵化時・移行時の性別決定。
 */
object SexAssigner {
    /** 既存データ移行用の固定ソルト。変更すると移行結果が変わるため変更禁止。 */
    private const val MIGRATION_SALT = "vitalmorph-sex-v1"

    /**
     * 既存ユーザーの現在世代へ一度だけ適用する決定的な性別。
     * 保存済みの seasonStart と固定ソルトから求めるため、
     * 何度呼んでも同じ結果になり、起動ごとに乱数を引かない。
     */
    fun deterministicFor(seasonStart: LocalDate): MonsterSex {
        val hash = "$MIGRATION_SALT:$seasonStart".hashCode()
        return if (hash and 1 == 0) MonsterSex.MALE else MonsterSex.FEMALE
    }

    /** 新しい孵化時に約50%で決定する。結果は必ず永続化し、再抽選しない。 */
    fun randomHatch(random: Random = Random.Default): MonsterSex =
        if (random.nextBoolean()) MonsterSex.MALE else MonsterSex.FEMALE
}

/**
 * 現在世代をどう扱うかの決定。
 */
sealed interface GenerationPlan {
    /** 保存済みの世代をそのまま使う。性別・世代番号は変化しない。 */
    data class Keep(val generation: MonsterGeneration) : GenerationPlan

    /** 開いている世代を閉じてから、新しい世代を作成する。 */
    data class CloseAndCreate(
        val toClose: MonsterGeneration,
        val create: MonsterGeneration,
    ) : GenerationPlan

    /** 世代が存在しないため新規作成する。 */
    data class Create(val create: MonsterGeneration) : GenerationPlan
}

/**
 * 保存済みゲーム状態と既存の世代レコードから、現在世代の扱いを純粋に決定する。
 * Roomや Android に依存しないため、単体テストで移行挙動を検証できる。
 */
object GenerationPlanner {

    /**
     * @param currentGeneration 開いている(seasonEndがnullの)世代。なければnull
     * @param hasAnyGenerationHistory DBに世代レコードが1件でも存在するか
     * @param seasonStart SharedPreferencesに保存されている現在シーズンの開始日
     * @param completedSeasons 完了済みシーズン数(世代番号の算出に使う)
     * @param random 新規孵化時の性別抽選に使う乱数源
     */
    fun plan(
        currentGeneration: MonsterGeneration?,
        hasAnyGenerationHistory: Boolean,
        seasonStart: LocalDate,
        completedSeasons: Int,
        random: Random = Random.Default,
    ): GenerationPlan {
        if (currentGeneration != null) {
            // 同じシーズンなら性別・世代を維持する。再抽選は行わない。
            if (currentGeneration.seasonStart == seasonStart) {
                return GenerationPlan.Keep(currentGeneration)
            }
            // seasonStartが変わっているのに世代が開いたままの場合の安全策。
            // 旧世代を閉じ、新しい孵化としてランダムに性別を決める。
            return GenerationPlan.CloseAndCreate(
                toClose = currentGeneration.copy(seasonEnd = seasonStart),
                create = newGeneration(
                    generationNumber = completedSeasons + 1,
                    sex = SexAssigner.randomHatch(random),
                    seasonStart = seasonStart,
                ),
            )
        }

        val sex = if (hasAnyGenerationHistory) {
            // 過去世代があるのに現在世代がない = シーズン完了直後の新しい孵化。
            SexAssigner.randomHatch(random)
        } else {
            // 履歴が空 = 既存SharedPreferencesユーザーの初回移行、または新規開始。
            // 保存済みseasonStartから一度だけ決定的に求める(DATA_MODEL.mdの移行規約)。
            SexAssigner.deterministicFor(seasonStart)
        }
        return GenerationPlan.Create(
            newGeneration(
                generationNumber = completedSeasons + 1,
                sex = sex,
                seasonStart = seasonStart,
            ),
        )
    }

    private fun newGeneration(generationNumber: Int, sex: MonsterSex, seasonStart: LocalDate) =
        MonsterGeneration(
            generationNumber = generationNumber,
            sex = sex,
            seasonStart = seasonStart,
            seasonEnd = null,
        )
}
