package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleEngineTest {
    private val monster = MonsterForm(
        "test", "テスト", MonsterStage.FINAL, MonsterFamily.BALANCE, "万能", "", 0xFF69E6A6,
    )
    private val metrics = EvolutionMetrics(nutritionScore = 80, activityScore = 90, consistencyScore = 90)

    @Test
    fun `tournament start is deterministic for a season seed`() {
        val first = BattleEngine.startTournament(monster, metrics, 42)
        val second = BattleEngine.startTournament(monster, metrics, 42)
        assertEquals(first, second)
        assertEquals(4, first.moves.size)
        assertEquals(3, first.items.size)
    }

    @Test
    fun `choosing a move advances a turn and deals damage`() {
        val initial = BattleEngine.startTournament(monster, metrics, 42)
        val updated = BattleEngine.useMove(initial, "core_strike")
        assertEquals(initial.turn + 1, updated.turn)
        assertTrue(updated.opponentHp < initial.opponentHp)
        assertTrue(updated.log.any { it.contains("コアストライク") })
    }

    @Test
    fun `using an item consumes its tournament stock`() {
        val initial = BattleEngine.startTournament(monster, metrics, 42).copy(playerHp = 50)
        val updated = BattleEngine.useItem(initial, "vita_tonic")
        assertEquals(1, updated.items.first { it.item.id == "vita_tonic" }.remaining)
        assertTrue(updated.playerHp > 50)
    }

    @Test
    fun `winning a round unlocks the next match and keeps inventory`() {
        val initial = BattleEngine.startTournament(monster, metrics, 42).copy(
            opponentHp = 1,
            playerAttack = 100,
        )
        val won = BattleEngine.useMove(initial, "core_strike")
        assertEquals(BattleOutcome.ROUND_WON, won.outcome)
        val next = BattleEngine.nextRound(won)
        assertEquals(1, next.roundIndex)
        assertEquals(BattleOutcome.IN_PROGRESS, next.outcome)
        assertEquals(won.items, next.items)
        assertTrue(next.playerHp >= won.playerHp)
    }

    @Test
    fun `final victory produces tournament result`() {
        val initial = BattleEngine.startTournament(monster, metrics, 42).copy(
            roundIndex = 2,
            roundLabel = "決勝",
            opponentHp = 1,
            playerAttack = 100,
        )
        val won = BattleEngine.useMove(initial, "core_strike")
        val result = BattleEngine.resultFor(won)
        assertEquals(BattleOutcome.TOURNAMENT_WON, won.outcome)
        assertEquals(1, result?.placement)
        assertEquals(10, result?.tournamentPoints)
    }

}
