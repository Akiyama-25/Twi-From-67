# Twi From 67

[![Build Signed APK](https://github.com/Akiyama-25/Twi-From-67/actions/workflows/build-signed-apk.yml/badge.svg)](https://github.com/Akiyama-25/Twi-From-67/actions/workflows/build-signed-apk.yml)
[![TargetSDK](https://img.shields.io/badge/TargetSDK-37-blue.svg)](https://developer.android.com/)
[![LSPosed](https://img.shields.io/badge/LSPosed%20API-101%2F102-orange.svg)](https://github.com/LSPosed/LSPosed)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

[English](#english) | [简体中文](#简体中文) | [繁體中文](#繁體中文) | [日本語](#日本語)

---

## English

### Overview
In X (Twitter) version 12.36+, tweet share URLs append client source tracking parameters (such as `?a=ZRipCP_Wn-MjXIXrZpU&s=19`), indicating whether the link originated from Android, iOS, or Web.

**Twi From 67** is a modern LSPosed (API 101/102) module designed to let you freely customize the `?s=` source parameter when sharing or copying tweet links in X, with optional removal of tracking parameters.

### Features
- **Custom Share Source**: Modify the numeric value of `?s=` attached to tweet URLs.
- **Input Sanitization**: Accepts up to 2 Arabic digits, automatically converting full-width numbers (`０`–`９`) to standard half-width numerals (`0`–`9`).
- **Quick Presets**: One-tap selection for known platform source identifiers:
  - `46`: iPhone X App
  - `19`: Android App
  - `20`: PC Web Copy Link
  - `21`: New Web Interface
  - `12`: iPhone System Share Sheet
  - `17`: PC Web Email Share
- **Track Parameter Stripping**: Optional toggle to remove excess tracking parameters (`?a=...`, `ref_...`) while preserving clean links.
- **Material 3 Expressive (M3E)**: Built adhering to Google's Material 3 Expressive design specifications with fluid card layouts, high curvature radii, and tonal elevation.
- **Dynamic Color (Monet)**: Real-time system theme palette extraction on Android 12+.
- **Predictive Back Gesture**: Full support for AOSP Predictive Back (`android:enableOnBackInvokedCallback="true"`).
- **Immersive Edge-to-Edge**: Full screen coverage with proper WindowInsets padding for status bars, navigation bars, display cutouts, and IME.
- **Multi-language with Hot Reload**: Supports Simplified Chinese (`zh-Hans`), Traditional Chinese (`zh-Hant`), English (`en`), and Japanese (`ja`) with instant in-app hot updating.
- **Live Test Simulation**: In-app playground to preview URL transformation live and copy results.
- **Automated CI/CD Signing**: Continuous integration workflow inspired by [TsukimiBox](https://github.com/Akiyama-25/TsukimiBox), automatically generating, verifying, and publishing signed APK releases.

### Installation & Usage
1. Install **Twi From 67** APK.
2. Open the LSPosed Manager and enable the **Twi From 67** module.
3. Ensure the module scope includes **X** (`com.twitter.android`).
4. Force close and restart X (Twitter).
5. Open Twi From 67, configure your desired `?s=` value, and tap **Save Settings**.

---

## 简体中文

### 概述
在 12.36+ / 12.37+ 版本的 X（Twitter）中，复制或分享推文链接时，客户端会自动追加识别分享来源的后缀参数（例如 `?a=ZRipCP_Wn-MjXIXrZpU&s=19`，其中 19 代表 Android 端）。

**Twi From 67** 是一款现代化的 LSPosed（API 101/102）模块，允许用户随心自定义推文分享链接中的 `?s=` 来源识别数字，并支持一键清理冗余的追踪识别后缀。

### 特性亮点
- **自定义分享来源**：自由指定推文链接中 `?s=` 后的数字代码。
- **智能输入校验**：数值最大输入两位数且仅允许阿拉伯数字，全角数字（`０`–`９`）自动转换为半角数字（`0`–`9`）。
- **常见来源快捷预设**：
  - `46`：iPhone 上的 X App
  - `19`：Android 端
  - `20`：电脑网页版按复制链接
  - `21`：新版网页界面
  - `12`：iPhone 系统分享
  - `17`：电脑网页用 Email 分享
- **追踪参数清理**：支持开关选择是否移除多余追踪参数（如 `?a=...`），保持链接纯净简洁。
- **Material 3 Expressive (M3E)**：遵循 Google M3 Expressive 设计规范，采用流线型大圆角卡片、生动的层级色调与优雅排版。
- **动态取色 (Monet)**：深度适配 Android 12+ 动态色彩机制。
- **AOSP 预见性返回手势**：完整适配 Android 系统返回动画 (`android:enableOnBackInvokedCallback="true"`)。
- **沉浸式全面屏**：完美适配 Edge-to-edge，精准处理状态栏、导航栏、屏幕挖孔与软键盘安全区域。
- **多语言自由选择与热更新**：内置简体中文（zh-Hans）、繁体中文（zh-Hant）、英语（en）与日语（ja），支持应用内即时无缝热切换。
- **实时模拟与测试工具**：内置推文链接转换预览框，支持即时预览与一键复制。
- **自动化签名与发布**：参考 [TsukimiBox](https://github.com/Akiyama-25/TsukimiBox)，基于 GitHub Actions 实现持久化 Release 签名密钥生成、校验与自动化发布。

### 安装与使用
1. 安装 **Twi From 67** APK。
2. 打开 LSPosed 管理器，勾选启用 **Twi From 67** 模块。
3. 确保模块作用域包含 **X**（`com.twitter.android`）。
4. 强行停止并重新启动 X（Twitter）。
5. 打开 Twi From 67 应用，输入或选择想要的 `?s=` 数值，点击**保存配置**即可生效。

---

## 繁體中文

### 概述
在 12.36+ / 12.37+ 版本的 X（Twitter）中，複製或分享貼文連結時，系統會自動在末端追加來源辨識參數（例如 `?a=ZRipCP_Wn-MjXIXrZpU&s=19`，其中 19 代表 Android 裝置）。

**Twi From 67** 是一款現代化的 LSPosed（API 101/102）模組，讓您可以隨心所欲自訂 `?s=` 來源識別代號，並提供清理額外追蹤參數的功能。

### 核心特色
- **自訂分享來源代碼**：任意修改推文連結末端的 `?s=` 數字。
- **智慧輸入轉換**：最多輸入兩位數且僅限阿拉伯數字，全形數字（`０`–`９`）自動轉換為半形數字（`0`–`9`）。
- **常見裝置快捷預設**：
  - `46`：iPhone 上的 X App
  - `19`：Android 裝置
  - `20`：電腦網頁版按複製連結
  - `21`：新版網頁介面
  - `12`：iPhone 系統分享
  - `17`：電腦網頁用 Email 分享
- **過濾多餘追蹤**：可選擇去除 `?a=...` 等追蹤參數，保持連結乾淨整潔。
- **Material 3 Expressive (M3E)**：採用全新大圓角容器、生動色調與細緻間距設計。
- **動態色彩 (Monet)**：支援 Android 12+ 系統桌布動態取色。
- **AOSP 預見性返回手勢**：支援 Android 返回手勢動畫預覽。
- **沉浸式全螢幕**：全面支援 Edge-to-edge，自動適配系統狀態列、導覽列及螢幕挖孔。
- **多語系即時熱更新**：支援簡體中文、繁體中文、英文與日文，切換語言後即時重載無須重啟。
- **即時模擬測試**：內建貼文連結轉換測試器，隨打即見轉換結果。
- **GitHub Actions 自動化簽名發布**：參考 [TsukimiBox](https://github.com/Akiyama-25/TsukimiBox) 工作流程，自動建立並驗證簽名產物。

---

## 日本語

### 概要
X (Twitter) 12.36+ 以降では、ツイート共有リンクにクライアント識別用のパラメータ（例：`?a=ZRipCP_Wn-MjXIXrZpU&s=19`。19 は Android 端末を表す）が付加されるようになりました。

**Twi From 67** は、ツイート共有リンクの `?s=` 識別番号をユーザーが自由にカスタマイズできるモダンな LSPosed (API 101/102) モジュールです。

### 主な機能
- **共有元パラメータのカスタマイズ**：`?s=` に付加される数値を自由に設定。
- **全角・半角自動変換 & 入力制限**：最大2桁のアラビア数字のみ入力可能。全角数字（`０`〜`９`）は自動で半角（`0`〜`9`）に変換。
- **クイックプリセット一覧**：
  - `46`：iPhone 版 X App
  - `19`：Android 版 X App
  - `20`：Web版「リンクをコピー」
  - `21`：新Web UI
  - `12`：iPhone システム共有シート
  - `17`：Web版 メール共有
- **トラッキング追跡の除去**：`?a=...` 等の追跡用識別子を削除するオプションを搭載。
- **Material 3 Expressive (M3E)**：Google 最新の M3 Expressive ガイドラインに準拠した流麗な UI。
- **ダイナミックカラー (Monet)**：壁紙に応じた自動配色に対応。
- **予測型戻るジェスチャー**：Android 14+ 戻るアニメーションに完全対応。
- **エッジ・ツー・エッジ (全画面没入型)**：ステータスバー・ナビゲーションバーの余白を完璧に処理。
- **多言語対応 & ホットリロード**：簡体中国語、繁体中国語、英語、日本語をサポート。再起動不要で即座に切り替え可能。
- **リアルタイムシミュレーション**：アプリ内で変換前後のURLを即座に確認・コピー可能。
- **CI/CD 自動ビルド & 署名**：[TsukimiBox](https://github.com/Akiyama-25/TsukimiBox) を参考にした堅牢な自動署名パイプライン。

---

## Technical Specifications
- **App Name**: Twi From 67
- **Package Name**: `Akari.Twitter.share`
- **Initial Version**: `release-1` (Code 1)
- **TargetSDK**: 37 (Android 17)
- **MinSDK**: 26 (Android 8.0 Oreo)
- **LSPosed API**: 101 / 102
- **Target App Scope**: `com.twitter.android`, `com.twitter.android.lite`
- **Hook Methodologies**:
  - `android.content.ClipboardManager#setPrimaryClip`
  - `android.app.Activity#startActivity`
  - `android.app.Activity#startActivityForResult`
  - `android.content.ContextWrapper#startActivity`
  - `android.content.Intent#putExtra`
  - `android.net.Uri.Builder#appendQueryParameter`
- **IPC Configuration**: `XSharedPreferences` with fallback to `ContentProvider` (`Akari.Twitter.share.provider`)

## License
MIT License. Copyright (c) 2026 Akari.