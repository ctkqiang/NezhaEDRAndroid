package xin.ctkqiang.nezhaedr.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TelemetryDatabaseHelperTest {

    private lateinit var dbHelper: TelemetryDatabaseHelper

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        dbHelper = TelemetryDatabaseHelper(context)
        dbHelper.clearAllVisitedSites()
    }

    @After
    fun tearDown() {
        dbHelper.clearAllVisitedSites()
        dbHelper.close()
    }

    @Test
    fun testInsertAndRetrieveVisitedSite() {
        val record = VisitedSiteRecord(
            destinationIp = "1.1.1.1",
            domain = "888casino-vip.com",
            protocol = "HTTPS",
            isThreat = true,
            appName = "Chrome",
            packageName = "com.android.chrome"
        )

        val id = dbHelper.insertVisitedSite(record)
        assertTrue(id > 0)

        val list = dbHelper.getAllVisitedSites()
        assertEquals(1, list.size)
        assertEquals("1.1.1.1", list[0].destinationIp)
        assertEquals("888casino-vip.com", list[0].domain)
        assertTrue(list[0].isThreat)
    }

    @Test
    fun testBlockedThreatAttemptsCount() {
        val cleanRecord = VisitedSiteRecord(
            destinationIp = "8.8.8.8",
            domain = "google.com",
            isThreat = false
        )
        val threatRecord1 = VisitedSiteRecord(
            destinationIp = "192.0.2.1",
            domain = "malicious-c2.example.com",
            isThreat = true
        )
        val threatRecord2 = VisitedSiteRecord(
            destinationIp = "198.51.100.14",
            domain = "free-bonus-claim.net",
            isThreat = true
        )

        dbHelper.insertVisitedSite(cleanRecord)
        dbHelper.insertVisitedSite(threatRecord1)
        dbHelper.insertVisitedSite(threatRecord2)

        val count = dbHelper.getBlockedThreatAttemptsCount()
        assertEquals(2, count)

        val threatList = dbHelper.getBlockedThreatAttempts()
        assertEquals(2, threatList.size)
        assertTrue(threatList.all { it.isThreat })
    }

    @Test
    fun testClearAllVisitedSites() {
        val record = VisitedSiteRecord(destinationIp = "10.0.0.1", domain = "test.com")
        dbHelper.insertVisitedSite(record)
        assertEquals(1, dbHelper.getAllVisitedSites().size)

        val deletedRows = dbHelper.clearAllVisitedSites()
        assertEquals(1, deletedRows)
        assertEquals(0, dbHelper.getAllVisitedSites().size)
    }

    @Test
    fun testExportToYamlAndCsv() {
        val record = VisitedSiteRecord(
            destinationIp = "1.2.3.4",
            domain = "example-scam.com",
            protocol = "HTTPS",
            isThreat = true,
            appName = "TestApp",
            packageName = "com.test.app"
        )
        dbHelper.insertVisitedSite(record)

        val yamlExport = dbHelper.exportVisitedSitesToYaml()
        assertTrue(yamlExport.contains("destination_ip: \"1.2.3.4\""))
        assertTrue(yamlExport.contains("domain: \"example-scam.com\""))
        assertTrue(yamlExport.contains("is_threat: true"))

        val csvExport = dbHelper.exportVisitedSitesToCsv()
        assertTrue(csvExport.contains("ID,Timestamp,Destination_IP,Domain,Protocol,Is_Threat,App_Name,Package_Name"))
        assertTrue(csvExport.contains("\"1.2.3.4\",\"example-scam.com\""))
    }
}
