package app.vitalmorph.domain

import kotlin.math.abs
import kotlin.random.Random

/** ミニゲームの種類(IMPLEMENTATION_PLAN.md Phase 3)。1回60〜90秒。 */
enum class MiniGameKind(val label: String, val summary: String) {
    CORE_CATCH("コアキャッチ", "1から25まで順番にタップ！時間内にコンプリートでクリア！"),
    PULSE_TRAINING("パルストレーニング", "パルスがリングに重なる瞬間にタップ！"),
    MEAL_BALANCE("ミールバランス", "食べ物のいちばん多い栄養素を当てよう！"),
}

/**
 * ミニゲーム共通の難易度(U5で追加、U6/U7で全ゲームが利用する)。
 * 難易度は制限時間や出題テンポなど各ゲームの調整と、成功時の報酬量に影響する。
 */
enum class MiniGameDifficulty(val label: String) {
    EASY("初級"),
    NORMAL("中級"),
    HARD("上級"),
    ONI("鬼"),
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
    val difficulty: MiniGameDifficulty = MiniGameDifficulty.NORMAL,
)

/**
 * ミニゲームの純ロジック。タイミングや描画はUI側、判定・スコア・成否はここに置く。
 * 報酬の1日上限は [InteractionEngine.onMiniGame] が管理する。
 */
object MiniGameRules {
    /** 機嫌・絆へ反映されるプレイ回数の上限(1日3回)。難易度に関わらず共通。 */
    const val REWARDS_PER_DAY = 3

    /** 成功時の機嫌ボーナス。難易度が上がるほど大きい(1日3回の反映上限は共通のまま)。 */
    fun successMood(difficulty: MiniGameDifficulty): Int = when (difficulty) {
        MiniGameDifficulty.EASY -> 2
        MiniGameDifficulty.NORMAL -> 3
        MiniGameDifficulty.HARD -> 4
        MiniGameDifficulty.ONI -> 5
    }

    /** 成功時の絆ボーナス。難易度が上がるほど大きい(1日3回の反映上限は共通のまま)。 */
    fun successBond(difficulty: MiniGameDifficulty): Int = when (difficulty) {
        MiniGameDifficulty.EASY, MiniGameDifficulty.NORMAL -> 1
        MiniGameDifficulty.HARD, MiniGameDifficulty.ONI -> 2
    }

    // ---- コアキャッチ: 5x5マスの1〜25を順番にタップ(U5でリニューアル) ----
    /** グリッドのマス数(5x5)。得点は到達した番号(1〜25)。 */
    const val CORE_CATCH_CELLS = 25

    /** ミス1回あたりの経過時間ペナルティ。実時間へ加算して制限時間と比べる。 */
    const val CORE_CATCH_MISTAKE_PENALTY_MS = 1_000L

    /**
     * 1〜25をseedで決定的にシャッフルした配置。indexがグリッド位置(row-major 5x5)、
     * 値がそのマスに表示される番号。
     */
    fun coreCatchNumbers(seed: Int): List<Int> =
        (1..CORE_CATCH_CELLS).shuffled(Random(seed))

    /** 難易度ごとの制限時間(ミリ秒)。 */
    fun coreCatchTimeLimitMs(difficulty: MiniGameDifficulty): Long = when (difficulty) {
        MiniGameDifficulty.EASY -> 60_000L
        MiniGameDifficulty.NORMAL -> 40_000L
        MiniGameDifficulty.HARD -> 28_000L
        MiniGameDifficulty.ONI -> 20_000L
    }

    /** 経過時間(ミス加算込み)が制限時間以内ならクリア。境界(==)はクリア扱い。 */
    fun coreCatchCleared(elapsedMs: Long, difficulty: MiniGameDifficulty): Boolean =
        elapsedMs <= coreCatchTimeLimitMs(difficulty)

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
        MiniGameKind.CORE_CATCH -> CORE_CATCH_CELLS
        MiniGameKind.PULSE_TRAINING -> PULSE_MAX_SCORE
        MiniGameKind.MEAL_BALANCE -> MEAL_ROUNDS
    }

    fun resultFor(kind: MiniGameKind, score: Int): MiniGameResult =
        resultFor(kind, score, MiniGameDifficulty.NORMAL)

    /**
     * 難易度つきの成否判定。難易度は報酬とメッセージにのみ使い、成功条件は種類ごとに固定。
     * コアキャッチは25(全マス到達)のみ成功。制限時間の判定はUI側で行い、時間内に
     * 完走できたときだけscore=25を渡す(間に合わなければ到達番号を渡す)。
     */
    fun resultFor(kind: MiniGameKind, score: Int, difficulty: MiniGameDifficulty): MiniGameResult {
        val max = maxScoreFor(kind)
        val threshold = when (kind) {
            MiniGameKind.CORE_CATCH -> CORE_CATCH_CELLS
            MiniGameKind.PULSE_TRAINING -> PULSE_SUCCESS_SCORE
            MiniGameKind.MEAL_BALANCE -> MEAL_SUCCESS_SCORE
        }
        val clamped = score.coerceIn(0, max)
        return MiniGameResult(kind, clamped, max, clamped >= threshold, difficulty)
    }
}
