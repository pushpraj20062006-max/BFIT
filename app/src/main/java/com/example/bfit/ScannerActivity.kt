package com.example.bfit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bfit.databinding.ActivityScannerBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

/**
 * ScannerActivity provides two scanning modes:
 * 1. **Barcode Mode** — scans product barcodes to fetch nutrition data from OpenFoodFacts
 * 2. **AI Vision Mode** — captures a photo of food and uses Gemini Vision AI to estimate macros
 */
class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var barcodeScanner: BarcodeScanner
    private var imageCapture: ImageCapture? = null
    private var isAiMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        barcodeScanner = BarcodeScanning.getClient()

        // Close button
        findViewById<Button>(R.id.closeButton).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Mode toggle button
        val modeToggleBtn = findViewById<Button>(R.id.modeToggleBtn)
        val modeHintText = findViewById<TextView>(R.id.modeHintText)

        modeToggleBtn.setOnClickListener {
            isAiMode = !isAiMode
            if (isAiMode) {
                modeToggleBtn.text = getString(R.string.scan_food)
                modeHintText.text = getString(R.string.scanner_ai_hint)
                findViewById<Button>(R.id.captureButton).visibility = View.VISIBLE
            } else {
                modeToggleBtn.text = getString(R.string.ai_scan_food)
                modeHintText.text = getString(R.string.scanner_hint)
                findViewById<Button>(R.id.captureButton).visibility = View.GONE
            }
        }

        // Capture button for AI mode
        val captureButton = findViewById<Button>(R.id.captureButton)
        captureButton.setOnClickListener {
            captureAndAnalyzeFood()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Image capture for AI mode
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isAiMode) {
                            processBarcode(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processBarcode(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes.first()
                        val rawValue = barcode.rawValue
                        Log.d(TAG, "Barcode detected: $rawValue")
                        val intent = Intent().apply {
                            putExtra("barcode", rawValue)
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Barcode scanning failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    /**
     * Captures a photo and sends it to Gemini Vision AI for food analysis.
     * Returns estimated name, calories, protein, carbs, and fats.
     */
    private fun captureAndAnalyzeFood() {
        val imageCapture = imageCapture ?: return

        val loadingIndicator = findViewById<ProgressBar>(R.id.aiLoadingIndicator)
        val captureButton = findViewById<Button>(R.id.captureButton)

        loadingIndicator.visibility = View.VISIBLE
        captureButton.isEnabled = false

        val photoFile = File(cacheDir, "food_capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    if (bitmap != null) {
                        analyzeWithGemini(bitmap)
                    } else {
                        loadingIndicator.visibility = View.GONE
                        captureButton.isEnabled = true
                        Toast.makeText(this@ScannerActivity, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    loadingIndicator.visibility = View.GONE
                    captureButton.isEnabled = true
                    Toast.makeText(this@ScannerActivity, "Photo capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun analyzeWithGemini(bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY
                )

                val prompt = """
                    Analyze this food image. Estimate the nutritional information per serving.
                    Respond ONLY in this exact format (one line each, no extra text):
                    NAME: [food name]
                    CALORIES: [number only]
                    PROTEIN: [number only in grams]
                    CARBS: [number only in grams]
                    FATS: [number only in grams]
                """.trimIndent()

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                val responseText = response.text ?: ""
                Log.d(TAG, "Gemini response: $responseText")
                parseAndReturnResult(responseText)

            } catch (e: Exception) {
                Log.e(TAG, "Gemini analysis failed", e)
                runOnUiThread {
                    findViewById<ProgressBar>(R.id.aiLoadingIndicator).visibility = View.GONE
                    findViewById<Button>(R.id.captureButton).isEnabled = true
                    Toast.makeText(this@ScannerActivity, getString(R.string.ai_analysis_failed) + ": ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parseAndReturnResult(responseText: String) {
        val lines = responseText.lines()
        val name = lines.find { it.startsWith("NAME:", ignoreCase = true) }
            ?.substringAfter(":")?.trim() ?: "Unknown Food"
        val calories = lines.find { it.startsWith("CALORIES:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val protein = lines.find { it.startsWith("PROTEIN:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val carbs = lines.find { it.startsWith("CARBS:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val fats = lines.find { it.startsWith("FATS:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.filter { it.isDigit() }?.toIntOrNull() ?: 0

        val intent = Intent().apply {
            putExtra("ai_food_name", name)
            putExtra("ai_calories", calories)
            putExtra("ai_protein", protein)
            putExtra("ai_carbs", carbs)
            putExtra("ai_fats", fats)
            putExtra("is_ai_result", true)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }

    companion object {
        private const val TAG = "ScannerActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}
