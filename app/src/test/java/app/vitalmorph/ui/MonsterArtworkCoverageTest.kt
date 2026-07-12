package app.vitalmorph.ui

import app.vitalmorph.domain.EvolutionEngine
import app.vitalmorph.domain.HumanoidRoster
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Codexのアセット差し替え後も、進化エンジンが返す全フォームIDに
 * 専用画像が割り当てられていることを保証する(未登録IDはモルフィへフォールバックするため、
 * フォールバックと一致しないことで専用画像の存在を確認できる)。
 */
class MonsterArtworkCoverageTest {
    private val fallback = MonsterArtwork.resourceFor("__unknown__")

    @Test
    fun `all forms in the catalog have dedicated artwork`() {
        EvolutionEngine.allForms.filter { it.id != "morphy" }.forEach { form ->
            assertNotEquals(
                "画像未登録: ${form.id}(${form.name})",
                fallback,
                MonsterArtwork.resourceFor(form.id),
            )
        }
    }

    @Test
    fun `all 28 humanoids have dedicated artwork`() {
        HumanoidRoster.all.forEach { form ->
            assertNotEquals("画像未登録: ${form.id}", fallback, MonsterArtwork.resourceFor(form.id))
        }
        assertTrue(HumanoidRoster.all.size == 28)
    }
}
