package com.example.englishtapreader

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.englishtapreader.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var wordList = mutableListOf<WordInfo>()
    private var currentBitmap: Bitmap? = null
    private var scaleFactor = 1.0f
    private var offsetX = 0f
    private var offsetY = 0f
    private var tts: TextToSpeech? = null
    private val BAIDU_APP_ID = "20260803002658335"
    private val BAIDU_KEY = "YBse1aPTrGHetn8aRiU8"
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }

        val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) processImage(bitmap)
        }
        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { processImage(MediaStore.Images.Media.getBitmap(contentResolver, it)) }
        }

        binding.btnCapture.setOnClickListener { takePictureLauncher.launch(null) }
        binding.btnGallery.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.highlightView.onWordClickListener = { word ->
            speakWord(word.text)
            fetchTranslation(word.text) { translation ->
                runOnUiThread { showTranslationDialog(word.text, translation) }
            }
        }
    }

    private fun processImage(bitmap: Bitmap) {
        currentBitmap = bitmap
        binding.imageView.setImageBitmap(bitmap)
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { visionText ->
                wordList.clear()
                for (block in visionText.textBlocks)
                    for (line in block.lines)
                        for (element in line.elements)
                            wordList.add(WordInfo(
                                text = element.text,
                                boundingBox = element.boundingBox,
                                cornerPoints = element.cornerPoints
                            ))
                updateOverlay()
            }
            .addOnFailureListener { Toast.makeText(this, "识别失败: ${it.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun updateOverlay() {
        binding.imageView.imageMatrix.let {
            val v = FloatArray(9); it.getValues(v)
            scaleFactor = v[Matrix.MSCALE_X]; offsetX = v[Matrix.MTRANS_X]; offsetY = v[Matrix.MTRANS_Y]
        }
        binding.highlightView.setWords(wordList, scaleFactor, offsetX, offsetY)
    }

    private fun speakWord(text: String) = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)

    private fun fetchTranslation(word: String, callback: (String) -> Unit) {
        val salt = UUID.randomUUID().toString()
        val sign = md5(BAIDU_APP_ID + word + salt + BAIDU_KEY)
        val url = "https://fanyi-api.baidu.com/api/trans/vip/translate?q=$word&from=en&to=zh&appid=$BAIDU_APP_ID&salt=$salt&sign=$sign"
        client.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback("翻译失败") }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string())
                    callback(json.getJSONArray("trans_result").getJSONObject(0).getString("dst"))
                } catch (e: Exception) { callback("翻译出错") }
            }
        })
    }

    private fun showTranslationDialog(word: String, translation: String) {
        AlertDialog.Builder(this).setTitle(word).setMessage(translation).setPositiveButton("关闭", null).show()
    }

    private fun md5(str: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(str.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); super.onDestroy()
    }
}
