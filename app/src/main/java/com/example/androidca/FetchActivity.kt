package com.example.androidca

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jsoup.Jsoup
import java.net.URL
import java.util.concurrent.CancellationException

class FetchActivity : AppCompatActivity() {

    private lateinit var urlEditText: EditText
    private lateinit var fetchButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressTextView: TextView
    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var confirmButton: Button
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var cancelButton: Button
    private lateinit var userProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var rankButton: Button

    private var downloadJob: Job? = null
    private val selectedImages = mutableListOf<String>()
    private val imageUrls = mutableListOf<String>()
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var isFetching: Boolean = false

    private val imageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isFinishing || isDestroyed) return
            when (intent?.action) {
                "IMAGE_DOWNLOADED" -> {
                    val imageUrl = intent.getStringExtra("imageUrl") ?: return
                    val currentCount = intent.getIntExtra("currentCount", 0)
                    val position = currentCount - 1

                    if (position >= 0 && position < imageUrls.size) {
                        imageUrls[position] = imageUrl
                        progressBar.progress = currentCount
                        progressTextView.text = getString(R.string.downloading_d_of_20_images, currentCount)
                        imageAdapter.notifyItemChanged(position)

                        if (currentCount == 20) {
                            confirmButton.visibility = View.VISIBLE
                        }
                    }
                }
                "DOWNLOAD_ERROR" -> {
                    val errorMessage = intent.getStringExtra("error")
                    Toast.makeText(this@FetchActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    resetFetchState()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fetch)

        urlEditText = findViewById(R.id.urlEditText)
        urlEditText.setText(getString(R.string.default_url))
        fetchButton = findViewById(R.id.fetchButton)
        progressBar = findViewById(R.id.progressBar)
        progressTextView = findViewById(R.id.progressTextView)
        imagesRecyclerView = findViewById(R.id.imagesRecyclerView)
        confirmButton = findViewById(R.id.confirmButton)
        cancelButton = findViewById(R.id.cancelButton)
        userProfileButton = findViewById(R.id.userProfileButton)
        logoutButton = findViewById(R.id.logoutButton)
        rankButton = findViewById(R.id.rankButton)

        setupRecyclerView()
        setupFetchButton()
        setupConfirmButton()
        setupCancelButton()
        setupBottomButtons()

        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        registerReceivers()
    }

    private fun setupRecyclerView() {
        imagesRecyclerView.layoutManager = GridLayoutManager(this, 4)
        imagesRecyclerView.itemAnimator = null
        imageAdapter = ImageAdapter(imageUrls, selectedImages) { imageUrl ->
            toggleImageSelection(imageUrl)
        }
        imagesRecyclerView.adapter = imageAdapter
    }

    private fun setupFetchButton() {
        urlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                fetchButton.isEnabled = !s.isNullOrEmpty()
            }
        })

        fetchButton.setOnClickListener {
            if (!isFetching) {
                startImageDownload()
            }
        }
    }

    private fun setupConfirmButton() {
        confirmButton.setOnClickListener {
            if (selectedImages.size == 6) {
                val intent = Intent(this, PlayActivity::class.java)
                intent.putStringArrayListExtra("selectedImages", ArrayList(selectedImages))
                startActivity(intent)
            }
        }
    }

    private fun setupCancelButton() {
        cancelButton.setOnClickListener {
            cancelDownload()
        }
    }

    private fun setupBottomButtons() {
        userProfileButton.setOnClickListener {

        }

        logoutButton.setOnClickListener {
            // Clear user session
            getSharedPreferences(LoginActivity.SHARED_PREFS_NAME, MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            
            // Navigate to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        rankButton.setOnClickListener {
            startActivity(Intent(this, LeaderBoardActivity::class.java))
        }
    }

    private fun cancelDownload() {
        downloadJob?.cancel()
        isFetching = false
        CoroutineScope(Dispatchers.Main).launch {
            cancelButton.visibility = View.GONE
            fetchButton.isEnabled = true
            Toast.makeText(this@FetchActivity, getString(R.string.image_download_cancelled), Toast.LENGTH_SHORT).show()
        }
        resetFetchState()
    }

    private fun resetFetchState() {
        isFetching = false
        CoroutineScope(Dispatchers.Main).launch {
            cancelButton.visibility = View.GONE
            fetchButton.isEnabled = true
            clearDownloadState()
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction("IMAGE_DOWNLOADED")
            addAction("DOWNLOAD_ERROR")
        }
        localBroadcastManager.registerReceiver(imageReceiver, filter)
    }

    private fun toggleImageSelection(imageUrl: String) {
        val position = imageUrls.indexOf(imageUrl) // 获取 position
        if (position == -1) return
        if (selectedImages.contains(imageUrl)) {
            selectedImages.remove(imageUrl)
        } else if (selectedImages.size < 6) {
            selectedImages.add(imageUrl)
        }
        // 更新确认按钮状态
        if (selectedImages.size == 6) {
            confirmButton.visibility = View.VISIBLE
            confirmButton.isEnabled = true
        } else {
            confirmButton.visibility = View.GONE
            confirmButton.isEnabled = false
        }
        imageAdapter.notifyItemChanged(position)
    }

    private fun startImageDownload() {
        val url = urlEditText.text.toString()
        if (!isValidUrl(url)) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
            return
        }

        isFetching = true
        fetchButton.isEnabled = false
        cancelButton.visibility = View.VISIBLE
        clearDownloadState()
        preloadPlaceholders()

        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get()

                val imageElements = doc.select("img[src]")
                val imageUrls = imageElements
                    .map { it.absUrl("src") }
                    .filter { it.isNotEmpty() }
                    .distinct()

                if (imageUrls.isNotEmpty()) {
                    processImageUrls(imageUrls)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FetchActivity, "No images found", Toast.LENGTH_SHORT).show()
                        resetFetchState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("FetchActivity", "Error: ${e.message}", e)
                    Toast.makeText(this@FetchActivity, "Error loading page: ${e.message}", Toast.LENGTH_SHORT).show()
                    resetFetchState()
                }
            }
        }
    }

    private fun processImageUrls(urls: List<String>) {
        var validImageCount = 0
        var currentPosition = 0

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (imageUrl in urls) {
                    if (!isActive) return@launch
                    if (!isValidImageUrl(imageUrl)) continue
                    try {
                
                        kotlinx.coroutines.delay(300)
                        withContext(Dispatchers.Main) {
                            imageUrls[currentPosition] = imageUrl
                            progressBar.progress = validImageCount + 1
                            progressTextView.text = getString(R.string.downloading_d_of_20_images, validImageCount + 1)
                            imageAdapter.notifyItemChanged(currentPosition)

                            validImageCount++
                            currentPosition++
                            if (validImageCount >= 20) return@withContext
                        }
                    } catch (e: Exception) {
                        // 跳过无法识别或加载的图，不占用位置
                        continue
                    }
                }
                withContext(Dispatchers.Main) {
                    if (validImageCount == 0) {
                        Toast.makeText(this@FetchActivity, "No valid images found", Toast.LENGTH_SHORT).show()
                        resetFetchState()
                    } else {
                        // ...existing logic...
                        confirmButton.visibility = View.VISIBLE
                        if (validImageCount < 20) {
                            Toast.makeText(
                                this@FetchActivity,
                                getString(R.string.download_incomplete, validImageCount),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        fetchButton.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                // ...existing error handling...
            }
        }
    }

    private fun isValidImageUrl(url: String): Boolean {
        val exts = listOf(".jpg", ".jpeg", ".png")
        val keywords = listOf("image", "photo", "picture", "/img/", "/images/", "imgur","/photos/")
        val lower = url.lowercase()

        return exts.any { lower.endsWith(it) } || keywords.any { lower.contains(it) }
    }

    private fun preloadPlaceholders() {
        imageUrls.clear()
        repeat(20) {
            imageUrls.add("placeholder")
        }
        imageAdapter.notifyDataSetChanged()
    }

    private fun clearDownloadState() {
        imageUrls.clear()
        selectedImages.clear()
        imageAdapter.notifyDataSetChanged()
        progressBar.progress = 0
        progressTextView.text = getString(R.string.downloading_0_of_20_images)
        confirmButton.apply {
            isEnabled = false
            visibility = View.GONE
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

        val options = RequestOptions()
            .timeout(10000)
            .centerCrop() .error(R.drawable.error_image)

        if (imageUrl == "placeholder") {
            Glide.with(holder.itemView.context)
                .load(R.drawable.close)
                .apply(options)
                .into(holder.imageView)
        } else {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .apply(options)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .into(holder.imageView)
        }
        // 设置选中状态的视觉效果
        if (selectedImages.contains(imageUrl)) {
            holder.itemView.setBackgroundColor(Color.LTGRAY)
            holder.imageView.scaleX = 0.7f
            holder.imageView.scaleY = 0.7f
        } else {
            holder.itemView.background = null
            holder.imageView.scaleX = 1.0f
            holder.imageView.scaleY = 1.0f
        }

        holder.itemView.setOnClickListener {
            onItemClick(imageUrl)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}
