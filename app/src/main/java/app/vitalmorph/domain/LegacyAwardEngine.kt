package app.vitalmorph.domain

/**
 * シーズン完了時の継承ポイント候補。各項目0か1で、合計は最大4候補。
 * 実際の付与は [LegacyStats.addingGeneration] が1世代3ポイントまでに切り詰める
 * (超過時はHP→攻撃→防御→素早さの順で優先)。
 */
data class LegacyAward(
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    /** 付与理由のユーザー向け説明。系譜画面と完了メッセージに使う。 */
    val reasons: List<String>,
) {
    val total: Int get() = hp + attack + defense + speed
}

/**
 * 28日シーズンの過ごし方から継承ポイントを決める(IMPLEMENTATION_PLAN.md Phase 4)。
 * 食事、運動、交流、大会のそれぞれから最大1ポイント。
 */
object LegacyAwardEngine {
    /** 交流: シーズン終了時の絆がこの値以上でHP+1。 */
    const val BOND_THRESHOLD = 40

    /** 大会: ベスト4以上(大会ポイント4以上)で攻撃+1。 */
    const val TOURNAMENT_POINTS_THRESHOLD = 4

    /** 食事: シーズン栄養スコアがこの値以上で防御+1。 */
    const val NUTRITION_THRESHOLD = 60

    /** 運動: 活動スコア70以上、または歩数目標達成12日以上で素早さ+1。 */
    const val ACTIVITY_THRESHOLD = 70
    const val STEP_DAYS_THRESHOLD = 12

    fun award(metrics: EvolutionMetrics, bond: Int, tournamentPoints: Int): LegacyAward {
        val reasons = mutableListOf<String>()
        val hp = if (bond >= BOND_THRESHOLD) {
            reasons += "交流を重ねた絆 → HP+1"
            1
        } else 0
        val attack = if (tournamentPoints >= TOURNAMENT_POINTS_THRESHOLD) {
            reasons += "大会での健闘 → 攻撃+1"
            1
        } else 0
        val defense = if (metrics.nutritionScore >= NUTRITION_THRESHOLD) {
            reasons += "バランスのよい食事 → 防御+1"
            1
        } else 0
        val speed = if (metrics.activityScore >= ACTIVITY_THRESHOLD || metrics.stepGoalDays >= STEP_DAYS_THRESHOLD) {
            reasons += "日々の運動 → 素早さ+1"
            1
        } else 0
        return LegacyAward(hp, attack, defense, speed, reasons)
    }

    /** 保存済みの大会ポイントから最高順位を推定する(結果画面が閉じられた後でも記録できるように)。 */
    fun placementForPoints(tournamentPoints: Int): Int? = when {
        tournamentPoints >= 10 -> 1
        tournamentPoints >= 7 -> 2
        tournamentPoints >= 4 -> 4
        tournamentPoints >= 2 -> 8
        else -> null
    }
}
