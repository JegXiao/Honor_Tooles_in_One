# 荣耀工具箱 | Honor Toolbox

> 导航、熄屏、无线调试、性能模式四合一，一个 App 管完荣耀手机的常用系统开关。

**中文** · [ English](#english)

---

## 兼容性（先看这里）

| 设备 / 系统 | 状态 | 说明 |
|---|---|---|
| 荣耀（MagicOS） | **已实机验证** | 荣耀 HEY3-W00（Android 16 / MagicOS 10.0.0.171）验证通过；导航、无线调试、性能模式通过 Shizuku 完成 |
| 华为（EMUI / HarmonyOS 兼容 Android 应用） | 代码按同一套荣耀路径处理，**未实机验证** | 导航与性能模式需要 Shizuku；取值和行为可能与荣耀不同 |
| HarmonyOS NEXT（纯血鸿蒙） | **不适用** | 不运行 Android APK，本项目无法安装 |
| AOSP / Pixel / Android One | 按 AOSP 机制实现，**未在 Pixel 实机验证** | 导航、无线调试需要 adb 授予 `WRITE_SECURE_SETTINGS`，或使用 Shizuku |
| 小米 / Redmi / 三星 / OPPO / vivo 等 | 导航功能未适配 | 这些 ROM 可能使用私有设置项；熄屏时间、无线调试、性能模式是否可用取决于系统是否保留对应设置项 |
| Android 10 以下 | 导航仅支持到三键 / 二键 | 手势导航从 Android 10（API 29）开始提供 |
| Android 11 以下 | 无线调试不可用 | 无线调试从 Android 11（API 30）开始提供 |
| 非荣耀 / 华为设备 | 性能模式不可用 | 系统项 `SmartModeStatus` 仅存在于 MagicOS / EMUI，App 会显示「当前设备不支持」 |
| 平板 / 折叠屏 / 桌面模式 | 未适配 | 导航栏形态与设置值可能不同 |
| 多用户 / 平行空间 / 分身应用 | 需分别授权 | 系统设置与 Shizuku 授权按用户隔离 |

> 本项目只在荣耀 HEY3-W00 上做过完整实机验证。其他品牌请先按上表判断，不要默认「装上就能用」。

## 功能特性

- **导航方式**：三键导航 ⇄ 手势导航一键切换；荣耀 / 华为通过 Shizuku 执行 `settings` + `cmd overlay`，AOSP 通过 `WRITE_SECURE_SETTINGS` 写 `navigation_mode`。
- **熄屏时间**：七档预设（15 秒 ~ 30 分钟），以及「永不熄屏」；控制中心磁贴在 2 分钟与永不熄屏之间两态切换。
- **无线调试**：一键开关 Android 11+ 的系统「无线调试」，无需进入开发者选项。
- **性能模式**：一键切换荣耀 MagicOS / 华为 EMUI 的性能模式与均衡模式。
- **四个控制中心磁贴**：导航方式、熄屏时间、无线调试、性能模式，可分别加入快捷面板。
- **2×2 桌面小组件**：四个格子对应四个功能，点按即切换，实时显示当前状态。
- **统一授权页**：集中管理 Shizuku、ADB、Root 与「修改系统设置」特殊权限。
- **无后台常驻服务、无网络权限、无统计、无广告**；界面支持深色模式与边到边显示。
- 所有写入都会回读校验，失败时明确提示，不假装成功。

## 快速开始

### 直接安装

1. 打开 [Releases](https://github.com/JegXiao/Honor_Tooles_in_One/releases/latest) 下载最新的 `Honor_Tooles_in_One.apk`。
   稳定直链：<https://github.com/JegXiao/Honor_Tooles_in_One/releases/latest/download/Honor_Tooles_in_One.apk>
2. 安装后打开 App，先进入「授权与帮助」完成 Shizuku 或 ADB 授权。
3. 回到主页，四个功能卡片会实时显示当前系统状态。

> 如果你之前安装的是 Android Studio 编译的 debug 包，首次安装 Release 包需要先卸载旧包：debug 与 release 的签名不同，不能覆盖安装。卸载会清除 adb 授权、Shizuku 授权和桌面小组件。
>
> 从 v1.0 开始，Release 使用固定证书签名，后续版本只要递增 `versionCode` 即可直接覆盖升级。

Release APK 签名证书 SHA-256：

```text
13:13:03:97:63:6C:E8:F6:B5:D2:4C:1A:90:0E:E6:A1:03:A6:F3:77:69:84:CA:D3:FA:DA:40:09:2B:C4:F7:62
```

### 从源码编译

```bash
git clone https://github.com/JegXiao/Honor_Tooles_in_One.git
cd Honor_Tooles_in_One
./gradlew assembleDebug
```

生成的 APK 在 `app/build/outputs/apk/debug/app-debug.apk`。

也可以用 Android Studio：打开项目 → 等待 Gradle Sync → 连接手机 → Run。

编译环境（来自 `app/build.gradle.kts`、`gradle/wrapper/gradle-wrapper.properties` 与实测）：

| 项目 | 版本 |
|---|---|
| Android Gradle Plugin | 9.5.1 |
| Gradle | 9.6.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 24（Android 7.0） |
| JDK | 17 或以上（CI 使用 Temurin 21，本机验证使用 25） |
| Kotlin | AGP 9.5.1 内置 Kotlin 支持（2.3.21） |
| AndroidX AppCompat | 1.8.0 |
| Material Components | 1.14.0 |
| core-ktx | 1.19.1 |
| Shizuku API / Provider | 13.1.5 |

`local.properties` 由 Android Studio 自动生成（`sdk.dir`），已被 `.gitignore` 忽略；项目不需要任何 API Key。

> debug APK 使用本机 debug keystore 签名，不能覆盖 Release 包，反之亦然。两者切换时必须先卸载。

## 授权

四个功能分别依赖不同的系统能力：

| 功能 | 系统项 | 需要的授权 |
|---|---|---|
| 导航方式（荣耀 / 华为） | `Settings.Secure.navigation_mode`、`secure_gesture_navigation` + 导航栏 overlay | **Shizuku** |
| 导航方式（AOSP 类） | `Settings.Secure.navigation_mode` | `WRITE_SECURE_SETTINGS`（ADB）或 Shizuku |
| 熄屏时间 | `Settings.System.SCREEN_OFF_TIMEOUT` | 「修改系统设置」`WRITE_SETTINGS` |
| 无线调试 | `Settings.Global.adb_wifi_enabled` | Shizuku，或 `WRITE_SECURE_SETTINGS`（ADB），或 Root |
| 性能模式（荣耀 / 华为） | `Settings.System.SmartModeStatus` | Shizuku（MagicOS 会拒绝普通 App 直接写入）；或「修改系统设置」+ 允许的 ROM |

授权只需要完成一次；卸载重装后需要重新做。Shizuku 服务重启或手机重启后，可能需要在 Shizuku 中重新确认授权。

### A. Shizuku（推荐）

1. 安装 [Shizuku](https://shizuku.rikka.app/)。
2. 按 Shizuku 内引导启动服务（Android 11+ 可以配合无线调试启动）。
3. 打开本 App →「授权与帮助」→ 点击「授予权限」，在系统弹窗中允许。
4. 状态显示「已授权，可直接一键切换」后即可使用全部功能。

荣耀 / 华为的导航切换**必须**使用 Shizuku：系统设置页是通过 signature 权限直接开关导航栏 overlay 的，普通 App 仅写设置项不会让 SystemUI 切换。

### B. ADB 授权（备用）

不使用 Shizuku 时，可以在电脑上通过 ADB 执行一次下面的命令。第一条授予安全设置权限（导航、无线调试），第二条授予「修改系统设置」特殊权限（熄屏时间、性能模式备用路径）：

```bash
adb shell pm grant me.jegxiao.honortoolbox android.permission.WRITE_SECURE_SETTINGS
adb shell appops set me.jegxiao.honortoolbox android:write_settings allow
```

也可以在「授权与帮助」页面点「复制命令」直接复制这两行。执行完回到 App 点「重新检查」。

说明：

- `WRITE_SECURE_SETTINGS` 是受保护权限，安装界面无法授予，只能通过 adb 或 root。
- 这两条命令赋予 App 修改系统设置的能力，请只在自己的、信任的设备上授权。
- 撤销权限：

  ```bash
  adb shell pm revoke me.jegxiao.honortoolbox android.permission.WRITE_SECURE_SETTINGS
  adb shell appops set me.jegxiao.honortoolbox android:write_settings deny
  ```

- 如果 `pm grant` 报 `Unknown package`，先安装 APK。
- 部分系统要求先打开「USB 调试（安全设置）」或登录账号，本项目未适配这些限制。

### C. Root（可选）

「授权与帮助」页面提供「没有电脑？尝试 Root 一键授权」。点击后 App 会执行：

```bash
pm grant me.jegxiao.honortoolbox android.permission.WRITE_SECURE_SETTINGS
appops set me.jegxiao.honortoolbox android:write_settings allow
```

没有可用的 `su` 时会提示失败，不会卡住界面。无线调试和性能模式的写入路径也会在需要时尝试 Root。

## 使用说明

### 主界面

- **导航方式卡片**：显示当前模式（三键 / 二键 / 手势）和说明；按钮文字会变成「切换为手势导航」或「切换为三键导航」。未授权时按钮变成「前往授权」。
- **熄屏时间卡片**：点按进入选择页，可设置七档预设，或直接授予权限后修改。
- **无线调试开关**：直接切换；需要 Android 11+。
- **性能模式开关**：直接切换；仅荣耀 / 华为设备显示可用状态。
- **授权与帮助**：统一管理 Shizuku、ADB、Root 与「修改系统设置」权限。

### 控制中心磁贴

四个磁贴的名称分别是「导航方式」「熄屏时间」「无线调试」「性能模式」。

1. 下拉控制中心，点按编辑（铅笔）按钮。
2. 在可用开关中找到对应磁贴，拖入常用区域。
3. 长按任意磁贴会回到本 App。

「熄屏时间」磁贴是两态开关：

| 磁贴状态 | 系统值 | 点按后 |
|---|---|---|
| 未激活 | 2 分钟（120000 ms） | 切换到永不熄屏 |
| 激活 | 2147483647 ms（约 24.85 天） | 切回 2 分钟 |

如果当前系统值既不是 2 分钟也不是永不熄屏（例如在系统设置里选了 30 分钟），磁贴显示未激活并展示当前值；第一次点按切到永不，第二次点按回到 2 分钟。

### 桌面小组件

在桌面长按空白处 → 小组件 → 找到「荣耀工具箱」（2×2）→ 拖到桌面。四个格子分别对应导航方式、熄屏时间、无线调试、性能模式：

- 点按导航格：在手势与三键之间切换。
- 点按熄屏格：在永不熄屏与 2 分钟之间切换。
- 点按无线调试格：开启 / 关闭无线调试。
- 点按性能格：在性能与均衡之间切换。
- 未授权时点按会自动打开「授权与帮助」。

## 实现说明

### 项目结构

```text
app/src/main/java/
├── me/jegxiao/honortoolbox/
│   ├── MainActivity.kt              # 四功能主页
│   ├── ScreenTimeoutActivity.kt     # 熄屏时间选择页
│   ├── SetupActivity.kt             # 授权与帮助
│   ├── core/
│   │   ├── ShizukuManager.kt        # Shizuku 状态、权限与 shell 执行
│   │   ├── RootShell.kt             # Root 备用通道
│   │   ├── UiUtils.kt               # 边到边与系统栏适配
│   │   ├── TileRefresh.kt           # 刷新四个磁贴
│   │   └── TileNavigation.kt        # Android 14 磁贴启动 Activity
│   ├── features/
│   │   ├── NavModeManager.kt        # 导航方式读写与 overlay 切换
│   │   ├── ScreenTimeout.kt         # 熄屏时间读写与格式化
│   │   ├── WirelessDebug.kt         # 无线调试开关
│   │   └── PerfMode.kt              # 性能模式
│   ├── tiles/                       # 四个 Quick Settings TileService
│   └── widget/
│       └── ToolboxWidgetProvider.kt # 2×2 RemoteViews 小组件
└── rikka/shizuku/
    └── ShizukuAccess.java           # 调用 Shizuku 的 newProcess（非公开 API）
```

### 各功能的系统实现

**导航方式**

- AOSP：`Settings.Secure.navigation_mode`，`0` = 三键，`1` = 二键，`2` = 手势。
- 荣耀 / 华为：`navigation_mode` 使用 `101`（三键）/ `105`（手势），同时维护 `secure_gesture_navigation`（`0` / `1`）。切换时还会用 Shizuku 执行：

  ```bash
  cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural
  # 或
  cmd overlay disable com.android.internal.systemui.navbar.gestural
  cmd overlay disable com.android.internal.systemui.navbar.threebutton
  ```

  最后通过 `cmd overlay list` 与设置值双重校验是否生效。
- AOSP 设备如果只有 Shizuku、没有 `WRITE_SECURE_SETTINGS`，也会走 shell 写 `navigation_mode`。

**熄屏时间**

- 读写 `Settings.System.SCREEN_OFF_TIMEOUT`。
- 预设：15 秒、30 秒、1 分钟、2 分钟、5 分钟、10 分钟、30 分钟；「永不熄屏」写入 `Integer.MAX_VALUE`（2147483647 ms，约 24.85 天）。
- 写入后立即回读，系统若把值改成其他档位会报告失败。

**无线调试**

- 读写 `Settings.Global.adb_wifi_enabled`（Android 11+ 的隐藏设置项）。
- 写入优先级：Shizuku shell → `WRITE_SECURE_SETTINGS` 直接写 → Root。
- Shizuku 授权但调用失败时，会继续尝试其他通道，并如实返回失败原因。

**性能模式**

- 读写 `Settings.System.SmartModeStatus`：`5` = 性能模式，`1` = 均衡模式。
- MagicOS 拒绝普通 App 直接写系统设置，因此优先通过 Shizuku 执行 `settings put system SmartModeStatus <值>`。
- 写入后回读校验；设备没有该设置项时，开关直接显示「当前设备不支持」。

### Shizuku 接入方式

- 依赖 `dev.rikka.shizuku:api:13.1.5` 与 `provider:13.1.5`，Manifest 中注册 `rikka.shizuku.ShizukuProvider`。
- Shizuku 13.1.5 没有公开 `newProcess()`，所以 `ShizukuAccess.java` 放在 `rikka.shizuku` 包内，直接调用 `Shizuku.requireService().newProcess(...)`，无需反射。
- 所有 shell 命令带超时（默认 8 秒），超时会销毁子进程并返回错误，不会让界面无限等待。
- 磁贴、主页、小组件共用 `ShizukuManager` 的授权状态；授权结果通过 `Shizuku.OnRequestPermissionResultListener` 回到界面刷新。

### 界面与刷新

- 四个功能的主页卡片、磁贴、小组件都由 `ContentObserver` 监听对应设置项 URI，系统值变化后自动刷新。
- 控制中心磁贴遵循 Android 14 的 `PendingIntent` 启动要求；同时声明 `TOGGLEABLE_TILE`。
- 小组件使用 `RemoteViews` 实现 2×2 布局，四个格子各自绑定 `PendingIntent`，操作在 `goAsync()` 后台线程执行，完成后主动刷新。
- 主题使用 Material 3 DayNight 配色，主界面与两个子页面均处理状态栏 / 导航栏 Insets。

## 安全边界与注意事项

- 本 App **不申请 `INTERNET` 权限**，没有联网、统计、广告或后台上传逻辑。
- 它确实需要很强的系统权限（Shizuku shell / `WRITE_SECURE_SETTINGS` / `WRITE_SETTINGS`）。请只在自己的设备上安装，并在授权前理解这些权限的影响。
- 关闭「无线调试」会立即断开已经建立的无线 ADB 连接；远程调试时请避免误触。
- 切换导航方式后，SystemUI 会短暂重载，这是正常现象。
- 「永不熄屏」会更耗电；它把超时设为约 24.85 天，并不是真正的无限。按电源键、锁屏、低电量策略仍可能关闭屏幕。
- 厂商 ROM 可能静默拒绝写入或把值限制成其他档位；App 会回读校验并提示失败，不会伪造成功。
- Shizuku 重启、手机重启或卸载重装后，授权可能失效；重新打开「授权与帮助」检查即可。
- Root 按钮只在用户主动点击时执行，不会在后台尝试提权。

## 已知限制

- 导航切换只在荣耀 HEY3-W00 上完整验证；AOSP、华为与其他品牌未实机验证。
- 熄屏时间、无线调试、性能模式依赖厂商是否保留对应系统设置项；缺失时 App 会显示不支持或失败。
- 多用户、平行空间、分身应用需要各自安装并授权。
- 没有适配 Android 桌面模式、折叠屏展开态的特殊导航栏形态。
- 本项目的 Release 附件名为固定名称 `Honor_Tooles_in_One.apk`，升级时同名覆盖，README 的 `releases/latest/download` 直链不需要随版本修改。

## 版本与发布

- 版本号单一源头：`app/build.gradle.kts` 的 `versionName`（对外）与 `versionCode`（升级序号）。
- 当前版本：**v1.1**（`versionCode = 2`）。
- v1.1 修复了 v1.0 Release 包启动闪退：AGP 9 默认的类重打包与 Kotlin 集合类冲突，`app/src/main/keepRules/rules.keep` 已用 `-dontrepackage` 关闭该优化。
- Release tag 必须与 `versionName` 的 `vX.Y` 一致；每次发版 `versionCode` 必须递增，否则无法覆盖安装。
- GitHub Actions 在推送 `v*` tag 时构建签名 Release APK，并校验 tag 与 `versionName`、APK 签名。
- 签名密钥只存在于 GitHub Secrets（`RELEASE_KEYSTORE_BASE64`、`RELEASE_KEYSTORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`），仓库内没有任何 keystore 或密码。
- 固定签名后，从 v1.0 开始后续版本保持同一 keystore + alias，并递增 `versionCode`，即可覆盖升级。

---

# English

> Navigation, screen timeout, wireless debugging and performance mode in one Honor toolbox app.

**English** · [中文](#荣耀工具箱--honor-toolbox)

---

## Compatibility (read this first)

| Device / OS | Status | Notes |
|---|---|---|
| HONOR (MagicOS) | **Verified on a real device** | Verified on HONOR HEY3-W00 (Android 16 / MagicOS 10.0.0.171); navigation, wireless debugging and performance mode use Shizuku |
| HUAWEI (EMUI / HarmonyOS with Android app support) | Same code path as HONOR, **not verified on a real device** | Navigation and performance mode need Shizuku; values may differ |
| HarmonyOS NEXT | **Not applicable** | Does not run Android APKs |
| AOSP / Pixel / Android One | Implemented per AOSP, **not verified on a Pixel** | Navigation and wireless debugging need adb `WRITE_SECURE_SETTINGS`, or Shizuku |
| Xiaomi / Redmi / Samsung / OPPO / vivo | Navigation not adapted | These ROMs may use private settings; the other features depend on whether the system settings exist |
| Below Android 10 | No gesture navigation | Gesture navigation is available from Android 10 (API 29) |
| Below Android 11 | Wireless debugging unavailable | Wireless debugging is available from Android 11 (API 30) |
| Non-HONOR / non-HUAWEI | Performance mode unavailable | `SmartModeStatus` only exists on MagicOS / EMUI; the switch shows "Not supported" |
| Tablets / foldables / desktop mode | Not adapted | Navigation bar forms and values may differ |
| Multi-user / Parallel Space / app cloning | Separate authorization | System settings and Shizuku grants are per user |

> The project has only been fully verified on HONOR HEY3-W00. Check the table before assuming it will work on another brand.

## Features

- **Navigation mode**: toggle between 3-button and gesture navigation. HONOR / HUAWEI use Shizuku to run `settings` + `cmd overlay`; AOSP writes `navigation_mode` with `WRITE_SECURE_SETTINGS`.
- **Screen timeout**: seven presets (15 s to 30 min) plus "never"; the Quick Settings tile toggles between 2 minutes and never.
- **Wireless debugging**: one-tap toggle for Android 11+ wireless debugging, without opening Developer options.
- **Performance mode**: toggle HONOR MagicOS / HUAWEI EMUI performance and balanced modes.
- **Four Quick Settings tiles**: Navigation, Screen timeout, Wireless debugging, Performance mode.
- **2×2 home screen widget**: four cells, one per feature, with live state.
- **Unified setup page**: manage Shizuku, ADB, Root and the "Modify system settings" special access in one place.
- **No always-on background service, no network permission, no analytics, no ads**; dark mode and edge-to-edge UI.
- Every write is verified by reading the setting back; failures are reported instead of silently pretending success.

## Quick Start

### Install the APK

1. Download the latest `Honor_Tooles_in_One.apk` from [Releases](https://github.com/JegXiao/Honor_Tooles_in_One/releases/latest).
   Stable direct link: <https://github.com/JegXiao/Honor_Tooles_in_One/releases/latest/download/Honor_Tooles_in_One.apk>
2. Open the app and complete Shizuku or ADB authorization in "Setup & help".
3. Return to the home screen; the four cards show the live system state.

> If you previously installed an Android Studio debug build, uninstall it before installing the Release APK: debug and release signatures differ, so they cannot be installed over each other. Uninstalling clears the adb grant, Shizuku grant and home screen widgets.
>
> Starting with v1.0, releases use a fixed certificate, so later versions can be installed over earlier ones as long as `versionCode` increases.

Release APK certificate SHA-256:

```text
13:13:03:97:63:6C:E8:F6:B5:D2:4C:1A:90:0E:E6:A1:03:A6:F3:77:69:84:CA:D3:FA:DA:40:09:2B:C4:F7:62
```

### Build from source

```bash
git clone https://github.com/JegXiao/Honor_Tooles_in_One.git
cd Honor_Tooles_in_One
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

You can also open the project in Android Studio, wait for Gradle Sync, connect a phone and press Run.

Build requirements (read from `app/build.gradle.kts`, the Gradle wrapper and actual builds):

| Item | Version |
|---|---|
| Android Gradle Plugin | 9.5.1 |
| Gradle | 9.6.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 24 (Android 7.0) |
| JDK | 17 or newer (CI uses Temurin 21; local verification used 25) |
| Kotlin | Built-in Kotlin support in AGP 9.5.1 (2.3.21) |
| AndroidX AppCompat | 1.8.0 |
| Material Components | 1.14.0 |
| core-ktx | 1.19.1 |
| Shizuku API / Provider | 13.1.5 |

`local.properties` is generated by Android Studio (`sdk.dir`) and is git-ignored. The project needs no API key.

> A debug APK uses the local debug keystore and cannot be installed over a release APK, or vice versa. Uninstall one before switching.

## Authorization

The four features rely on different system capabilities:

| Feature | System setting | Required access |
|---|---|---|
| Navigation (HONOR / HUAWEI) | `Settings.Secure.navigation_mode`, `secure_gesture_navigation` + navigation bar overlays | **Shizuku** |
| Navigation (AOSP-like) | `Settings.Secure.navigation_mode` | `WRITE_SECURE_SETTINGS` (adb) or Shizuku |
| Screen timeout | `Settings.System.SCREEN_OFF_TIMEOUT` | "Modify system settings" `WRITE_SETTINGS` |
| Wireless debugging | `Settings.Global.adb_wifi_enabled` | Shizuku, or `WRITE_SECURE_SETTINGS` (adb), or root |
| Performance mode (HONOR / HUAWEI) | `Settings.System.SmartModeStatus` | Shizuku (MagicOS rejects direct writes from third-party apps); or `WRITE_SETTINGS` on ROMs that allow it |

Authorization is one-time; uninstalling and reinstalling requires it again. After a phone reboot or a Shizuku service restart, you may need to re-confirm the grant in Shizuku.

### A. Shizuku (recommended)

1. Install [Shizuku](https://shizuku.rikka.app/).
2. Start its service following the in-app guide (Android 11+ can use wireless debugging to start it).
3. Open this app → "Setup & help" → "Grant permission" and allow the system dialog.
4. Once the status reads "Granted", all features are available.

Navigation switching on HONOR / HUAWEI **requires** Shizuku: the system Settings app toggles the navigation bar overlay with a signature permission, and a third-party app cannot make SystemUI switch by writing the setting alone.

### B. ADB (fallback)

Without Shizuku, run these commands once from a computer. The first grants secure-settings access (navigation, wireless debugging); the second grants the "Modify system settings" special access (screen timeout and the performance-mode fallback):

```bash
adb shell pm grant me.jegxiao.honortoolbox android.permission.WRITE_SECURE_SETTINGS
adb shell appops set me.jegxiao.honortoolbox android:write_settings allow
```

The "Copy command" button in "Setup & help" copies both lines. Press "Recheck" when done.

- `WRITE_SECURE_SETTINGS` is a protected permission; it cannot be granted from the install UI, only via adb or root.
- These commands give the app the ability to change system settings. Only authorize on your own, trusted device.
- Revoke with:

  ```bash
  adb shell pm revoke me.jegxiao.honortoolbox android.permission.WRITE_SECURE_SETTINGS
  adb shell appops set me.jegxiao.honortoolbox android:write_settings deny
  ```

- If `pm grant` reports `Unknown package`, install the APK first.
- Some systems require "USB debugging (Security settings)" or an account login; these restrictions are not handled by this project.

### C. Root (optional)

"Setup & help" offers "No computer? Try one-tap Root authorization". The app runs:

```bash
pm grant me.jegxiao.honortoolbox android.permission.WRITE_SECURE_SETTINGS
appops set me.jegxiao.honortoolbox android:write_settings allow
```

It reports failure if `su` is unavailable instead of freezing the UI. Wireless debugging and performance mode also try root when needed.

## Usage

### Home screen

- **Navigation card**: shows the current mode (3-button / 2-button / gesture); the button changes to "Switch to gesture navigation" or "Switch to 3-button navigation". Without authorization it becomes "Go to setup".
- **Screen timeout card**: opens the picker with seven presets and the never option.
- **Wireless debugging switch**: toggles directly; requires Android 11+.
- **Performance mode switch**: toggles directly; only meaningful on HONOR / HUAWEI.
- **Setup & help**: manages Shizuku, ADB, Root and "Modify system settings".

### Quick Settings tiles

The four tiles are "Navigation", "Screen timeout", "Wireless debugging" and "Performance mode".

1. Swipe down to open Quick Settings, tap Edit (pencil).
2. Find a tile and drag it into the active area.
3. Long-press any tile to open this app.

The "Screen timeout" tile is a two-state switch:

| Tile state | System value | After tap |
|---|---|---|
| Inactive | 2 minutes (120000 ms) | Never |
| Active | 2147483647 ms (~24.85 days) | 2 minutes |

If the current value is neither 2 minutes nor never (for example 30 minutes chosen in system Settings), the tile is inactive and shows the current value; the first tap sets never, the second returns to 2 minutes.

### Home screen widget

Long-press an empty area → Widgets → "Honor Toolbox" (2×2) → drag to the home screen. The four cells control navigation, screen timeout, wireless debugging and performance mode:

- Navigation cell: toggles gesture / 3-button.
- Timeout cell: toggles never / 2 minutes.
- Wireless debugging cell: on / off.
- Performance cell: performance / balanced.
- Tapping a cell without authorization opens "Setup & help".

## Implementation Notes

### Project layout

```text
app/src/main/java/
├── me/jegxiao/honortoolbox/
│   ├── MainActivity.kt              # Four-feature dashboard
│   ├── ScreenTimeoutActivity.kt     # Screen timeout picker
│   ├── SetupActivity.kt             # Setup & help
│   ├── core/
│   │   ├── ShizukuManager.kt        # Shizuku state, permission and shell execution
│   │   ├── RootShell.kt             # Root fallback
│   │   ├── UiUtils.kt               # Edge-to-edge and system bar handling
│   │   ├── TileRefresh.kt           # Refresh all four tiles
│   │   └── TileNavigation.kt        # Android 14 tile-to-activity launch
│   ├── features/
│   │   ├── NavModeManager.kt        # Navigation mode + overlay switching
│   │   ├── ScreenTimeout.kt         # Screen timeout read/write/format
│   │   ├── WirelessDebug.kt         # Wireless debugging switch
│   │   └── PerfMode.kt              # Performance mode
│   ├── tiles/                       # Four Quick Settings TileServices
│   └── widget/
│       └── ToolboxWidgetProvider.kt # 2×2 RemoteViews widget
└── rikka/shizuku/
    └── ShizukuAccess.java           # Calls Shizuku newProcess (non-public API)
```

### Per-feature implementation

**Navigation**

- AOSP: `Settings.Secure.navigation_mode`, `0` = 3-button, `1` = 2-button, `2` = gesture.
- HONOR / HUAWEI: `navigation_mode` uses `101` (3-button) / `105` (gesture), with `secure_gesture_navigation` (`0` / `1`). The switch also runs, through Shizuku:

  ```bash
  cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural
  # or
  cmd overlay disable com.android.internal.systemui.navbar.gestural
  cmd overlay disable com.android.internal.systemui.navbar.threebutton
  ```

  The result is verified against both `cmd overlay list` and the settings value.
- AOSP devices with Shizuku but no `WRITE_SECURE_SETTINGS` use the shell to write `navigation_mode`.

**Screen timeout**

- Reads/writes `Settings.System.SCREEN_OFF_TIMEOUT`.
- Presets: 15 s, 30 s, 1 min, 2 min, 5 min, 10 min, 30 min. "Never" writes `Integer.MAX_VALUE` (2147483647 ms, ~24.85 days).
- The value is read back immediately; if the ROM clamps it to another preset, the app reports failure.

**Wireless debugging**

- Reads/writes `Settings.Global.adb_wifi_enabled` (hidden setting on Android 11+).
- Write order: Shizuku shell → `WRITE_SECURE_SETTINGS` direct write → root.
- If Shizuku is granted but fails, the app tries the other paths and reports the real failure.

**Performance mode**

- Reads/writes `Settings.System.SmartModeStatus`: `5` = performance, `1` = balanced.
- MagicOS rejects direct system-setting writes from ordinary apps, so the app prefers `settings put system SmartModeStatus <value>` through Shizuku.
- The value is read back; if the setting does not exist, the switch shows "Not supported".

### Shizuku integration

- Depends on `dev.rikka.shizuku:api:13.1.5` and `provider:13.1.5`; the manifest registers `rikka.shizuku.ShizukuProvider`.
- Shizuku 13.1.5 does not expose `newProcess()` publicly, so `ShizukuAccess.java` lives in the `rikka.shizuku` package and calls `Shizuku.requireService().newProcess(...)` directly, without reflection.
- Every shell command has a timeout (8 s by default); on timeout the child process is destroyed and an error is returned.
- Tiles, the home screen and the widget share `ShizukuManager`; permission results refresh the UI through `Shizuku.OnRequestPermissionResultListener`.

### UI and refresh

- The dashboard cards, tiles and widget observe the relevant settings URIs with `ContentObserver`, so external changes also refresh the UI.
- Tiles follow the Android 14 `PendingIntent` requirement and declare `TOGGLEABLE_TILE`.
- The 2×2 widget uses `RemoteViews`; each cell has its own `PendingIntent`. Actions run on a background thread via `goAsync()` and refresh the widget afterwards.
- Material 3 DayNight colors; all three screens handle status/navigation bar insets and edge-to-edge display.

## Security Boundaries

- The app **does not request the `INTERNET` permission**; there is no networking, analytics, advertising or upload code.
- It does require powerful system permissions (Shizuku shell / `WRITE_SECURE_SETTINGS` / `WRITE_SETTINGS`). Only install it on your own device and understand these permissions before granting them.
- Turning off wireless debugging immediately disconnects any established wireless ADB connection. Avoid accidental taps while debugging remotely.
- Switching the navigation mode makes SystemUI reload briefly; this is expected.
- "Never" timeout uses more battery. It sets the timeout to ~24.85 days, not true infinity; the power button, lock screen, low battery and device policies can still turn the screen off.
- Vendor ROMs may silently reject writes or clamp values. The app reads values back and reports failures rather than faking success.
- Shizuku grants can be lost after a Shizuku service restart, a phone reboot or a reinstall; check "Setup & help" again.
- The Root button only runs when the user taps it; the app never attempts privilege escalation in the background.

## Known Limitations

- Navigation switching is fully verified only on HONOR HEY3-W00; AOSP, HUAWEI and other brands are untested.
- Screen timeout, wireless debugging and performance mode depend on the vendor keeping the corresponding system settings. If missing, the app reports unsupported or failure.
- Multi-user, Parallel Space and app cloning require separate installs and grants.
- Android desktop mode and special navigation bar layouts on unfolded foldables are not adapted.
- The release asset uses the fixed name `Honor_Tooles_in_One.apk`, so upgrades overwrite it by name and the README `releases/latest/download` link never needs updating.

## Version and Releases

- Single version source: `versionName` (public) and `versionCode` (upgrade counter) in `app/build.gradle.kts`.
- Current version: **v1.1** (`versionCode = 2`).
- v1.1 fixes a startup crash in the v1.0 release build: AGP 9's default class repackaging conflicted with Kotlin collection classes. `app/src/main/keepRules/rules.keep` now disables that optimization with `-dontrepackage`.
- A release tag must match `versionName` as `vX.Y`; `versionCode` must increase every release, otherwise the APK cannot be installed over the previous one.
- GitHub Actions builds the signed release APK when a `v*` tag is pushed, and verifies the tag/version and APK signature.
- Signing material exists only in GitHub Secrets (`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`); the repository contains no keystore or password.
- With the fixed certificate, later versions can be installed over v1.0 as long as the same keystore/alias is used and `versionCode` increases.