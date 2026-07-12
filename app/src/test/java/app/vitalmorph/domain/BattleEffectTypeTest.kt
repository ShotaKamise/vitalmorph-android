package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * イベント→エフェクト種類のマッピング(U11)が仕様どおりであることを保証する。
 */
class BattleEffectTypeTest {
    @Test
    fun `small damage maps to slash`() {
        assertEquals(BattleEffectType.SLASH, effectFor(BattleEventKind.DAMAGE_DEALT, 12))
        assertEquals(BattleEffectType.SLASH, effectFor(BattleEventKind.DAMAGE_DEALT, 39))
    }

    @Test
    fun `high damage maps to burst at threshold`() {
        assertEquals(BattleEffectType.BURST, effectFor(BattleEventKind.DAMAGE_DEALT, 40))
        assertEquals(BattleEffectType.BURST, effectFor(BattleEventKind.DAMAGE_DEALT, 88))
    }

    @Test
    fun `guard maps to shield and heal maps to heal`() {
        assertEquals(BattleEffectType.SHIELD, effectFor(BattleEventKind.GUARD, 0))
        assertEquals(BattleEffectType.HEAL, effectFor(BattleEventKind.HEAL, 0))
    }

    @Test
    fun `announce and move have no effect`() {
        assertNull(effectFor(BattleEventKind.ANNOUNCE, 0))
        assertNull(effectFor(BattleEventKind.MOVE, 30))
        assertNull(effectFor(BattleEventKind.ITEM, 0))
    }

    @Test
    fun `event overload matches kind and damage overload`() {
        val event = TurnEvent(
            actor = BattleActor.PLAYER,
            kind = BattleEventKind.DAMAGE_DEALT,
            label = "必殺",
            damage = 55,
        )
        assertEquals(BattleEffectType.BURST, effectFor(event))
    }
}
