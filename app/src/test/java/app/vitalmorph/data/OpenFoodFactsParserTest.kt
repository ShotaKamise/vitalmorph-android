package app.vitalmorph.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsParserTest {
    @Test
    fun `parses product response with nutriments`() {
        val json = """
            {
              "status": 1,
              "product": {
                "code": "4901234567890",
                "product_name": "Protein Bar",
                "product_name_ja": "プロテインバー",
                "brands": "TestBrand",
                "nutriments": {
                  "energy-kcal_100g": 380.5,
                  "proteins_100g": 30.2,
                  "fat_100g": 12.1,
                  "carbohydrates_100g": 40.0,
                  "vitamin-c_100g": 0.05,
                  "calcium_100g": 0.2,
                  "iron_100g": 0.004
                }
              }
            }
        """.trimIndent()
        val food = OpenFoodFactsParser.parseProduct(json)!!
        assertEquals("4901234567890", food.barcode)
        assertEquals("プロテインバー", food.name)
        assertEquals("プロテインバー(TestBrand)", food.displayName)
        assertEquals(380.5, food.caloriesPer100g, 0.001)
        assertEquals(30.2, food.proteinPer100g, 0.001)
        // g単位で返る微量栄養素はmgへ換算される。
        assertEquals(50.0, food.vitaminCMgPer100g, 0.001)
        assertEquals(200.0, food.calciumMgPer100g, 0.001)
        assertEquals(4.0, food.ironMgPer100g, 0.001)
    }

    @Test
    fun `product not found returns null`() {
        assertNull(OpenFoodFactsParser.parseProduct("""{"status": 0}"""))
    }

    @Test
    fun `search response skips products without name or calories`() {
        val json = """
            {
              "products": [
                {"code": "1", "product_name": "Valid Food", "nutriments": {"energy-kcal_100g": 100}},
                {"code": "2", "product_name": "", "nutriments": {"energy-kcal_100g": 100}},
                {"code": "3", "product_name": "No Nutriments"},
                {"product_name": "No Code", "nutriments": {"energy-kcal_100g": 100}}
              ]
            }
        """.trimIndent()
        val foods = OpenFoodFactsParser.parseSearch(json)
        assertEquals(1, foods.size)
        assertEquals("Valid Food", foods[0].name)
    }

    @Test
    fun `catalog item conversion uses 100g standard`() {
        val food = ExternalFood("123", "Rice Snack", null, 400.0, 8.0, 2.0, 85.0)
        val item = food.toCatalogItem()
        assertEquals("off:123", item.foodId)
        assertEquals(100.0, item.standardAmount, 0.001)
        val half = item.scaledTo(50.0)
        assertEquals(200.0, half.calories, 0.001)
    }

    @Test
    fun `empty search response is handled`() {
        assertTrue(OpenFoodFactsParser.parseSearch("""{"products": []}""").isEmpty())
        assertTrue(OpenFoodFactsParser.parseSearch("{}").isEmpty())
    }
}
