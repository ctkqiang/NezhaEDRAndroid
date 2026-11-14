package xin.ctkqiang.nezhaedr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xin.ctkqiang.nezhaedr.data.model.ContainmentAction
import xin.ctkqiang.nezhaedr.data.parser.YamlRuleParser

class YamlRuleParserTest {

    @Test
    fun testParseV2SchemaNetworkBlockRule() {
        val yamlText = """
            schema_version: "2.0"
            rule_id: "RULE-SPY-001"
            id: "pegasus_spyware_block"
            name: "Pegasus Spyware Domain Block"
            description: "Blocks egress connections to known commercial spyware exfiltration domains"
            severity: "CRITICAL"
            category: "NETWORK"
            enabled: true
            engine_type: "signature"
            target_source: "dns_query"
            condition:
              target_domains:
                - "pegasus-exfil.nso-group.net"
                - "mercenary-spyware-c2.org"
              target_ips:
                - "198.51.100.99"
            action: "BLOCK"
        """.trimIndent()

        val rule = YamlRuleParser.parseYamlRule(yamlText)
        assertNotNull(rule)
        assertEquals("2.0", rule?.schemaVersion)
        assertEquals("RULE-SPY-001", rule?.canonicalId)
        assertEquals("Pegasus Spyware Domain Block", rule?.name)
        assertEquals("CRITICAL", rule?.severity)
        assertEquals("NETWORK", rule?.category)
        assertEquals("signature", rule?.engineType)
        assertEquals("dns_query", rule?.targetSource)
        assertEquals(ContainmentAction.BLOCK, rule?.action)
        assertEquals(2, rule?.condition?.targetDomains?.size)
        assertTrue(rule?.condition?.targetDomains?.contains("pegasus-exfil.nso-group.net") == true)
        assertEquals("198.51.100.99", rule?.condition?.targetIps?.firstOrNull())
    }

    @Test
    fun testParseBehavioralRule() {
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
              max_entropy: 4.2
            action: "BLOCK"
        """.trimIndent()

        val rule = YamlRuleParser.parseYamlRule(yamlText)
        assertNotNull(rule)
        assertEquals("behavioral", rule?.engineType)
        assertEquals(3, rule?.condition?.maxSubdomainDepth)
        assertEquals(4.2, rule?.condition?.maxEntropy!!, 0.001)
    }

    @Test
    fun testParseDisabledRule() {
        val yamlText = """
            schema_version: "2.0"
            rule_id: "RULE-TEST-000"
            name: "Disabled Test Rule"
            enabled: false
            action: "LOG"
        """.trimIndent()

        val rule = YamlRuleParser.parseYamlRule(yamlText)
        assertNull(rule)
    }

    @Test
    fun testParseInvalidYaml() {
        val invalidYaml = "invalid: : yaml: content"
        val rule = YamlRuleParser.parseYamlRule(invalidYaml)
        assertNull(rule)
    }
}
