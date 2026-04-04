package com.sktc.ticketprinter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketFormatter {

    private fun fitTextSize(paint: Paint, text: String, preferredSize: Float, minSize: Float, maxWidth: Float): Float {
        var size = preferredSize
        paint.textSize = size
        while (size > minSize && paint.measureText(text) > maxWidth) {
            size -= 1f
            paint.textSize = size
        }
        return size.coerceAtLeast(minSize)
    }

    private fun removeVowelsForFit(name: String): String {
        return name.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.length <= 2) {
                    word
                } else {
                    val head = word.first()
                    val tail = word.substring(1).replace(Regex("[AEIOUaeiou]"), "")
                    "$head$tail"
                }
            }
    }

    // Thermal ticket rendering logic
    fun createTicketBitmap(
        ticketNo: String,
        fromStopEN: String,
        fromStopKA: String,
        toStopEN: String,
        toStopKA: String,
        adults: Int,
        childs: Int,
        totalFare: Double,
        rrn: String,
        division: String = "",
        routeNumber: String = "",
        busType: String = "",
        busNumber: String = "",
        adultFareTotal: Double = 0.0,
        childFareTotal: Double = 0.0,
        adultFarePerPassenger: Double = 0.0,
        childFarePerPassenger: Double = 0.0,
        passType: String = "",
        passFareTotal: Double = 0.0,
        luggageFareTotal: Double = 0.0,
        printDateTime: String = ""
    ): Bitmap {
        val width = 384
        val height = 620
        val contentLeft = 12f
        val contentRight = width - 12f
        val contentWidth = contentRight - contentLeft

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        val nowString = if (printDateTime.isNotBlank()) {
            printDateTime
        } else {
            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        }
        val dateTimeParts = nowString.split(" ")
        val dateStr = dateTimeParts.getOrNull(0).orEmpty()
        val timeStr = dateTimeParts.getOrNull(1).orEmpty()

        // SKRTC spaced letters
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 56f
        val headerPositions = listOf(
            contentLeft + 20f,
            contentLeft + (contentWidth * 0.30f),
            contentLeft + (contentWidth * 0.52f),
            contentLeft + (contentWidth * 0.74f),
            contentRight - 12f
        )
        val letters = listOf("S", "K", "R", "T", "C")
        for (i in letters.indices) {
            canvas.drawText(letters[i], headerPositions[i], 66f, paint)
        }

        // Division
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 18f
        canvas.drawText(division.ifBlank { "Division name" }, contentLeft + contentWidth / 2f, 98f, paint)

        fun drawThreeColumnRow(y: Float, left: String, mid: String, right: String, preferredSize: Float = 16f) {
            val c1 = contentLeft + contentWidth / 6f
            val c2 = contentLeft + contentWidth / 2f
            val c3 = contentLeft + (contentWidth * 5f / 6f)
            val cellWidth = contentWidth / 3f - 8f

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER

            paint.textSize = fitTextSize(paint, left, preferredSize, 11f, cellWidth)
            canvas.drawText(left, c1, y, paint)

            paint.textSize = fitTextSize(paint, mid, preferredSize, 11f, cellWidth)
            canvas.drawText(mid, c2, y, paint)

            paint.textSize = fitTextSize(paint, right, preferredSize, 11f, cellWidth)
            canvas.drawText(right, c3, y, paint)
        }

        drawThreeColumnRow(136f, ticketNo.ifBlank { "-" }, dateStr.ifBlank { "-" }, timeStr.ifBlank { "-" })
        drawThreeColumnRow(
            174f,
            routeNumber.ifBlank { "-" },
            busType.ifBlank { "-" },
            busNumber.ifBlank { "-" }
        )

        // From/To line with adaptive fit
        var displayFrom = fromStopEN
        var displayTo = toStopEN
        var routeText = "${displayFrom} -- ${displayTo}"
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val routePreferred = 62f
        val routeMin = 24f
        paint.textSize = fitTextSize(paint, routeText, routePreferred, routeMin, contentWidth - 6f)
        if (paint.measureText(routeText) > contentWidth - 6f) {
            val fromOnly = "${removeVowelsForFit(fromStopEN)} -- ${toStopEN}"
            val toOnly = "${fromStopEN} -- ${removeVowelsForFit(toStopEN)}"
            val bothShort = "${removeVowelsForFit(fromStopEN)} -- ${removeVowelsForFit(toStopEN)}"

            paint.textSize = routeMin
            routeText = when {
                paint.measureText(fromOnly) <= contentWidth - 6f -> fromOnly
                paint.measureText(toOnly) <= contentWidth - 6f -> toOnly
                else -> bothShort
            }
            paint.textSize = fitTextSize(paint, routeText, routePreferred, routeMin, contentWidth - 6f)
        }
        canvas.drawText(routeText, contentLeft + contentWidth / 2f, 248f, paint)

        // Fare lines
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 20f
        val adultUnitFare = adultFarePerPassenger
        val childUnitFare = childFarePerPassenger
        canvas.drawText(
            "ADULTS   $adults* ${String.format(Locale.getDefault(), "%.2f", adultUnitFare)} = ${String.format(Locale.getDefault(), "%.2f", adultFareTotal)}",
            contentLeft,
            320f,
            paint
        )
        canvas.drawText(
            "CHILD   $childs* ${String.format(Locale.getDefault(), "%.2f", childUnitFare)} = ${String.format(Locale.getDefault(), "%.2f", childFareTotal)}",
            contentLeft,
            356f,
            paint
        )

        // Grand total
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val grandText = "GRAND TOTAL: Rs. ${String.format(Locale.getDefault(), "%.2f", totalFare)}"
        paint.textSize = fitTextSize(paint, grandText, 52f, 20f, contentWidth)
        canvas.drawText(grandText, contentLeft + contentWidth / 2f, 438f, paint)

        // Footer
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 18f
        canvas.drawText("RRN NO.: $rrn", contentLeft, 500f, paint)
        canvas.drawText("VISIT {official website}", contentLeft, 534f, paint)

        val usedHeight = 560
        return Bitmap.createBitmap(bitmap, 0, 0, width, usedHeight.coerceAtMost(height))
    }

    fun createSeniorTicketBitmap(
        ticketNo: String,
        seniorCount: Int,
        fromStopEN: String,
        fromStopKA: String,
        toStopEN: String,
        toStopKA: String,
        seniorFareTotal: Double,
        totalFare: Double,
        rrn: String,
        division: String = "",
        routeNumber: String = "",
        busType: String = "",
        busNumber: String = "",
        printDateTime: String = ""
    ): Bitmap {
        val width = 384
        val height = 620
        val contentLeft = 12f
        val contentRight = width - 12f
        val contentWidth = contentRight - contentLeft

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        val nowString = if (printDateTime.isNotBlank()) {
            printDateTime
        } else {
            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        }
        val dateTimeParts = nowString.split(" ")
        val dateStr = dateTimeParts.getOrNull(0).orEmpty()
        val timeStr = dateTimeParts.getOrNull(1).orEmpty()

        // SKRTC header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 56f
        val headerPositions = listOf(
            contentLeft + 20f,
            contentLeft + (contentWidth * 0.30f),
            contentLeft + (contentWidth * 0.52f),
            contentLeft + (contentWidth * 0.74f),
            contentRight - 12f
        )
        val letters = listOf("S", "K", "R", "T", "C")
        for (i in letters.indices) {
            canvas.drawText(letters[i], headerPositions[i], 66f, paint)
        }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 18f
        canvas.drawText(division.ifBlank { "Division name" }, contentLeft + contentWidth / 2f, 98f, paint)

        fun drawThreeColumnRow(y: Float, left: String, mid: String, right: String, preferredSize: Float = 16f) {
            val c1 = contentLeft + contentWidth / 6f
            val c2 = contentLeft + contentWidth / 2f
            val c3 = contentLeft + (contentWidth * 5f / 6f)
            val cellWidth = contentWidth / 3f - 8f

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER

            paint.textSize = fitTextSize(paint, left, preferredSize, 11f, cellWidth)
            canvas.drawText(left, c1, y, paint)
            paint.textSize = fitTextSize(paint, mid, preferredSize, 11f, cellWidth)
            canvas.drawText(mid, c2, y, paint)
            paint.textSize = fitTextSize(paint, right, preferredSize, 11f, cellWidth)
            canvas.drawText(right, c3, y, paint)
        }

        drawThreeColumnRow(136f, ticketNo.ifBlank { "-" }, dateStr.ifBlank { "-" }, timeStr.ifBlank { "-" })
        drawThreeColumnRow(174f, routeNumber.ifBlank { "-" }, busType.ifBlank { "-" }, busNumber.ifBlank { "-" })

        var displayFrom = fromStopEN
        var displayTo = toStopEN
        var routeText = "${displayFrom} -- ${displayTo}"
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val routePreferred = 58f
        val routeMin = 24f
        paint.textSize = fitTextSize(paint, routeText, routePreferred, routeMin, contentWidth - 6f)
        if (paint.measureText(routeText) > contentWidth - 6f) {
            val fromOnly = "${removeVowelsForFit(fromStopEN)} -- ${toStopEN}"
            val toOnly = "${fromStopEN} -- ${removeVowelsForFit(toStopEN)}"
            val bothShort = "${removeVowelsForFit(fromStopEN)} -- ${removeVowelsForFit(toStopEN)}"

            paint.textSize = routeMin
            routeText = when {
                paint.measureText(fromOnly) <= contentWidth - 6f -> fromOnly
                paint.measureText(toOnly) <= contentWidth - 6f -> toOnly
                else -> bothShort
            }
            paint.textSize = fitTextSize(paint, routeText, routePreferred, routeMin, contentWidth - 6f)
        }
        canvas.drawText(routeText, contentLeft + contentWidth / 2f, 248f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 58f
        canvas.drawText("SENIOR CITIZEN", contentLeft + contentWidth / 2f, 328f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 22f
        val perFare = if (seniorCount > 0) seniorFareTotal / seniorCount else 0.0
        val fareLine = "$seniorCount* ${String.format(Locale.getDefault(), "%.2f", perFare)} = ${String.format(Locale.getDefault(), "%.2f", seniorFareTotal)}"
        paint.textSize = fitTextSize(paint, fareLine, 22f, 14f, contentWidth)
        canvas.drawText(fareLine, contentLeft + contentWidth / 2f, 374f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val grandText = "GRAND TOTAL: Rs. ${String.format(Locale.getDefault(), "%.2f", totalFare)}"
        paint.textSize = fitTextSize(paint, grandText, 52f, 20f, contentWidth)
        canvas.drawText(grandText, contentLeft + contentWidth / 2f, 436f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 18f
        canvas.drawText("RRN NO.: $rrn", contentLeft, 500f, paint)
        canvas.drawText("VISIT {official website}", contentLeft, 534f, paint)

        val usedHeight = 560
        return Bitmap.createBitmap(bitmap, 0, 0, width, usedHeight.coerceAtMost(height))
    }

    fun createDayPassTicketBitmap(
        ticketNo: String,
        dayPassCount: Int,
        dayPassAmount: Double,
        totalFare: Double,
        rrn: String,
        division: String = "",
        routeNumber: String = "",
        busType: String = "",
        busNumber: String = "",
        printDateTime: String = ""
    ): Bitmap {
        val width = 384
        val height = 620
        val contentLeft = 12f
        val contentRight = width - 12f
        val contentWidth = contentRight - contentLeft

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        val nowString = if (printDateTime.isNotBlank()) {
            printDateTime
        } else {
            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        }
        val dateTimeParts = nowString.split(" ")
        val dateStr = dateTimeParts.getOrNull(0).orEmpty()
        val timeStr = dateTimeParts.getOrNull(1).orEmpty()
        val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

        // SKRTC header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 56f
        val headerPositions = listOf(
            contentLeft + 20f,
            contentLeft + (contentWidth * 0.30f),
            contentLeft + (contentWidth * 0.52f),
            contentLeft + (contentWidth * 0.74f),
            contentRight - 12f
        )
        val letters = listOf("S", "K", "R", "T", "C")
        for (i in letters.indices) {
            canvas.drawText(letters[i], headerPositions[i], 66f, paint)
        }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 18f
        canvas.drawText(division.ifBlank { "Division name" }, contentLeft + contentWidth / 2f, 98f, paint)

        fun drawThreeColumnRow(y: Float, left: String, mid: String, right: String, preferredSize: Float = 16f) {
            val c1 = contentLeft + contentWidth / 6f
            val c2 = contentLeft + contentWidth / 2f
            val c3 = contentLeft + (contentWidth * 5f / 6f)
            val cellWidth = contentWidth / 3f - 8f

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER

            paint.textSize = fitTextSize(paint, left, preferredSize, 11f, cellWidth)
            canvas.drawText(left, c1, y, paint)
            paint.textSize = fitTextSize(paint, mid, preferredSize, 11f, cellWidth)
            canvas.drawText(mid, c2, y, paint)
            paint.textSize = fitTextSize(paint, right, preferredSize, 11f, cellWidth)
            canvas.drawText(right, c3, y, paint)
        }

        drawThreeColumnRow(136f, ticketNo.ifBlank { "-" }, dateStr.ifBlank { "-" }, timeStr.ifBlank { "-" })
        drawThreeColumnRow(174f, routeNumber.ifBlank { "-" }, busType.ifBlank { "-" }, busNumber.ifBlank { "-" })

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 60f
        canvas.drawText("DAY PASS", contentLeft + contentWidth / 2f, 246f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = fitTextSize(
            paint,
            "Valid throughout ${division.ifBlank { "division" }} Division for $dayName",
            19f,
            12f,
            contentWidth
        )
        canvas.drawText(
            "Valid throughout ${division.ifBlank { "division" }} Division for $dayName",
            contentLeft + contentWidth / 2f,
            308f,
            paint
        )

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val grandText = "GRAND TOTAL: Rs. ${String.format(Locale.getDefault(), "%.2f", totalFare)}"
        paint.textSize = fitTextSize(paint, grandText, 52f, 20f, contentWidth)
        canvas.drawText(grandText, contentLeft + contentWidth / 2f, 402f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 18f
        canvas.drawText("RRN NO.: $rrn", contentLeft, 500f, paint)
        canvas.drawText("VISIT {official website}", contentLeft, 534f, paint)

        val usedHeight = 560
        return Bitmap.createBitmap(bitmap, 0, 0, width, usedHeight.coerceAtMost(height))
    }

    // Convert Android Bitmap to ESC/POS Raster format
    fun decodeBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val widthPos = bitmap.width / 8
        val height = bitmap.height
        val data = ByteArray(8 + widthPos * height)
        
        // ESC * m n1 n2 d1...dk   (Raster bit image)
        // Set line spacing standard
        data[0] = 0x1B; data[1] = 0x33; data[2] = 0x00
        
        // Print raster bit image
        data[3] = 0x1D; data[4] = 0x76; data[5] = 0x30; data[6] = 0x00
        data[7] = widthPos.toByte() // xL
        data[8] = 0x00 // xH
        data[9] = (height % 256).toByte() // yL
        data[10] = (height / 256).toByte() // yH

        var index = 11
        for (y in 0 until height) {
            for (x in 0 until bitmap.width step 8) {
                var byte = 0
                for (b in 0..7) {
                    val pixel = bitmap.getPixel(x + b, y)
                    // If pixel is dark, set bit to 1 (print black dot)
                    if (Color.red(pixel) < 128) {
                        byte = byte or (1 shl (7 - b))
                    }
                }
                data[index++] = byte.toByte()
            }
        }
        
        // Feed lines and cut paper
        val tail = byteArrayOf(0x0A, 0x0A, 0x1D, 0x56, 0x41, 0x00)
        return data + tail
    }
}
