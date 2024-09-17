package com.udemy.ocrlibrary

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognitionProcessor {
    fun recognizeText(bitmap: Bitmap, onSuccess: (Text) -> Unit, onFailure: (Exception) -> Unit) {


        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onSuccess(visionText)
            }
            .addOnFailureListener { e ->
                onFailure(e)
                Log.e("TextRecognition", "Text recognition failed: ${e.message}", e)
            }
    }
}