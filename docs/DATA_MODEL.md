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

v0.7で確定した列(DBバージョン3)。stage・currentHp・battleStats・evolutionMetricsは
毎回計算で求まるため永続化しない方針に変更した(変更理由: 進化は純関数 `EvolutionEngine.evaluate`
の再計算で常に導出でき、二重管理による不整合を避けるため)。

```text
generationId
generationNumber
sex               MALE / FEMALE
route             HUMANOID / BEAST(v0.10追加)
personality       HARDWORKER / EASYGOING / COOL / AFFECTIONATE / CAPRICIOUS(v0.11追加)
seasonStart
seasonEnd          進行中はnull
mood               0から100
bond               0から100
finalFormId        シーズン完了時の形態ID(v0.7追加)
finalPlacement     大会最高順位。未参加はnull(v0.7追加)
awardedHp          実際に付与された継承ポイント(v0.7追加)
awardedAttack
awardedDefense
awardedSpeed
```

性別・ルート・性格は孵化時に決定し、同じ `generationId` では変更しない。次世代で再抽選する。
性格(v0.11)は会話トーンと連続タッチ許容回数だけに影響し、能力差・バトル補正は付けない。
既存の進行中世代はv7マイグレーションでHARDWORKER(がんばりや)が既定になる。

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

## 進行中の大会状態(battle_state)

進行中のターン制大会(`TurnBattleState`)は、`GameStore` のSharedPreferences値 `battle_state`
へJSONスナップショットとして保存する(`BattleStateCodec`、org.json使用、Roomは使わない)。

- 一時的な進行状態のため `StoredGameState`/`load()` には含めない。
- JSONへ `version` を持たせ、バージョン不一致・破損時は復元せず安全に破棄する(nullを返す)。
- 保存: 各バトル操作後。消去(null): 大会終了(優勝・敗退)・シーズン完了・全リセット時。
- 全フィールドを保存し、往復でデータクラスの等価性を保つ。

## DiscoveredForm(図鑑、v0.11)

v0.11で `discovered_form` テーブルとして確定(DBバージョン8、COMPLETION_PLAN T4)。
出会った(その姿になった)フォームを1件ずつ記録し、図鑑の発見済み判定に使う。

```
formId            主キー。MonsterForm.id(例: morphy / astelion / valeria_f)
firstSeenAt       初回発見時刻(epoch millis)
generationNumber  発見時の世代番号(遡及取り込みは過去世代の番号)
```

- DAOはIGNORE挿入で、初回発見だけを残す(再発見しても上書きしない)。
- `refresh` で現在の進化経路(`evolution.path`)をその世代番号で記録する。
- 遡及取り込み: 既存ユーザーの系譜(過去世代の `finalFormId`)を古い世代から順に取り込み、初回起動時に自動で図鑑を埋める。
- 全リセット時に他テーブルとあわせてクリアする。
- 表示順は純ロジック `DexCatalog` が決める(共通7体→人型28体→動物36体、♂→♀)。全71体が対象。

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

