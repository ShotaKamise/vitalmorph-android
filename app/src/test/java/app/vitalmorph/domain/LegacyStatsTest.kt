package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyStatsTest {

    @Test
    fun `one generation grants at most three points`() {
        val stats = LegacyStats().addingGeneration(hp = 3, attack = 3, defense = 3, speed = 3)
        val total = stats.hpPoints + stats.attackPoints + stats.defensePoints + stats.speedPoints
        assertEquals(LegacyStats.MAX_POINTS_PER_GENERATION, total)
        assertEquals(1, stats.totalGenerations)
    }

    @Test
    fun `single stat is capped at fifteen points`() {
        var stats = LegacyStats()
        repeat(10) { stats = stats.addingGeneration(hp = 3) }
        assertEquals(LegacyStats.MAX_POINTS_PER_STAT, stats.hpPoints)
        assertEquals(10, stats.totalGenerations)
    }

    @Test
    fun `points never decrease when adding generations`() {
        var stats = LegacyStats(hpPoints = 5, attackPoints = 2, defensePoints = 0, speedPoints = 1, totalGenerations = 4)
        val before = stats
        stats = stats.addingGeneration(defense = 2, speed = 1)
        assertTrue(stats.hpPoints >= before.hpPoints)
        assertTrue(stats.attackPoints >= before.attackPoints)
        assertTrue(stats.defensePoints >= before.defensePoints)
        assertTrue(stats.speedPoints >= before.speedPoints)
        assertEquals(before.totalGenerations + 1, stats.totalGenerations)
    }

    @Test
    fun `negative requests grant nothing`() {
        val stats = LegacyStats().addingGeneration(hp = -3, attack = -1)
        assertEquals(0, stats.hpPoints)
        assertEquals(0, stats.attackPoints)
        assertEquals(1, stats.totalGenerations)
    }

    @Test
    fun `mood and bond are clamped to their ranges`() {
        assertEquals(0, MonsterGeneration.clampMood(-5))
        assertEquals(100, MonsterGeneration.clampMood(140))
        assertEquals(50, MonsterGeneration.clampMood(50))
        assertEquals(0, MonsterGeneration.clampBond(-1))
        assertEquals(100, MonsterGeneration.clampBond(101))
    }
}
