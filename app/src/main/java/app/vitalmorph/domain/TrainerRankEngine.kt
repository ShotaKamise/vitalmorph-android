package app.vitalmorph.domain

import kotlin.math.roundToInt

object TrainerRankEngine {
    private data class Rule(val name: String, val xp: Int, val seasons: Int)

    private val rules = listOf(
        Rule("ビギナー", 0, 0),
        Rule("ブロンズ", 100, 1),
        Rule("シルバー", 250, 3),
        Rule("ゴールド", 450, 5),
        Rule("プラチナ", 700, 8),
        Rule("ダイヤモンド", 900, 10),
        Rule("マスター", 1_100, 13),
    )

    fun rankFor(progress: TrainerProgress): TrainerRank {
        val index = rules.indexOfLast { progress.xp >= it.xp && progress.completedSeasons >= it.seasons }.coerceAtLeast(0)
        val current = rules[index]
        val next = rules.getOrNull(index + 1)
        return TrainerRank(current.name, current.xp, current.seasons, next?.xp)
    }

    fun seasonPoints(days: List<DailyHealthData>, metrics: EvolutionMetrics, tournamentPoints: Int): Int {
        if (days.isEmpty()) return tournamentPoints.coerceIn(0, 10)
        val recordPoints = (days.count { it.hasNutrition && it.hasActivity } / 28.0 * 30).roundToInt().coerceIn(0, 30)
        val nutritionPoints = (metrics.nutritionScore / 100.0 * 25).roundToInt().coerceIn(0, 25)
        val activityPoints = (metrics.activityScore.coerceAtMost(100) / 100.0 * 25).roundToInt().coerceIn(0, 25)
        val safeConsistency = (metrics.consistencyScore / 100.0 * 10).roundToInt().coerceIn(0, 10)
        return recordPoints + nutritionPoints + activityPoints + safeConsistency + tournamentPoints.coerceIn(0, 10)
    }
}
