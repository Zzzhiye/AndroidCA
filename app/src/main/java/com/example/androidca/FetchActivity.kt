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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URL
import java.util.concurrent.CancellationException

class FetchActivity : AppCompatActivity() {

    private lateinit var urlEditText: EditText
    private lateinit var fetchButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressTextView: TextView
    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var selectButton: Button
    private lateinit var imageAdapter: ImageAdapter

    private var downloadJob: Job? = null
    private val selectedImages = mutableListOf<String>()
    private val imageUrls = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fetch)

        urlEditText = findViewById(R.id.urlEditText)
        fetchButton = findViewById(R.id.fetchButton)
        progressBar = findViewById(R.id.progressBar)
        progressTextView = findViewById(R.id.progressTextView)
        imagesRecyclerView = findViewById(R.id.imagesRecyclerView)
        selectButton = findViewById(R.id.selectButton)

        imagesRecyclerView.layoutManager = GridLayoutManager(this, 4)
        imageAdapter = ImageAdapter(imageUrls, selectedImages) { imageUrl ->
            toggleImageSelection(imageUrl)
        }
        imagesRecyclerView.adapter = imageAdapter

        fetchButton.setOnClickListener {
            startImageDownload()
        }

        selectButton.setOnClickListener {
            if (selectedImages.size == 6) {
                val intent = Intent(this, PlayActivity::class.java)
                intent.putStringArrayListExtra("selectedImages", ArrayList(selectedImages))
                startActivity(intent)
            } else {
                Toast.makeText(this, R.string.please_select_6_images, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleImageSelection(imageUrl: String) {
        if (selectedImages.contains(imageUrl)) {
            selectedImages.remove(imageUrl)
        } else if (selectedImages.size < 6) {
            selectedImages.add(imageUrl)
        }
        imageAdapter.notifyDataSetChanged()
    }

    private fun startImageDownload() {
        val url = urlEditText.text.toString()
        if (!isValidUrl(url)) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
            return
        }

        downloadJob?.cancel() // Cancel previous job

        imageUrls.clear()
        selectedImages.clear()
        imageAdapter.notifyDataSetChanged()
        progressBar.progress = 0
        progressTextView.text = getString(R.string.downloading_0_of_20_images) // Reset to 0
        selectButton.visibility = View.GONE

        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = Jsoup.connect(url).userAgent("Mozilla").get()
                val imageElements = doc.select("img[src~=(?i)\\.(png|jpe?g)]")

                for ((index, element) in imageElements.withIndex()) {
                    if (index >= 20) break

                    val imageUrl = element.absUrl("src")
                    imageUrls.add(imageUrl)

                    withContext(Dispatchers.Main) {
                        progressBar.progress = index + 1
                        // Update progress text dynamically
                        progressTextView.text = getString(R.string.downloading_d_of_20_images, index + 1)
                        imageAdapter.notifyItemInserted(index)
                        if (imageUrls.size == 20) {
                            selectButton.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d("FetchActivity", getString(R.string.image_download_cancelled))
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FetchActivity, getString(R.string.error_downloading_images), Toast.LENGTH_SHORT).show()
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

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(holder.imageView)

        if (selectedImages.contains(imageUrl)) {
            holder.itemView.setBackgroundColor(Color.LTGRAY)
        } else {
            holder.itemView.background = null
        }

        holder.itemView.setOnClickListener {
            onItemClick(imageUrl)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}