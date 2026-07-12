package app.vitalmorph.domain

/**
 * ターンイベントから選ぶCanvasエフェクトの種類(演出専用・純粋ロジック)。
 * UI(ui/BattleEffects.kt)がこの分類を見て斬撃・閃光・シールド・回復光を描画する。
 */
enum class BattleEffectType { SLASH, BURST, SHIELD, HEAL }

/** 必殺・強打とみなすダメージのしきい値(以上でBURST/専用モーション)。 */
const val BATTLE_BURST_DAMAGE = 40

/**
 * イベント種別とダメージ量からエフェクト種類を決める純関数。
 * - DAMAGE_DEALT: 40以上ならBURST(閃光)、それ未満はSLASH(斬撃)
 * - GUARD: SHIELD、HEAL: HEAL
 * - ANNOUNCE / MOVE / ITEM 等: エフェクトなし(null)
 */
fun effectFor(kind: BattleEventKind, damage: Int): BattleEffectType? = when (kind) {
    BattleEventKind.DAMAGE_DEALT -> if (damage >= BATTLE_BURST_DAMAGE) BattleEffectType.BURST else BattleEffectType.SLASH
    BattleEventKind.GUARD -> BattleEffectType.SHIELD
    BattleEventKind.HEAL -> BattleEffectType.HEAL
    else -> null
}

/** TurnEventからエフェクト種類を決める。 */
fun effectFor(event: TurnEvent): BattleEffectType? = effectFor(event.kind, event.damage)
