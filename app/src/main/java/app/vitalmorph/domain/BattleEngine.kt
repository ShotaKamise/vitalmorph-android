package app.vitalmorph.domain

import kotlin.random.Random

object BattleEngine {
    private val opponentNames = listOf(
        "アイアンホーン", "クリムゾウル", "ナイトグライド", "コバルトスク", "ロックバイター", "ミラージュパウ",
    )

    fun runTournament(monster: MonsterForm, metrics: EvolutionMetrics, seed: Int): TournamentResult {
        val random = Random(seed)
        val basePower = 70 + metrics.nutritionScore / 3 + metrics.activityScore.coerceAtMost(130) / 3 +
            metrics.consistencyScore / 4 + monster.stage.ordinal * 18
        val matches = mutableListOf<BattleMatch>()
        val rounds = listOf("準々決勝", "準決勝", "決勝")

        for ((roundIndex, round) in rounds.withIndex()) {
            val opponent = opponentNames[(seed + roundIndex).mod(opponentNames.size)]
            val opponentBase = basePower - 8 + roundIndex * 8
            val playerScore = (basePower * (0.90 + random.nextDouble() * 0.22)).toInt()
            val opponentScore = (opponentBase * (0.90 + random.nextDouble() * 0.22)).toInt()
            val won = playerScore >= opponentScore
            matches += BattleMatch(round, opponent, playerScore, opponentScore, won)
            if (!won) {
                val placement = when (roundIndex) {
                    0 -> 8
                    1 -> 4
                    else -> 2
                }
                val points = when (placement) {
                    2 -> 7
                    4 -> 4
                    else -> 2
                }
                return TournamentResult(placement, points, matches)
            }
        }
        return TournamentResult(1, 10, matches)
    }
}
