package app.vitalmorph.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitalmorph.data.ExternalFood
import app.vitalmorph.domain.DailyHealthData
import app.vitalmorph.domain.DayNutritionChoice
import app.vitalmorph.domain.FoodCatalog
import app.vitalmorph.domain.FoodCatalogItem
import app.vitalmorph.domain.FoodEntry
import app.vitalmorph.domain.MealSlot
import app.vitalmorph.domain.NutritionSource
import app.vitalmorph.domain.UserGoals
import kotlin.math.roundToInt

private val MealMint = Color(0xFF69E6A6)
private val MealGold = Color(0xFFFFD166)
private val MealSurfaceHigh = Color(0xFF1D344A)

@Composable
fun MealsScreen(
    state: GameUiState,
    onAddCatalog: (MealSlot, FoodCatalogItem, Double) -> Unit,
    onAddManual: (MealSlot, String, Double, String, Double, Double, Double, Double, Boolean, Double, Double, Double) -> Unit,
    onDelete: (FoodEntry) -> Unit,
    onCopyYesterday: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSearchExternal: (String) -> Unit,
    onLookupBarcode: (String) -> Unit,
    onAddExternal: (MealSlot, ExternalFood, Double) -> Unit,
    onClearExternal: () -> Unit,
    onSetTodayChoice: (DayNutritionChoice) -> Unit,
    onSaveRecipe: (String, List<Pair<FoodCatalogItem, Double>>) -> Unit,
) {
    var addingSlot by remember { mutableStateOf<MealSlot?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    if (showScanner) {
        BarcodeScannerOverlay(
            onDetected = { code ->
                showScanner = false
                onLookupBarcode(code)
            },
            onCancel = { showScanner = false },
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("今日の食事", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                "優先元: ${state.nutritionSource.label}(設定タブで変更できます)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
        if (state.nutritionSource == NutritionSource.SELECT_PER_DAY) {
            item { DayChoiceCard(state.todayNutritionChoice, onSetTodayChoice) }
        }
        item { DailyNutritionCard(state.foodEntriesToday, state.goals) }
        item {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onCopyYesterday) {
                Text("昨日の記録をコピー")
            }
        }
        MealSlot.entries.forEach { slot ->
            item {
                MealSlotCard(
                    slot = slot,
                    entries = state.foodEntriesToday.filter { it.mealSlot == slot },
                    isAdding = addingSlot == slot,
                    state = state,
                    onStartAdd = {
                        addingSlot = if (addingSlot == slot) null else slot
                        onClearExternal()
                    },
                    onAddCatalog = { item, amount ->
                        onAddCatalog(slot, item, amount)
                        addingSlot = null
                    },
                    onAddManual = { name, amount, unit, kcal, p, f, c, save, vc, ca, fe ->
                        onAddManual(slot, name, amount, unit, kcal, p, f, c, save, vc, ca, fe)
                        addingSlot = null
                    },
                    onDelete = onDelete,
                    onToggleFavorite = onToggleFavorite,
                    onSearchExternal = onSearchExternal,
                    onScanBarcode = { showScanner = true },
                    onAddExternal = { food, grams ->
                        onAddExternal(slot, food, grams)
                        addingSlot = null
                    },
                )
            }
        }
        item { RecipeBuilderCard(state, onSaveRecipe) }
        item { WeeklyCaloriesCard(state.days.takeLast(7), state.goals) }
        item {
            Text(
                FoodCatalog.ATTRIBUTION,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
            )
        }
        item {
            Text(
                "ネット検索はOpen Food Facts(world.openfoodfacts.org)を利用します。送信されるのは検索キーワードまたはバーコード番号のみです。",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun DayChoiceCard(
    current: DayNutritionChoice?,
    onSetTodayChoice: (DayNutritionChoice) -> Unit,
) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MealSurfaceHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("今日の優先元", fontWeight = FontWeight.Bold)
            Text(
                "今日どちらの記録を育成へ使うか選べます。未選択なら記録した側を使います。",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayNutritionChoice.entries.forEach { choice ->
                    if (current == choice) {
                        Button(onClick = {}) { Text(choice.label) }
                    } else {
                        OutlinedButton(onClick = { onSetTodayChoice(choice) }) { Text(choice.label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeBuilderCard(
    state: GameUiState,
    onSaveRecipe: (String, List<Pair<FoodCatalogItem, Double>>) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var recipeName by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val parts = remember { mutableStateListOf<Pair<FoodCatalogItem, Double>>() }

    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("レシピを作る", fontWeight = FontWeight.Bold)
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "閉じる" else "開く") }
            }
            if (!expanded) {
                Text("よく食べる組み合わせを1つの自作食品として保存できます。", style = MaterialTheme.typography.bodySmall)
            } else {
                OutlinedTextField(
                    value = recipeName,
                    onValueChange = { recipeName = it.take(20) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("レシピ名(例: いつもの朝食)") },
                    singleLine = true,
                )
                parts.forEachIndexed { index, (item, amount) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${item.name} ${amount.roundToInt()}${item.amountUnit}(${item.scaledTo(amount).calories.roundToInt()}kcal)",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = { parts.removeAt(index) }) { Text("外す") }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("材料をさがして追加") },
                    singleLine = true,
                )
                if (query.isNotBlank()) {
                    FoodCatalog.search(query, state.customFoods).take(4).forEach { item ->
                        TextButton(onClick = {
                            parts += item to item.standardAmount
                            query = ""
                        }) {
                            Text("＋ ${item.name}(${item.standardAmount.roundToInt()}${item.amountUnit})", color = Color.White)
                        }
                    }
                }
                if (parts.isNotEmpty()) {
                    val total = parts.sumOf { (item, amount) -> item.scaledTo(amount).calories }
                    Text("合計 ${total.roundToInt()}kcal・1食として保存されます", color = MealGold, style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = {
                        onSaveRecipe(recipeName, parts.toList())
                        recipeName = ""
                        parts.clear()
                        expanded = false
                    },
                    enabled = recipeName.isNotBlank() && parts.isNotEmpty(),
                ) { Text("レシピを保存") }
            }
        }
    }
}

@Composable
private fun DailyNutritionCard(entries: List<FoodEntry>, goals: UserGoals) {
    val calories = entries.sumOf { it.calories }
    val protein = entries.sumOf { it.proteinGrams }
    val fat = entries.sumOf { it.fatGrams }
    val carbs = entries.sumOf { it.carbsGrams }
    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VitaMorphでの記録", fontWeight = FontWeight.Bold)
                Text("${calories.roundToInt()} / ${goals.calories} kcal", color = MealGold, fontWeight = FontWeight.Bold)
            }
            NutrientBar("エネルギー", calories, goals.calories.toDouble(), "kcal")
            NutrientBar("たんぱく質", protein, goals.proteinGrams.toDouble(), "g")
            NutrientBar("脂質", fat, goals.fatGrams.toDouble(), "g")
            NutrientBar("炭水化物", carbs, goals.carbsGrams.toDouble(), "g")
            val diff = goals.calories - calories.roundToInt()
            Text(
                when {
                    entries.isEmpty() -> "今日の記録はまだありません。少しずつで大丈夫。"
                    diff >= 0 -> "目標まであと ${diff}kcal"
                    else -> "目標より ${-diff}kcal 多め。明日にいかそう。"
                },
                style = MaterialTheme.typography.bodySmall,
            )
            val vitaminC = entries.sumOf { it.vitaminCMg }
            val calcium = entries.sumOf { it.calciumMg }
            val iron = entries.sumOf { it.ironMg }
            if (vitaminC > 0 || calcium > 0 || iron > 0) {
                Text(
                    "ビタミンC ${vitaminC.roundToInt()}mg・カルシウム ${calcium.roundToInt()}mg・鉄 ${"%.1f".format(iron)}mg(記録がある食品のみの参考値)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun NutrientBar(label: String, value: Double, goal: Double, unit: String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("${value.roundToInt()} / ${goal.roundToInt()}$unit", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { (value / goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
    }
}

@Composable
private fun MealSlotCard(
    slot: MealSlot,
    entries: List<FoodEntry>,
    isAdding: Boolean,
    state: GameUiState,
    onStartAdd: () -> Unit,
    onAddCatalog: (FoodCatalogItem, Double) -> Unit,
    onAddManual: (String, Double, String, Double, Double, Double, Double, Boolean, Double, Double, Double) -> Unit,
    onDelete: (FoodEntry) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSearchExternal: (String) -> Unit,
    onScanBarcode: () -> Unit,
    onAddExternal: (ExternalFood, Double) -> Unit,
) {
    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(slot.label, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${entries.sumOf { it.calories }.roundToInt()} kcal", color = MealGold, style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = onStartAdd) { Text(if (isAdding) "閉じる" else "＋ 追加") }
                }
            }
            entries.forEach { entry ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.foodName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${entry.amount.roundToInt()}${entry.amountUnit}・${entry.calories.roundToInt()}kcal " +
                                "P${entry.proteinGrams.roundToInt()} F${entry.fatGrams.roundToInt()} C${entry.carbsGrams.roundToInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                    TextButton(onClick = { onDelete(entry) }) { Text("削除", color = Color(0xFFFF6B6B)) }
                }
            }
            if (entries.isEmpty() && !isAdding) {
                Text("記録なし", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
            }
            if (isAdding) {
                HorizontalDivider()
                AddFoodPanel(
                    state = state,
                    onAddCatalog = onAddCatalog,
                    onAddManual = onAddManual,
                    onToggleFavorite = onToggleFavorite,
                    onSearchExternal = onSearchExternal,
                    onScanBarcode = onScanBarcode,
                    onAddExternal = onAddExternal,
                )
            }
        }
    }
}

@Composable
private fun AddFoodPanel(
    state: GameUiState,
    onAddCatalog: (FoodCatalogItem, Double) -> Unit,
    onAddManual: (String, Double, String, Double, Double, Double, Double, Boolean, Double, Double, Double) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSearchExternal: (String) -> Unit,
    onScanBarcode: () -> Unit,
    onAddExternal: (ExternalFood, Double) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<FoodCatalogItem?>(null) }
    var selectedExternal by remember { mutableStateOf<ExternalFood?>(null) }
    var amountText by remember { mutableStateOf("") }
    var manualMode by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!manualMode) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; selected = null; selectedExternal = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("食品をさがす") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (query.isNotBlank()) onSearchExternal(query) }, enabled = query.isNotBlank()) {
                    Text("🌐 ネットで検索")
                }
                OutlinedButton(onClick = onScanBarcode) { Text("📷 バーコード") }
            }
            if (state.externalSearching) {
                Text("Open Food Factsを検索中…", style = MaterialTheme.typography.bodySmall, color = MealGold)
            }
            val externalSelection = selectedExternal
            if (externalSelection != null) {
                Text(externalSelection.displayName, fontWeight = FontWeight.Bold)
                Text(
                    "100gあたり ${externalSelection.caloriesPer100g.roundToInt()}kcal " +
                        "P${externalSelection.proteinPer100g.roundToInt()} F${externalSelection.fatPer100g.roundToInt()} C${externalSelection.carbsPer100g.roundToInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6) },
                        modifier = Modifier.width(120.dp),
                        label = { Text("数量") },
                        suffix = { Text("g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    val grams = amountText.toDoubleOrNull() ?: 0.0
                    Text("${(externalSelection.caloriesPer100g * grams / 100.0).roundToInt()}kcal", color = MealGold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val grams = amountText.toDoubleOrNull()
                            if (grams != null && grams > 0) onAddExternal(externalSelection, grams)
                        },
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    ) { Text("確認して記録") }
                    OutlinedButton(onClick = { selectedExternal = null }) { Text("選び直す") }
                }
            } else if (state.externalResults.isNotEmpty() && selected == null) {
                Text("ネット検索の結果(内容を確認してから記録):", style = MaterialTheme.typography.labelMedium, color = MealGold)
                state.externalResults.take(6).forEach { food ->
                    TextButton(onClick = {
                        selectedExternal = food
                        amountText = "100"
                    }) {
                        Text("🌐 ${food.displayName}(${food.caloriesPer100g.roundToInt()}kcal/100g)", color = Color.White)
                    }
                }
            }
            val results = remember(query, state.customFoods, state.favoriteFoodIds) {
                val all = FoodCatalog.search(query, state.customFoods)
                // お気に入りを先頭へ。
                all.sortedByDescending { it.foodId in state.favoriteFoodIds }.take(8)
            }
            val currentSelection = selected
            if (currentSelection == null && selectedExternal == null && state.externalResults.isEmpty()) {
                results.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = {
                            selected = item
                            amountText = item.standardAmount.roundToInt().toString()
                        }) {
                            Text(
                                (if (item.isCustom) "📝 " else "") + item.name +
                                    "(${item.standardAmount.roundToInt()}${item.amountUnit}・${item.calories.roundToInt()}kcal)",
                                color = Color.White,
                            )
                        }
                        TextButton(onClick = { onToggleFavorite(item.foodId) }) {
                            Text(if (item.foodId in state.favoriteFoodIds) "★" else "☆", color = MealGold, fontSize = 18.sp)
                        }
                    }
                }
                if (state.recentFoods.isNotEmpty() && query.isBlank()) {
                    Text("最近の記録", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                    state.recentFoods.take(5).forEach { recent ->
                        TextButton(onClick = {
                            selected = FoodCatalogItem(
                                foodId = "recent:${recent.entryId}",
                                name = recent.foodName,
                                standardAmount = recent.amount,
                                amountUnit = recent.amountUnit,
                                calories = recent.calories,
                                proteinGrams = recent.proteinGrams,
                                fatGrams = recent.fatGrams,
                                carbsGrams = recent.carbsGrams,
                            )
                            amountText = recent.amount.roundToInt().toString()
                        }) {
                            Text("${recent.foodName}(${recent.calories.roundToInt()}kcal)", color = Color.White)
                        }
                    }
                }
            } else if (currentSelection != null) {
                Text(currentSelection.name, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6) },
                        modifier = Modifier.width(120.dp),
                        label = { Text("数量") },
                        suffix = { Text(currentSelection.amountUnit) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val preview = currentSelection.scaledTo(amount)
                    Text("${preview.calories.roundToInt()}kcal", color = MealGold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null && amount > 0) onAddCatalog(currentSelection, amount)
                        },
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    ) { Text("この内容で記録") }
                    OutlinedButton(onClick = { selected = null }) { Text("選び直す") }
                }
            }
            TextButton(onClick = { manualMode = true }) { Text("見つからない場合は手入力") }
        } else {
            ManualFoodForm(onAdd = onAddManual, onBack = { manualMode = false })
        }
    }
}

@Composable
private fun ManualFoodForm(
    onAdd: (String, Double, String, Double, Double, Double, Double, Boolean, Double, Double, Double) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var vitaminC by remember { mutableStateOf("") }
    var calcium by remember { mutableStateOf("") }
    var iron by remember { mutableStateOf("") }
    var saveAsCustom by remember { mutableStateOf(false) }

    fun numeric(value: String) = value.filter { it.isDigit() || it == '.' }.take(6)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(30) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("食品名") },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = numeric(it) },
                modifier = Modifier.weight(1f),
                label = { Text("数量") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it.take(4) },
                modifier = Modifier.weight(1f),
                label = { Text("単位") },
                singleLine = true,
            )
            OutlinedTextField(
                value = kcal,
                onValueChange = { kcal = numeric(it) },
                modifier = Modifier.weight(1f),
                label = { Text("kcal") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = protein,
                onValueChange = { protein = numeric(it) },
                modifier = Modifier.weight(1f),
                label = { Text("P(g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = fat,
                onValueChange = { fat = numeric(it) },
                modifier = Modifier.weight(1f),
                label = { Text("F(g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = carbs,
                onValueChange = { carbs = numeric(it) },
                modifier = Modifier.weight(1f),
                label = { Text("C(g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Text("ビタミン・ミネラル(任意)", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = vitaminC,
                onValueChange = { vitaminC = numeric(it) },
                modifier = Modifier.weight(1f),
                label = { Text("VC(mg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = calcium,
                onValueChange = { calcium = numeric(it) },
                modifier = Modifier.weight(1f),
                label = { Text("Ca(mg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = iron,
                onValueChange = { iron = numeric(it) },
                modifier = Modifier.weight(1f),
                label = { Text("鉄(mg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = saveAsCustom, onCheckedChange = { saveAsCustom = it })
            Text("自作食品として保存(次回から検索できます)", style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onAdd(
                        name,
                        amount.toDoubleOrNull() ?: 0.0,
                        unit,
                        kcal.toDoubleOrNull() ?: 0.0,
                        protein.toDoubleOrNull() ?: 0.0,
                        fat.toDoubleOrNull() ?: 0.0,
                        carbs.toDoubleOrNull() ?: 0.0,
                        saveAsCustom,
                        vitaminC.toDoubleOrNull() ?: 0.0,
                        calcium.toDoubleOrNull() ?: 0.0,
                        iron.toDoubleOrNull() ?: 0.0,
                    )
                },
                enabled = name.isNotBlank() && (kcal.toDoubleOrNull() ?: 0.0) >= 0 && (amount.toDoubleOrNull() ?: 0.0) > 0,
            ) { Text("記録する") }
            OutlinedButton(onClick = onBack) { Text("検索に戻る") }
        }
    }
}

@Composable
private fun WeeklyCaloriesCard(days: List<DailyHealthData>, goals: UserGoals) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MealSurfaceHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("直近7日のエネルギー", fontWeight = FontWeight.Bold)
            if (days.isEmpty()) {
                Text("データがまだありません。", style = MaterialTheme.typography.bodySmall)
            } else {
                val maxValue = maxOf(days.maxOf { it.calories }, goals.calories.toDouble()) * 1.15
                Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                    val barSpace = size.width / days.size
                    val barWidth = barSpace * 0.55f
                    days.forEachIndexed { index, day ->
                        val heightRatio = (day.calories / maxValue).toFloat().coerceIn(0f, 1f)
                        val barHeight = size.height * heightRatio
                        drawRect(
                            color = if (day.hasNutrition) MealMint else MealMint.copy(alpha = 0.2f),
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = index * barSpace + (barSpace - barWidth) / 2f,
                                y = size.height - barHeight,
                            ),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        )
                    }
                    // 目標ライン
                    val goalY = size.height * (1f - (goals.calories / maxValue).toFloat().coerceIn(0f, 1f))
                    drawLine(
                        color = MealGold,
                        start = androidx.compose.ui.geometry.Offset(0f, goalY),
                        end = androidx.compose.ui.geometry.Offset(size.width, goalY),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    days.forEach { day ->
                        Text(
                            "${day.date.monthValue}/${day.date.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("黄色の線が1日の目標(${goals.calories}kcal)。優先元ルールで選ばれた記録を表示しています。", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
