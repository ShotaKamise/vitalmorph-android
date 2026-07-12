# VitaMorph キャラクターデザイン仕様

## 共通ルール

- 全形態に紺色の「生命コア」を配置し、同じ生命から進化したことが分かるようにする
- 成長するほど体格、装甲、翼、エネルギー表現を増やし、週ごとの達成感を強くする
- 128px程度でも輪郭と系統が判別できる、太めの濃紺アウトラインとセル調の陰影
- 既存作品のキャラクターを模倣せず、VitaMorph専用のオリジナル造形とする
- 健康状態を罰する見た目にはせず、どの生活傾向から生まれた形態にも魅力と役割を持たせる

## 6系統

| 系統 | 主な色 | 造形テーマ | バトルでの印象 |
|---|---|---|---|
| 調和 | ミント・白・金 | 葉、星、水、旋律 | 万能・支援 |
| 筋力 | 赤・黒・金 | 剛腕、牙、重装甲 | 攻撃・反撃 |
| 俊足 | 青・白・紫 | 風切羽、雷、流線 | 連撃・回避 |
| 蓄積 | 黄・橙・鉄色 | 甲殻、炉、重力 | 防御・蓄積解放 |
| 静養 | 紫・青緑・月色 | 苔、月、霧、古木 | 回復・幻惑 |
| 過活動 | 橙・黒・白 | タービン、亀裂、太陽炉 | 高速高火力 |

## 画像生成仕様

画像は組み込みの画像生成機能で制作した。共通プロンプトは次の仕様を基礎にし、各系統について成長体1体、成熟体2体、最終形態4体を具体的に記述した。

```text
Use case: stylized-concept
Asset type: Android monster-raising game character sprite atlas
Primary request: Create seven original monsters in a strict 4-column by 2-row grid.
Style/medium: polished 2D Japanese game character illustration, clean deep-navy outlines,
cel shading, subtle texture, readable at 128px, original designs that do not copy any franchise.
Composition/framing: centered full body, front three-quarter view, equal scale, generous padding.
Constraints: exact grid order, flat chroma-key background, no shadow, no text, no logo, no watermark.
Avoid: photorealism, pixel art, cropped limbs, combined creatures, copyrighted character resemblance.
```

`source/*-chroma.png` は生成原画、`source/*-transparent.png` は透過処理済みの系統別シート。現行ゲームでは `app/src/main/res/drawable-nodpi/monster_*.webp` の43個の画像を利用する。

## 人型28体

![人型28体](humanoid-roster-preview.png)

新しい43枠構成へ向け、7職業 x 男女 x 成熟体・最終形態の28体を制作した。男性キャラクターは獣人ではなく成人男性ベースに統一し、モンスターらしさは装備、生命コア、エフェクト、シルエットで表現する。正式名とIDは `docs/MONSTER_ROSTER.md` を参照する。

職業ごとのベースカラーは次の7色で固定する。

| 職業 | ベースカラー |
|---|---|
| 剣士 | 赤 |
| 二刀流剣士 | 青 |
| 大剣使い | 緑 |
| 槍使い | 黄 |
| ショットガン使い | 黒 |
| 魔法使い | 白 |
| 忍者 | 紫 |

生成には組み込み画像生成を使い、男性成熟体、男性最終形態、女性成熟体、女性最終形態の4シートに分けた。共通プロンプト仕様は次の通り。

```text
Use case: stylized-concept
Asset type: Android monster-raising game humanoid character sheet
Primary request: seven original humanoid monsters in a strict 7-column lineup:
sword, dual blades, greatsword, spear, shotgun, mage, ninja.
Style: premium Japanese fantasy RPG illustration, deep-navy outlines, cel shading,
deep-navy/gold shared life-core emblem, readable silhouette, front three-quarter full body.
Male characters: adult human-based men, not beastmen, not animal-headed, not furry.
Role colors: sword red, dual blades blue, greatsword green, spear yellow,
shotgun black, mage white, ninja purple.
Progression: final forms preserve their role while gaining more elaborate armor,
weapons, energy effects, authority, and a clearly stronger silhouette.
Women: unmistakably adult, glamorous and attractive, fitted fantasy battle outfits,
app-store-safe and non-explicit; never childlike.
Constraints: flat chroma green background, separated characters, no text, no logo,
no watermark, no cropped limbs, no existing-franchise resemblance.
```

原画は `source/humanoid/`、個別WebPへの再処理は `tools/process_humanoid_sheets.py` に保存している。アプリ用素材はすべて透過512 x 512 WebP。

## アニメーション仕様

| 動き | 内容 | 使用場所 |
|---|---|---|
| IDLE | 呼吸、微小な浮遊、生命オーラの脈動 | ホーム、進化画面 |
| ATTACK | 溜め、前方への踏み込み、反動 | ターン制大会バトル |
| HIT | 後退、傾き、明滅 | 大会バトル |
| VICTORY | ジャンプ、拡大、オーラ強調 | 勝利結果、優勝画面 |

アニメーションは `MonsterArtwork.kt` にまとめ、画像と動作ロジックを分離している。
