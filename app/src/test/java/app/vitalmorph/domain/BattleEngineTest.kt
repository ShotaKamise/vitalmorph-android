package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleEngineTest {
    @Test
    fun `tournament is deterministic for a season seed`() {
        val monster = MonsterForm("test", "テスト", MonsterStage.FINAL, MonsterFamily.BALANCE, "万能", "", 0xFF69E6A6)
        val metrics = EvolutionMetrics(80, 90, 90)
        val first = BattleEngine.runTournament(monster, metrics, 42)
        val second = BattleEngine.runTournament(monster, metrics, 42)
        assertEquals(first, second)
        assertTrue(first.matches.size in 1..3)
    }
}
