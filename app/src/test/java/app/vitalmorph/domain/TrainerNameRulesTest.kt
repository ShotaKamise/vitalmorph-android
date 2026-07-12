package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TrainerNameRulesTest {

    @Test
    fun `accepts valid names`() {
        assertNull(TrainerNameRules.validate("ショウタ"))
        assertNull(TrainerNameRules.validate("A"))
        assertNull(TrainerNameRules.validate("あ".repeat(12)))
        assertNull(TrainerNameRules.validate("  トレーナー  "))
    }

    @Test
    fun `rejects empty and blank names`() {
        assertNotNull(TrainerNameRules.validate(""))
        assertNotNull(TrainerNameRules.validate("   "))
    }

    @Test
    fun `rejects names longer than twelve characters`() {
        assertNotNull(TrainerNameRules.validate("あ".repeat(13)))
        assertNull(TrainerNameRules.validate("あ".repeat(12)))
    }

    @Test
    fun `rejects newline and control characters`() {
        assertNotNull(TrainerNameRules.validate("たろ\nう"))
        assertNotNull(TrainerNameRules.validate("たろ\rう"))
        assertNotNull(TrainerNameRules.validate("た\u0000ろう"))
        assertNotNull(TrainerNameRules.validate("た\tろう"))
    }

    @Test
    fun `normalize trims surrounding whitespace`() {
        assertEquals("ショウタ", TrainerNameRules.normalize("  ショウタ "))
    }
}
