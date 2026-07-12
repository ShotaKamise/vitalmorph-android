package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeFactoryTest {

    private fun item(
        name: String,
        standardAmount: Double,
        calories: Double,
        protein: Double,
        fat: Double,
        carbs: Double,
        unit: String = "g",
    ) = FoodCatalogItem(
        foodId = "b:$name",
        name = name,
        standardAmount = standardAmount,
        amountUnit = unit,
        calories = calories,
        proteinGrams = protein,
        fatGrams = fat,
        carbsGrams = carbs,
    )

    @Test
    fun `recipe totals sum item nutrition`() {
        val recipe = Recipe(
            recipeId = 1,
            name = "テスト",
            items = listOf(
                RecipeItem("A", 100.0, "g", 200.0, 10.0, 5.0, 30.0),
                RecipeItem("B", 50.0, "g", 100.0, 4.0, 2.0, 12.0),
            ),
            createdAt = 0L,
        )
        assertEquals(300.0, recipe.totalCalories, 1e-9)
        assertEquals(14.0, recipe.totalProtein, 1e-9)
        assertEquals(7.0, recipe.totalFat, 1e-9)
        assertEquals(42.0, recipe.totalCarbs, 1e-9)
    }

    @Test
    fun `fromParts builds one item per part`() {
        val recipe = RecipeFactory.fromParts(
            name = "いつもの朝食",
            parts = listOf(
                item("ごはん", 150.0, 234.0, 3.8, 0.5, 55.7) to 200.0,
                item("納豆", 50.0, 100.0, 8.3, 5.0, 6.0) to 50.0,
            ),
            createdAt = 42L,
        )
        assertEquals(2, recipe.items.size)
        assertEquals("いつもの朝食", recipe.name)
        assertEquals(42L, recipe.createdAt)
    }

    @Test
    fun `fromParts scales nutrition and keeps requested amount and unit`() {
        val recipe = RecipeFactory.fromParts(
            name = "レシピ",
            parts = listOf(item("ごはん", 100.0, 168.0, 2.5, 0.3, 37.1, unit = "g") to 200.0),
            createdAt = 0L,
        )
        val first = recipe.items.single()
        // 数量200gは基準100gの2倍。栄養も2倍になる。
        assertEquals(200.0, first.amount, 1e-9)
        assertEquals("g", first.amountUnit)
        assertEquals(336.0, first.calories, 1e-9)
        assertEquals(5.0, first.proteinGrams, 1e-9)
        assertEquals(0.6, first.fatGrams, 1e-9)
        assertEquals(74.2, first.carbsGrams, 1e-9)
    }

    @Test
    fun `fromParts totals equal sum of scaled parts`() {
        val recipe = RecipeFactory.fromParts(
            name = "合計テスト",
            parts = listOf(
                item("A", 100.0, 200.0, 10.0, 5.0, 30.0) to 100.0,
                item("B", 100.0, 100.0, 4.0, 2.0, 12.0) to 200.0,
            ),
            createdAt = 0L,
        )
        // Bは2倍換算される。
        assertEquals(200.0 + 200.0, recipe.totalCalories, 1e-9)
        assertEquals(10.0 + 8.0, recipe.totalProtein, 1e-9)
        assertEquals(5.0 + 4.0, recipe.totalFat, 1e-9)
        assertEquals(30.0 + 24.0, recipe.totalCarbs, 1e-9)
    }

    @Test
    fun `fromParts trims recipe name`() {
        val recipe = RecipeFactory.fromParts(
            name = "  夕食  ",
            parts = listOf(item("A", 100.0, 100.0, 1.0, 1.0, 1.0) to 100.0),
            createdAt = 0L,
        )
        assertEquals("夕食", recipe.name)
    }
}
