package com.dicoding.asclepius.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.R
import com.dicoding.asclepius.database.HistoryAnalyze
import com.dicoding.asclepius.database.HistoryAnalyzeRoom
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.dicoding.asclepius.repository.HistoryAnalyzeRepository
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentImageUri: Uri? = null
    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private lateinit var historyAnalyzeRepository: HistoryAnalyzeRepository
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var mainViewModel: MainViewModel

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            mainViewModel.setImageUri(uri)
            showImage()
        } else {
            showToast("Gagal memilih gambar")
        }
    }

    private val uCropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    val resultUri = UCrop.getOutput(result.data!!)
                    if (resultUri != null) {
                        showCroppedImage(resultUri)
                    } else {
                        showToast("Gagal mendapatkan hasil cropping")
                    }
                }

                RESULT_CANCELED -> {
                    currentImageUri?.let {
                        binding.previewImageView.setImageURI(it)
                        moveToResult("Proses cropping dibatalkan")
                    } ?: showToast("Proses cropping dibatalkan dan tidak ada gambar yang dipilih")
                }


                UCrop.RESULT_ERROR -> {
                    val cropError = UCrop.getError(result.data!!)
                    showToast("Error cropping: ${cropError?.message}")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = findViewById(R.id.toolbarHome)
        setSupportActionBar(toolbar)

        val historyAnalyzeDao = HistoryAnalyzeRoom.getDatabase(application).historyAnalyzeDao()

        historyAnalyzeRepository = HistoryAnalyzeRepository(historyAnalyzeDao)

        val viewModelFactory = ViewModelFactory(historyAnalyzeRepository)
        historyViewModel = ViewModelProvider(this, viewModelFactory)[HistoryViewModel::class.java]

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        imageClassifierHelper = ImageClassifierHelper(this)

        mainViewModel.currentImageUri.observe(this) { uri ->
            if (uri != null) {
                binding.previewImageView.setImageURI(uri)
            }
        }

        binding.galleryButton.setOnClickListener {
            startGallery()
        }

        binding.analyzeButton.setOnClickListener {
            analyzeImage()
        }

        binding.historyButton.setOnClickListener {
            moveToHistory()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_view_news -> {
                val intent = Intent(this, NewsActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showImage() {
        mainViewModel.currentImageUri.value?.let { uri ->
            val destinationUri = Uri.fromFile(File(cacheDir, "${System.currentTimeMillis()}.jpg"))
            val uCropIntent = UCrop.of(uri, destinationUri)
                .withAspectRatio(16f, 9f)
                .withMaxResultSize(1080, 1920)

            uCropLauncher.launch(uCropIntent.getIntent(this))
        } ?: showToast("Tidak ada gambar yang dipilih")
    }

    private fun showCroppedImage(uri: Uri) {
        binding.previewImageView.setImageURI(uri)
        currentImageUri = uri
    }

    private fun analyzeImage() {
        currentImageUri?.let { uri ->
            lifecycleScope.launch {
                imageClassifierHelper.classifyStaticImage(uri) { results, errorMessage ->
                    if (errorMessage != null) {
                        showToast("Error: $errorMessage")
                    } else if (!results.isNullOrEmpty()) {
                        val bestResult = results.maxByOrNull { it.second }
                        bestResult?.let { (label, score) ->
                            val resultText = "$label, ${(score * 100).toInt()}%"
                            saveToHistory(label, score)
                            moveToResult(resultText)
                        } ?: showToast("Gagal mengklasifikasikan gambar")
                    } else {
                        showToast("Gagal mengklasifikasikan gambar")
                    }
                }
            }
        } ?: showToast("Tidak ada gambar yang dipilih")
    }

    private fun saveToHistory(label: String, score: Float) {
        val imageUriString = currentImageUri?.toString() ?: ""
        val historyAnalyze = HistoryAnalyze(
            classificationLabel = label,
            confidenceScore = score,
            imageUri = imageUriString
        )

        lifecycleScope.launch {
            try {
                historyAnalyzeRepository.insert(historyAnalyze)
                showToast("Hasil analisis disimpan ke History")
            } catch (e: Exception) {
                showToast("Gagal menyimpan hasil: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageClassifierHelper.close()
    }

    private fun moveToResult(resultText: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("classification_result", resultText)
            putExtra("image_uri", currentImageUri.toString())
        }
        startActivity(intent)
    }

    private fun moveToHistory() {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
