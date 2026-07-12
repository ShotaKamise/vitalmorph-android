# VitaMorph 予定データモデル

これは実装契約の初期案。Room導入時にテーブル名と型を確定し、変更理由を記録する。

## TrainerProfile

```text
id
name              1から12文字
createdAt
updatedAt
```

トレーナー名は会話表示へ利用する。空文字、改行、制御文字を保存しない。

## MonsterGeneration

```text
generationId
generationNumber
sex               MALE / FEMALE
stage
formId
seasonStart
seasonEnd          進行中はnull
mood               0から100
bond               0から100
currentHp
battleStats
evolutionMetrics
finalPlacement
```

性別は孵化時に決定し、同じ `generationId` では変更しない。次世代で再抽選する。

## LegacyStats

```text
hpPoints
attackPoints
defensePoints
speedPoints
totalGenerations
```

- 1世代最大3ポイント
- 1ポイントは基礎能力約1%
- 各能力15ポイント上限
- データ移行や再計算でポイントが減らないこと

## InteractionState

v0.5で `interaction_state` テーブルとして確定(DBバージョン2)。

```text
lastInteractionAt      旧名lastTouchedAt。会話でも更新するため改名
consecutiveTouches     連続タッチの検出用に追加(v0.5)
touchRewardCountToday
conversationCountToday
miniGameRewardCountToday
lastDailyResetDate
```

端末時刻変更だけで報酬が無制限に復活しない設計にする。
日付が過去へ戻った場合は1日カウントをリセットしない(`InteractionEngine.resetIfNewDay`)。

## DialogueContext

永続化する会話履歴と、表示時に計算するコンテキストを分ける。

```text
trainerName
formId
sex
personality
moodBand
bondBand
timeOfDay
seasonDay
todayNutritionSummary
todayActivitySummary
lastBattleResult
```

会話は医療診断や人格否定をしない。食事が目標外でも責めず、次の行動を提案する。

## FoodEntry

```text
entryId
dateTime
mealType          BREAKFAST / LUNCH / DINNER / SNACK
foodId
displayName
amount
amountUnit
caloriesKcal
proteinGrams
fatGrams
carbsGrams
source             VITALMORPH / HEALTH_CONNECT / MANUAL_IMPORT
healthConnectId
clientRecordId
createdAt
updatedAt
```

## FoodCatalogItem

```text
foodId
displayName
category
standardAmount
amountUnit
nutritionPerStandardAmount
barcode
source
sourceVersion
isFavorite
```

## NutritionSourcePreference

```text
mode               ASKEN_FIRST / VITALMORPH_FIRST / SELECT_PER_DAY
selectedOriginPackages
```

Data Originが異なるNutritionRecordを推測だけで重複排除しない。ユーザーが優先元を選択できることを必須とする。

## 既存データ移行

現在の `GameStore` から次を保持する。

- onboardingComplete
- UserGoals
- seasonStart
- TrainerProgress
- demoMode / demoDayOffset
- tournamentPoints
- workoutTags

現在世代の性別は、保存済み `seasonStart` と固定ソルトから一度だけ決定し、その結果を保存する。アプリ起動ごとに乱数を引かない。

