---
name: "Ruleset Suggestion / 规则库建议"
about: "Propose new threat domains, C2 IPs, or malicious package patterns / 建议新增威胁域名、C2 IP 或风险应用包名"
title: "[RULESET] "
labels: "ruleset"
assignees: "ctkqiang"
---

## Threat Category / 威胁类型

- [ ] C2 Server / Botnet Egress (C2 服务器 / 僵尸网络)
- [ ] Commercial Spyware / Stalking Malware (商业间谍软件 / 偷窥木马)
- [ ] Banking Overlay Trojan (银行钓鱼劫持木马)
- [ ] Cryptojacking Mining Pool (未经授权的挖矿脚本)
- [ ] Phishing / Credential Harvester (钓鱼网站 / 凭据窃取)

## Target Identifiers / 目标标识符

```yaml
target_domains:
  - "example-malicious-domain.com"
target_ips:
  - "192.0.2.1"
package_patterns:
  - ".*malware\\.package.*"
```

## Threat Evidence / 威胁证据与说明

[EN] Provide threat intelligence links or technical evidence for this ruleset.
[ZH] 请提供该威胁规则相关的安全分析报告或技术证据说明。
