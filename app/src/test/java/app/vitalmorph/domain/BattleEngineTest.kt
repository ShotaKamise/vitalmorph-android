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
    fun `resolving a turn emits ordered turn events`() {
        val initial = BattleEngine.startTournament(monster, metrics, 42)
        val updated = BattleEngine.useMove(initial, "core_strike")
        assertTrue("events should be recorded", updated.lastTurnEvents.isNotEmpty())
    }

    @Test
    fun `priority move makes the player act first`() {
        // ガードシフトは優先度2。相手が満タンで回復せず、素早さでも上回るため必ずプレイヤーが先。
        val initial = BattleEngine.startTournament(monster, metrics, 42)
        assertTrue(initial.playerSpeed >= initial.opponentSpeed)
        val updated = BattleEngine.useMove(initial, "guard_shift")
        assertEquals(BattleActor.PLAYER, updated.lastTurnEvents.first().actor)
    }

    @Test
    fun `slower player lets the opponent act first`() {
        // 素早さを相手より大幅に下げ、優先度0の技を使うと、CPUの行動種別に関わらず相手が先手。
        val initial = BattleEngine.startTournament(monster, metrics, 42)
            .copy(playerSpeed = 1, opponentSpeed = 99)
        val updated = BattleEngine.useMove(initial, "core_strike")
        assertEquals(BattleActor.OPPONENT, updated.lastTurnEvents.first().actor)
    }

    @Test
    fun `damage and heal events reconcile with final hp`() {
        val initial = BattleEngine.startTournament(monster, metrics, 42)
        val updated = BattleEngine.useMove(initial, "core_strike")
        val events = updated.lastTurnEvents
        // 相手へのダメージ(通常攻撃はtargetHpAfterを持つ)と相手の回復。
        val damageToOpponent = events
            .filter { it.actor == BattleActor.PLAYER && it.kind == BattleEventKind.DAMAGE_DEALT && it.targetHpAfter >= 0 }
            .sumOf { it.damage }
        val healToOpponent = events
            .filter { it.actor == BattleActor.OPPONENT && it.kind == BattleEventKind.HEAL }
            .sumOf { it.heal }
        // プレイヤーへのダメージ(CPU攻撃)、プレイヤーの反動(actorHpAfterを持つ自傷)、プレイヤーの回復。
        val damageToPlayer = events
            .filter { it.actor == BattleActor.OPPONENT && it.kind == BattleEventKind.DAMAGE_DEALT }
            .sumOf { it.damage } +
            events.filter { it.actor == BattleActor.PLAYER && it.kind == BattleEventKind.DAMAGE_DEALT && it.targetHpAfter < 0 }
                .sumOf { it.damage }
        val healToPlayer = events
            .filter { it.actor == BattleActor.PLAYER && it.kind == BattleEventKind.HEAL }
            .sumOf { it.heal }
        assertEquals(initial.opponentHp - damageToOpponent + healToOpponent, updated.opponentHp)
        assertEquals(initial.playerHp - damageToPlayer + healToPlayer, updated.playerHp)
    }

    @Test
    fun `turn events reset when advancing to the next round`() {
        val initial = BattleEngine.startTournament(monster, metrics, 42).copy(
            opponentHp = 1,
            playerAttack = 100,
        )
        val won = BattleEngine.useMove(initial, "core_strike")
        assertTrue(won.lastTurnEvents.isNotEmpty())
        val next = BattleEngine.nextRound(won)
        assertTrue("round transition clears events", next.lastTurnEvents.isEmpty())
    }

    @Test
    fun `higher week scales opponent stats up for the same seed`() {
        val week1 = BattleEngine.startTournament(monster, metrics, 42, week = 1)
        val week4 = BattleEngine.startTournament(monster, metrics, 42, week = 4)
        assertTrue(week1.opponentAttack < week4.opponentAttack)
        assertTrue(week1.opponentHp < week4.opponentHp)
        assertTrue(week1.opponentDefense <= week4.opponentDefense)
        assertTrue(week1.opponentSpeed <= week4.opponentSpeed)
    }

    @Test
    fun `opponent form is chosen from the week pool`() {
        for (week in 1..4) {
            val battle = BattleEngine.startTournament(monster, metrics, 42, week = week)
            assertTrue("opponentFormId should be set", battle.opponentFormId.isNotBlank())
            val pool = EvolutionEngine.opponentPoolFor(week).map { it.id }
            assertTrue("opponent must come from the week pool", battle.opponentFormId in pool)
            assertEquals(week, battle.week)
        }
    }

    @Test
    fun `practice flag defaults to false and can be set`() {
        assertEquals(false, BattleEngine.startTournament(monster, metrics, 42).practice)
        assertEquals(true, BattleEngine.startTournament(monster, metrics, 42, practice = true).practice)
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
