// v1.1で食事入力UIは削除。テーブルとともに将来の再有効化に備えて保持。
package app.vitalmorph.data

import app.vitalmorph.data.db.CustomFoodEntity
import app.vitalmorph.data.db.FoodEntryEntity
import app.vitalmorph.data.db.FoodFavoriteEntity
import app.vitalmorph.data.db.RecipeEntity
import app.vitalmorph.data.db.RecipeItemEntity
import app.vitalmorph.data.db.VitaMorphDatabase
import app.vitalmorph.domain.FoodCatalogItem
import app.vitalmorph.domain.FoodEntry
import app.vitalmorph.domain.MealSlot
import app.vitalmorph.domain.Recipe
import app.vitalmorph.domain.RecipeItem
import java.time.LocalDate
import java.util.UUID

/**
 * 食事記録・自作食品・お気に入りのRoom永続化。
 * Health Connectへの書き込みは呼び出し側([app.vitalmorph.ui.GameViewModel])が
 * [FoodEntry.clientRecordId] を使って行う。
 */
@Suppress("unused")
class FoodRepository(
    private val database: VitaMorphDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun entriesBetween(start: LocalDate, endInclusive: LocalDate): List<FoodEntry> =
        database.foodEntryDao().between(start.toString(), endInclusive.toString()).map { it.toDomain() }

    suspend fun entriesOn(date: LocalDate): List<FoodEntry> =
        database.foodEntryDao().byDate(date.toString()).map { it.toDomain() }

    /** 記録を保存し、確定したID付きで返す。 */
    suspend fun addEntry(
        date: LocalDate,
        mealSlot: MealSlot,
        foodName: String,
        amount: Double,
        amountUnit: String,
        calories: Double,
        proteinGrams: Double,
        fatGrams: Double,
        carbsGrams: Double,
        vitaminCMg: Double = 0.0,
        calciumMg: Double = 0.0,
        ironMg: Double = 0.0,
    ): FoodEntry {
        val timestamp = now()
        val entry = FoodEntry(
            date = date,
            mealSlot = mealSlot,
            foodName = foodName.trim(),
            amount = amount,
            amountUnit = amountUnit.trim().ifEmpty { "g" },
            calories = calories.coerceAtLeast(0.0),
            proteinGrams = proteinGrams.coerceAtLeast(0.0),
            fatGrams = fatGrams.coerceAtLeast(0.0),
            carbsGrams = carbsGrams.coerceAtLeast(0.0),
            vitaminCMg = vitaminCMg.coerceAtLeast(0.0),
            calciumMg = calciumMg.coerceAtLeast(0.0),
            ironMg = ironMg.coerceAtLeast(0.0),
            clientRecordId = "vitalmorph-food-${UUID.randomUUID()}",
            createdAt = timestamp,
            updatedAt = timestamp,
        )
        val id = database.foodEntryDao().insert(FoodEntryEntity.fromDomain(entry))
        return entry.copy(entryId = id)
    }

    suspend fun deleteEntry(entryId: Long) {
        database.foodEntryDao().delete(entryId)
    }

    /** 昨日の記録を今日へコピーする。コピーした件数を返す。 */
    suspend fun copyDay(from: LocalDate, to: LocalDate): Int {
        val source = entriesOn(from)
        source.forEach { entry ->
            addEntry(
                date = to,
                mealSlot = entry.mealSlot,
                foodName = entry.foodName,
                amount = entry.amount,
                amountUnit = entry.amountUnit,
                calories = entry.calories,
                proteinGrams = entry.proteinGrams,
                fatGrams = entry.fatGrams,
                carbsGrams = entry.carbsGrams,
                vitaminCMg = entry.vitaminCMg,
                calciumMg = entry.calciumMg,
                ironMg = entry.ironMg,
            )
        }
        return source.size
    }

    suspend fun customFoods(): List<FoodCatalogItem> =
        database.customFoodDao().all().map { it.toDomain() }

    /** 自作食品を保存する。 */
    suspend fun saveCustomFood(
        name: String,
        standardAmount: Double,
        amountUnit: String,
        calories: Double,
        proteinGrams: Double,
        fatGrams: Double,
        carbsGrams: Double,
    ): FoodCatalogItem {
        val item = FoodCatalogItem(
            foodId = "c:${UUID.randomUUID()}",
            name = name.trim(),
            standardAmount = standardAmount,
            amountUnit = amountUnit.trim().ifEmpty { "g" },
            calories = calories.coerceAtLeast(0.0),
            proteinGrams = proteinGrams.coerceAtLeast(0.0),
            fatGrams = fatGrams.coerceAtLeast(0.0),
            carbsGrams = carbsGrams.coerceAtLeast(0.0),
            isCustom = true,
        )
        database.customFoodDao().upsert(CustomFoodEntity.fromDomain(item, now()))
        return item
    }

    /** 保存済みレシピを材料付きで新しい順に返す。 */
    suspend fun recipes(): List<Recipe> {
        val dao = database.recipeDao()
        return dao.recipes().map { entity ->
            Recipe(
                recipeId = entity.recipeId,
                name = entity.name,
                items = dao.itemsFor(entity.recipeId).map { it.toDomain() },
                createdAt = entity.createdAt,
            )
        }
    }

    /**
     * レシピを材料付きで保存する。合計栄養は自作食品(1食)としても同時に保存し、
     * 従来どおりカタログから1タップで記録できるようにする。
     */
    suspend fun saveRecipe(name: String, items: List<RecipeItem>): Recipe {
        val timestamp = now()
        val cleanName = name.trim()
        val dao = database.recipeDao()
        val recipeId = dao.insertRecipe(RecipeEntity(name = cleanName, createdAt = timestamp))
        dao.insertItems(items.map { RecipeItemEntity.fromDomain(recipeId, it) })
        val recipe = Recipe(recipeId = recipeId, name = cleanName, items = items, createdAt = timestamp)
        // 合計を自作食品としても保存(従来挙動の維持)。
        saveCustomFood(
            name = cleanName,
            standardAmount = 1.0,
            amountUnit = "食",
            calories = recipe.totalCalories,
            proteinGrams = recipe.totalProtein,
            fatGrams = recipe.totalFat,
            carbsGrams = recipe.totalCarbs,
        )
        return recipe
    }

    suspend fun deleteRecipe(recipeId: Long) {
        database.recipeDao().deleteItemsFor(recipeId)
        database.recipeDao().deleteRecipe(recipeId)
    }

    suspend fun favoriteIds(): Set<String> =
        database.foodFavoriteDao().all().toSet()

    suspend fun setFavorite(foodId: String, favorite: Boolean) {
        if (favorite) {
            database.foodFavoriteDao().add(FoodFavoriteEntity(foodId))
        } else {
            database.foodFavoriteDao().remove(foodId)
        }
    }

    /** 最近記録した食品名(重複除去)。クイック入力用。 */
    suspend fun recentFoods(limit: Int = 10): List<FoodEntry> =
        database.foodEntryDao().recent(50)
            .map { it.toDomain() }
            .distinctBy { it.foodName }
            .take(limit)

    suspend fun clearAll() {
        database.foodEntryDao().clear()
        database.customFoodDao().clear()
        database.foodFavoriteDao().clear()
        database.recipeDao().clearItems()
        database.recipeDao().clearRecipes()
    }
}
