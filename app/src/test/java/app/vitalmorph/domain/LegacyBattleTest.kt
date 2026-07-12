package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyBattleTest {
    private val monster = EvolutionEngine.baby
    private val metrics = EvolutionMetrics(
        nutritionScore = 80,
        activityScore = 90,
        consistencyScore = 80,
        stepGoalDays = 4,
    )

    private fun start(legacy: LegacyStats) =
        BattleEngine.startTournament(monster, metrics, seed = 7, legacy = legacy)

    @Test
    fun `no legacy points leaves stats unchanged`() {
        val base = start(LegacyStats())
        val alsoBase = BattleEngine.startTournament(monster, metrics, seed = 7)
        assertEquals(alsoBase.playerMaxHp, base.playerMaxHp)
        assertEquals(alsoBase.playerAttack, base.playerAttack)
    }

    @Test
    fun `each point adds about one percent`() {
        val base = start(LegacyStats())
        val boosted = start(LegacyStats(hpPoints = 10, attackPoints = 5, defensePoints = 3, speedPoints = 15))
        assertEquals(base.playerMaxHp * 110 / 100, boosted.playerMaxHp)
        assertEquals(base.playerAttack * 105 / 100, boosted.playerAttack)
        assertEquals(base.playerDefense * 103 / 100, boosted.playerDefense)
        assertEquals(base.playerSpeed * 115 / 100, boosted.playerSpeed)
    }

    @Test
    fun `max legacy stays within fifteen percent`() {
        val base = start(LegacyStats())
        val maxed = start(
            LegacyStats(
                hpPoints = LegacyStats.MAX_POINTS_PER_STAT,
                attackPoints = LegacyStats.MAX_POINTS_PER_STAT,
                defensePoints = LegacyStats.MAX_POINTS_PER_STAT,
                speedPoints = LegacyStats.MAX_POINTS_PER_STAT,
            ),
        )
        assertTrue(maxed.playerMaxHp <= base.playerMaxHp * 115 / 100)
        assertTrue(maxed.playerMaxHp > base.playerMaxHp)
    }

    @Test
    fun `legacy boost composes with mood speed modifier`() {
        val legacy = LegacyStats(speedPoints = 10)
        val normalMood = BattleEngine.startTournament(monster, metrics, seed = 7, mood = 50, legacy = legacy)
        val goodMood = BattleEngine.startTournament(monster, metrics, seed = 7, mood = 70, legacy = legacy)
        assertTrue(goodMood.playerSpeed > normalMood.playerSpeed)
    }
}
