package app.vitalmorph.domain

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BattleStateCodecTest {
    private val monster = EvolutionEngine.baby
    private val metrics = EvolutionMetrics(
        nutritionScore = 80,
        activityScore = 90,
        consistencyScore = 80,
        stepGoalDays = 4,
    )

    @Test
    fun `round trip reproduces a freshly started tournament`() {
        val state = BattleEngine.startTournament(monster, metrics, seed = 7)
        val restored = BattleStateCodec.fromJson(BattleStateCodec.toJson(state))
        assertEquals(state, restored)
    }

    @Test
    fun `round trip reproduces a mid battle state with cheer item and mood modifiers`() {
        // 絆80で応援アイテムを、機嫌90で開始シールド・開始エネルギーを行使する。
        var state = BattleEngine.startTournament(monster, metrics, seed = 11, mood = 90, bond = 80)
        state = BattleEngine.useMove(state, "core_strike")
        state = BattleEngine.useItem(state, "vita_tonic")
        state = BattleEngine.useMove(state, "guard_shift")
        state = BattleEngine.useItem(state, "trainer_cheer")
        val json = BattleStateCodec.toJson(state)
        val restored = BattleStateCodec.fromJson(json)
        assertEquals(state, restored)
        // シールド・開始エネルギー・アイテム在庫が実際に往復対象になっていることを確認する。
        assertEquals(state.playerStartShield, restored?.playerStartShield)
        assertEquals(state.playerStartEnergy, restored?.playerStartEnergy)
        assertEquals(state.items, restored?.items)
    }

    @Test
    fun `garbage input returns null`() {
        assertNull(BattleStateCodec.fromJson("garbage"))
    }

    @Test
    fun `wrong version returns null`() {
        val state = BattleEngine.startTournament(monster, metrics, seed = 7)
        val json = JSONObject(BattleStateCodec.toJson(state))
        json.put("version", BattleStateCodec.VERSION + 1)
        assertNull(BattleStateCodec.fromJson(json.toString()))
    }
}
