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
    fun testParseNetworkBlockRule() {
        val yamlText = """
            id: "pegasus_spyware_block"
            name: "Pegasus Spyware Domain Block"
            description: "Blocks egress connections to known commercial spyware exfiltration domains"
            severity: "CRITICAL"
            category: "NETWORK"
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
        assertEquals("pegasus_spyware_block", rule?.id)
        assertEquals("Pegasus Spyware Domain Block", rule?.name)
        assertEquals("CRITICAL", rule?.severity)
        assertEquals("NETWORK", rule?.category)
        assertEquals(ContainmentAction.BLOCK, rule?.action)
        assertEquals(2, rule?.condition?.targetDomains?.size)
        assertTrue(rule?.condition?.targetDomains?.contains("pegasus-exfil.nso-group.net") == true)
        assertEquals("198.51.100.99", rule?.condition?.targetIps?.firstOrNull())
    }

    @Test
    fun testParsePackageTerminateRule() {
        val yamlText = """
            id: "banking_trojan_alert"
            name: "Banking Trojan Sideload Detection"
            description: "Detects malware overlay apps targeting financial tokens"
            severity: "CRITICAL"
            category: "PACKAGE"
            condition:
              package_patterns:
                - ".*trojan\\.banker.*"
                - ".*overlay\\.stealer.*"
            action: "TERMINATE"
        """.trimIndent()

        val rule = YamlRuleParser.parseYamlRule(yamlText)
        assertNotNull(rule)
        assertEquals("banking_trojan_alert", rule?.id)
        assertEquals(ContainmentAction.TERMINATE, rule?.action)
        assertEquals(2, rule?.condition?.packagePatterns?.size)
        assertTrue(rule?.condition?.packagePatterns?.firstOrNull()?.contains("trojan") == true)
    }

    @Test
    fun testParseInvalidYaml() {
        val invalidYaml = "invalid: : yaml: content"
        val rule = YamlRuleParser.parseYamlRule(invalidYaml)
        assertNull(rule)
    }
}
