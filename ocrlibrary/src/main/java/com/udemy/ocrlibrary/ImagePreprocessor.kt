package com.udemy.ocrlibrary

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ImagePreprocessor {
    @RequiresApi(Build.VERSION_CODES.O)
    fun preprocessImageAsync(bitmap: Bitmap, onResult: (Bitmap) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val deskewedBitmap = deskewImage(bitmap)
            val denoisedBitmap = denoiseImage(deskewedBitmap)
            withContext(Dispatchers.Main) {
                onResult(denoisedBitmap)
            }
        }
    }

    private fun deskewImage(bitmap: Bitmap): Bitmap {
        val skewAngle = detectSkewAngle(bitmap)
        val matrix = Matrix()
        matrix.postRotate(-skewAngle.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun detectSkewAngle(bitmap: Bitmap): Double {
        return -3.0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun denoiseImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val denoisedBitmap = Bitmap.createBitmap(width, height, bitmap.config)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val neighbors = mutableListOf<Color>()

                neighbors.add(Color.valueOf(pixel))

                if (x > 0) neighbors.add(Color.valueOf(bitmap.getPixel(x - 1, y)))
                if (x < width - 1) neighbors.add(Color.valueOf(bitmap.getPixel(x + 1, y)))
                if (y > 0) neighbors.add(Color.valueOf(bitmap.getPixel(x, y - 1)))
                if (y < height - 1) neighbors.add(Color.valueOf(bitmap.getPixel(x, y + 1)))

                val avgRed = neighbors.map { it.red() }.average().toFloat()
                val avgGreen = neighbors.map { it.green() }.average().toFloat()
                val avgBlue = neighbors.map { it.blue() }.average().toFloat()

                val denoisedPixel = Color.rgb(
                    (avgRed * 255).toInt(),
                    (avgGreen * 255).toInt(),
                    (avgBlue * 255).toInt()
                )
                denoisedBitmap.setPixel(x, y, denoisedPixel)
            }
        }
        return denoisedBitmap
    }

    fun rotateBitmapIfNeeded(imagePath: String, bitmap: Bitmap): Bitmap {
        val exif = ExifInterface(imagePath)
        val rotationDegrees = when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    fun resizeImage(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val widthImage = bitmap.width
        val heightImage = bitmap.height
        val aspectRatio = widthImage.toFloat() / heightImage.toFloat()

        var newWidthImage = maxWidth
        var newHeightImage = (newWidthImage / aspectRatio).toInt()

        if (newHeightImage > maxHeight) {
            newHeightImage = maxHeight
            newWidthImage = (newHeightImage * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidthImage, newHeightImage, true)
    }

    fun cropDocument(bitmap: Bitmap): Bitmap {
        val grayscale = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(i, j)
                val avg = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                grayscale.setPixel(i, j, Color.rgb(avg, avg, avg))
            }
        }

        val croppedBitmap =
            Bitmap.createBitmap(bitmap, 50, 50, bitmap.width - 100, bitmap.height - 100)
        return croppedBitmap
    }

    fun extractLeftToRightText(visionText: Text): List<String> {
        val horizontalText = mutableListOf<String>()
        val lines = mutableListOf<Text.Line>()

        for (block in visionText.textBlocks) {
            lines.addAll(block.lines)
        }

        lines.sortWith(compareBy({ it.boundingBox?.top }, { it.boundingBox?.left }))

        for (line in lines) {
            val sortedWords = line.elements.sortedBy { it.boundingBox?.left }
            val sortedLineText = sortedWords.joinToString(" ") { it.text }
            horizontalText.add(sortedLineText)
        }

        return horizontalText
    }
}