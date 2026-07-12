package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyAwardEngineTest {
    private val goodMetrics = EvolutionMetrics(
        nutritionScore = 75,
        activityScore = 90,
        consistencyScore = 85,
        stepGoalDays = 15,
    )

    @Test
    fun `great season earns all four candidates`() {
        val award = LegacyAwardEngine.award(goodMetrics, bond = 60, tournamentPoints = 10)
        assertEquals(1, award.hp)
        assertEquals(1, award.attack)
        assertEquals(1, award.defense)
        assertEquals(1, award.speed)
        assertEquals(4, award.total)
        assertEquals(4, award.reasons.size)
    }

    @Test
    fun `quiet season earns nothing and that is allowed`() {
        val award = LegacyAwardEngine.award(EvolutionMetrics(), bond = 0, tournamentPoints = 0)
        assertEquals(0, award.total)
        assertTrue(award.reasons.isEmpty())
    }

    @Test
    fun `four candidates are capped to three points per generation`() {
        val award = LegacyAwardEngine.award(goodMetrics, bond = 60, tournamentPoints = 10)
        val stats = LegacyStats().addingGeneration(award.hp, award.attack, award.defense, award.speed)
        val granted = stats.hpPoints + stats.attackPoints + stats.defensePoints + stats.speedPoints
        assertEquals(LegacyStats.MAX_POINTS_PER_GENERATION, granted)
        // HP→攻撃→防御の順で優先され、素早さが翌世代へ持ち越しになる。
        assertEquals(1, stats.hpPoints)
        assertEquals(1, stats.attackPoints)
        assertEquals(1, stats.defensePoints)
        assertEquals(0, stats.speedPoints)
    }

    @Test
    fun `each source has its own threshold`() {
        assertEquals(1, LegacyAwardEngine.award(EvolutionMetrics(), bond = LegacyAwardEngine.BOND_THRESHOLD, tournamentPoints = 0).hp)
        assertEquals(0, LegacyAwardEngine.award(EvolutionMetrics(), bond = LegacyAwardEngine.BOND_THRESHOLD - 1, tournamentPoints = 0).hp)
        assertEquals(1, LegacyAwardEngine.award(EvolutionMetrics(), bond = 0, tournamentPoints = 4).attack)
        assertEquals(1, LegacyAwardEngine.award(EvolutionMetrics(nutritionScore = 60), bond = 0, tournamentPoints = 0).defense)
        assertEquals(1, LegacyAwardEngine.award(EvolutionMetrics(stepGoalDays = 12), bond = 0, tournamentPoints = 0).speed)
        assertEquals(1, LegacyAwardEngine.award(EvolutionMetrics(activityScore = 70), bond = 0, tournamentPoints = 0).speed)
    }

    @Test
    fun `placement is derived from tournament points`() {
        assertEquals(1, LegacyAwardEngine.placementForPoints(10))
        assertEquals(2, LegacyAwardEngine.placementForPoints(7))
        assertEquals(4, LegacyAwardEngine.placementForPoints(4))
        assertEquals(8, LegacyAwardEngine.placementForPoints(2))
        assertNull(LegacyAwardEngine.placementForPoints(0))
    }
}
