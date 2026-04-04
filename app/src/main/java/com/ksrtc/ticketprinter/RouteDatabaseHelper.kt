package com.sktc.ticketprinter

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class RouteDatabaseHelper(private val appContext: Context) :
    SQLiteOpenHelper(appContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "RouteManager.db"
        
        // Stops Table
        const val TABLE_STOPS = "Stops"
        const val COLUMN_STOP_ID = "stop_id"
        const val COLUMN_NAME_EN = "name_en"
        const val COLUMN_NAME_KA = "name_ka"

        // Stages Table (Mapping Stops to Distances on a Route)
        const val TABLE_STAGES = "Stages"
        const val COLUMN_STAGE_ID = "stage_id"
        const val COLUMN_STAGE_STOP_ID = "stop_id"
        const val COLUMN_STAGE_NUM = "stage_num"
        const val COLUMN_DISTANCE = "distance_km"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createStopsTable = ("CREATE TABLE " + TABLE_STOPS + "("
                + COLUMN_STOP_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME_EN + " TEXT,"
                + COLUMN_NAME_KA + " TEXT" + ")")
        db.execSQL(createStopsTable)

        val createStagesTable = ("CREATE TABLE " + TABLE_STAGES + "("
                + COLUMN_STAGE_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_STAGE_STOP_ID + " INTEGER,"
                + COLUMN_STAGE_NUM + " INTEGER,"
                + COLUMN_DISTANCE + " REAL" + ")")
        db.execSQL(createStagesTable)
        
        parseRoutesFile(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STOPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STAGES")
        onCreate(db)
    }

    private fun parseRoutesFile(db: SQLiteDatabase) {
        var stopIdCounter = 1
        var stageIdCounter = 1
        var currentStageNum = 1
        
        // Maps to keep track of created stops
        val stopMap = mutableMapOf<String, Int>()

        try {
            val stream = appContext.assets.open("routes")
            BufferedReader(InputStreamReader(stream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split(",").map { it.trim() }
                    if (parts.isEmpty()) continue
                    
                    if (parts[0] == "Route") {
                        currentStageNum = 1 // Reset stages for new route
                    } else if (parts[0] == "Stop") {
                        val stopNameEn = parts[1]
                        
                        // Insert Stop if it doesn't exist
                        if (!stopMap.containsKey(stopNameEn)) {
                            insertStop(db, stopIdCounter, stopNameEn, stopNameEn) // Using EN for KA too for now
                            stopMap[stopNameEn] = stopIdCounter
                            stopIdCounter++
                        }
                        
                        val thisStopId = stopMap[stopNameEn]!!
                        
                        // Parse literal fare
                        var fare = 5.0 // Default minimum fare if not specified
                        if (parts.size > 2) {
                             // E.g., Stop, Mandya, 6, 8, 10
                             // The first fare (parts[2]) is to the very next stop.
                             // For simplicity of this basic DB, we'll store the 'base fare' as the first jump.
                             // In a more complex production DB, we'd need a 'FareMatrix' table to map (FromStop, ToStop) -> Price.
                             try {
                                 fare = parts[2].toDouble()
                             } catch (e: NumberFormatException) {
                                 fare = 5.0
                             }
                        }
                        
                        insertStage(db, stageIdCounter++, thisStopId, currentStageNum++, fare) 
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RouteDB", "routes file not available or invalid. Seeding fallback data.", e)
            seedFallbackData(db)
        }
    }

    private fun seedFallbackData(db: SQLiteDatabase) {
        // Sample stops based on user screenshot
        insertStop(db, 1, "MANDYA", "ಮಂಡ್ಯ")
        insertStop(db, 2, "KALLAHALLI", "ಕಲ್ಲಹಳ್ಳಿ")
        insertStop(db, 3, "INDUVALU", "ಇಂದುವಾಳು")
        insertStop(db, 4, "SUNDAHALLI", "ಸುಂಡಳ್ಳಿ")
        insertStop(db, 5, "SRIRANGAPATNA", "ಶ್ರೀರಂಗಪಟ್ಟಣ")

        // Sample stages (Distance from start of route in KM)
        insertStage(db, 1, 1, 1, 0.0)
        insertStage(db, 2, 2, 2, 4.5)
        insertStage(db, 3, 3, 3, 8.2)
        insertStage(db, 4, 4, 4, 12.0)
        insertStage(db, 5, 5, 5, 20.5)
    }

    private fun insertStop(db: SQLiteDatabase, id: Int, nameEn: String, nameKa: String) {
        val values = ContentValues().apply {
            put(COLUMN_STOP_ID, id)
            put(COLUMN_NAME_EN, nameEn)
            put(COLUMN_NAME_KA, nameKa)
        }
        db.insert(TABLE_STOPS, null, values)
    }
    
    private fun insertStage(db: SQLiteDatabase, stageId: Int, stopId: Int, stageNum: Int, dist: Double) {
        val values = ContentValues().apply {
            put(COLUMN_STAGE_ID, stageId)
            put(COLUMN_STAGE_STOP_ID, stopId)
            put(COLUMN_STAGE_NUM, stageNum)
            put(COLUMN_DISTANCE, dist)
        }
        db.insert(TABLE_STAGES, null, values)
    }

    // Helper functions for UI
    fun getAllStops(): List<Stop> {
        val stops = mutableListOf<Stop>()
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_STOPS", null)
        if (cursor.moveToFirst()) {
            do {
                stops.add(
                    Stop(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return stops
    }

    // Basic Fare Calculation (Literal Fare Lookup)
    fun calculateFare(fromStopId: Int, toStopId: Int, rateMultiplier: Double = 1.0): Double {
        val db = this.readableDatabase
        
        var baseFare = 0.0
        
        // Since we stored the first jump fare as distance, let's just sum the fares between stages to get a proxy total fare
        var fromStageNum = 0
        var toStageNum = 0
        
        val cursorFrom = db.rawQuery("SELECT $COLUMN_STAGE_NUM FROM $TABLE_STAGES WHERE $COLUMN_STAGE_STOP_ID = ?", arrayOf(fromStopId.toString()))
        if (cursorFrom.moveToFirst()) fromStageNum = cursorFrom.getInt(0)
        cursorFrom.close()
        
        val cursorTo = db.rawQuery("SELECT $COLUMN_STAGE_NUM FROM $TABLE_STAGES WHERE $COLUMN_STAGE_STOP_ID = ?", arrayOf(toStopId.toString()))
        if (cursorTo.moveToFirst()) toStageNum = cursorTo.getInt(0)
        cursorTo.close()
        
        val startStage = Math.min(fromStageNum, toStageNum)
        val endStage = Math.max(fromStageNum, toStageNum)

        if (startStage > 0 && endStage > 0 && startStage != endStage) {
            val cursorFare = db.rawQuery("SELECT SUM($COLUMN_DISTANCE) FROM $TABLE_STAGES WHERE $COLUMN_STAGE_NUM >= ? AND $COLUMN_STAGE_NUM < ?", arrayOf(startStage.toString(), endStage.toString()))
            if (cursorFare.moveToFirst()) baseFare = cursorFare.getDouble(0)
            cursorFare.close()
        }
        
        db.close()
        
        if(baseFare == 0.0) baseFare = 10.0 // Fallback
        
        return baseFare * rateMultiplier
    }
}
