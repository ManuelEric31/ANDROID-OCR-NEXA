package com.internship.ocrnexa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.internship.ocrnexa.databinding.ActivityCameraBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private var currentImageUri: Uri? = null
    private var camera: androidx.camera.core.Camera? = null
    private var isFlashlightOn: Boolean = false
    private var currentDialog: SweetAlertDialog? = null
    private var isProcessing: Boolean = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("IS_PROCESSING", isProcessing)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState != null) {
            isProcessing = savedInstanceState.getBoolean("IS_PROCESSING")
        }

        if (allPermissionsGranted()) {
            startCameraX()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.apply {
            fabCapture.setOnClickListener {
                takePhoto()
            }

            fabFlashlight.setOnClickListener {
                toggleFlashLight()
            }
            fabGallery.setOnClickListener {
                startGallery()
            }

            ivBackNavs.setOnClickListener {
                val intent = Intent(this@CameraActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }


    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val message = "Photo capture succeeded: $savedUri"
                    showToast(message)
                    Log.d(TAG, message)

                    val croppedBitmap = cropImage(photoFile)
                    val croppedFile = saveCroppedImage(croppedBitmap)
                    processImage(croppedFile.absolutePath, Uri.fromFile(croppedFile).toString())
                }
            }
        )
    }

    private fun cropImage(photoFile: File): Bitmap {
        val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val boxRect = binding.boundingBoxOverlay.getBoxRect()

        val previewAspectRatio = binding.viewFinder.width.toFloat() / binding.viewFinder.height
        val imageAspectRatio = originalBitmap.width.toFloat() / originalBitmap.height

        val (scaleX, scaleY) = if (imageAspectRatio > previewAspectRatio) {
            // Gambar lebih lebar, sesuaikan berdasarkan tinggi
            val scale = originalBitmap.height.toFloat() / binding.viewFinder.height
            Pair(scale, scale)
        } else {
            // Gambar lebih tinggi, sesuaikan berdasarkan lebar
            val scale = originalBitmap.width.toFloat() / binding.viewFinder.width
            Pair(scale, scale)
        }

        val offsetX = (originalBitmap.width - binding.viewFinder.width * scaleX) / 2
        val offsetY = (originalBitmap.height - binding.viewFinder.height * scaleY) / 2

        val left = (boxRect.left * scaleX + offsetX).toInt().coerceIn(0, originalBitmap.width)
        val top = (boxRect.top * scaleY + offsetY).toInt().coerceIn(0, originalBitmap.height)
        val right = (boxRect.right * scaleX + offsetX).toInt().coerceIn(0, originalBitmap.width)
        val bottom = (boxRect.bottom * scaleY + offsetY).toInt().coerceIn(0, originalBitmap.height)

        val width = right - left
        val height = bottom - top

        return Bitmap.createBitmap(originalBitmap, left, top, width, height)
    }

    private fun saveCroppedImage(bitmap: Bitmap): File {
        val file = File(
            externalMediaDirs.firstOrNull(),
            "CROPPED_${
                SimpleDateFormat(
                    FILENAME_FORMAT,
                    Locale.US
                ).format(System.currentTimeMillis())
            }.jpg"
        )
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return file
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun processImage(imagePath: String, uriString: String) {
        if (isProcessing) return

        isProcessing = true

        val bitmap = BitmapFactory.decodeFile(imagePath)

        currentDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
            setTitle("Processing")
            setContentText("Please wait while we process the image")
            progressHelper.barColor = ContextCompat.getColor(this@CameraActivity, R.color.black)
            show()
            setCancelable(false)
        }

        val resizedBitmap = com.udemy.ocrlibrary.ImagePreprocessor.resizeImage(bitmap, 1200, 1200)
        val croppedBitmap = com.udemy.ocrlibrary.ImagePreprocessor.cropDocument(resizedBitmap)
        val rotatedBitmap =
            com.udemy.ocrlibrary.ImagePreprocessor.rotateBitmapIfNeeded(imagePath, croppedBitmap)

        com.udemy.ocrlibrary.ImagePreprocessor.preprocessImageAsync(rotatedBitmap) { preprocessedBitmap ->
            val textRecognitionProcessor = com.udemy.ocrlibrary.TextRecognitionProcessor()
            textRecognitionProcessor.recognizeText(preprocessedBitmap, { visionText ->

                val extractedText =
                    com.udemy.ocrlibrary.ImagePreprocessor.extractLeftToRightText(visionText)
                        .joinToString(" \n")
                        .uppercase()

                val normalizedText =
                    com.udemy.ocrlibrary.TextNormalizer.normalizeText(extractedText.trim())
                Log.d(TAG, "Extracted Text: ${normalizedText.trimIndent()}")

                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("EXTRACTED_TEXT", normalizedText)
                    putExtra("URI", uriString)
                }

                currentDialog?.setTitleText("Success")
                    ?.setContentText("The image has been processed successfully.")
                    ?.setConfirmClickListener {
                        it.dismissWithAnimation()
                        isProcessing = false
                        startActivity(intent)
                    }?.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)

            }, { e ->
                Log.e(TAG, "Text recognition failed: ${e.message}", e)
                currentDialog?.setTitleText("Failed")
                    ?.setContentText("Image processing failed. Please try again.")
                    ?.setConfirmClickListener {
                        it.dismissWithAnimation()
                        isProcessing = false
                    }?.changeAlertType(SweetAlertDialog.ERROR_TYPE)
            })
        }
    }


    private fun startCameraX() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_OFF).build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleFlashLight() {
        if (!isFlashlightOn) {
            turnOnFlashlight()
            binding.fabFlashlight.setImageResource(R.drawable.flashlight_svgrepo_com_yellow)
            showToast(getString(R.string.flashlight_on))
        } else {
            turnOffFlashlight()
            binding.fabFlashlight.setImageResource(R.drawable.flashlight_svgrepo_com)
            showToast(getString(R.string.flashlight_off))
        }
        binding.fabFlashlight.invalidate()
    }

    private fun turnOnFlashlight() {
        camera?.cameraControl?.enableTorch(true)
        isFlashlightOn = true
    }

    private fun turnOffFlashlight() {
        camera?.cameraControl?.enableTorch(false)
        isFlashlightOn = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri

            var filePhoto: File? = null
            val inputStream = this.contentResolver.openInputStream(uri)
            val cursor = this.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst()) {
                    val name = c.getString(nameIndex)
                    inputStream?.let { inputStream ->
                        val file = File(this.cacheDir, name)
                        val os = file.outputStream()
                        os.use {
                            inputStream.copyTo(it)
                        }
                        filePhoto = file
                    }
                }
            }

            if (filePhoto != null) {
                processImage(filePhoto!!.absolutePath, uri.toString())
            }
        } else {
            Log.d(GALLERY, "No media selected")
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCameraX()
            } else {
                showToast(getString(R.string.perm_not_granted))
                finish()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        currentDialog?.dismiss()
        currentDialog = null
    }

    override fun onDestroy() {
        super.onDestroy()
        currentDialog?.dismiss()
        currentDialog = null
    }

    companion object {
        private const val TAG = "CameraX"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val GALLERY = "Photo Picker"
    }

}