package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainerRankEngineTest {
    @Test
    fun `master requires thirteen completed seasons`() {
        assertEquals("ダイヤモンド", TrainerRankEngine.rankFor(TrainerProgress(1_300, 12)).name)
        assertEquals("マスター", TrainerRankEngine.rankFor(TrainerProgress(1_100, 13)).name)
    }

    @Test
    fun `steady year reaches master threshold`() {
        assertEquals("マスター", TrainerRankEngine.rankFor(TrainerProgress(85 * 13, 13)).name)
    }
}
