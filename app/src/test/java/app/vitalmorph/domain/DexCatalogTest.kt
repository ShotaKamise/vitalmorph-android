package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DexCatalogTest {
    private val sections = DexCatalog.sections
    private fun section(title: String) = sections.first { it.title == title }

    @Test
    fun `全71体がセクションへ過不足なく含まれる`() {
        val total = sections.sumOf { it.forms.size }
        assertEquals(71, total)
        assertEquals(71, DexCatalog.totalForms)
        assertEquals(EvolutionEngine.allForms.size, total)
    }

    @Test
    fun `フォームの重複がない`() {
        val ids = sections.flatMap { it.forms }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `セクションの区分と件数`() {
        assertEquals(3, sections.size)
        assertEquals(7, section("共通").forms.size)
        assertEquals(28, section("人型ルート").forms.size)
        assertEquals(36, section("動物ルート").forms.size)
    }

    @Test
    fun `共通は幼生と成長体のみで性別マークを持たない`() {
        val common = section("共通").forms
        assertTrue(common.all { it.stage == MonsterStage.BABY || it.stage == MonsterStage.FAMILY })
        assertTrue(common.all { DexCatalog.sexMark(it.id) == null })
    }

    @Test
    fun `人型は前半がオス後半がメスで並ぶ`() {
        val humanoid = section("人型ルート").forms
        assertEquals(14, humanoid.count { it.id.endsWith("_m") })
        assertEquals(14, humanoid.count { it.id.endsWith("_f") })
        val firstFemaleIndex = humanoid.indexOfFirst { it.id.endsWith("_f") }
        // 先頭14体はすべて♂、それ以降が♀。
        assertTrue(humanoid.take(firstFemaleIndex).all { it.id.endsWith("_m") })
        assertTrue(humanoid.drop(firstFemaleIndex).all { it.id.endsWith("_f") })
        assertEquals("♂", DexCatalog.sexMark(humanoid.first().id))
        assertEquals("♀", DexCatalog.sexMark(humanoid.last().id))
    }

    @Test
    fun `動物は前半がオス後半がメスで並ぶ`() {
        val beast = section("動物ルート").forms
        assertEquals(18, beast.count { EvolutionEngine.animalFormSex[it.id] == MonsterSex.MALE })
        assertEquals(18, beast.count { EvolutionEngine.animalFormSex[it.id] == MonsterSex.FEMALE })
        val firstFemaleIndex = beast.indexOfFirst { EvolutionEngine.animalFormSex[it.id] == MonsterSex.FEMALE }
        assertTrue(beast.take(firstFemaleIndex).all { EvolutionEngine.animalFormSex[it.id] == MonsterSex.MALE })
        assertTrue(beast.drop(firstFemaleIndex).all { EvolutionEngine.animalFormSex[it.id] == MonsterSex.FEMALE })
        // 動物ルートは全体が性別割り振り済み。
        assertTrue(beast.all { DexCatalog.sexMark(it.id) != null })
    }

    @Test
    fun `未登録IDの性別マークはnull`() {
        assertNull(DexCatalog.sexMark("morphy"))
        assertNull(DexCatalog.sexMark("unknown_id"))
    }
}
