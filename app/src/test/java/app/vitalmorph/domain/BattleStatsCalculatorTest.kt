package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [BattleStatsCalculator] が [BattleEngine.startTournament] と完全に同じ能力・補正を
 * 算出することを保証する(U10のリファクタで数値が変わっていないこと)。
 */
class BattleStatsCalculatorTest {
    private val monster = EvolutionEngine.baby
    private val metrics = EvolutionMetrics(
        nutritionScore = 80,
        activityScore = 90,
        consistencyScore = 80,
        stepGoalDays = 4,
    )
    private val maxedLegacy = LegacyStats(
        hpPoints = LegacyStats.MAX_POINTS_PER_STAT,
        attackPoints = LegacyStats.MAX_POINTS_PER_STAT,
        defensePoints = LegacyStats.MAX_POINTS_PER_STAT,
        speedPoints = LegacyStats.MAX_POINTS_PER_STAT,
        totalGenerations = 5,
    )

    private fun assertMatchesStartTournament(mood: Int, bond: Int, legacy: LegacyStats) {
        val stats = BattleStatsCalculator.statsFor(monster, metrics, mood, bond, legacy)
        val state = BattleEngine.startTournament(monster, metrics, seed = 7, mood = mood, bond = bond, legacy = legacy)
        val label = "mood=$mood bond=$bond legacy=$legacy"
        assertEquals("maxHp $label", state.playerMaxHp, stats.maxHp)
        assertEquals("attack $label", state.playerAttack, stats.attack)
        assertEquals("defense $label", state.playerDefense, stats.defense)
        assertEquals("speed $label", state.playerSpeed, stats.speed)
        assertEquals("startEnergy $label", state.playerEnergy, stats.startEnergy)
        assertEquals("startShield $label", state.playerGuarding, stats.startShield)
        val cheerPresent = state.items.any { it.item.id == "trainer_cheer" }
        assertEquals("cheer $label", cheerPresent, stats.cheerAvailable)
    }

    @Test
    fun `matches across mood bands with no legacy`() {
        // NORMAL, GREAT, GOOD, LOW, BAD の各帯。
        listOf(50, 90, 70, 30, 10).forEach { mood ->
            assertMatchesStartTournament(mood = mood, bond = 0, legacy = LegacyStats())
        }
    }

    @Test
    fun `matches with maxed legacy across mood bands`() {
        listOf(50, 90, 70, 30, 10).forEach { mood ->
            assertMatchesStartTournament(mood = mood, bond = 0, legacy = maxedLegacy)
        }
    }

    @Test
    fun `cheer availability matches around the bond threshold`() {
        assertMatchesStartTournament(mood = 50, bond = BattleEngine.CHEER_BOND_THRESHOLD - 1, legacy = LegacyStats())
        assertMatchesStartTournament(mood = 50, bond = BattleEngine.CHEER_BOND_THRESHOLD, legacy = LegacyStats())
        assertMatchesStartTournament(mood = 50, bond = 100, legacy = maxedLegacy)
    }

    @Test
    fun `mood speed delta reflects the applied speed change`() {
        val normal = BattleStatsCalculator.statsFor(monster, metrics, mood = 50, bond = 0, legacy = LegacyStats())
        val good = BattleStatsCalculator.statsFor(monster, metrics, mood = 70, bond = 0, legacy = LegacyStats())
        val low = BattleStatsCalculator.statsFor(monster, metrics, mood = 30, bond = 0, legacy = LegacyStats())
        assertEquals(0, normal.moodSpeedDelta)
        assertEquals(good.speed - good.baseSpeed, good.moodSpeedDelta)
        assertEquals(low.speed - low.baseSpeed, low.moodSpeedDelta)
    }
}
