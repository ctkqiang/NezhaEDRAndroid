package xin.ctkqiang.nezhaedr.data.repository

import kotlinx.serialization.json.Json
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.DetectionRulePackage
import xin.ctkqiang.nezhaedr.data.parser.YamlRuleParser
import xin.ctkqiang.nezhaedr.util.EDRLogger
import java.io.File

interface RuleRepository {
    suspend fun fetchRules(): Result<DetectionRulePackage>
    fun getCachedRules(): DetectionRulePackage?
    fun saveRulesToCache(rulePackage: DetectionRulePackage)
    fun parseRulesJson(jsonString: String): DetectionRulePackage
}

class RuleRepositoryImpl(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
) : RuleRepository {

    private var cachedRules: DetectionRulePackage? = null

    override suspend fun fetchRules(): Result<DetectionRulePackage> {
        EDRLogger.i(TAG, "fetchRules: Initiating detection rules load process")
        return try {
            val rootDir = File("/Users/johnmelodyme/Documents/NezhaEDRAndroid")
            val rulesDir = File(rootDir, "rules")

            val yamlFiles = mutableListOf<File>()
            if (rulesDir.exists() && rulesDir.isDirectory) {
                EDRLogger.d(TAG, "Scanning rules directory: ${rulesDir.absolutePath}")
                rulesDir.listFiles { _, name -> name.endsWith(".yaml", ignoreCase = true) }?.let {
                    yamlFiles.addAll(it)
                }
            }
            if (rootDir.exists() && rootDir.isDirectory) {
                rootDir.listFiles { _, name -> name.endsWith(".yaml", ignoreCase = true) }?.let {
                    yamlFiles.addAll(it)
                }
            }

            EDRLogger.d(TAG, "Found ${yamlFiles.size} candidate .yaml rule files")

            val parsedYamlRules = mutableListOf<DetectionRule>()
            for (file in yamlFiles) {
                try {
                    EDRLogger.v(TAG, "Reading YAML file: ${file.name}")
                    val rule = YamlRuleParser.parseYamlRule(file.readText())
                    if (rule != null) {
                        parsedYamlRules.add(rule)
                    }
                } catch (e: Exception) {
                    EDRLogger.e(TAG, "Error parsing YAML file ${file.name}", e)
                }
            }

            val rulePackage = if (parsedYamlRules.isNotEmpty()) {
                EDRLogger.i(TAG, "Successfully loaded ${parsedYamlRules.size} rules from YAML files")
                DetectionRulePackage(
                    version = "1.4.0-yaml",
                    updatedAt = "2026-03-01T16:00:00Z",
                    rules = parsedYamlRules.distinctBy { it.id }
                )
            } else {
                EDRLogger.w(TAG, "No YAML rules found. Checking fallback rules.json")
                val jsonFile = File(rootDir, "rules.json")
                if (jsonFile.exists() && jsonFile.canRead()) {
                    EDRLogger.d(TAG, "Parsing rules from ${jsonFile.absolutePath}")
                    parseRulesJson(jsonFile.readText())
                } else {
                    EDRLogger.w(TAG, "rules.json not found or unreadable; using embedded fallback ruleset")
                    getEmbeddedDefaultRules()
                }
            }

            cachedRules = rulePackage
            Result.success(rulePackage)
        } catch (e: Exception) {
            EDRLogger.e(TAG, "Failed to fetch rules package, reverting to embedded default ruleset", e)
            val fallback = getEmbeddedDefaultRules()
            cachedRules = fallback
            Result.success(fallback)
        }
    }

    override fun getCachedRules(): DetectionRulePackage? {
        val rules = cachedRules ?: getEmbeddedDefaultRules()
        EDRLogger.d(TAG, "getCachedRules: returning package version ${rules.version} with ${rules.rules.size} rules")
        return rules
    }

    override fun saveRulesToCache(rulePackage: DetectionRulePackage) {
        EDRLogger.d(TAG, "saveRulesToCache: caching rule package version ${rulePackage.version}")
        cachedRules = rulePackage
    }

    override fun parseRulesJson(jsonString: String): DetectionRulePackage {
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            EDRLogger.e(TAG, "parseRulesJson: JSON parsing error", e)
            getEmbeddedDefaultRules()
        }
    }

    private fun getEmbeddedDefaultRules(): DetectionRulePackage {
        EDRLogger.i(TAG, "getEmbeddedDefaultRules: Constructing embedded emergency ruleset")
        val defaultJson = """
        {
          "version": "1.3.0",
          "updated_at": "2026-03-01T15:00:00Z",
          "rules": [
            {
              "id": "RULE-NET-001",
              "name": "Known C2 Threat IP Block",
              "description": "Detects and blocks egress connection to known malicious C2 addresses",
              "severity": "HIGH",
              "category": "NETWORK",
              "condition": {
                "target_ips": ["192.0.2.1", "198.51.100.14"],
                "target_domains": ["malicious-c2.example.com"]
              },
              "action": "BLOCK"
            },
            {
              "id": "RULE-NET-002",
              "name": "Ransomware Beaconing Domain Block",
              "description": "Prevents outbound botnet beaconing to ransomware command centers",
              "severity": "CRITICAL",
              "category": "NETWORK",
              "condition": {
                "target_ips": ["203.0.113.55"],
                "target_domains": ["beacon.darkransom.net", "botnet.cc-server.io"]
              },
              "action": "BLOCK"
            },
            {
              "id": "RULE-NET-003",
              "name": "Adult & Explicit Content Domain Block",
              "description": "Blocks outbound connections to flagged adult and explicit web destinations",
              "severity": "CRITICAL",
              "category": "NETWORK",
              "condition": {
                "target_domains": [
                  "toppornsites.com",
                  "91porna.com",
                  "pornhub.com",
                  "xvideos.com",
                  "xnxx.com",
                  "xhamster.com",
                  "redtube.com",
                  "youporn.com",
                  "spankbang.com",
                  "eporner.com",
                  "brazzers.com",
                  "stripchat.com",
                  "chaturbate.com",
                  "beeg.com",
                  "4tube.com",
                  "tnaflix.com",
                  "jav.guru",
                  "rat.xxx",
                  "youjizz.com",
                  "redgifs.com"
                ]
              },
              "action": "BLOCK"
            },
            {
              "id": "RULE-PKG-001",
              "name": "Suspicious Rogue Package Alert",
              "description": "Detects rogue software installers attempting sideload persistence",
              "severity": "CRITICAL",
              "category": "PACKAGE",
              "condition": {
                "package_patterns": [".*test\\.malware.*", ".*rogue\\.installer.*"]
              },
              "action": "NOTIFY"
            },
            {
              "id": "RULE-PKG-002",
              "name": "Spyware & Keylogger Package Detection",
              "description": "Flags unauthorized background keyloggers and telemetry stealers",
              "severity": "HIGH",
              "category": "PACKAGE",
              "condition": {
                "package_patterns": [".*stealer\\.keylog.*", ".*hidden\\.spyware.*"]
              },
              "action": "NOTIFY"
            },
            {
              "id": "RULE-PROC-001",
              "name": "Unauthorized Backdoor Service Detection",
              "description": "Monitors rogue processes attempting unauthorized background execution",
              "severity": "MEDIUM",
              "category": "PROCESS",
              "condition": {
                "package_patterns": [".*backdoor\\.service.*"]
              },
              "action": "LOG"
            }
          ]
        }
        """.trimIndent()
        return parseRulesJson(defaultJson)
    }

    companion object {
        private const val TAG = "RuleRepository"
    }
}
