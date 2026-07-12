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
import app.vitalmorph.domain.DailyHealthData
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
    onAddManual: (MealSlot, String, Double, String, Double, Double, Double, Double, Boolean) -> Unit,
    onDelete: (FoodEntry) -> Unit,
    onCopyYesterday: () -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    var addingSlot by remember { mutableStateOf<MealSlot?>(null) }

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
                    onStartAdd = { addingSlot = if (addingSlot == slot) null else slot },
                    onAddCatalog = { item, amount ->
                        onAddCatalog(slot, item, amount)
                        addingSlot = null
                    },
                    onAddManual = { name, amount, unit, kcal, p, f, c, save ->
                        onAddManual(slot, name, amount, unit, kcal, p, f, c, save)
                        addingSlot = null
                    },
                    onDelete = onDelete,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
        item { WeeklyCaloriesCard(state.days.takeLast(7), state.goals) }
        item {
            Text(
                FoodCatalog.ATTRIBUTION,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
            )
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
    onAddManual: (String, Double, String, Double, Double, Double, Double, Boolean) -> Unit,
    onDelete: (FoodEntry) -> Unit,
    onToggleFavorite: (String) -> Unit,
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
                )
            }
        }
    }
}

@Composable
private fun AddFoodPanel(
    state: GameUiState,
    onAddCatalog: (FoodCatalogItem, Double) -> Unit,
    onAddManual: (String, Double, String, Double, Double, Double, Double, Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<FoodCatalogItem?>(null) }
    var amountText by remember { mutableStateOf("") }
    var manualMode by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!manualMode) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; selected = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("食品をさがす") },
                singleLine = true,
            )
            val results = remember(query, state.customFoods, state.favoriteFoodIds) {
                val all = FoodCatalog.search(query, state.customFoods)
                // お気に入りを先頭へ。
                all.sortedByDescending { it.foodId in state.favoriteFoodIds }.take(8)
            }
            val currentSelection = selected
            if (currentSelection == null) {
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
            } else {
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
    onAdd: (String, Double, String, Double, Double, Double, Double, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
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
