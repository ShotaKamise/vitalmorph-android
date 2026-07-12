package app.vitalmorph.domain

/**
 * 大会開始時のプレイヤー能力値と機嫌・絆補正のスナップショット。
 *
 * [BattleEngine.startTournament] の内部計算をそのまま抽出した純データで、
 * バトル開始と詳細ステータス画面(U10)が同じ数値を参照するための単一の真実源。
 */
data class BattleStats(
    /** 最大HP(継承ボーナス適用後)。 */
    val maxHp: Int,
    /** こうげき(継承ボーナス適用後)。 */
    val attack: Int,
    /** ぼうぎょ(継承ボーナス適用後)。 */
    val defense: Int,
    /** すばやさ(継承ボーナス+機嫌補正適用後の実効値)。 */
    val speed: Int,
    /** 機嫌補正を含まない素のすばやさ。 */
    val baseSpeed: Int,
    /** 機嫌によるすばやさの増減(実効値-素の値)。0なら補正なし。 */
    val moodSpeedDelta: Int,
    /** 各試合の開始エネルギー(通常3、機嫌が「元気がない」帯で2)。 */
    val startEnergy: Int,
    /** 機嫌「絶好調」帯で各試合開始時に張られる小さなシールドの有無。 */
    val startShield: Boolean,
    /** 絆が[BattleEngine.CHEER_BOND_THRESHOLD]以上で応援アイテムが使えるか。 */
    val cheerAvailable: Boolean,
)

/**
 * 大会のプレイヤー能力算出を純関数として提供する(U10)。
 * IMPLEMENTATION_PLAN.md Phase 3の機嫌補正基準(能力への影響は最大±5%相当)に従う。
 */
object BattleStatsCalculator {
    /** バトルの最大(通常開始)エネルギー。 */
    const val MAX_ENERGY = 3

    /**
     * フォーム・成長指標・機嫌・絆・継承から大会開始時の能力を求める。
     * ここに集約した式が[BattleEngine.startTournament]の唯一の算出元になる。
     */
    fun statsFor(
        form: MonsterForm,
        metrics: EvolutionMetrics,
        mood: Int,
        bond: Int,
        legacy: LegacyStats,
    ): BattleStats {
        val stageBonus = form.stage.ordinal * 12
        // 継承ポイントは1ptにつき基礎能力+1%(各能力15%上限は保存時に保証済み)。
        fun inherit(base: Int, points: Int): Int = base * (100 + points) / 100
        val maxHp = inherit(120 + metrics.consistencyScore / 2 + metrics.nutritionScore / 3 + stageBonus, legacy.hpPoints)
        val attack = inherit(18 + metrics.activityScore.coerceAtMost(130) / 8 + metrics.nutritionScore / 20 + stageBonus / 3, legacy.attackPoints)
        val defense = inherit(10 + metrics.nutritionScore / 12 + metrics.consistencyScore / 15 + stageBonus / 4, legacy.defensePoints)
        val baseSpeed = inherit(10 + metrics.activityScore.coerceAtMost(130) / 10 + metrics.stepGoalDays * 2, legacy.speedPoints)
        val moodBand = MoodEngine.moodBand(mood)
        val speedDelta = (baseSpeed * 5 / 100).coerceAtLeast(1)
        val speed = when (moodBand) {
            MoodBand.GOOD -> baseSpeed + speedDelta
            MoodBand.LOW -> (baseSpeed - speedDelta).coerceAtLeast(1)
            else -> baseSpeed
        }
        return BattleStats(
            maxHp = maxHp,
            attack = attack,
            defense = defense,
            speed = speed,
            baseSpeed = baseSpeed,
            moodSpeedDelta = speed - baseSpeed,
            startEnergy = if (moodBand == MoodBand.BAD) MAX_ENERGY - 1 else MAX_ENERGY,
            startShield = moodBand == MoodBand.GREAT,
            cheerAvailable = bond >= BattleEngine.CHEER_BOND_THRESHOLD,
        )
    }
}
