const i18n = {
  en: {
    title: "NezhaEDR Developer Docs",
    subtitle: "ZERO-TELEMETRY LOCAL DEFENSE ENGINE",
    heroTitle: "NezhaEDR Developer API Reference & Rule Flow Playground",
    heroDesc: "A comprehensive technical reference for creating, testing, and deploying declarative YAML rulesets for NezhaEDR Android.",
    tabOverview: "Architecture & Overview",
    tabSchema: "YAML API Reference",
    tabPlayground: "Interactive Node Playground",
    tabContribute: "How to Contribute",
    tabRules: "Conduct & Standards",
    viewGithub: "View on GitHub",
    toggleTheme: "Theme",
    toggleLang: "中文",
    archTitle: "NezhaEDR Security Architecture",
    archDesc: "NezhaEDR captures network telemetry and package installations on-device, evaluating events via an O(1) memory rule engine.",
    capTitle: "Captive Subnet Routing TUN VPN",
    capDesc: "Utilizes Android VpnService to establish a zero-log TUN interface. General traffic bypasses VPN completely for zero performance impact.",
    sensorTitle: "Package Installation Sensor",
    sensorDesc: "Monitors app sideloading via BroadcastReceiver and evaluates package names against compiled Regular Expression rulesets.",
    yamlTitle: "O(1) Memory Rule Engine",
    yamlDesc: "Uses dual-index memory HashSets for instant domain and IP lookups, evaluating network events in sub-millisecond speeds.",
    dbTitle: "Local SQLite Privacy Storage",
    dbDesc: "100% on-device SQLite database. Zero remote telemetry. Users maintain complete ownership to export or wipe data.",
    contribTitle: "How to Contribute",
    rulesetContrib: "Ruleset Contribution Workflow",
    codeContrib: "Code Contribution Workflow",
    stepFork: "Fork the NezhaEDRAndroid repository on GitHub.",
    stepBranch: "Create a branch off 'dev' matching naming conventions: feature/*, bugfix/*, chore/*, or ruleset/*.",
    stepEdit: "Add or edit YAML detection rules in the /rules directory.",
    stepTest: "Run local unit tests to verify rule syntax and test cases: ./gradlew app:testDebugUnitTest",
    stepPR: "Submit a Pull Request targeting the 'dev' branch. CI/CD will validate compliance automatically.",
    conductTitle: "Code of Conduct & Project Principles",
    conductP1: "Zero Racism & Zero Politics Policy: NezhaEDR is strictly a technical security project created to block malicious actors.",
    conductP2: "Agent & AI Coding Policy: The use of AI coding assistants is permitted, but all code must be thoroughly inspected, tested, and verified before submitting PRs.",
    conductP3: "Contributor Recognition: Anyone whose ruleset PR passes CI compliance and gets merged is recognized as an official contributor.",
    adminTitle: "Project Admin Application",
    adminDesc: "If you wish to join the NezhaEDR core administrative team, please contact johnmelodymel@qq.com or ping GitHub @ctkqiang.",
    playgroundTitle: "NezhaEDR Rule Playground",
    btnRun: "Run",
    tmplC2: "C2 Threat Block (Signature)",
    tmplScam: "Scam Keyword Block (Conditional)",
    tmplTrojan: "Banking Trojan (Package)",
    tmplEntropy: "DGA Domain (Behavioral Entropy)",
    backToDocs: "← Back to Docs",
    paneEditor: "YAML RULE EDITOR (v2.0 Schema)",
    paneConsole: "EVALUATION CONSOLE LOG",
    paneVisualizer: "INTERACTIVE NODE EXECUTION GRAPH"
  },
  zh: {
    title: "哪吒 EDR 开发者文档",
    subtitle: "零遥测本地安全防护引擎",
    heroTitle: "哪吒 EDR 开发者 API 语法参考与节点流 Playground",
    heroDesc: "哪吒 EDR Android 声明式 YAML 规则库编写、节点流仿真与部署的完整技术文档。",
    tabOverview: "概述与架构",
    tabSchema: "YAML API 语法字典",
    tabPlayground: "节点流仿真 Playground",
    tabContribute: "贡献指南",
    tabRules: "准则与规定",
    viewGithub: "访问 GitHub 仓库",
    toggleTheme: "主题切换",
    toggleLang: "English",
    archTitle: "哪吒 EDR 终端安全防护架构",
    archDesc: "哪吒 EDR 在端侧实时捕获网络与应用安装遥测，通过 O(1) 内存规则引擎完成极速匹配评估。",
    capTitle: "囚笼子网捕获 (Captive Subnet Routing)",
    capDesc: "基于 Android VpnService 建立零日志 TUN 虚拟接口，仅路由已知威胁 IP/域名，常规流量完全绕过，实现 0% 性能与续航损耗。",
    sensorTitle: "应用安装感知 (Package Sensor)",
    sensorDesc: "通过 BroadcastReceiver 实时监听 sideload 应用安装事件，并与预编译正则表达式规则库进行正则匹配。",
    yamlTitle: "O(1) 高性能 YAML 评估引擎",
    yamlDesc: "采用内存双索引 Hash 表实现 IP 和域名的毫秒级检索匹配，瞬间阻断风险连接。",
    dbTitle: "SQLite 本地 0 遥测隐私保证",
    dbDesc: "100% 端侧 SQLite 存储，绝不向任何远程服务器上传任何日志或隐私数据，用户随时可全量导出或清空。",
    contribTitle: "如何参与项目贡献",
    rulesetContrib: "规则库贡献流程",
    codeContrib: "代码贡献流程",
    stepFork: "在 GitHub 上 Fork 哪吒 EDR NezhaEDRAndroid 代码仓库。",
    stepBranch: "从 'dev' 分支切出新分支，规范命名: feature/*, bugfix/*, chore/*, 或 ruleset/*。",
    stepEdit: "在 /rules 目录下新增或修改 YAML 格式检测规则文件。",
    stepTest: "运行本地单元测试验证规则语法: ./gradlew app:testDebugUnitTest",
    stepPR: "向 'dev' 分支提交 Pull Request，CI/CD 将自动执行规范与恶意代码检测。",
    conductTitle: "项目行为准则与核心原则",
    conductP1: "零种族主义与零政治原则：哪吒 EDR 纯粹是技术驱动的网络安全项目，旨在阻断黑客与恶意攻击行为。",
    conductP2: "AI / Coding Agent 规范：允许使用 AI 编程助手协助代码编写，但提交 PR 前必须经过人工代码审查与单元测试验证。",
    conductP3: "贡献者认证：任何通过 CI 自动检测并合入规则库 PR 的开发者均自动获得官方 Contributor 认证！",
    adminTitle: "申请成为项目管理员",
    adminDesc: "如果您希望加入哪吒 EDR 核心管理团队，请发送邮件至 johnmelodymel@qq.com 或在 GitHub 上联系 @ctkqiang。",
    playgroundTitle: "哪吒 EDR 规则 Playground 仿真器",
    btnRun: "运行规则",
    tmplC2: "C2 威胁阻断 (Signature 引擎)",
    tmplScam: "诈骗/赌博关键词阻断 (Conditional 引擎)",
    tmplTrojan: "银行劫持木马 (Package 引擎)",
    tmplEntropy: "高熵 DGA 域名 (Behavioral 熵引擎)",
    backToDocs: "← 返回主文档",
    paneEditor: "YAML 规则代码编辑器 (v2.0 Schema)",
    paneConsole: "规则评估控制台输出",
    paneVisualizer: "交互式节点流执行图"
  }
};

let currentLang = "en";

function initAutoThemeAndLocale() {
  const userLang = navigator.language || navigator.userLanguage || "en";
  if (userLang.startsWith("zh")) {
    setLanguage("zh");
  } else {
    setLanguage("en");
  }

  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    document.documentElement.setAttribute("data-theme", "dark");
  } else {
    document.documentElement.setAttribute("data-theme", "light");
  }
}

function setLanguage(lang) {
  currentLang = lang;
  document.querySelectorAll("[data-i18n]").forEach(el => {
    const key = el.getAttribute("data-i18n");
    if (i18n[lang] && i18n[lang][key]) {
      el.textContent = i18n[lang][key];
    }
  });
  const langBtn = document.getElementById("langBtn");
  if (langBtn) langBtn.textContent = i18n[lang].toggleLang;
  document.documentElement.lang = lang === "zh" ? "zh-CN" : "en";
}

function toggleTheme() {
  const currentTheme = document.documentElement.getAttribute("data-theme") || "light";
  const newTheme = currentTheme === "light" ? "dark" : "light";
  document.documentElement.setAttribute("data-theme", newTheme);
}

function switchTab(tabId) {
  document.querySelectorAll(".tab-btn").forEach(btn => {
    btn.classList.toggle("active", btn.getAttribute("onclick").includes(tabId));
  });
  document.querySelectorAll(".tab-content").forEach(content => {
    content.classList.toggle("active", content.id === tabId);
  });
}

const yamlTemplates = {
  c2: `schema_version: "2.0"
rule_id: "RULE-C2-001"
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
action: "BLOCK"`,

  scam: `schema_version: "2.0"
rule_id: "RULE-SCAM-001"
name: "Scam & Gambling Keyword URL Block"
description: "Detects and blocks URLs containing high-risk keywords (888, vip, free, claim)"
severity: "HIGH"
category: "NETWORK"
enabled: true
engine_type: "conditional"
target_source: "url_request"
condition:
  target_domains:
    - "888casino-vip.com"
    - "free-bonus-claim.net"
  expression: "contains(domain, \\"888\\") || contains(domain, \\"vip\\") || contains(domain, \\"free\\") || contains(domain, \\"claim\\")"
action: "BLOCK"`,

  trojan: `schema_version: "2.0"
rule_id: "RULE-PKG-003"
name: "Banking Trojan Sideload Detection"
description: "Detects malware overlay apps targeting financial tokens"
severity: "CRITICAL"
category: "PACKAGE"
enabled: true
engine_type: "signature"
target_source: "http_payload"
condition:
  package_patterns:
    - ".*trojan\\\\.banker.*"
    - ".*overlay\\\\.stealer.*"
action: "TERMINATE"`,

  entropy: `schema_version: "2.0"
rule_id: "RULE-SCAN-001"
name: "High Entropy DGA Domain Block"
description: "Blocks algorithmic domain generation probes"
severity: "HIGH"
category: "NETWORK"
enabled: true
engine_type: "behavioral"
target_source: "dns_query"
condition:
  max_subdomain_depth: 3
  max_entropy: 4.2
action: "BLOCK"`
};

function loadTemplate(type) {
  const editor = document.getElementById("yamlEditor");
  if (editor && yamlTemplates[type]) {
    editor.value = yamlTemplates[type];
    updateNodeVisualizer();
  }
}

function updateNodeVisualizer() {
  const text = document.getElementById("yamlEditor")?.value || "";

  let ruleId = "RULE-CUSTOM-001";
  let engineType = "signature";
  let targetSource = "dns_query";
  let action = "BLOCK";

  text.split("\n").forEach(line => {
    const trimmed = line.trim();
    if (trimmed.startsWith("rule_id:")) ruleId = trimmed.split(":")[1].trim().replace(/"/g, '');
    if (trimmed.startsWith("engine_type:")) engineType = trimmed.split(":")[1].trim().replace(/"/g, '');
    if (trimmed.startsWith("target_source:")) targetSource = trimmed.split(":")[1].trim().replace(/"/g, '');
    if (trimmed.startsWith("action:")) action = trimmed.split(":")[1].trim().replace(/"/g, '');
  });

  const nodeSource = document.getElementById("nodeSource");
  const nodeEngine = document.getElementById("nodeEngine");
  const nodeAction = document.getElementById("nodeAction");

  if (nodeSource) nodeSource.textContent = `Source: ${targetSource.toUpperCase()}`;
  if (nodeEngine) nodeEngine.textContent = `Engine: ${engineType.toUpperCase()} (ID: ${ruleId})`;
  if (nodeAction) nodeAction.textContent = `Action: ${action.toUpperCase()} DISPATCHED`;
}

function runSimulation() {
  const consoleBox = document.getElementById("consoleOutput");
  const text = document.getElementById("yamlEditor")?.value || "";

  if (!consoleBox) return;

  consoleBox.innerHTML = `[${new Date().toLocaleTimeString()}] [NezhaEDR Engine] Parsing YAML rule snippet...\n`;

  let ruleId = "RULE-CUSTOM";
  let name = "Custom Rule";
  let engine = "signature";
  let action = "BLOCK";

  text.split("\n").forEach(line => {
    const trimmed = line.trim();
    if (trimmed.startsWith("rule_id:")) ruleId = trimmed.split(":")[1].trim().replace(/"/g, '');
    if (trimmed.startsWith("name:")) name = trimmed.split(":")[1].trim().replace(/"/g, '');
    if (trimmed.startsWith("engine_type:")) engine = trimmed.split(":")[1].trim().replace(/"/g, '');
    if (trimmed.startsWith("action:")) action = trimmed.split(":")[1].trim().replace(/"/g, '');
  });

  setTimeout(() => {
    consoleBox.innerHTML += `[${new Date().toLocaleTimeString()}] [NezhaEDR Engine] Schema v2.0 validation: PASSED\n`;
    consoleBox.innerHTML += `[${new Date().toLocaleTimeString()}] [NezhaEDR Engine] Compiled engine strategy: ${engine.toUpperCase()}\n`;
    consoleBox.innerHTML += `[${new Date().toLocaleTimeString()}] [NezhaEDR Engine] Simulating event evaluation against '${ruleId}' (${name})...\n`;
    consoleBox.innerHTML += `[${new Date().toLocaleTimeString()}] [NezhaEDR Engine] ⚠️ THREAT MATCH DETECTED! Executing containment action: ${action.toUpperCase()}\n`;
    consoleBox.innerHTML += `[${new Date().toLocaleTimeString()}] [NezhaEDR Engine] Evaluation complete. Status: MATCH FOUND.`;
    consoleBox.scrollTop = consoleBox.scrollHeight;
  }, 300);
}

document.addEventListener("DOMContentLoaded", () => {
  initAutoThemeAndLocale();
  updateNodeVisualizer();
});
