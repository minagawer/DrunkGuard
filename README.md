<div align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="120" alt="DrunkGuard icon" />

  # DrunkGuard

  **飲みすぎた夜の LINE 送信を防ぐ Android アプリ**

  LINE を開こうとするとブロック画面が割り込み、計算問題に全問正解しないと解除できない。

  ![Android](https://img.shields.io/badge/Android-14%2B-3DDC84?logo=android&logoColor=white)
  ![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
  ![License](https://img.shields.io/badge/License-MIT-blue)
</div>

---

## 動作フロー

```
[スタート] → AppMonitorService 起動
                ↓ 1秒ごとにポーリング
           LINE がフォアグラウンドに来た？
                ↓ Yes
           BlockActivity を表示（LINE をバックグラウンドへ）
                ↓
           ユーザーが DrunkGuard アプリを開く
                ↓
           SobrietyCheckActivity で計算問題 3 問全問正解
                ↓
           ガード解除・サービス停止
```

---

## アーキテクチャ

### AppMonitorService

フォアグラウンドサービスとして常駐し、1秒間隔でアプリの切り替えを監視する。

**フォアグラウンド検出**

`UsageStatsManager.queryEvents()` で過去10秒間の `ACTIVITY_RESUMED` イベントを取得し、最後に取得したパッケージ名を現在のフォアグラウンドアプリとして扱う。`queryUsageStats()` よりイベントベースの方が精度が高い。

**ブロック判定ロジック**

```
lastKnownForeground: 前回チェック時のフォアグラウンドパッケージ

1. foreground == 自パッケージ (BlockActivity 表示中)
   → lastKnownForeground = null にリセットして return
     ※ null にしないとタスク一覧から LINE を再度開いたとき再検出されない

2. foreground == LINE_PACKAGE && foreground != lastKnownForeground
   → BlockActivity を起動

3. それ以外
   → lastKnownForeground を更新するだけ
```

`lastKnownForeground` を保持することで、同じアプリが前面にいる間の毎秒チェックで重複起動しないようにしている。

**バックグラウンドからの Activity 起動**

Android 10 以降はバックグラウンドサービスから `startActivity()` するには `SYSTEM_ALERT_WINDOW` 権限が必要。起動前に `Settings.canDrawOverlays()` で確認する。

### BlockActivity

- `taskAffinity=""` + `launchMode="singleTask"` で LINE のタスクスタックと分離
- `excludeFromRecents="true"` でタスク一覧に表示しない
- `showWhenLocked="true"` + `turnScreenOn="true"` でロック画面上にも表示
- バックキーを `onBackPressedDispatcher.addCallback` で無効化
- 「ホームに戻る」ボタンのみ提供し、LINE に戻れなくする

### SobrietyCheckActivity

計算問題を 3 問出題し、1 問でも不正解なら最初からやり直し。

**問題生成**

以下の 4 パターンをランダムで出題:

| パターン | 例 |
|---|---|
| `a × b` | `18 × 7 = ?` |
| `a × b + c` | `23 × 4 + 15 = ?` |
| `a × b − c` | `15 × 6 − 22 = ?` |
| `(a + b) × d` | `(12 + 9) × 5 = ?` |

範囲: `a ∈ [10, 30]`, `b, d ∈ [3, 9]`, `c ∈ [10, 30]`

---

## 必要な権限

| 権限 | 用途 | 取得方法 |
|---|---|---|
| `PACKAGE_USAGE_STATS` | フォアグラウンドアプリ検出 | 設定 → 使用状況へのアクセス |
| `SYSTEM_ALERT_WINDOW` | バックグラウンドから Activity 起動 | 設定 → 他のアプリの上に表示 |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | フォアグラウンドサービス実行 | 自動付与 |
| `POST_NOTIFICATIONS` | 常駐通知の表示 | 起動時にランタイムリクエスト |

---

## 要件

- Android 14 (API 34) 以上
- ターゲット: LINE (`jp.naver.line.android`)
