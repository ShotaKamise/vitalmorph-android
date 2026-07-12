package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodBattleTest {
    private val monster = EvolutionEngine.baby
    private val metrics = EvolutionMetrics(
        nutritionScore = 80,
        activityScore = 90,
        consistencyScore = 80,
        stepGoalDays = 4,
    )

    private fun start(mood: Int = 50, bond: Int = 0) =
        BattleEngine.startTournament(monster, metrics, seed = 7, mood = mood, bond = bond)

    @Test
    fun `normal mood has no modifiers`() {
        val state = start(mood = 50)
        assertEquals(3, state.playerEnergy)
        assertFalse(state.playerGuarding)
        assertFalse(state.items.any { it.item.id == "trainer_cheer" })
    }

    @Test
    fun `great mood starts each round with a shield`() {
        val state = start(mood = 90)
        assertTrue(state.playerGuarding)
        assertTrue(state.playerStartShield)
        assertEquals(3, state.playerEnergy)
    }

    @Test
    fun `good mood slightly raises speed within five percent`() {
        val normal = start(mood = 50)
        val good = start(mood = 70)
        assertTrue(good.playerSpeed > normal.playerSpeed)
        assertTrue(good.playerSpeed - normal.playerSpeed <= (normal.playerSpeed * 5 / 100).coerceAtLeast(1))
    }

    @Test
    fun `low mood slightly lowers speed`() {
        val normal = start(mood = 50)
        val low = start(mood = 30)
        assertTrue(low.playerSpeed < normal.playerSpeed)
        assertTrue(normal.playerSpeed - low.playerSpeed <= (normal.playerSpeed * 5 / 100).coerceAtLeast(1))
    }

    @Test
    fun `bad mood lowers starting energy but never blocks entry`() {
        val state = start(mood = 5)
        assertEquals(2, state.playerEnergy)
        assertEquals(2, state.playerStartEnergy)
        assertEquals(BattleOutcome.IN_PROGRESS, state.outcome)
    }

    @Test
    fun `high bond adds one cheer for the tournament`() {
        val state = start(bond = BattleEngine.CHEER_BOND_THRESHOLD)
        val cheer = state.items.first { it.item.id == "trainer_cheer" }
        assertEquals(1, cheer.remaining)
    }

    @Test
    fun `cheer heals and restores energy once`() {
        val state = start(bond = 80, mood = 5)
        val damaged = state.copy(playerHp = state.playerMaxHp / 2)
        val after = BattleEngine.useItem(damaged, "trainer_cheer")
        // 同じターンにCPUの反撃も入るため、回復自体はログで検証する。
        assertTrue(after.log.any { it.contains("応援が届いた") && it.contains("HPが25回復") })
        assertEquals(0, after.items.first { it.item.id == "trainer_cheer" }.remaining)
        val again = BattleEngine.useItem(after, "trainer_cheer")
        assertTrue(again.log.last().contains("残っていない"))
    }
}
