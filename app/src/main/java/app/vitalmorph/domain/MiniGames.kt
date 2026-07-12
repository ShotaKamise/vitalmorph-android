package app.vitalmorph.domain

import kotlin.math.abs
import kotlin.random.Random

/** ミニゲームの種類(IMPLEMENTATION_PLAN.md Phase 3)。1回60〜90秒。 */
enum class MiniGameKind(val label: String, val summary: String) {
    CORE_CATCH("コアキャッチ", "光るコアが消える前にタップしてキャッチ！"),
    PULSE_TRAINING("パルストレーニング", "パルスがリングに重なる瞬間にタップ！"),
    MEAL_BALANCE("ミールバランス", "食べ物のいちばん多い栄養素を当てよう！"),
}

/** ミールバランスで答える主要栄養素。 */
enum class MacroNutrient(val label: String) {
    PROTEIN("たんぱく質"),
    FAT("脂質"),
    CARBS("炭水化物"),
}

data class MealQuestion(val foodName: String, val answer: MacroNutrient)

data class MiniGameResult(
    val kind: MiniGameKind,
    val score: Int,
    val maxScore: Int,
    val success: Boolean,
)

/**
 * ミニゲームの純ロジック。タイミングや描画はUI側、判定・スコア・成否はここに置く。
 * 報酬の1日上限は [InteractionEngine.onMiniGame] が管理する。
 */
object MiniGameRules {
    /** 機嫌・絆へ反映されるプレイ回数の上限(1日3回)。 */
    const val REWARDS_PER_DAY = 3

    // ---- コアキャッチ: 3x3マスに次々現れるコアをタップ ----
    const val CORE_CATCH_ROUNDS = 20
    const val CORE_CATCH_CELLS = 9
    const val CORE_CATCH_SHOW_MS = 1_100L
    const val CORE_CATCH_INTERVAL_MS = 350L
    const val CORE_CATCH_SUCCESS_SCORE = 12

    /** 出現マスの列。同じマスが連続しないよう決定的に生成する。 */
    fun coreCatchCells(seed: Int): List<Int> {
        val random = Random(seed)
        val cells = mutableListOf<Int>()
        repeat(CORE_CATCH_ROUNDS) {
            var next = random.nextInt(CORE_CATCH_CELLS)
            if (cells.isNotEmpty() && next == cells.last()) {
                next = (next + 1 + random.nextInt(CORE_CATCH_CELLS - 1)) % CORE_CATCH_CELLS
            }
            cells += next
        }
        return cells
    }

    // ---- パルストレーニング: リングが閉じる瞬間(progress=1.0)にタップ ----
    const val PULSE_ROUNDS = 10
    const val PULSE_CYCLE_MS = 1_400

    /** タップ時の進行度(0.0〜1.0)から得点を判定する。1.0ちょうどが最高。 */
    fun pulseJudge(progress: Float): Int {
        val error = abs(1f - progress)
        return when {
            error <= 0.08f -> 3
            error <= 0.18f -> 2
            error <= 0.30f -> 1
            else -> 0
        }
    }

    const val PULSE_MAX_SCORE = PULSE_ROUNDS * 3
    const val PULSE_SUCCESS_SCORE = 16

    // ---- ミールバランス: 主要栄養素あてクイズ ----
    val mealQuestions: List<MealQuestion> = listOf(
        MealQuestion("鶏むね肉", MacroNutrient.PROTEIN),
        MealQuestion("ゆで卵", MacroNutrient.PROTEIN),
        MealQuestion("木綿豆腐", MacroNutrient.PROTEIN),
        MealQuestion("サケの切り身", MacroNutrient.PROTEIN),
        MealQuestion("マグロの刺身", MacroNutrient.PROTEIN),
        MealQuestion("納豆", MacroNutrient.PROTEIN),
        MealQuestion("ギリシャヨーグルト", MacroNutrient.PROTEIN),
        MealQuestion("バター", MacroNutrient.FAT),
        MealQuestion("オリーブオイル", MacroNutrient.FAT),
        MealQuestion("アーモンド", MacroNutrient.FAT),
        MealQuestion("アボカド", MacroNutrient.FAT),
        MealQuestion("マヨネーズ", MacroNutrient.FAT),
        MealQuestion("くるみ", MacroNutrient.FAT),
        MealQuestion("ごはん", MacroNutrient.CARBS),
        MealQuestion("食パン", MacroNutrient.CARBS),
        MealQuestion("うどん", MacroNutrient.CARBS),
        MealQuestion("パスタ", MacroNutrient.CARBS),
        MealQuestion("バナナ", MacroNutrient.CARBS),
        MealQuestion("じゃがいも", MacroNutrient.CARBS),
        MealQuestion("さつまいも", MacroNutrient.CARBS),
    )

    const val MEAL_ROUNDS = 10
    const val MEAL_SUCCESS_SCORE = 7

    /** 出題順を決定的にシャッフルして10問選ぶ。 */
    fun mealRound(seed: Int): List<MealQuestion> =
        mealQuestions.shuffled(Random(seed)).take(MEAL_ROUNDS)

    fun maxScoreFor(kind: MiniGameKind): Int = when (kind) {
        MiniGameKind.CORE_CATCH -> CORE_CATCH_ROUNDS
        MiniGameKind.PULSE_TRAINING -> PULSE_MAX_SCORE
        MiniGameKind.MEAL_BALANCE -> MEAL_ROUNDS
    }

    fun resultFor(kind: MiniGameKind, score: Int): MiniGameResult {
        val max = maxScoreFor(kind)
        val threshold = when (kind) {
            MiniGameKind.CORE_CATCH -> CORE_CATCH_SUCCESS_SCORE
            MiniGameKind.PULSE_TRAINING -> PULSE_SUCCESS_SCORE
            MiniGameKind.MEAL_BALANCE -> MEAL_SUCCESS_SCORE
        }
        val clamped = score.coerceIn(0, max)
        return MiniGameResult(kind, clamped, max, clamped >= threshold)
    }
}
