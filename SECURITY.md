# NezhaEDR Security Policy & Code of Conduct

## Vulnerability Reporting Procedure / 漏洞响应机制

If you discover a security vulnerability or a critical bypass inside **NezhaEDR (NezhaCyberEDR Android)**, please do not open a public GitHub issue. Instead, report it directly to the maintainer:

- **Primary Security Contact**: `johnmelodymel@qq.com`
- **GitHub Maintainer**: `@ctkqiang`

We respond to security reports within **24 hours** and issue patch updates promptly.

---

## Rules & Regulations for Contributors / 贡献者行为准则与规定

### 1. Pure Technical Focus (No Racism & No Politics)
NezhaEDR is an open-source cybersecurity project created strictly to protect Android endpoints from malicious bad actors, spyware, and C2 threats. 
- **Zero Racism & Zero Politics**: Racism, hate speech, and political agendas are strictly prohibited.
- Submissions focusing on cybersecurity defense, threat containment, and ruleset quality are welcomed regardless of the contributor's background.

### 2. AI / Coding Agent Policy
The usage of AI coding assistants and AI Agents (e.g. Android Studio Agent, Cursor, Claude) is explicitly permitted in this project.
- **Mandatory Verification**: Contributors using AI agents **MUST** inspect, test, and verify all generated code locally.
- **Unit Test Requirement**: All PRs containing code logic changes must pass `./gradlew app:testDebugUnitTest` prior to submission.
- Unverified, buggy, or untested AI-generated code ("slop") will be rejected during CI inspection.

### 3. Contributor Recognition
Anyone who contributes a valid, compliant YAML security ruleset or code patch that gets merged into the `dev` or `main` branch is recognized as an official project **Contributor**.

### 4. Joining the Core Administrative Team
If you are passionate about open-source mobile EDR security and wish to apply for project **Administrator** / Maintainer status, please reach out via email to:
`johnmelodymel@qq.com` or contact `@ctkqiang` on GitHub.
