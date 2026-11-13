package xin.ctkqiang.nezhaedr.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VISITED_SITES")
        onCreate(db)
    }

    fun insertVisitedSite(record: VisitedSiteRecord): Long {
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
        return db.insert(TABLE_VISITED_SITES, null, values)
    }

    fun getAllVisitedSites(): List<VisitedSiteRecord> {
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
        return records
    }

    fun clearAllVisitedSites(): Int {
        val db = writableDatabase
        return db.delete(TABLE_VISITED_SITES, null, null)
    }

    fun exportVisitedSitesToJson(): String {
        val list = getAllVisitedSites()
        return Json { prettyPrint = true }.encodeToString(list)
    }

    companion object {
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
