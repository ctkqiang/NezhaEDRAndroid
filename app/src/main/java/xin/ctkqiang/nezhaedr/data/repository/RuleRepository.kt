package xin.ctkqiang.nezhaedr.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.DetectionRulePackage
import xin.ctkqiang.nezhaedr.data.parser.YamlRuleParser
import xin.ctkqiang.nezhaedr.util.EDRLogger
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class RemoteRuleCatalogItem(
    val fileName: String,
    val ruleId: String,
    val name: String,
    val category: String,
    val severity: String
)

interface RuleRepository {
    suspend fun fetchRules(): Result<DetectionRulePackage>
    suspend fun syncRulesFromGithub(): Result<DetectionRulePackage>
    suspend fun syncSelectedRulesFromGithub(selectedFiles: List<String>): Result<DetectionRulePackage>
    suspend fun fetchRemoteGithubCatalog(): List<RemoteRuleCatalogItem>
    suspend fun deleteRule(ruleId: String): Result<DetectionRulePackage>
    suspend fun importCustomYamlRule(yamlText: String, fileName: String): Result<DetectionRulePackage>
    fun getFallbackRemoteRuleCatalog(): List<RemoteRuleCatalogItem>
    fun getCachedRules(): DetectionRulePackage?
    fun saveRulesToCache(rulePackage: DetectionRulePackage)
    fun parseRulesJson(jsonString: String): DetectionRulePackage
}

class RuleRepositoryImpl(
    private val context: Context? = null,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
) : RuleRepository {

    private var cachedRules: DetectionRulePackage? = null

    private val knownRepositoryRuleFiles = listOf(
        "adware_click_farm_block.yaml",
        "adult_content_block.yaml",
        "backdoor_service_detection.yaml",
        "banking_trojan_alert.yaml",
        "c2_threat_block.yaml",
        "cryptojacking_pool_block.yaml",
        "pegasus_spyware_block.yaml",
        "phishing_credential_block.yaml",
        "ransomware_beacon_block.yaml",
        "reconnaissance_probe_block.yaml",
        "rogue_package_alert.yaml",
        "scam_gambling_url_block.yaml",
        "spyware_keylogger_detection.yaml"
    )

    override fun getFallbackRemoteRuleCatalog(): List<RemoteRuleCatalogItem> {
        return listOf(
            RemoteRuleCatalogItem("c2_threat_block.yaml", "c2_threat_block", "Known C2 Threat IP Block", "NETWORK", "HIGH"),
            RemoteRuleCatalogItem("ransomware_beacon_block.yaml", "ransomware_beacon_block", "Ransomware Beaconing Domain Block", "NETWORK", "CRITICAL"),
            RemoteRuleCatalogItem("adult_content_block.yaml", "adult_content_block", "Adult & Explicit Content Domain Block", "NETWORK", "CRITICAL"),
            RemoteRuleCatalogItem("pegasus_spyware_block.yaml", "pegasus_spyware_block", "Pegasus & Advanced Cyber Espionage Block", "NETWORK", "CRITICAL"),
            RemoteRuleCatalogItem("cryptojacking_pool_block.yaml", "cryptojacking_pool_block", "Cryptojacking Mining Pool Network Block", "NETWORK", "HIGH"),
            RemoteRuleCatalogItem("phishing_credential_block.yaml", "phishing_credential_block", "Phishing & Credential Harvester Block", "NETWORK", "HIGH"),
            RemoteRuleCatalogItem("adware_click_farm_block.yaml", "adware_click_farm_block", "Fraudulent Adware & Click Farm Block", "NETWORK", "MEDIUM"),
            RemoteRuleCatalogItem("reconnaissance_probe_block.yaml", "reconnaissance_probe_block", "Reconnaissance & Network Probe Block", "NETWORK", "HIGH"),
            RemoteRuleCatalogItem("scam_gambling_url_block.yaml", "scam_gambling_url_block", "Scam & Gambling Keyword URL Block", "NETWORK", "HIGH"),
            RemoteRuleCatalogItem("rogue_package_alert.yaml", "rogue_package_alert", "Suspicious Rogue Package Alert", "PACKAGE", "CRITICAL"),
            RemoteRuleCatalogItem("spyware_keylogger_detection.yaml", "spyware_keylogger_detection", "Spyware & Keylogger Package Detection", "PACKAGE", "HIGH"),
            RemoteRuleCatalogItem("banking_trojan_alert.yaml", "banking_trojan_alert", "Banking Trojan & Overlay Sideload Detection", "PACKAGE", "CRITICAL"),
            RemoteRuleCatalogItem("backdoor_service_detection.yaml", "backdoor_service_detection", "Unauthorized Backdoor Service Detection", "PROCESS", "MEDIUM")
        )
    }

    override suspend fun fetchRemoteGithubCatalog(): List<RemoteRuleCatalogItem> = withContext(Dispatchers.IO) {
        EDRLogger.i(TAG, "fetchRemoteGithubCatalog: Querying live single source of truth at $GITHUB_RULES_TREE_URL")
        val catalogItems = mutableListOf<RemoteRuleCatalogItem>()

        try {
            val url = URL(GITHUB_CONTENTS_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "NezhaCyberEDR-Android")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonResponse = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = Json.parseToJsonElement(jsonResponse).jsonArray
                for (element in jsonArray) {
                    val name = element.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                    val downloadUrl = element.jsonObject["download_url"]?.jsonPrimitive?.content ?: "$GITHUB_RULES_BASE_URL/$name"
                    if (name.endsWith(".yaml", ignoreCase = true) || name.endsWith(".yml", ignoreCase = true)) {
                        val yamlContent = fetchRawContent(downloadUrl)
                        if (yamlContent != null) {
                            val parsedRule = YamlRuleParser.parseYamlRule(yamlContent)
                            if (parsedRule != null) {
                                catalogItems.add(
                                    RemoteRuleCatalogItem(
                                        fileName = name,
                                        ruleId = parsedRule.id,
                                        name = parsedRule.name,
                                        category = parsedRule.category,
                                        severity = parsedRule.severity
                                    )
                                )
                            }
                        }
                    }
                }
                EDRLogger.i(TAG, "fetchRemoteGithubCatalog: Discovered ${catalogItems.size} live rulesets via GitHub API")
            } else {
                EDRLogger.w(TAG, "fetchRemoteGithubCatalog: GitHub API returned HTTP ${connection.responseCode}")
            }
            connection.disconnect()
        } catch (e: Exception) {
            EDRLogger.w(TAG, "fetchRemoteGithubCatalog: GitHub API query failed: ${e.message}")
        }

        if (catalogItems.isEmpty()) {
            EDRLogger.i(TAG, "fetchRemoteGithubCatalog: Querying GitHub raw CDN directly for repository rulesets")
            for (fileName in knownRepositoryRuleFiles) {
                try {
                    val rawUrl = "$GITHUB_RULES_BASE_URL/$fileName"
                    val yamlContent = fetchRawContent(rawUrl)
                    if (yamlContent != null) {
                        val parsed = YamlRuleParser.parseYamlRule(yamlContent)
                        if (parsed != null) {
                            catalogItems.add(
                                RemoteRuleCatalogItem(
                                    fileName = fileName,
                                    ruleId = parsed.id,
                                    name = parsed.name,
                                    category = parsed.category,
                                    severity = parsed.severity
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    EDRLogger.v(TAG, "Failed direct CDN fetch for $fileName: ${e.message}")
                }
            }
        }

        if (catalogItems.isEmpty()) {
            EDRLogger.w(TAG, "fetchRemoteGithubCatalog: Returning fallback catalog items")
            return@withContext getFallbackRemoteRuleCatalog()
        }
        return@withContext catalogItems.distinctBy { it.fileName }
    }

    private fun fetchRawContent(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "NezhaCyberEDR-Android")
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun fetchRules(): Result<DetectionRulePackage> {
        EDRLogger.i(TAG, "fetchRules: Initiating detection rules load process")
        return loadLocalYamlRules()
    }

    override suspend fun syncRulesFromGithub(): Result<DetectionRulePackage> {
        val catalog = fetchRemoteGithubCatalog().map { it.fileName }
        return syncSelectedRulesFromGithub(catalog)
    }

    override suspend fun syncSelectedRulesFromGithub(selectedFiles: List<String>): Result<DetectionRulePackage> = withContext(Dispatchers.IO) {
        EDRLogger.i(TAG, "syncSelectedRulesFromGithub: Downloading ${selectedFiles.size} selected rulesets from GitHub single source of truth CDN ($GITHUB_RULES_BASE_URL)")
        val localRulesDir = getLocalRulesDirectory()

        var downloadedCount = 0
        for (fileName in selectedFiles) {
            try {
                val urlString = "$GITHUB_RULES_BASE_URL/$fileName"
                EDRLogger.d(TAG, "Downloading live rule from single source of truth URL: $urlString")
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "NezhaCyberEDR-Android")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val yamlText = connection.inputStream.bufferedReader().use { it.readText() }
                    val destFile = File(localRulesDir, fileName)
                    destFile.writeText(yamlText)
                    downloadedCount++
                    EDRLogger.i(TAG, "Successfully downloaded $fileName from GitHub and updated local storage")
                } else {
                    EDRLogger.w(TAG, "HTTP ${connection.responseCode} while fetching $fileName from GitHub")
                }
                connection.disconnect()
            } catch (e: Exception) {
                EDRLogger.w(TAG, "Network exception syncing $fileName from GitHub single source of truth: ${e.message}")
            }
        }

        EDRLogger.i(TAG, "syncSelectedRulesFromGithub: Successfully updated $downloadedCount live rules from GitHub single source of truth")
        return@withContext loadLocalYamlRules()
    }

    override suspend fun deleteRule(ruleId: String): Result<DetectionRulePackage> = withContext(Dispatchers.IO) {
        EDRLogger.w(TAG, "deleteRule: Deleting local YAML rule '$ruleId'")
        val candidateDirs = mutableListOf<File>()
        context?.filesDir?.let {
            candidateDirs.add(File(it, "rules"))
            candidateDirs.add(File(it, "custom_rules"))
        }

        var deletedFileCount = 0
        for (dir in candidateDirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { _, name -> name.endsWith(".yaml", ignoreCase = true) || name.endsWith(".yml", ignoreCase = true) }?.forEach { file ->
                    try {
                        val parsed = YamlRuleParser.parseYamlRule(file.readText())
                        if (parsed?.id == ruleId) {
                            if (file.delete()) {
                                deletedFileCount++
                                EDRLogger.i(TAG, "Deleted rule file: ${file.absolutePath}")
                            }
                        }
                    } catch (e: Exception) {
                        EDRLogger.v(TAG, "Error checking file ${file.name} for deletion: ${e.message}")
                    }
                }
            }
        }
        EDRLogger.i(TAG, "deleteRule: Removed $deletedFileCount rule files for '$ruleId'")
        return@withContext loadLocalYamlRules()
    }

    override suspend fun importCustomYamlRule(yamlText: String, fileName: String): Result<DetectionRulePackage> = withContext(Dispatchers.IO) {
        EDRLogger.i(TAG, "importCustomYamlRule: Importing custom YAML rule '$fileName'")
        return@withContext try {
            val parsedRule = YamlRuleParser.parseYamlRule(yamlText)
            if (parsedRule == null) {
                EDRLogger.e(TAG, "Failed to parse custom YAML rule: invalid schema")
                Result.failure(IllegalArgumentException("Invalid YAML rule format"))
            } else {
                val customDir = getCustomRulesDirectory()
                val destFile = File(customDir, fileName.ifEmpty { "custom_${parsedRule.id}.yaml" })
                destFile.writeText(yamlText)
                EDRLogger.i(TAG, "Saved custom rule ${parsedRule.id} to ${destFile.absolutePath}")
                loadLocalYamlRules()
            }
        } catch (e: Exception) {
            EDRLogger.e(TAG, "Exception importing custom YAML rule", e)
            Result.failure(e)
        }
    }

    private fun loadLocalYamlRules(): Result<DetectionRulePackage> {
        return try {
            val candidateDirs = mutableListOf<File>()

            context?.filesDir?.let {
                candidateDirs.add(File(it, "rules"))
                candidateDirs.add(File(it, "custom_rules"))
            }

            val yamlFiles = mutableListOf<File>()
            for (dir in candidateDirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles { _, name -> name.endsWith(".yaml", ignoreCase = true) || name.endsWith(".yml", ignoreCase = true) }?.let {
                        yamlFiles.addAll(it)
                    }
                }
            }

            EDRLogger.d(TAG, "loadLocalYamlRules: Found ${yamlFiles.size} candidate .yaml files in internal app storage")

            val parsedYamlRules = mutableListOf<DetectionRule>()
            for (file in yamlFiles.distinctBy { it.absolutePath }) {
                try {
                    val rule = YamlRuleParser.parseYamlRule(file.readText())
                    if (rule != null) {
                        parsedYamlRules.add(rule)
                    }
                } catch (e: Exception) {
                    EDRLogger.e(TAG, "Error parsing YAML file ${file.name}", e)
                }
            }

            val rulePackage = if (parsedYamlRules.isNotEmpty()) {
                val distinctRules = parsedYamlRules.distinctBy { it.id }
                EDRLogger.i(TAG, "Successfully loaded ${distinctRules.size} distinct YAML rulesets")
                DetectionRulePackage(
                    version = "2.1.0-gitops",
                    updatedAt = "2026-03-01T21:00:00Z",
                    rules = distinctRules
                )
            } else {
                EDRLogger.w(TAG, "No local YAML rules found; falling back to embedded default rules")
                getEmbeddedDefaultRules()
            }

            cachedRules = rulePackage
            Result.success(rulePackage)
        } catch (e: Exception) {
            EDRLogger.e(TAG, "Exception loading local YAML rules, falling back to embedded default", e)
            val fallback = getEmbeddedDefaultRules()
            cachedRules = fallback
            Result.success(fallback)
        }
    }

    private fun getLocalRulesDirectory(): File {
        val dir = if (context != null) File(context.filesDir, "rules") else File("/tmp/nezha_rules")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getCustomRulesDirectory(): File {
        val dir = if (context != null) File(context.filesDir, "custom_rules") else File("/tmp/nezha_custom_rules")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun getCachedRules(): DetectionRulePackage? {
        val rules = cachedRules ?: getEmbeddedDefaultRules()
        return rules
    }

    override fun saveRulesToCache(rulePackage: DetectionRulePackage) {
        cachedRules = rulePackage
    }

    override fun parseRulesJson(jsonString: String): DetectionRulePackage {
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            getEmbeddedDefaultRules()
        }
    }

    private fun getEmbeddedDefaultRules(): DetectionRulePackage {
        val defaultJson = """
        {
          "version": "1.3.0",
          "updated_at": "2026-03-01T15:00:00Z",
          "rules": [
            {
              "id": "c2_threat_block",
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
              "id": "ransomware_beacon_block",
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
              "id": "adult_content_block",
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
              "id": "rogue_package_alert",
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
              "id": "spyware_keylogger_detection",
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
              "id": "backdoor_service_detection",
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
        private const val GITHUB_RULES_TREE_URL = "https://github.com/ctkqiang/NezhaEDRAndroid/tree/main/rules"
        private const val GITHUB_CONTENTS_API_URL = "https://api.github.com/repos/ctkqiang/NezhaEDRAndroid/contents/rules"
        private const val GITHUB_RULES_BASE_URL = "https://raw.githubusercontent.com/ctkqiang/NezhaEDRAndroid/main/rules"
    }
}
