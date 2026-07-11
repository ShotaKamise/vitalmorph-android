package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EvolutionEngineTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val goals = UserGoals()

    @Test
    fun `season changes stage every seven days`() {
        val days = (0L..27L).map { offset ->
            DailyHealthData(
                date = start.plusDays(offset),
                calories = 2_000.0,
                proteinGrams = 80.0,
                carbsGrams = 250.0,
                fatGrams = 60.0,
                steps = 7_000,
                exerciseMinutes = 15,
                hasNutrition = true,
                hasActivity = true,
            )
        }

        assertEquals(MonsterStage.BABY, EvolutionEngine.evaluate(days, goals, start, start.plusDays(6)).form.stage)
        assertEquals(MonsterStage.FAMILY, EvolutionEngine.evaluate(days, goals, start, start.plusDays(7)).form.stage)
        assertEquals(MonsterStage.INTERMEDIATE, EvolutionEngine.evaluate(days, goals, start, start.plusDays(14)).form.stage)
        assertEquals(MonsterStage.FINAL, EvolutionEngine.evaluate(days, goals, start, start.plusDays(21)).form.stage)
    }

    @Test
    fun `every intermediate exposes two final candidates`() {
        val days = (0L..20L).map { offset ->
            DailyHealthData(
                date = start.plusDays(offset),
                calories = 2_000.0,
                proteinGrams = 80.0,
                carbsGrams = 250.0,
                fatGrams = 60.0,
                steps = 6_500,
                exerciseMinutes = 10,
                hasNutrition = true,
                hasActivity = true,
            )
        }
        val result = EvolutionEngine.evaluate(days, goals, start, start.plusDays(20))
        assertEquals(MonsterStage.INTERMEDIATE, result.form.stage)
        assertEquals(2, result.finalCandidates.size)
        assertTrue(result.finalCandidates.all { it.stage == MonsterStage.FINAL })
    }
}
