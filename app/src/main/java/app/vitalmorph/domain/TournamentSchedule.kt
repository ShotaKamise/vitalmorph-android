package app.vitalmorph.domain

import kotlin.math.roundToInt

/**
 * 週末大会制のスケジュール(U9 / Issue #12・#11)。
 *
 * 大会はシーズン日7・14・21・28にのみ開催され、それ以外の日はトレーニング期間。
 * 週ごとに1回だけ順位ポイント(最大10)を記録し、シーズンXP寄与は4週平均とする。
 * 相手はプレイヤーの成長段階に合わせ、週係数で能力をスケールする。
 */
object TournamentSchedule {
    /** シーズンは4週で構成される(28日)。 */
    const val WEEKS = 4

    /** 大会の日(シーズン日7・14・21・28)かどうか。 */
    fun isTournamentDay(seasonDay: Int): Boolean = seasonDay in 1..28 && seasonDay % 7 == 0

    /** シーズン日が属する週(1..4)。 */
    fun weekOf(seasonDay: Int): Int = ((seasonDay.coerceIn(1, 28) - 1) / 7) + 1

    /** 次の大会まで残り何日か。大会当日は0。 */
    fun daysUntilTournament(seasonDay: Int): Int {
        val day = seasonDay.coerceIn(1, 28)
        val nextTournamentDay = ((day + 6) / 7) * 7
        return nextTournamentDay - day
    }

    /** 週ごとの相手能力スケール(%): 第1週60 / 第2週80 / 第3週100 / 第4週115。 */
    fun weekMultiplierPercent(week: Int): Int = when (week) {
        1 -> 60
        2 -> 80
        3 -> 100
        else -> 115
    }

    /**
     * シーズンの大会ポイント(既存のTrainerRankEngineスケール0..10を維持)。
     * 記録済みの週ポイントを4週固定分母で平均し、四捨五入する。
     * 未参加週は0として平均する(4週固定分母)。空マップは0。
     */
    fun seasonTournamentPoints(weeklyPoints: Map<Int, Int>): Int {
        if (weeklyPoints.isEmpty()) return 0
        val sum = (1..WEEKS).sumOf { (weeklyPoints[it] ?: 0).coerceIn(0, 10) }
        return (sum.toDouble() / WEEKS).roundToInt()
    }
}
