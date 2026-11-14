package xin.ctkqiang.nezhaedr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xin.ctkqiang.nezhaedr.data.parser.YamlRuleParser
import xin.ctkqiang.nezhaedr.data.repository.RuleRepositoryImpl
import xin.ctkqiang.nezhaedr.engine.EDREvaluationEngine
import xin.ctkqiang.nezhaedr.service.telemetry.TelemetryEvent

class EDREvaluationEngineTest {

    @Test
    fun testRuleParsingAndEvaluation() {
        val repository = RuleRepositoryImpl()
        val rulePackage = repository.getCachedRules()
        assertNotNull(rulePackage)

        val engine = EDREvaluationEngine()
        engine.updateRules(rulePackage!!)

        val netEvent = TelemetryEvent.Network(
            destinationIp = "192.0.2.1",
            destinationPort = 80,
            protocol = "IPv4"
        )
        val matchResult = engine.evaluateNetworkEvent(netEvent)
        assertNotNull(matchResult)
        assertEquals("c2_threat_block", matchResult?.rule?.canonicalId)

        val cleanNetEvent = TelemetryEvent.Network(
            destinationIp = "8.8.8.8",
            destinationPort = 443,
            protocol = "IPv4"
        )
        assertNull(engine.evaluateNetworkEvent(cleanNetEvent))

        val pkgEvent = TelemetryEvent.Package(
            packageName = "com.test.malware.app",
            action = "android.intent.action.PACKAGE_ADDED"
        )
        val pkgMatchResult = engine.evaluatePackageEvent(pkgEvent)
        assertNotNull(pkgMatchResult)
        assertEquals("rogue_package_alert", pkgMatchResult?.rule?.canonicalId)
    }

    @Test
    fun testAdultContentDomainBlocking() {
        val repository = RuleRepositoryImpl()
        val rulePackage = repository.getCachedRules()
        assertNotNull(rulePackage)

        val engine = EDREvaluationEngine()
        engine.updateRules(rulePackage!!)

        val topPornEvent = TelemetryEvent.Network(
            destinationIp = "toppornsites.com",
            destinationPort = 443,
            protocol = "HTTPS"
        )
        val matchTopPorn = engine.evaluateNetworkEvent(topPornEvent)
        assertNotNull(matchTopPorn)
        assertEquals("adult_content_block", matchTopPorn?.rule?.canonicalId)
        assertEquals("BLOCK", matchTopPorn?.rule?.action?.name)
    }

    @Test
    fun testBehavioralRuleEvaluation() {
        val yamlText = """
            schema_version: "2.0"
            rule_id: "RULE-SCAN-001"
            id: "reconnaissance_probe_block"
            name: "Reconnaissance Probe Block"
            description: "Blocks automated network scanners"
            severity: "HIGH"
            category: "NETWORK"
            enabled: true
            engine_type: "behavioral"
            target_source: "dns_query"
            condition:
              max_subdomain_depth: 3
            action: "BLOCK"
        """.trimIndent()

        val rule = YamlRuleParser.parseYamlRule(yamlText)
        assertNotNull(rule)

        val engine = EDREvaluationEngine()
        engine.updateRules(
            xin.ctkqiang.nezhaedr.data.model.DetectionRulePackage(
                version = "2.0",
                updatedAt = "2026-03-01T22:00:00Z",
                rules = listOf(rule!!)
            )
        )

        // Subdomain depth 4 (> 3) -> MATCH
        val deepSubdomainEvent = TelemetryEvent.Network(
            destinationIp = "1.2.3.4",
            domain = "a.b.c.d.example.com",
            destinationPort = 53,
            protocol = "DNS/UDP"
        )
        val matchResult = engine.evaluateNetworkEvent(deepSubdomainEvent)
        assertNotNull(matchResult)
        assertEquals("RULE-SCAN-001", matchResult?.rule?.canonicalId)

        // Subdomain depth 1 (<= 3) -> CLEAN
        val normalDomainEvent = TelemetryEvent.Network(
            destinationIp = "1.2.3.4",
            domain = "sub.example.com",
            destinationPort = 53,
            protocol = "DNS/UDP"
        )
        assertNull(engine.evaluateNetworkEvent(normalDomainEvent))
    }

    @Test
    fun testShannonEntropyCalculation() {
        // High entropy random string vs low entropy repeated string
        val highEntropyStr = "a8f9c7b2e1d0x4z8w9v2"
        val lowEntropyStr = "aaaaa"

        val highEntropy = EDREvaluationEngine.calculateShannonEntropy(highEntropyStr)
        val lowEntropy = EDREvaluationEngine.calculateShannonEntropy(lowEntropyStr)

        assertTrue(highEntropy > 3.5)
        assertEquals(0.0, lowEntropy, 0.001)
    }
}
