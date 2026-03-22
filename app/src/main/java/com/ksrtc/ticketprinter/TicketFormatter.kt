package com.ksrtc.ticketprinter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketFormatter {

    // KSRTC Ticket Rendering Logic
    fun createTicketBitmap(
        ticketNo: String,
        fromStopEN: String,
        fromStopKA: String,
        toStopEN: String,
        toStopKA: String,
        adults: Int,
        childs: Int,
        totalFare: Double,
        rrn: String
    ): Bitmap {
        // Physical ticket is approx 384 pixels wide for a 58mm printer (usually 8 dots/mm)
        val width = 384
        val height = 600 // We will crop this later or draw dynamically
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        var currentY = 40f

        // 1. Header (Kannada)
        // Note: For real environment, you need a custom Typeface for accurate Kannada rendering
        paint.textSize = 32f
        canvas.drawText("ಕ. ರಾ. ರ. ಸಾ. ನಿಗಮ", width / 2f, currentY, paint)
        currentY += 40f
        
        paint.textSize = 24f
        canvas.drawText("ವಿಜಯನಗರ", width / 2f, currentY, paint)
        currentY += 50f

        // 2. Ticket No & Date
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 20f
        
        val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        
        canvas.drawText("No: $ticketNo  $dateString", 20f, currentY, paint)
        currentY += 30f

        // 3. Service Type
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("CITY_ORD KA09-F5551", width / 2f, currentY, paint)
        currentY += 60f

        // 4. Route (Kannada then English)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 26f
        canvas.drawText("$fromStopKA -- $toStopKA", width / 2f, currentY, paint)
        currentY += 35f
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 22f
        canvas.drawText("$fromStopEN -- $toStopEN", width / 2f, currentY, paint)
        currentY += 60f

        // 5. Passenger Breakdown
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 22f
        
        if (adults > 0) {
            val adultFare = totalFare / (adults + (childs * 0.5)) // Example logic
            canvas.drawText("ವಯಸ್ಕರು $adults * $adultFare = ₹ ${adultFare * adults}", 20f, currentY, paint)
            currentY += 35f
        }
        
        if (childs > 0) {
            val childFare = (totalFare / (adults + (childs * 0.5))) * 0.5 // Example logic
            canvas.drawText("ಮಕ್ಕಳು $childs * $childFare = ₹ ${childFare * childs}", 20f, currentY, paint)
            currentY += 35f
        }

        // 6. RRN & Total
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("RRN No. $rrn", width / 2f, currentY, paint)
        currentY += 45f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 30f
        canvas.drawText("ಮೊತ್ತ: ರೂ = $totalFare", width / 2f, currentY, paint)
        currentY += 60f

        // 7. Footer
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 16f
        canvas.drawText("Visit - KSRTC.KARNATAKA.GOV.IN", width / 2f, currentY, paint)

        // Return a cropped bitmap exactly to the drawn height to save paper
        return Bitmap.createBitmap(bitmap, 0, 0, width, (currentY + 20).toInt())
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
