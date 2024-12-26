package com.example.androidca

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URL
import java.util.concurrent.CancellationException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.media.MediaPlayer
import android.view.animation.ScaleAnimation

class FetchActivity : AppCompatActivity() {

    private lateinit var urlEditText: EditText
    private lateinit var fetchButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressTextView: TextView
    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var confirmButton: Button
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var mediaPlayer: MediaPlayer

    private var downloadJob: Job? = null
    private val selectedImages = mutableListOf<String>()
    private val imageUrls = mutableListOf<String>()
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private val imageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "IMAGE_DOWNLOADED" -> {
                    val imageUrl = intent.getStringExtra("imageUrl") ?: return
                    val currentCount = intent.getIntExtra("currentCount", 0)

                    imageUrls.add(imageUrl)
                    progressBar.progress = currentCount
                    progressTextView.text = getString(R.string.downloading_d_of_20_images, currentCount)
                    imageAdapter.notifyItemInserted(imageUrls.size - 1)

                    if (currentCount == 20) {
                        confirmButton.visibility = View.VISIBLE
                    }
                }
                "DOWNLOAD_ERROR" -> {
                    val errorMessage = intent.getStringExtra("error")
                    Toast.makeText(this@FetchActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fetch)

        urlEditText = findViewById(R.id.urlEditText)
        // 设置默认URL
        urlEditText.setText(getString(R.string.default_url))

        fetchButton = findViewById(R.id.fetchButton)
        progressBar = findViewById(R.id.progressBar)
        progressTextView = findViewById(R.id.progressTextView)
        imagesRecyclerView = findViewById(R.id.imagesRecyclerView)
        confirmButton = findViewById(R.id.confirmButton)

        imagesRecyclerView.layoutManager = GridLayoutManager(this, 4)
        imageAdapter = ImageAdapter(imageUrls, selectedImages) { imageUrl ->
            toggleImageSelection(imageUrl)
        }
        imagesRecyclerView.adapter = imageAdapter

        fetchButton.setOnClickListener {
            startImageDownload()
        }

        confirmButton.setOnClickListener {
            if (selectedImages.size == 6) {
                val intent = Intent(this, PlayActivity::class.java)
                intent.putStringArrayListExtra("selectedImages", ArrayList(selectedImages))
                startActivity(intent)
            }
        }

        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        registerReceivers()

        // 初始化音效
        mediaPlayer = MediaPlayer.create(this, R.raw.click_sound)
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction("IMAGE_DOWNLOADED")
            addAction("DOWNLOAD_ERROR")
        }
        localBroadcastManager.registerReceiver(imageReceiver, filter)
    }

    private fun toggleImageSelection(imageUrl: String) {
        if (selectedImages.contains(imageUrl)) {
            selectedImages.remove(imageUrl)
        } else if (selectedImages.size < 6) {
            selectedImages.add(imageUrl)
            // 播放音效
            mediaPlayer.start()
        }
        // 更新confirm按钮状态
        confirmButton.isEnabled = selectedImages.size == 6
        imageAdapter.notifyDataSetChanged()
    }

    private fun startImageDownload() {
        val url = urlEditText.text.toString()
        if (!isValidUrl(url)) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
            return
        }

        downloadJob?.cancel()
        imageUrls.clear()
        selectedImages.clear()
        imageAdapter.notifyDataSetChanged()
        progressBar.progress = 0
        progressTextView.text = getString(R.string.downloading_0_of_20_images)
        confirmButton.isEnabled = false

        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla")
                    .timeout(5000)
                    .get()

                val imageElements = doc.select("img[src~=(?i)\\.(png|jpe?g)]")
                var validImageCount = 0

                for (element in imageElements) {
                    if (validImageCount >= 20) break

                    try {
                        val imageUrl = element.absUrl("src")
                        if (imageUrl.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                val intent = Intent("IMAGE_DOWNLOADED").apply {
                                    putExtra("imageUrl", imageUrl)
                                    putExtra("currentCount", validImageCount + 1)
                                }
                                localBroadcastManager.sendBroadcast(intent)
                            }
                            validImageCount++
                        }
                    } catch (e: Exception) {
                        Log.w("FetchActivity", "Skipping invalid image: ${e.message}")
                        continue
                    }
                }

                if (validImageCount == 0) {
                    withContext(Dispatchers.Main) {
                        val intent = Intent("DOWNLOAD_ERROR").apply {
                            putExtra("error", "No valid images found")
                        }
                        localBroadcastManager.sendBroadcast(intent)
                    }
                }
            } catch (e: CancellationException) {
                Log.d("FetchActivity", getString(R.string.image_download_cancelled))
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val intent = Intent("DOWNLOAD_ERROR").apply {
                        putExtra("error", getString(R.string.error_downloading_images))
                    }
                    localBroadcastManager.sendBroadcast(intent)
                }
                Log.e("FetchActivity", "Error downloading images: ${e.message}")
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel()
        localBroadcastManager.unregisterReceiver(imageReceiver)
        mediaPlayer.release()
    }
}

class ImageAdapter(
    private val imageUrls: MutableList<String>,
    private val selectedImages: MutableList<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.image_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUrl = imageUrls[position]

        // 设置 Glide 的加载超时时间
        val options = RequestOptions()
            .timeout(10000) // 10 秒超时

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .apply(options) // 应用超时设置
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(holder.imageView)

        // 根据选中状态设置视觉效果
        if (selectedImages.contains(imageUrl)) {
            holder.itemView.setBackgroundColor(Color.LTGRAY)
            holder.imageView.scaleX = 0.5f
            holder.imageView.scaleY = 0.5f
        } else {
            holder.itemView.background = null
            holder.imageView.scaleX = 1.0f
            holder.imageView.scaleY = 1.0f
        }

        // 设置点击事件
        holder.itemView.setOnClickListener {
            onItemClick(imageUrl)
            // 点击时添加动画效果
            val isSelected = selectedImages.contains(imageUrl)
            val scaleAnimation = ScaleAnimation(
                if (isSelected) 0.5f else 1.0f,  // 开始大小
                if (isSelected) 1.0f else 0.5f,  // 结束大小
                if (isSelected) 0.5f else 1.0f,  // 开始大小
                if (isSelected) 1.0f else 0.5f,  // 结束大小
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 200
                fillAfter = true
            }
            holder.imageView.startAnimation(scaleAnimation)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}