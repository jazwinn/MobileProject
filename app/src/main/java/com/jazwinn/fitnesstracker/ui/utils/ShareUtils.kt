package com.jazwinn.fitnesstracker.ui.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    fun shareStats(context: Context, steps: Long, distanceKm: Float, calories: Float) {
        val bitmap = generateStatsImage(steps, distanceKm, calories)
        val uri = saveBitmapToCache(context, bitmap)
        shareImage(context, uri)
    }

    private fun generateStatsImage(steps: Long, distanceKm: Float, calories: Float): Bitmap {
        val width = 600
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply { color = Color.DKGRAY } // Dark Background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.YELLOW
            textSize = 60f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        val centerX = width / 2f
        canvas.drawText("Fitness Tracker Daily", centerX, 80f, titlePaint)
        
        canvas.drawText("Steps: $steps", centerX, 180f, textPaint)
        canvas.drawText("Distance: %.2f km".format(distanceKm), centerX, 240f, textPaint)
        canvas.drawText("Calories: %.0f kcal".format(calories), centerX, 300f, textPaint)

        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
        val imagesFolder = File(context.cacheDir, "images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "shared_stats.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        stream.flush()
        stream.close()
        
        // Authority must match AndroidManifest FileProvider
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Stats via"))
    }
}
