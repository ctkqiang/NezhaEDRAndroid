# NezhaEDR (NezhaCyberEDR Android)

![Kotlin Version](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat&logo=kotlin)
![Android SDK](https://img.shields.io/badge/API_Level-28%2B-3DDC84?style=flat&logo=android)
![Compose Version](https://img.shields.io/badge/Jetpack_Compose-M3-4285F4?style=flat&logo=jetpackcompose)
![GitOps](https://img.shields.io/badge/GitOps-Single_Source_of_Truth-2088FF?style=flat&logo=github)
![License](https://img.shields.io/badge/License-Apache_2.0-D22128?style=flat)

**NezhaEDR (NezhaCyberEDR)** is a lightweight, zero-telemetry, high-performance Endpoint Detection & Response (EDR) engine for Android devices. Built natively using Kotlin and Jetpack Compose, NezhaEDR features on-device captive subnet packet inspection, package installation telemetry sensors, multi-engine v2.0 YAML rule evaluation, and GitOps ruleset synchronization.

[中文文档 (Chinese README)](README.md)

---

## Key Features

- **Captive Subnet Routing Engine**: Leverages Android `VpnService` to establish a zero-log TUN virtual interface. Only flagged C2 IPs and threat domains are routed into the evaluation engine while all general traffic bypasses the VPN, maintaining 0% performance impact and maximum battery life.
- **Declarative v2.0 YAML Evaluation Engine**: Supports Signature O(1) matching, Behavioral Shannon Entropy & Subdomain Depth checks, and Conditional boolean expression evaluation.
- **GitOps Single Source of Truth**: Connects directly to the GitHub ruleset repository for selective or bulk synchronization, as well as local custom YAML rule imports.
- **Zero-Telemetry Privacy Guarantee**: All telemetry logs, intercepted threat events, and domain records are strictly stored inside your device's local SQLite database. No data is ever transmitted to remote servers.
- **Multi-Language & Theme System**: Supports instant switching across 5 languages (English, 中文, 日本語, 한국어, ไทย) and 10 theme accent palettes with Auto/Light/Dark mode.

---

## Application Screenshots

<div align="center">
  <img src="assets/Screenshot_2026-11-15-14-50-54-917_xin.ctkqiang.nezhaedr.jpg" width="180" alt="Sentinel Dashboard" />
  <img src="assets/Screenshot_2026-11-15-14-50-57-697_xin.ctkqiang.nezhaedr.jpg" width="180" alt="Rulesets" />
  <img src="assets/Screenshot_2026-11-15-14-51-13-437_xin.ctkqiang.nezhaedr.jpg" width="180" alt="Threat Alerts" />
  <img src="assets/Screenshot_2026-11-15-14-51-29-726_xin.ctkqiang.nezhaedr.jpg" width="180" alt="Telemetry Vault" />
  <img src="assets/Screenshot_2026-11-15-14-51-31-357_xin.ctkqiang.nezhaedr.jpg" width="180" alt="Settings & Preferences" />
</div>

---

## Technical Architecture & Workflow

```
+-------------------------------------------------------------------------+
|                  NezhaEDR Android Security Architecture                 |
+-------------------------------------------------------------------------+
                                    |
            +-----------------------+-----------------------+
            |                                               |
            v                                               v
+-----------------------+                       +-----------------------+
| Network Telemetry TUN |                       | Package Sensor Vector |
| Captive Subnet Vector |                       |   BroadcastReceiver   |
+-----------------------+                       +-----------------------+
            |                                               |
            +-----------------------+-----------------------+
                                    |
                                    v
            +-----------------------------------------------+
            |  Declarative YAML Engine (EDREvaluationEngine)|
            |    - IP / Domain O(1) HashSet Matching        |
            |    - Behavioral Entropy & Subdomain Depth     |
            |    - Package Name Compiled Regex Matching     |
            +-----------------------------------------------+
                                    |
                    +---------------+---------------+
                    |                               |
                    v (Rule Match)                  v (No Match)
        +-----------------------+       +-----------------------+
        | Security Containment  |       | Pass Traffic & Log to |
        | Threat Alert Popup    |       | Local SQLite Database |
        +-----------------------+       +-----------------------+
```

---

## Declarative YAML Rule Schema (v2.0)

Example rule definition (`c2_threat_block.yaml`):

```yaml
schema_version: "2.0"
rule_id: "RULE-C2-001"
id: "c2_threat_block"
name: "Known C2 Threat IP Block"
description: "Detects and blocks egress connection to known malicious C2 addresses"
severity: "HIGH"
category: "NETWORK"
enabled: true
engine_type: "signature"
target_source: "dns_query"
condition:
  target_ips:
    - "192.0.2.1"
    - "198.51.100.14"
  target_domains:
    - "malicious-c2.example.com"
action: "BLOCK"
```

---

## Project Directory Tree

```text
NezhaEDRAndroid/
├── assets/                    # Application screenshots and image assets
├── app/src/main/
│   ├── java/xin/ctkqiang/nezhaedr/
│   │   ├── containment/       # Threat containment & notification dispatcher
│   │   ├── data/
│   │   │   ├── local/         # Local SQLite telemetry helper & daily midnight cron
│   │   │   ├── model/         # Detection rule & event data models (v2.0 Schema)
│   │   │   ├── parser/        # YAML rule parser (YamlRuleParser)
│   │   │   └── repository/    # GitOps repository synchronization (RuleRepositoryImpl)
│   │   ├── engine/            # Signature, behavioral, and conditional evaluation engine
│   │   ├── service/telemetry/ # VpnService captive routing & package receiver
│   │   ├── ui/
│   │   │   ├── main/          # Jetpack Compose UI screens, ViewModel & State
│   │   │   └── theme/         # 10-theme color palette & Material 3 styles
│   │   └── util/              # EDR Logger & LocaleHelper
│   └── res/                   # Localized string resources (en, zh, ja, ko, th)
├── docs/                      # Developer docs & DartPad-style Playground
└── rules/                     # GitOps Single Source of Truth rule repository
```

---

## Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/ctkqiang/NezhaEDRAndroid.git
cd NezhaEDRAndroid
```

### 2. Build & Test
```bash
# Run unit tests
./gradlew app:testDebugUnitTest

# Assemble Debug APK
./gradlew app:assembleDebug
```

---

## License

Distributed under the [Apache License 2.0](LICENSE).
