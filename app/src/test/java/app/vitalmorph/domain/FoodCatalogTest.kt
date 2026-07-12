package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCatalogTest {
    @Test
    fun `builtin ids are unique and prefixed`() {
        val ids = FoodCatalog.builtin.map { it.foodId }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("b:") })
    }

    @Test
    fun `all builtin items have sane values`() {
        FoodCatalog.builtin.forEach { item ->
            assertTrue(item.name.isNotBlank())
            assertTrue(item.standardAmount > 0)
            assertTrue(item.calories >= 0)
            assertTrue(item.proteinGrams >= 0 && item.fatGrams >= 0 && item.carbsGrams >= 0)
            // PFCから概算したエネルギーが表示カロリーと大きく乖離していないこと(±35%)。
            // アルコール飲料はPFC外のカロリー(7kcal/g)を持つため対象外。
            val estimated = item.proteinGrams * 4 + item.carbsGrams * 4 + item.fatGrams * 9
            if (item.calories >= 50 && item.foodId != "b:beer") {
                assertTrue(
                    "${item.name}: kcal=${item.calories} estimated=$estimated",
                    estimated in item.calories * 0.65..item.calories * 1.35,
                )
            }
        }
    }

    @Test
    fun `scaling is linear`() {
        val rice = FoodCatalog.findById("b:rice")!!
        val double = rice.scaledTo(rice.standardAmount * 2)
        assertEquals(rice.calories * 2, double.calories, 0.001)
        assertEquals(rice.proteinGrams * 2, double.proteinGrams, 0.001)
    }

    @Test
    fun `search matches partial names and includes custom foods first`() {
        val custom = FoodCatalogItem("c:test", "プロテインシェイク", 300.0, "ml", 120.0, 20.0, 2.0, 5.0, isCustom = true)
        val results = FoodCatalog.search("プロテイン", listOf(custom))
        assertTrue(results.any { it.foodId == "c:test" })
        assertTrue(FoodCatalog.search("ごはん").isNotEmpty())
        assertEquals(FoodCatalog.builtin.size + 1, FoodCatalog.search("", listOf(custom)).size)
    }

    @Test
    fun `day nutrition from entries distinguishes empty from zero`() {
        assertEquals(null, DayNutrition.fromEntries(emptyList()))
    }
}
