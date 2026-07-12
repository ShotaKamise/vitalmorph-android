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
        val result = MiniGameRules.resultFor(
            MiniGameKind.MEAL_BALANCE,
            MiniGameRules.mealSuccessScore(MiniGameDifficulty.NORMAL),
        )
        assertEquals(MiniGameDifficulty.NORMAL, result.difficulty)
    }

    @Test
    fun `pulse cycle speeds up with difficulty`() {
        assertEquals(1_400, MiniGameRules.pulseCycleMs(MiniGameDifficulty.EASY))
        assertEquals(1_000, MiniGameRules.pulseCycleMs(MiniGameDifficulty.NORMAL))
        assertEquals(750, MiniGameRules.pulseCycleMs(MiniGameDifficulty.HARD))
        assertEquals(550, MiniGameRules.pulseCycleMs(MiniGameDifficulty.ONI))
    }

    @Test
    fun `pulse judge windows for normal match legacy behavior`() {
        val d = MiniGameDifficulty.NORMAL
        assertEquals(3, MiniGameRules.pulseJudge(1.0f, d))
        assertEquals(3, MiniGameRules.pulseJudge(0.93f, d)) // error 0.07 <= 0.08
        assertEquals(2, MiniGameRules.pulseJudge(0.85f, d)) // error 0.15 <= 0.18
        assertEquals(1, MiniGameRules.pulseJudge(0.72f, d)) // error 0.28 <= 0.30
        assertEquals(0, MiniGameRules.pulseJudge(0.5f, d))
        assertEquals(0, MiniGameRules.pulseJudge(0.0f, d))
    }

    @Test
    fun `pulse judge windows for easy are wide with clear band edges`() {
        val d = MiniGameDifficulty.EASY // 3点<=0.10 / 2点<=0.20 / 1点<=0.32
        // 3点↔2点(誤差0.10付近): 内側は3点、外側は2点
        assertEquals(3, MiniGameRules.pulseJudge(0.905f, d)) // 誤差約0.095
        assertEquals(2, MiniGameRules.pulseJudge(0.895f, d)) // 誤差約0.105
        // 2点↔1点(誤差0.20付近)
        assertEquals(2, MiniGameRules.pulseJudge(0.81f, d)) // 誤差約0.19
        assertEquals(1, MiniGameRules.pulseJudge(0.795f, d)) // 誤差約0.205
        // 1点↔0点(誤差0.32付近)
        assertEquals(1, MiniGameRules.pulseJudge(0.69f, d)) // 誤差約0.31
        assertEquals(0, MiniGameRules.pulseJudge(0.675f, d)) // 誤差約0.325
    }

    @Test
    fun `pulse judge windows for hard are narrow`() {
        val d = MiniGameDifficulty.HARD // 3点<=0.06 / 2点<=0.13 / 1点<=0.22
        assertEquals(3, MiniGameRules.pulseJudge(0.945f, d)) // 誤差約0.055
        assertEquals(2, MiniGameRules.pulseJudge(0.875f, d)) // 誤差約0.125
        assertEquals(1, MiniGameRules.pulseJudge(0.785f, d)) // 誤差約0.215
        assertEquals(0, MiniGameRules.pulseJudge(0.78f, d)) // 誤差約0.22超で0点
    }

    @Test
    fun `pulse judge windows for oni are tightest with clear band edges`() {
        val d = MiniGameDifficulty.ONI // 3点<=0.04 / 2点<=0.09 / 1点<=0.15
        // 3点↔2点(誤差0.04付近)
        assertEquals(3, MiniGameRules.pulseJudge(0.965f, d)) // 誤差約0.035
        assertEquals(2, MiniGameRules.pulseJudge(0.955f, d)) // 誤差約0.045
        // 2点↔1点(誤差0.09付近)
        assertEquals(2, MiniGameRules.pulseJudge(0.915f, d)) // 誤差約0.085
        assertEquals(1, MiniGameRules.pulseJudge(0.905f, d)) // 誤差約0.095
        // 1点↔0点(誤差0.15付近)
        assertEquals(1, MiniGameRules.pulseJudge(0.855f, d)) // 誤差約0.145
        assertEquals(0, MiniGameRules.pulseJudge(0.845f, d)) // 誤差約0.155
    }

    @Test
    fun `pulse success score rises with difficulty`() {
        assertEquals(14, MiniGameRules.pulseSuccessScore(MiniGameDifficulty.EASY))
        assertEquals(16, MiniGameRules.pulseSuccessScore(MiniGameDifficulty.NORMAL))
        assertEquals(18, MiniGameRules.pulseSuccessScore(MiniGameDifficulty.HARD))
        assertEquals(21, MiniGameRules.pulseSuccessScore(MiniGameDifficulty.ONI))
    }

    @Test
    fun `pulse result uses difficulty specific success threshold`() {
        for (d in MiniGameDifficulty.entries) {
            val threshold = MiniGameRules.pulseSuccessScore(d)
            val atThreshold = MiniGameRules.resultFor(MiniGameKind.PULSE_TRAINING, threshold, d)
            assertTrue(atThreshold.success)
            assertEquals(d, atThreshold.difficulty)
            val below = MiniGameRules.resultFor(MiniGameKind.PULSE_TRAINING, threshold - 1, d)
            assertFalse(below.success)
        }
        // 2引数オーバーロードは中級として判定する(16点でクリア)。
        assertTrue(MiniGameRules.resultFor(MiniGameKind.PULSE_TRAINING, 16).success)
        assertFalse(MiniGameRules.resultFor(MiniGameKind.PULSE_TRAINING, 15).success)
    }

    @Test
    fun `meal basic pool is at least ten items and a subset of standard`() {
        assertTrue(MiniGameRules.mealQuestionsBasic.size >= 10)
        val standardNames = MiniGameRules.mealQuestionsStandard.map { it.foodName }.toSet()
        MiniGameRules.mealQuestionsBasic.forEach { q ->
            assertTrue("${q.foodName} は標準プールにない", q.foodName in standardNames)
            // 部分集合なので正解も標準と一致していること。
            val standard = MiniGameRules.mealQuestionsStandard.first { it.foodName == q.foodName }
            assertEquals(standard.answer, q.answer)
        }
    }

    @Test
    fun `meal tricky pool has at least ten items with non-null answers`() {
        assertTrue(MiniGameRules.mealQuestionsTricky.size >= 10)
        MiniGameRules.mealQuestionsTricky.forEach { q ->
            @Suppress("SENSELESS_COMPARISON")
            assertTrue("${q.foodName} の正解が未設定", q.answer != null)
        }
    }

    @Test
    fun `each meal pool covers all three macros with unique food names`() {
        for (difficulty in MiniGameDifficulty.entries) {
            val pool = MiniGameRules.mealPool(difficulty)
            val answers = pool.map { it.answer }.toSet()
            assertEquals("$difficulty プールが3栄養素を網羅していない", MacroNutrient.entries.toSet(), answers)
            assertEquals(
                "$difficulty プールに重複食材がある",
                pool.size,
                pool.map { it.foodName }.toSet().size,
            )
        }
    }

    @Test
    fun `meal pools map difficulty to the expected tier`() {
        assertEquals(MiniGameRules.mealQuestionsBasic, MiniGameRules.mealPool(MiniGameDifficulty.EASY))
        assertEquals(MiniGameRules.mealQuestionsStandard, MiniGameRules.mealPool(MiniGameDifficulty.NORMAL))
        assertEquals(
            MiniGameRules.mealQuestionsStandard + MiniGameRules.mealQuestionsTricky,
            MiniGameRules.mealPool(MiniGameDifficulty.HARD),
        )
        // 鬼は紛らわしいプールのみ。
        assertEquals(MiniGameRules.mealQuestionsTricky, MiniGameRules.mealPool(MiniGameDifficulty.ONI))
    }

    @Test
    fun `meal round picks ten unique deterministic questions from the right pool`() {
        for (difficulty in MiniGameDifficulty.entries) {
            val round = MiniGameRules.mealRound(seed = 3, difficulty = difficulty)
            assertEquals(MiniGameRules.MEAL_ROUNDS, round.size)
            // 10問すべて食材名が重複しない。
            assertEquals(round.map { it.foodName }.toSet().size, round.size)
            // 決定的: 同じ(seed, 難易度)なら同じ出題。
            assertEquals(round, MiniGameRules.mealRound(seed = 3, difficulty = difficulty))
            // 出題はその難易度のプールから引かれている。
            val poolNames = MiniGameRules.mealPool(difficulty).map { it.foodName }.toSet()
            assertTrue(round.all { it.foodName in poolNames })
        }
    }

    @Test
    fun `meal success score rises with difficulty`() {
        assertEquals(7, MiniGameRules.mealSuccessScore(MiniGameDifficulty.EASY))
        assertEquals(7, MiniGameRules.mealSuccessScore(MiniGameDifficulty.NORMAL))
        assertEquals(8, MiniGameRules.mealSuccessScore(MiniGameDifficulty.HARD))
        assertEquals(9, MiniGameRules.mealSuccessScore(MiniGameDifficulty.ONI))
    }

    @Test
    fun `meal result uses difficulty specific success threshold`() {
        for (d in MiniGameDifficulty.entries) {
            val threshold = MiniGameRules.mealSuccessScore(d)
            val atThreshold = MiniGameRules.resultFor(MiniGameKind.MEAL_BALANCE, threshold, d)
            assertTrue(atThreshold.success)
            assertEquals(d, atThreshold.difficulty)
            val below = MiniGameRules.resultFor(MiniGameKind.MEAL_BALANCE, threshold - 1, d)
            assertFalse(below.success)
        }
        // 2引数オーバーロードは中級として判定する(7問でクリア)。
        assertTrue(MiniGameRules.resultFor(MiniGameKind.MEAL_BALANCE, 7).success)
        assertFalse(MiniGameRules.resultFor(MiniGameKind.MEAL_BALANCE, 6).success)
    }

    @Test
    fun `result clamps score and applies thresholds`() {
        val success = MiniGameRules.resultFor(
            MiniGameKind.MEAL_BALANCE,
            MiniGameRules.mealSuccessScore(MiniGameDifficulty.NORMAL),
        )
        assertTrue(success.success)
        val fail = MiniGameRules.resultFor(
            MiniGameKind.MEAL_BALANCE,
            MiniGameRules.mealSuccessScore(MiniGameDifficulty.NORMAL) - 1,
        )
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
