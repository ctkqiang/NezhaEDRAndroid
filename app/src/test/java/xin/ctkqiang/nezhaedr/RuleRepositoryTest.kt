package xin.ctkqiang.nezhaedr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xin.ctkqiang.nezhaedr.data.repository.RuleRepositoryImpl

class RuleRepositoryTest {

    @Test
    fun testFallbackRemoteCatalog() {
        val repository = RuleRepositoryImpl()
        val catalog = repository.getFallbackRemoteRuleCatalog()

        assertNotNull(catalog)
        assertEquals(21, catalog.size)

        val c2Item = catalog.find { it.fileName == "c2_threat_block.yaml" }
        assertNotNull(c2Item)
        assertEquals("c2_threat_block", c2Item?.ruleId)
        assertEquals("Known C2 Threat IP Block", c2Item?.name)
        assertEquals("NETWORK", c2Item?.category)
        assertEquals("HIGH", c2Item?.severity)

        val wifiItem = catalog.find { it.fileName == "evil_twin_wifi_alert.yaml" }
        assertNotNull(wifiItem)
        assertEquals("evil_twin_wifi_alert", wifiItem?.ruleId)
        assertEquals("WIFI", wifiItem?.category)
        assertEquals("CRITICAL", wifiItem?.severity)

        val smsItem = catalog.find { it.fileName == "sms_stealer_trojan_block.yaml" }
        assertNotNull(smsItem)
        assertEquals("sms_stealer_trojan_block", smsItem?.ruleId)
        assertEquals("PACKAGE", smsItem?.category)
        assertEquals("CRITICAL", smsItem?.severity)
    }

    @Test
    fun testEmbeddedDefaultRulesPackage() {
        val repository = RuleRepositoryImpl()
        val rulePackage = repository.getCachedRules()

        assertNotNull(rulePackage)
        assertTrue(rulePackage!!.rules.isNotEmpty())

        val ids = rulePackage.rules.map { it.id }
        assertTrue(ids.contains("c2_threat_block"))
        assertTrue(ids.contains("ransomware_beacon_block"))
        assertTrue(ids.contains("adult_content_block"))
        assertTrue(ids.contains("rogue_package_alert"))
    }
}
