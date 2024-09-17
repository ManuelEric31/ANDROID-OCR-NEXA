package com.internship.ocrnexa

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.internship.ocrnexa.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extractedTextResult = intent.getStringExtra(EXTRA_EXTRACTED_TEXT)
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)


        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri))
            binding.imageViewResult.setImageBitmap(bitmap)
        }

        if (extractedTextResult != null) {
            binding.edNIK.setText(extractValueAfterKeyword(extractedTextResult, "NIK"))
            binding.edName.setText(extractValueAfterKeyword(extractedTextResult, "NAMA"))
            binding.edDOB.setText(
                extractValueAfterKeyword(
                    extractedTextResult,
                    "TEMPAT / TGL LAHIR"
                )
            )
            binding.edGender.setText(extractValueAfterKeyword(extractedTextResult, "JENIS KELAMIN"))
            binding.edBloodCategory.setText(
                extractValueAfterKeyword(
                    extractedTextResult,
                    "GOL DARAH"
                )
            )
            binding.edAddress.setText(extractValueAfterKeyword(extractedTextResult, "ALAMAT"))
            binding.edRTRW.setText(extractValueAfterKeyword(extractedTextResult, "RT / RW"))
            binding.edWard.setText(extractValueAfterKeyword(extractedTextResult, "KEL / DESA"))
            binding.edSubdistrict.setText(
                extractValueAfterKeyword(
                    extractedTextResult,
                    "KECAMATAN"
                )
            )
            binding.edReligion.setText(extractValueAfterKeyword(extractedTextResult, "AGAMA"))
            binding.edMarriageStatus.setText(
                extractValueAfterKeyword(
                    extractedTextResult,
                    "STATUS PERKAWINAN"
                )
            )
            binding.edJob.setText(extractValueAfterKeyword(extractedTextResult, "PEKERJAAN"))
            binding.edCitizen.setText(
                extractValueAfterKeyword(
                    extractedTextResult,
                    "KEWARGANEGARAAN"
                )
            )
            binding.edValidUntil.setText(
                extractValueAfterKeyword(
                    extractedTextResult,
                    "BERLAKU HINGGA"
                )
            )
        }
    }

    private fun extractValueAfterKeyword(text: String, keyword: String): String {
        val lines = text.split("\n")

        for (i in lines.indices) {
            if (lines[i].contains(keyword, ignoreCase = true)) {
                val keywordIndex = lines[i].indexOf(keyword, ignoreCase = true)
                val valueInSameLine = lines[i].substring(keywordIndex + keyword.length).trim()
                if (valueInSameLine.isNotEmpty()) {
                    return valueInSameLine
                }
                if (i + 1 < lines.size) {
                    val valueInNextLine = lines[i + 1].trim()
                    if (valueInNextLine.isNotEmpty()) {
                        return valueInNextLine
                    }
                }
            }
        }
        return ""
    }

    companion object {
        const val EXTRA_EXTRACTED_TEXT = "EXTRACTED_TEXT"
        const val EXTRA_IMAGE_URI = "URI"
    }
}
