const i18n = {
  en: {
    title: "NezhaEDR Developer Docs",
    subtitle: "ZERO-TELEMETRY LOCAL DEFENSE ENGINE",
    heroTitle: "NezhaEDR Open-Source Developer & Ruleset Documentation",
    heroDesc: "A lightweight, high-performance Endpoint Detection & Response engine for Android. Built with Kotlin, Jetpack Compose, and Declarative YAML Rulesets.",
    tabOverview: "Architecture & Overview",
    tabSchema: "YAML Ruleset Spec",
    tabContribute: "How to Contribute",
    tabRules: "Conduct & Rules",
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
    adminDesc: "If you wish to join the NezhaEDR core administrative team, please contact johnmelodymel@qq.com or ping GitHub @ctkqiang."
  },
  zh: {
    title: "哪吒 EDR 开发者文档",
    subtitle: "零遥测本地安全防护引擎",
    heroTitle: "哪吒 EDR 开源开发者与规则库技术文档",
    heroDesc: "一款面向 Android 终端的轻量级、零遥测、高性能终端检测与响应（EDR）防护引擎。基于 Kotlin、Jetpack Compose 及声明式 YAML 规则库构建。",
    tabOverview: "概述与架构",
    tabSchema: "YAML 规则定义规范",
    tabContribute: "贡献指南",
    tabRules: "行为准则与规定",
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
    adminDesc: "如果您希望加入哪吒 EDR 核心管理团队，请发送邮件至 johnmelodymel@qq.com 或在 GitHub 上联系 @ctkqiang。"
  }
};

let currentLang = "en";

function setLanguage(lang) {
  currentLang = lang;
  document.querySelectorAll("[data-i18n]").forEach(el => {
    const key = el.getAttribute("data-i18n");
    if (i18n[lang][key]) {
      el.textContent = i18n[lang][key];
    }
  });
  document.getElementById("langBtn").textContent = i18n[lang].toggleLang;
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

document.addEventListener("DOMContentLoaded", () => {
  setLanguage("en");
});
