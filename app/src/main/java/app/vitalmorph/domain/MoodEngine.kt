package app.vitalmorph.domain

/**
 * 機嫌の帯域。IMPLEMENTATION_PLAN.md Phase 3の基準と同じ区分で、
 * Phase 2では会話トーンと進化分岐に使い、バトル補正はPhase 3で追加する。
 */
enum class MoodBand(val label: String, val range: IntRange) {
    GREAT("絶好調", 80..100),
    GOOD("好調", 60..79),
    NORMAL("通常", 40..59),
    LOW("不調", 20..39),
    BAD("元気がない", 0..19),
}

/** 絆の帯域。会話の口調と応援演出(Phase 3)に使う。 */
enum class BondBand(val label: String, val range: IntRange) {
    DISTANT("様子見", 0..19),
    FRIENDLY("仲良し", 20..59),
    DEVOTED("相棒", 60..100),
}

/**
 * 機嫌(0〜100)と絆(0〜100)の変動ルール。
 * - 変動は必ずクランプし、絆は交流でのみ増える(自然減少しない)。
 * - 1日の獲得回数は InteractionState 側の上限で制御し、連打や時刻変更で無限に増えない。
 */
object MoodEngine {
    /** タッチ: 喜び反応の機嫌変化。 */
    const val TOUCH_HAPPY_MOOD = 2

    /** タッチ: 照れ反応の機嫌変化。 */
    const val TOUCH_SHY_MOOD = 1

    /** タッチ: 嫌がり反応の機嫌変化(しつこいタッチ)。 */
    const val TOUCH_ANNOYED_MOOD = -2

    /** タッチ1回で深まる絆(1日の上限あり)。 */
    const val TOUCH_BOND = 1

    /** 1日に機嫌・絆へ反映されるタッチ回数の上限。 */
    const val TOUCH_REWARDS_PER_DAY = 10

    /** 1日に機嫌・絆へ反映される会話回数の上限。 */
    const val DIALOGUE_REWARDS_PER_DAY = 5

    /** ミニゲーム成功時の機嫌・絆ボーナス(1日の反映上限あり)。 */
    const val MINIGAME_SUCCESS_MOOD = 4
    const val MINIGAME_SUCCESS_BOND = 2

    /** ミニゲームに挑戦した(成功に届かなかった)ときの機嫌ボーナス。挑戦を責めない。 */
    const val MINIGAME_TRY_MOOD = 1

    /** 前日に栄養か活動の記録があった場合の機嫌ボーナス。 */
    const val RECORDED_DAY_MOOD = 3

    /** 前日に記録も交流も無かった場合の機嫌低下。責めずに小さく下げるだけ。 */
    const val NEGLECTED_DAY_MOOD = -2

    fun moodBand(mood: Int): MoodBand =
        MoodBand.entries.first { MonsterGeneration.clampMood(mood) in it.range }

    fun bondBand(bond: Int): BondBand =
        BondBand.entries.first { MonsterGeneration.clampBond(bond) in it.range }

    /** 機嫌・絆の差分を適用した新しい世代を返す。範囲外はクランプする。 */
    fun applyDelta(generation: MonsterGeneration, moodDelta: Int, bondDelta: Int): MonsterGeneration =
        generation.copy(
            mood = MonsterGeneration.clampMood(generation.mood + moodDelta),
            bond = MonsterGeneration.clampBond(generation.bond + bondDelta.coerceAtLeast(0)),
        )

    /**
     * 日付が変わったときに1回だけ適用する機嫌変化。
     * 記録があれば上がり、記録も交流も無ければ少し下がる。両方無くても-2に留める。
     */
    fun dailyMoodDelta(recordedYesterday: Boolean, interactedYesterday: Boolean): Int = when {
        recordedYesterday -> RECORDED_DAY_MOOD
        interactedYesterday -> 0
        else -> NEGLECTED_DAY_MOOD
    }
}
