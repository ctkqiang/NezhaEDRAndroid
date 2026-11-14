# 哪吒 EDR (NezhaCyberEDR Android)

[![Android CI](https://github.com/ctkqiang/NezhaEDRAndroid/actions/workflows/android.yml/badge.svg)](https://github.com/ctkqiang/NezhaEDRAndroid/actions)
![Kotlin Version](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat&logo=kotlin)
![Android SDK](https://img.shields.io/badge/API_Level-26%2B-3DDC84?style=flat&logo=android)
![Compose Version](https://img.shields.io/badge/Jetpack_Compose-M3-4285F4?style=flat&logo=jetpackcompose)
![GitOps](https://img.shields.io/badge/GitOps-Single_Source_of_Truth-2088FF?style=flat&logo=github)
![License](https://img.shields.io/badge/License-Apache_2.0-D22128?style=flat)

**哪吒 EDR (NezhaCyberEDR)** 是一款面向 Android 终端的轻量级、零遥测（Zero-Telemetry）、高性能终端检测与响应（EDR）防护引擎。基于 Kotlin 与 Jetpack Compose 构建，哪吒 EDR 实现了端侧流式网络遥测捕获、包安装监控感知、声明式 YAML 规则实时评估及 GitOps 在线规则库同步。

[English Documentation (English README)](README_EN.md)

---

## 核心特性

- **囚笼子网捕获 (Captive Subnet Routing Engine)**：利用 Android `VpnService` 建立零日志 TUN 虚拟接口，仅将已知恶意 C2 IP 与黑名单域名路由至端侧评估引擎，普通网络流量完全绕过，实现 0% 性能与续航损耗。
- **声明式 YAML 规则评估引擎 (Declarative YAML Engine)**：采用高性能内存双索引哈希表（Dual Index Hash Map）与预编译正则表达式，实现对网络流量（IP/域名）和应用安装（Package）事件的 O(1) 实时匹配。
- **GitOps 在线同步 (GitOps Single Source of Truth)**：直接对接 GitHub 规则仓库，支持单选与批量同步更新规则库，支持本地 YAML 规则导入。
- **100% 本地隐私与零遥测 (0-Telemetry Privacy Guarantee)**：所有网络访问日志、阻断事件与遥测历史均严格存储于端侧 SQLite 数据库中，不向任何远程服务器发送遥测数据。
- **现代多语言与主题系统**：集成 5 种语言（中文、English、日本語、한국어、ไทย）实时切换，支持 6 种主题配色（Crimson Red、Cyber Blue、Magenta Pink、Emerald Green、Amber Gold、Amethyst Purple）。

---

## 系统架构与工作流

```
+-------------------------------------------------------------------------+
|                         哪吒 EDR 终端安全防护架构                         |
+-------------------------------------------------------------------------+
                                    |
            +-----------------------+-----------------------+
            |                                               |
            v                                               v
+-----------------------+                       +-----------------------+
|  网络遥测 (TUN/VPN)    |                       |  包安装感知 (Broadcast) |
| Captive Subnet Vector |                       | Package Sensor Vector |
+-----------------------+                       +-----------------------+
            |                                               |
            +-----------------------+-----------------------+
                                    |
                                    v
            +-----------------------------------------------+
            |    YAML 评估规则引擎 (EDREvaluationEngine)    |
            |       - IP / Domain O(1) HashSet 匹配         |
            |       - Package Compiled Regex 正则匹配        |
            +-----------------------------------------------+
                                    |
                    +---------------+---------------+
                    |                               |
                    v (匹配成功)                     v (未匹配)
        +-----------------------+       +-----------------------+
        |  安全处置与阻断告警   |       |  正常放行与 SQLite 记录|
        | Containment Action    |       | Local Telemetry DB    |
        +-----------------------+       +-----------------------+
```

---

## 声明式 YAML 规则语法

规则示例（例如 `c2_threat_block.yaml`）：

```yaml
id: "c2_threat_block"
name: "Known C2 Threat IP Block"
description: "Detects and blocks egress connection to known malicious C2 addresses"
severity: "HIGH"
category: "NETWORK"
condition:
  target_ips:
    - "192.0.2.1"
    - "198.51.100.14"
  target_domains:
    - "malicious-c2.example.com"
action: "BLOCK"
```

---

## 项目目录结构

```text
NezhaEDRAndroid/
├── app/src/main/
│   ├── java/xin/ctkqiang/nezhaedr/
│   │   ├── containment/       # 威胁处置与告警推送模块
│   │   ├── data/
│   │   │   ├── local/         # SQLite 本地遥测数据库 (TelemetryDatabaseHelper)
│   │   │   ├── model/         # 检测规则与事件数据模型
│   │   │   ├── parser/        # YAML 规则解析器 (YamlRuleParser)
│   │   │   └── repository/    # GitOps 规则仓库同步逻辑 (RuleRepositoryImpl)
│   │   ├── engine/            # O(1) 规则评估匹配引擎 (EDREvaluationEngine)
│   │   ├── service/telemetry/ # VpnService 囚笼路由与 Package 广播接收器
│   │   ├── ui/
│   │   │   ├── main/          # Jetpack Compose 主界面、ViewModel 与 State
│   │   │   └── theme/         # 6 色主题与 Material 3 样式配置
│   │   └── util/              # 日志工具与多语言 LocaleHelper
│   └── res/                   # 多语言字符串资源 (zh, en, ja, ko, th)
└── rules/                     # GitOps 在线规则库单源真理 (Single Source of Truth)
```

---

## 快速开始

### 1. 克隆代码仓库
```bash
git clone https://github.com/ctkqiang/NezhaEDRAndroid.git
cd NezhaEDRAndroid
```

### 2. 构建与单元测试
```bash
# 运行单元测试
./gradlew app:testDebugUnitTest

# 编译 Debug APK
./gradlew app:assembleDebug
```

---

## 开源协议

本项目基于 [Apache License 2.0](LICENSE) 协议开源。
