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
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestOptions
import com.example.androidca.LoginActivity.Companion.SHARED_PREFS_NAME
import com.example.androidca.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URL

class FetchActivity : AppCompatActivity() {

    private lateinit var urlEditText: EditText
    private lateinit var fetchButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressTextView: TextView
    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var confirmButton: Button
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var userProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var rankButton: Button

    private lateinit var sessionManager: SessionManager

    private var downloadJob: Job? = null
    private var processJob: Job? = null
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
                        progressTextView.text =
                            getString(R.string.downloading_d_of_20_images, currentCount)
                        imageAdapter.notifyItemChanged(position)

                        if (currentCount == 20) {
                            confirmButton.visibility = View.VISIBLE
                        }
                    }
                }

                "DOWNLOAD_ERROR" -> {
                    val errorMessage = intent.getStringExtra("error")
                    Toast.makeText(this@FetchActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    // 直接在这里重置状态
                    isFetching = false
                    fetchButton.isEnabled = true
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
        userProfileButton = findViewById(R.id.userProfileButton)
        logoutButton = findViewById(R.id.logoutButton)
        rankButton = findViewById(R.id.rankButton)

        setupRecyclerView()
        setupFetchButton()
        setupConfirmButton()
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
            startImageDownload()
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

    private fun setupBottomButtons() {
        userProfileButton.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            // Clear user session
            sessionManager = SessionManager(this)
            sessionManager.logoutUser()

            // Navigate to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        rankButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
                    val userId = sharedPreferences.getInt("userId", -1)
                    if (userId == -1) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FetchActivity,
                                "Invalid user ID",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }
                    val rankingsResponse = ApiClient.apiService.getUserTopRanking(userId)
                    withContext(Dispatchers.Main) {
                        val time: String = if (rankingsResponse.isSuccessful) {
                            val responseBody = rankingsResponse.body()
                            if (responseBody.isNullOrEmpty()) {
                                "User has no rankings available"
                            } else {
                                responseBody
                            }
                        } else {
                            "Failed to load user rankings"
                        }
                        val intent =
                            Intent(this@FetchActivity, LeaderBoardActivity::class.java).apply {
                                putExtra("completionTime", time)
                            }
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    Log.e("FetchActivity", "Error loading user rankings", e)
                }
            }

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
        val position = imageUrls.indexOf(imageUrl)
        if (position == -1) return
        if (selectedImages.contains(imageUrl)) {
            selectedImages.remove(imageUrl)
        } else if (selectedImages.size < 6) {
            selectedImages.add(imageUrl)
        }
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

        downloadJob?.cancel()
        processJob?.cancel()

        isFetching = false
        selectedImages.clear()
        imageUrls.clear()

        // Update UI state
        progressBar.progress = 0
        progressTextView.text = getString(R.string.downloading_0_of_20_images)
        confirmButton.apply {
            isEnabled = false
            visibility = View.GONE
        }

        // Prepare placeholder images
        repeat(20) {
            imageUrls.add("placeholder")
        }
        imageAdapter.notifyDataSetChanged()

        // Start new download
        isFetching = true
        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get()

                val imageElements = doc.select("img[src]")
                val down_imageUrls = imageElements
                    .map { it.absUrl("src") }
                    .filter { it.isNotEmpty() }
                    .filter { it.endsWith(".jpg") || it.contains(".jpg") }
                    .distinct()
                    .take(20)

                if (down_imageUrls.isNotEmpty()) {
                    processImageUrls(down_imageUrls)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FetchActivity, "No images found", Toast.LENGTH_SHORT)
                            .show()
                        isFetching = false
                        fetchButton.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("FetchActivity", "Error: ${e.message}", e)
                    Toast.makeText(
                        this@FetchActivity,
                        "Error loading page: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    isFetching = false
                    fetchButton.isEnabled = true
                }
            }
        }
    }

    private fun processImageUrls(urls: List<String>) {
        var validImageCount = 0

        processJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                for ((index, imageUrl) in urls.withIndex()) {
                    if (!isActive) {
                        Log.d("FetchActivity", "Process cancelled")
                        return@launch
                    }

                    try {
                        kotlinx.coroutines.delay(300)

                        if (!isActive) {
                            Log.d("FetchActivity", "Process cancelled during delay")
                            return@launch
                        }

                        withContext(Dispatchers.Main) {
                            if (index < imageUrls.size) {
                                imageUrls[index] = imageUrl
                                validImageCount++
                                progressBar.progress = validImageCount
                                progressTextView.text =
                                    getString(R.string.downloading_d_of_20_images, validImageCount)
                                imageAdapter.notifyItemChanged(index)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FetchActivity", "Error processing image: ${e.message}")
                        continue
                    }
                }
                withContext(Dispatchers.Main) {
                    if (validImageCount == 0) {
                        Toast.makeText(
                            this@FetchActivity,
                            "No valid images found",
                            Toast.LENGTH_SHORT
                        ).show()
                        isFetching = false
                        fetchButton.isEnabled = true
                    } else {
                        confirmButton.visibility = View.VISIBLE
                        imageAdapter.isClickable = true
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
        processJob?.cancel()
        localBroadcastManager.unregisterReceiver(imageReceiver)
    }
}

class ImageAdapter(
    private val imageUrls: MutableList<String>,
    private val selectedImages: MutableList<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    var isClickable: Boolean = false

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
            .error(R.drawable.error_image)

        if (imageUrl == "placeholder") {
            Glide.with(holder.itemView.context)
                .load(R.drawable.placeholder)
                .apply(options)
                .into(holder.imageView)
        } else {

            val glideUrl = GlideUrl(
                imageUrl,
                LazyHeaders.Builder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
            )
            Glide.with(holder.itemView.context)
                .load(glideUrl)
                .apply(options)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .into(holder.imageView)
        }
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
            if (isClickable) {
                onItemClick(imageUrl)
            }
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}