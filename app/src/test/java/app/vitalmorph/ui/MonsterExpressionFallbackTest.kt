package app.vitalmorph.ui

import app.vitalmorph.domain.EvolutionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 表情差分画像があるフォームは専用画像へ、未制作フォームは通常画像へ戻ることを保証する。
 */
class MonsterExpressionFallbackTest {
    private val expressionReadyForms = setOf(
        "morphy",
        "leafang",
        "galvol",
        "rapizel",
        "motchigrow",
        "mossleep",
        "runpact",
    )

    @Test
    fun `common growth forms use dedicated expression artwork`() {
        val expressions = MonsterExpression.entries.filter { it != MonsterExpression.NORMAL }
        expressionReadyForms.forEach { formId ->
            val base = MonsterArtwork.resourceFor(formId)
            expressions.forEach { expression ->
                assertNotEquals(
                    "表情画像未登録: ${formId}_${expression.suffix}",
                    base,
                    MonsterArtwork.resourceFor(formId, expression),
                )
            }
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

    @Test
    fun `forms without expression assets fall back to base resource`() {
        EvolutionEngine.allForms.filter { it.id !in expressionReadyForms }.forEach { form ->
            val base = MonsterArtwork.resourceFor(form.id)
            assertEquals(
                "未制作フォームの表情フォールバック不一致: ${form.id}",
                base,
                MonsterArtwork.resourceFor(form.id, MonsterExpression.HAPPY),
            )
        }
    }
}
