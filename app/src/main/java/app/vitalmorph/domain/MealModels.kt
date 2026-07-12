package app.vitalmorph.domain

import java.time.LocalDate

/** 食事区分(DATA_MODEL.mdのFoodEntry.mealType)。 */
enum class MealSlot(val label: String) {
    BREAKFAST("朝食"),
    LUNCH("昼食"),
    DINNER("夕食"),
    SNACK("間食"),
}

/**
 * VitaMorph内の食事記録1件。Health Connectへは [clientRecordId] を付けて書き込み、
 * 読み取り時の二重計上を避ける。
 */
data class FoodEntry(
    val entryId: Long = 0,
    val date: LocalDate,
    val mealSlot: MealSlot,
    val foodName: String,
    val amount: Double,
    val amountUnit: String,
    val calories: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
    val carbsGrams: Double,
    /** 代表的なビタミン・ミネラル(v0.9)。不明な場合は0のまま。 */
    val vitaminCMg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    /** Health Connectとの対応付けID。作成時にUUIDで確定し変更しない。 */
    val clientRecordId: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 食品カタログの1項目。同梱食品(builtin)と自作食品(custom)の両方を表す。 */
data class FoodCatalogItem(
    val foodId: String,
    val name: String,
    val standardAmount: Double,
    val amountUnit: String,
    val calories: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
    val carbsGrams: Double,
    val isCustom: Boolean = false,
) {
    /** 数量を変えたときの栄養値を線形換算する。 */
    fun scaledTo(amount: Double): FoodCatalogItem {
        if (standardAmount <= 0.0) return this
        val ratio = amount / standardAmount
        return copy(
            standardAmount = amount,
            calories = calories * ratio,
            proteinGrams = proteinGrams * ratio,
            fatGrams = fatGrams * ratio,
            carbsGrams = carbsGrams * ratio,
        )
    }
}

/** レシピを構成する材料1件。数量換算後の栄養値を保持する。 */
data class RecipeItem(
    val name: String,
    val amount: Double,
    val amountUnit: String,
    val calories: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
    val carbsGrams: Double,
)

/**
 * よく食べる組み合わせを材料付きで保存したレシピ(DATA_MODEL.mdのrecipe / recipe_item)。
 * 合計栄養は材料の総和として算出する。
 */
data class Recipe(
    val recipeId: Long = 0,
    val name: String,
    val items: List<RecipeItem>,
    val createdAt: Long,
) {
    val totalCalories: Double get() = items.sumOf { it.calories }
    val totalProtein: Double get() = items.sumOf { it.proteinGrams }
    val totalFat: Double get() = items.sumOf { it.fatGrams }
    val totalCarbs: Double get() = items.sumOf { it.carbsGrams }
}

/** レシピ組み立ての純粋ロジック。UI・永続化から切り離してテスト可能にする。 */
object RecipeFactory {
    /**
     * カタログ材料と数量の組から [Recipe] を作る。
     * 各材料は [FoodCatalogItem.scaledTo] で数量換算し、その数量・単位・栄養を [RecipeItem] に落とす。
     */
    fun fromParts(
        name: String,
        parts: List<Pair<FoodCatalogItem, Double>>,
        createdAt: Long,
    ): Recipe {
        val items = parts.map { (item, amount) ->
            val scaled = item.scaledTo(amount)
            RecipeItem(
                name = item.name,
                amount = amount,
                amountUnit = item.amountUnit,
                calories = scaled.calories,
                proteinGrams = scaled.proteinGrams,
                fatGrams = scaled.fatGrams,
                carbsGrams = scaled.carbsGrams,
            )
        }
        return Recipe(name = name.trim(), items = items, createdAt = createdAt)
    }
}

/** 1日分の栄養サマリー。 */
data class DayNutrition(
    val calories: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
    val carbsGrams: Double,
) {
    val hasAny: Boolean
        get() = calories > 0 || proteinGrams > 0 || fatGrams > 0 || carbsGrams > 0

    companion object {
        /** 記録が1件もなければnull(0埋めの日と未記録の日を区別する)。 */
        fun fromEntries(entries: List<FoodEntry>): DayNutrition? {
            if (entries.isEmpty()) return null
            return DayNutrition(
                calories = entries.sumOf { it.calories },
                proteinGrams = entries.sumOf { it.proteinGrams },
                fatGrams = entries.sumOf { it.fatGrams },
                carbsGrams = entries.sumOf { it.carbsGrams },
            )
        }
    }
}

/**
 * 栄養データの優先元(DATA_MODEL.mdのNutritionSourcePreference)。
 * 異なるアプリの栄養記録を無条件に合算しない。
 * SELECT_PER_DAYは列挙のみ定義し、日ごとの選択UIはPhase 6で提供する。
 */
enum class NutritionSource(val label: String, val description: String) {
    ASKEN_FIRST("あすけん優先", "あすけん等の記録がある日はそちらだけを使う"),
    VITALMORPH_FIRST("VitaMorph優先", "VitaMorphの記録がある日はそちらだけを使う"),
    SELECT_PER_DAY("日ごとに選択", "日ごとに優先元を選ぶ(今後提供)"),
}

/** 「日ごとに選択」モードでの、その日の優先元。 */
enum class DayNutritionChoice(val label: String) {
    VITALMORPH("VitaMorph"),
    ASKEN("あすけん等"),
}

/**
 * VitaMorphのローカル記録と、Health Connect上の他アプリ(あすけん等)の記録を
 * 優先元ルールで解決する。合算はしない。
 */
object NutritionResolver {
    fun resolveDay(
        mode: NutritionSource,
        vitamorph: DayNutrition?,
        external: DayNutrition?,
        dayChoice: DayNutritionChoice? = null,
    ): DayNutrition? = when (mode) {
        NutritionSource.VITALMORPH_FIRST -> vitamorph ?: external
        NutritionSource.ASKEN_FIRST -> external ?: vitamorph
        NutritionSource.SELECT_PER_DAY -> when (dayChoice) {
            DayNutritionChoice.ASKEN -> external ?: vitamorph
            DayNutritionChoice.VITALMORPH -> vitamorph ?: external
            // 未選択の日は記録した側を尊重する。
            null -> vitamorph ?: external
        }
    }

    /**
     * Health Connect由来の日次データ(栄養はVitaMorph以外のOrigin合計)へ、
     * ローカルの食事記録を優先元ルールで重ねる。
     * Health Connectが使えない場合(baseDaysが空)でもローカル記録だけで日次データを作る。
     */
    fun mergeDays(
        baseDays: List<DailyHealthData>,
        entriesByDate: Map<LocalDate, List<FoodEntry>>,
        mode: NutritionSource,
        start: LocalDate,
        endInclusive: LocalDate,
        dayChoices: Map<LocalDate, DayNutritionChoice> = emptyMap(),
    ): List<DailyHealthData> {
        val baseByDate = baseDays.associateBy { it.date }
        val days = mutableListOf<DailyHealthData>()
        var date = start
        while (!date.isAfter(endInclusive)) {
            val base = baseByDate[date] ?: DailyHealthData(date = date)
            val local = DayNutrition.fromEntries(entriesByDate[date].orEmpty())
            val external = if (base.hasNutrition) {
                DayNutrition(base.calories, base.proteinGrams, base.fatGrams, base.carbsGrams)
            } else null
            val resolved = resolveDay(mode, local, external, dayChoices[date])
            days += base.copy(
                calories = resolved?.calories ?: 0.0,
                proteinGrams = resolved?.proteinGrams ?: 0.0,
                fatGrams = resolved?.fatGrams ?: 0.0,
                carbsGrams = resolved?.carbsGrams ?: 0.0,
                hasNutrition = resolved?.hasAny == true,
            )
            date = date.plusDays(1)
        }
        return days
    }
}
