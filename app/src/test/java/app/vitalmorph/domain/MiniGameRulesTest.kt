package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MiniGameRulesTest {
    private val today: LocalDate = LocalDate.of(2026, 7, 12)

    @Test
    fun `core catch numbers are a deterministic permutation of 1 to 25`() {
        val numbers = MiniGameRules.coreCatchNumbers(seed = 42)
        assertEquals(MiniGameRules.CORE_CATCH_CELLS, numbers.size)
        // 決定的: 同じseedなら同じ配置
        assertEquals(numbers, MiniGameRules.coreCatchNumbers(seed = 42))
        // 1..25 の並べ替え(重複なし・全数含む)
        assertEquals((1..MiniGameRules.CORE_CATCH_CELLS).toList(), numbers.sorted())
        // seedが違えば並びも変わる(散らばっている)
        assertTrue(numbers != MiniGameRules.coreCatchNumbers(seed = 7))
    }

    @Test
    fun `core catch time limits scale by difficulty`() {
        assertEquals(60_000L, MiniGameRules.coreCatchTimeLimitMs(MiniGameDifficulty.EASY))
        assertEquals(40_000L, MiniGameRules.coreCatchTimeLimitMs(MiniGameDifficulty.NORMAL))
        assertEquals(28_000L, MiniGameRules.coreCatchTimeLimitMs(MiniGameDifficulty.HARD))
        assertEquals(20_000L, MiniGameRules.coreCatchTimeLimitMs(MiniGameDifficulty.ONI))
    }

    @Test
    fun `core catch cleared includes the exact limit boundary`() {
        val limit = MiniGameRules.coreCatchTimeLimitMs(MiniGameDifficulty.NORMAL)
        assertTrue(MiniGameRules.coreCatchCleared(limit - 1, MiniGameDifficulty.NORMAL))
        assertTrue(MiniGameRules.coreCatchCleared(limit, MiniGameDifficulty.NORMAL))
        assertFalse(MiniGameRules.coreCatchCleared(limit + 1, MiniGameDifficulty.NORMAL))
    }

    @Test
    fun `success rewards scale by difficulty`() {
        assertEquals(2, MiniGameRules.successMood(MiniGameDifficulty.EASY))
        assertEquals(3, MiniGameRules.successMood(MiniGameDifficulty.NORMAL))
        assertEquals(4, MiniGameRules.successMood(MiniGameDifficulty.HARD))
        assertEquals(5, MiniGameRules.successMood(MiniGameDifficulty.ONI))
        assertEquals(1, MiniGameRules.successBond(MiniGameDifficulty.EASY))
        assertEquals(1, MiniGameRules.successBond(MiniGameDifficulty.NORMAL))
        assertEquals(2, MiniGameRules.successBond(MiniGameDifficulty.HARD))
        assertEquals(2, MiniGameRules.successBond(MiniGameDifficulty.ONI))
    }

    @Test
    fun `core catch succeeds only when reaching 25`() {
        val full = MiniGameRules.resultFor(MiniGameKind.CORE_CATCH, 25, MiniGameDifficulty.HARD)
        assertTrue(full.success)
        assertEquals(25, full.score)
        assertEquals(MiniGameDifficulty.HARD, full.difficulty)
        val short = MiniGameRules.resultFor(MiniGameKind.CORE_CATCH, 24, MiniGameDifficulty.HARD)
        assertFalse(short.success)
        // clamp: 上限は25、下限は0
        val over = MiniGameRules.resultFor(MiniGameKind.CORE_CATCH, 999, MiniGameDifficulty.NORMAL)
        assertEquals(MiniGameRules.CORE_CATCH_CELLS, over.score)
        assertTrue(over.success)
        val under = MiniGameRules.resultFor(MiniGameKind.CORE_CATCH, -5, MiniGameDifficulty.NORMAL)
        assertEquals(0, under.score)
        assertFalse(under.success)
    }

    @Test
    fun `resultFor defaults difficulty to normal`() {
        val result = MiniGameRules.resultFor(MiniGameKind.MEAL_BALANCE, MiniGameRules.MEAL_SUCCESS_SCORE)
        assertEquals(MiniGameDifficulty.NORMAL, result.difficulty)
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
        assertEquals(MiniGameRules.CORE_CATCH_CELLS, clamped.score)
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
