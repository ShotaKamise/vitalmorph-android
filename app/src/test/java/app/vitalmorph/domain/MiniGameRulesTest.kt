package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MiniGameRulesTest {
    private val today: LocalDate = LocalDate.of(2026, 7, 12)

    @Test
    fun `core catch cells are deterministic and never repeat consecutively`() {
        val cells = MiniGameRules.coreCatchCells(seed = 42)
        assertEquals(MiniGameRules.CORE_CATCH_ROUNDS, cells.size)
        assertEquals(cells, MiniGameRules.coreCatchCells(seed = 42))
        cells.zipWithNext().forEach { (a, b) ->
            assertTrue("cell repeated: $a", a != b)
        }
        assertTrue(cells.all { it in 0 until MiniGameRules.CORE_CATCH_CELLS })
    }

    @Test
    fun `pulse judge rewards accurate timing`() {
        assertEquals(3, MiniGameRules.pulseJudge(1.0f))
        assertEquals(3, MiniGameRules.pulseJudge(0.93f))
        assertEquals(2, MiniGameRules.pulseJudge(0.85f))
        assertEquals(1, MiniGameRules.pulseJudge(0.72f))
        assertEquals(0, MiniGameRules.pulseJudge(0.5f))
        assertEquals(0, MiniGameRules.pulseJudge(0.0f))
    }

    @Test
    fun `meal round picks ten unique deterministic questions`() {
        val round = MiniGameRules.mealRound(seed = 3)
        assertEquals(MiniGameRules.MEAL_ROUNDS, round.size)
        assertEquals(round.map { it.foodName }.toSet().size, round.size)
        assertEquals(round, MiniGameRules.mealRound(seed = 3))
    }

    @Test
    fun `meal questions cover all three macros`() {
        val answers = MiniGameRules.mealQuestions.map { it.answer }.toSet()
        assertEquals(MacroNutrient.entries.toSet(), answers)
        assertEquals(
            MiniGameRules.mealQuestions.size,
            MiniGameRules.mealQuestions.map { it.foodName }.toSet().size,
        )
    }

    @Test
    fun `result clamps score and applies thresholds`() {
        val success = MiniGameRules.resultFor(MiniGameKind.MEAL_BALANCE, MiniGameRules.MEAL_SUCCESS_SCORE)
        assertTrue(success.success)
        val fail = MiniGameRules.resultFor(MiniGameKind.MEAL_BALANCE, MiniGameRules.MEAL_SUCCESS_SCORE - 1)
        assertFalse(fail.success)
        val clamped = MiniGameRules.resultFor(MiniGameKind.CORE_CATCH, 999)
        assertEquals(MiniGameRules.CORE_CATCH_ROUNDS, clamped.score)
    }

    @Test
    fun `mini game rewards stop after three per day`() {
        var state = InteractionState(lastDailyResetDate = today)
        var rewarded = 0
        repeat(MiniGameRules.REWARDS_PER_DAY + 2) { index ->
            val result = InteractionEngine.onMiniGame(state, 1_000L + index, today)
            state = result.state
            if (result.rewarded) rewarded++
        }
        assertEquals(MiniGameRules.REWARDS_PER_DAY, rewarded)
        assertEquals(MiniGameRules.REWARDS_PER_DAY, state.miniGameRewardCountToday)
    }

    @Test
    fun `mini game rewards reset on a new day but not on clock rollback`() {
        val used = InteractionState(
            miniGameRewardCountToday = MiniGameRules.REWARDS_PER_DAY,
            lastDailyResetDate = today,
        )
        val nextDay = InteractionEngine.onMiniGame(used, 2_000L, today.plusDays(1))
        assertTrue(nextDay.rewarded)
        val rolledBack = InteractionEngine.onMiniGame(used, 2_000L, today.minusDays(1))
        assertFalse(rolledBack.rewarded)
    }
}
