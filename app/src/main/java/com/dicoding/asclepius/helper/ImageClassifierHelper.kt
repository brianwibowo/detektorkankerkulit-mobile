package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.TensorImage
import com.dicoding.asclepius.ml.CancerClassification

@Suppress("DEPRECATION")
class ImageClassifierHelper(private val context: Context) {
    private var model: CancerClassification? = null

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        model = CancerClassification.newInstance(context)
    }

    fun classifyStaticImage(imageUri: Uri, callback: (List<Pair<String, Float>>?, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = uriToBitmap(imageUri)
            if (bitmap == null) {
                withContext(Dispatchers.Main) {
                    callback(null, "Gagal mengubah URI menjadi bitmap.")
                }
                return@launch
            }
            try {
                val tensorImage = TensorImage.fromBitmap(bitmap)
                val outputs = model?.process(tensorImage)
                val probability = outputs?.probabilityAsCategoryList
                val result = probability?.map { category ->
                    category.label to category.score
                }
                withContext(Dispatchers.Main) {
                    callback(result, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null, "Terjadi kesalahan saat proses analisis: ${e.message}")
                }
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() {
        model?.close()
        model = null
    }
}
