# Monster Asset Contract

この文書は当初、Claude実装とCodex制作の境界を定義していた。2026-07-12以降は全作業をClaudeが担当する(旧Codex担当分を引き取り)。役割分担は解消したが、以下の公開API・画像仕様・ID規則は引き続き不変とし、既存のセーブデータと画像アセットの互換性を保つ。

## 現在の公開API

実装場所:

```text
app/src/main/java/app/vitalmorph/ui/MonsterArtwork.kt
```

フォームから画像とアニメーションを表示する。

```kotlin
MonsterVisual(
    form = monsterForm,
    modifier = Modifier.size(180.dp),
    motion = MonsterMotion.IDLE,
    facingRight = true,
    showAura = true,
)
```

CPUなど、フォームモデルを持たない表示:

```kotlin
MonsterSprite(
    drawableRes = MonsterArtwork.resourceForOpponent(opponentName),
    contentDescription = opponentName,
    accent = accentColor,
    motion = MonsterMotion.HIT,
    facingRight = false,
)
```

## 実装済みモーション

`MonsterMotion` は全10種。いずれも `scale` / `translationX` / `translationY` / `rotation` / `alpha` の
変形アニメーション(専用画像は不要)として `MonsterArtwork.kt` に実装済み。

既存4種:

- `IDLE`
- `ATTACK`
- `VICTORY`
- `HIT`

追加6種(2026-07-12にClaudeがコードで実装。旧Codex担当分の引き取りに伴う。詳細は `docs/COMPLETION_PLAN.md` T1):

- `TOUCH_HAPPY` — 小さく2回跳ねる+わずかに拡大
- `TOUCH_SHY` — 縮こまって左右に小さく揺れる
- `TOUCH_ANNOYED` — 素早く首を振る左右回転+わずかに後退
- `TALK` — 前傾しつつ上下に小刻みに揺れる(会話返答の表示中)
- `SAD` — 下に沈み傾く(機嫌BAD帯のアイドル代替)
- `MINIGAME_SUCCESS` — 大きく1回跳ねて回転(ミニゲーム成功時)

## 画像仕様

- 保存先: `app/src/main/res/drawable-nodpi/`
- 命名: `monster_<stable_form_id>.webp`
- 形式: 透過WebP
- 標準キャンバス: 512 x 512
- 左上を含む四隅が透明であること
- 画像内へキャラクター名やUI文字を描き込まない
- `contentDescription` は画面側で設定する

## 制作済み人型アセット

- 男性人型14体、成人女性人型14体の計28体を制作済み
- 正式IDと表示名: `docs/MONSTER_ROSTER.md`
- 一覧プレビュー: `art/humanoid-roster-preview.png`
- 生成原画と透過シート: `art/source/humanoid/`
- 再切り出し処理: `art/tools/process_humanoid_sheets.py`
- アプリ用個別画像: `app/src/main/res/drawable-nodpi/monster_<stable_form_id>.webp`

## ID規則

- 公開後の `formId` はセーブデータキーとして扱い、名前変更しない。
- 表示名変更と `formId` 変更を分離する。
- 性別別フォームは別IDを持つ。
- 人型28体の正式IDは `docs/MONSTER_ROSTER.md` で承認済み。Claudeはそのまま利用できる。
- 動物系14体は未確定のため、職業enumなどの論理分類を使い、仮の永続IDを作らない。

## Claudeがしてよいこと

- `MonsterVisual` を画面へ配置
- 状態に応じて既存 `MonsterMotion` を選択
- サイズ、向き、オーラ表示を指定
- タップイベントを外側のコンテナで受ける
- キャラクターの状態や会話ロジックを実装

## Claudeがしてはいけないこと

- 既存モンスター画像の上書き、削除、色変換
- `MonsterArtwork` のIDマッピング変更
- 独自の代替キャラクター画像追加
- 既存の生成原画やプロンプトの削除
- `docs/MONSTER_ROSTER.md` に存在しないキャラクターIDをセーブデータへ保存

## タッチ統合予定

タッチ座標や部位判定はClaude側で管理し、表示モーションは公開APIへ渡す。

```text
UI tap
  -> InteractionEngine determines reaction
  -> mood / bond update
  -> requested MonsterMotion
  -> MonsterVisual renders animation
  -> DialogueEngine selects local text
```
