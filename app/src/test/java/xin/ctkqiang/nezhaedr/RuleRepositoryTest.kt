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
        assertEquals(12, catalog.size)

        val c2Item = catalog.find { it.fileName == "c2_threat_block.yaml" }
        assertNotNull(c2Item)
        assertEquals("c2_threat_block", c2Item?.ruleId)
        assertEquals("Known C2 Threat IP Block", c2Item?.name)
        assertEquals("NETWORK", c2Item?.category)
        assertEquals("HIGH", c2Item?.severity)

        val adultItem = catalog.find { it.fileName == "adult_content_block.yaml" }
        assertNotNull(adultItem)
        assertEquals("adult_content_block", adultItem?.ruleId)
        assertEquals("Adult & Explicit Content Domain Block", adultItem?.name)
        assertEquals("CRITICAL", adultItem?.severity)
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
