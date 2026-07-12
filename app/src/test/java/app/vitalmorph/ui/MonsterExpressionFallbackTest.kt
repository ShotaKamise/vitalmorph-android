package app.vitalmorph.ui

import app.vitalmorph.domain.EvolutionEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 表情差分画像がまだ存在しない現状(expressionResourcesが空)では、
 * HAPPY/SAD いずれの表情も通常画像へフォールバックすることを保証する。
 * Codexが画像を追加してマップへ登録すると、この振る舞いは自動的に変わる。
 */
class MonsterExpressionFallbackTest {
    @Test
    fun `happy and sad fall back to base while map is empty`() {
        EvolutionEngine.allForms.forEach { form ->
            val base = MonsterArtwork.resourceFor(form.id)
            assertEquals(
                "HAPPYフォールバック不一致: ${form.id}",
                base,
                MonsterArtwork.resourceFor(form.id, MonsterExpression.HAPPY),
            )
            assertEquals(
                "SADフォールバック不一致: ${form.id}",
                base,
                MonsterArtwork.resourceFor(form.id, MonsterExpression.SAD),
            )
        }
    }

    @Test
    fun `normal expression equals base resource`() {
        val id = EvolutionEngine.allForms.first().id
        assertEquals(
            MonsterArtwork.resourceFor(id),
            MonsterArtwork.resourceFor(id, MonsterExpression.NORMAL),
        )
    }
}
