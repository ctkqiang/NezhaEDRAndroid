package xin.ctkqiang.nezhaedr.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.Serializable
import xin.ctkqiang.nezhaedr.util.EDRLogger
import java.util.Date

@Serializable
data class VisitedSiteRecord(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val destinationIp: String,
    val domain: String = "",
    val protocol: String = "IPv4",
    val isThreat: Boolean = false,
    val appName: String = "Unknown Application",
    val packageName: String = ""
)

class TelemetryDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        EDRLogger.i(TAG, "onCreate: Creating SQLite database table $TABLE_VISITED_SITES")
        val createTableQuery = """
            CREATE TABLE $TABLE_VISITED_SITES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_DEST_IP TEXT NOT NULL,
                $COLUMN_DOMAIN TEXT,
                $COLUMN_PROTOCOL TEXT,
                $COLUMN_IS_THREAT INTEGER DEFAULT 0,
                $COLUMN_APP_NAME TEXT,
                $COLUMN_PACKAGE_NAME TEXT
            )
        """.trimIndent()
        try {
            db.execSQL(createTableQuery)
            EDRLogger.i(TAG, "Table $TABLE_VISITED_SITES created successfully")
        } catch (e: Exception) {
            EDRLogger.fatal(TAG, "Failed to create database table $TABLE_VISITED_SITES", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        EDRLogger.w(TAG, "onUpgrade: Upgrading database from version $oldVersion to $newVersion")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VISITED_SITES")
        onCreate(db)
    }

    fun insertVisitedSite(record: VisitedSiteRecord): Long {
        EDRLogger.v(TAG, "insertVisitedSite: Inserting IP ${record.destinationIp} (isThreat: ${record.isThreat})")
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TIMESTAMP, record.timestamp)
            put(COLUMN_DEST_IP, record.destinationIp)
            put(COLUMN_DOMAIN, record.domain)
            put(COLUMN_PROTOCOL, record.protocol)
            put(COLUMN_IS_THREAT, if (record.isThreat) 1 else 0)
            put(COLUMN_APP_NAME, record.appName)
            put(COLUMN_PACKAGE_NAME, record.packageName)
        }
        val rowId = db.insert(TABLE_VISITED_SITES, null, values)
        if (rowId == -1L) {
            EDRLogger.e(TAG, "insertVisitedSite failed for record: ${record.destinationIp}")
        } else {
            EDRLogger.d(TAG, "insertVisitedSite: Row inserted successfully with ID $rowId")
        }
        return rowId
    }

    fun getAllVisitedSites(): List<VisitedSiteRecord> {
        EDRLogger.v(TAG, "getAllVisitedSites: Querying all telemetry records from SQLite")
        val records = mutableListOf<VisitedSiteRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_VISITED_SITES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COLUMN_ID)
            val timeIdx = c.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val ipIdx = c.getColumnIndexOrThrow(COLUMN_DEST_IP)
            val domainIdx = c.getColumnIndexOrThrow(COLUMN_DOMAIN)
            val protoIdx = c.getColumnIndexOrThrow(COLUMN_PROTOCOL)
            val threatIdx = c.getColumnIndexOrThrow(COLUMN_IS_THREAT)
            val appIdx = c.getColumnIndexOrThrow(COLUMN_APP_NAME)
            val pkgIdx = c.getColumnIndexOrThrow(COLUMN_PACKAGE_NAME)

            while (c.moveToNext()) {
                records.add(
                    VisitedSiteRecord(
                        id = c.getLong(idIdx),
                        timestamp = c.getLong(timeIdx),
                        destinationIp = c.getString(ipIdx) ?: "",
                        domain = c.getString(domainIdx) ?: "",
                        protocol = c.getString(protoIdx) ?: "IPv4",
                        isThreat = c.getInt(threatIdx) == 1,
                        appName = c.getString(appIdx) ?: "Unknown Application",
                        packageName = c.getString(pkgIdx) ?: ""
                    )
                )
            }
        }
        EDRLogger.d(TAG, "getAllVisitedSites: Retrieved ${records.size} records")
        return records
    }

    fun clearAllVisitedSites(): Int {
        EDRLogger.w(TAG, "clearAllVisitedSites: Clearing all SQLite records")
        val db = writableDatabase
        val rowsDeleted = db.delete(TABLE_VISITED_SITES, null, null)
        EDRLogger.i(TAG, "clearAllVisitedSites: Deleted $rowsDeleted records")
        return rowsDeleted
    }

    fun exportVisitedSitesToYaml(): String {
        EDRLogger.i(TAG, "exportVisitedSitesToYaml: Exporting SQLite records to YAML format")
        val list = getAllVisitedSites()
        val sb = StringBuilder()
        sb.append("# NezhaEDR Local Telemetry Export (YAML)\n")
        sb.append("# Exported: ${Date()}\n\n")
        for (item in list) {
            sb.append("- id: ${item.id}\n")
            sb.append("  timestamp: ${item.timestamp}\n")
            sb.append("  destination_ip: \"${item.destinationIp}\"\n")
            sb.append("  domain: \"${item.domain}\"\n")
            sb.append("  protocol: \"${item.protocol}\"\n")
            sb.append("  is_threat: ${item.isThreat}\n")
            sb.append("  app_name: \"${item.appName}\"\n")
            sb.append("  package_name: \"${item.packageName}\"\n\n")
        }
        val result = sb.toString()
        EDRLogger.d(TAG, "exportVisitedSitesToYaml: Produced ${result.length} bytes of YAML data")
        return result
    }

    fun exportVisitedSitesToCsv(): String {
        EDRLogger.i(TAG, "exportVisitedSitesToCsv: Exporting SQLite records to CSV format")
        val list = getAllVisitedSites()
        val sb = StringBuilder()
        sb.append("ID,Timestamp,Destination_IP,Domain,Protocol,Is_Threat,App_Name,Package_Name\n")
        for (item in list) {
            val safeIp = item.destinationIp.replace("\"", "\"\"")
            val safeDomain = item.domain.replace("\"", "\"\"")
            val safeApp = item.appName.replace("\"", "\"\"")
            val safePkg = item.packageName.replace("\"", "\"\"")
            sb.append("${item.id},${item.timestamp},\"$safeIp\",\"$safeDomain\",\"${item.protocol}\",${item.isThreat},\"$safeApp\",\"$safePkg\"\n")
        }
        val result = sb.toString()
        EDRLogger.d(TAG, "exportVisitedSitesToCsv: Produced ${result.length} bytes of CSV data")
        return result
    }

    companion object {
        private const val TAG = "TelemetryDatabaseHelper"
        private const val DATABASE_NAME = "nezha_edr_telemetry.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_VISITED_SITES = "visited_sites"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_DEST_IP = "destination_ip"
        private const val COLUMN_DOMAIN = "domain"
        private const val COLUMN_PROTOCOL = "protocol"
        private const val COLUMN_IS_THREAT = "is_threat"
        private const val COLUMN_APP_NAME = "app_name"
        private const val COLUMN_PACKAGE_NAME = "package_name"
    }
}
