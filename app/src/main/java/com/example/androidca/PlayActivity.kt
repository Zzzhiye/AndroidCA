package com.example.androidca

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Camera
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.androidca.api.ApiClient
import com.example.androidca.api.RankingRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch


class PlayActivity : AppCompatActivity() {
    private lateinit var  matchCountView:TextView
    private lateinit var  timerView:TextView
    private lateinit var  gameGrid:GridLayout
    private lateinit var adContainer:FrameLayout
    private var matchCount = 0
    private lateinit var adManager: AdManager
    private lateinit var closeButton:ImageButton


    private val matchedCards = mutableSetOf<String>()
    private var firstCard: ImageView? = null
    private var secondCard: ImageView? = null
    private var isProcessing = false

    private var startTime = 0L

    private var revealedCards = mutableListOf<View>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        setDynamicGradientBackground()
        startService(Intent(this, BackgroundMusicService::class.java))
        //Initialise and load ads
        MobileAds.initialize(this) {}

        matchCountView = findViewById(R.id.mC)
        timerView = findViewById(R.id.timer)
        gameGrid = findViewById(R.id.gG)
        adContainer = findViewById(R.id.adContainer)
        closeButton = findViewById(R.id.closeButton)

        //initialise ads
        adManager = AdManager(this, adContainer)
        adManager.initializeAds()

        val selectedImages = intent.getStringArrayListExtra("selectedImages") ?: ArrayList()

        startTimer()
        if (selectedImages.size == 6) {
            initGameGrid(selectedImages)
        } else {
            Toast.makeText(this, "Failed to load images for the game", Toast.LENGTH_SHORT).show()
            finish() // 结束活动，返回上一界面
        }

        closeButton.setBackgroundResource(R.drawable.close)
        closeButton.setOnClickListener {
            val intent = Intent(this, FetchActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
    private fun setDynamicGradientBackground() {
        val layout = findViewById<View>(R.id.main)

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#D8BFD8"), Color.parseColor("#FFFFFF"))
        )
        layout.background = gradient

        val animator = ValueAnimator.ofArgb(Color.parseColor("#D8BFD8"), Color.parseColor("#FFE4E1"))
        animator.duration = 3000
        animator.addUpdateListener { animation ->
            val newColor = animation.animatedValue as Int
            gradient.colors = intArrayOf(newColor, Color.parseColor("#FFFFFF"))
        }

        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = ValueAnimator.INFINITE
        animator.start()
    }

    private fun startTimer(){
        startTime = System.currentTimeMillis()
        val handler = Handler(Looper.getMainLooper())
        handler.post  (object : Runnable{
            override fun run(){
                val elapsedTime = System.currentTimeMillis() - startTime
                val seconds = (elapsedTime/1000)%60
                val minutes = (elapsedTime/1000)/60
                timerView.text = String.format("%02d:%02d",minutes,seconds)
                handler.postDelayed(this,1000)
            }
        })
    }
    private fun initGameGrid(images: List<String>) {
        val shuffledImages = (images + images).shuffled()
        gameGrid.columnCount = 4

        for (i in shuffledImages.indices) {
            val card = createCardView(shuffledImages[i])
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = when (i) {
                    0 -> GridLayout.spec(1, 1f)
                    1 -> GridLayout.spec(2, 1f)
                    2 -> GridLayout.spec(0, 1f)
                    3 -> GridLayout.spec(1, 1f)
                    4 -> GridLayout.spec(2, 1f)
                    5 -> GridLayout.spec(3, 1f)
                    6 -> GridLayout.spec(0, 1f)
                    7 -> GridLayout.spec(1, 1f)
                    8 -> GridLayout.spec(2, 1f)
                    9 -> GridLayout.spec(3, 1f)
                    10 -> GridLayout.spec(1, 1f)
                    11 -> GridLayout.spec(2, 1f)
                    else -> GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }

                rowSpec = when (i) {
                    in 0..1 -> GridLayout.spec(0, 1f)
                    in 2..5 -> GridLayout.spec(1, 1f)
                    in 6..9 -> GridLayout.spec(2, 1f)
                    in 10..11 -> GridLayout.spec(3, 1f)
                    else -> GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }

                if (i in 0..1) {
                    setMargins(16, 64, 16, 16)
                }else if(i in 10..11){
                    setMargins(16, 16, 16, 64)
                } else {
                    setMargins(16, 16, 16, 16)
                }
            }

            card.layoutParams = params

            val rotation = (-15..15).random().toFloat()
            card.rotation = rotation


            gameGrid.addView(card)
        }
    }




    private fun createCardView(imageUrl: String): View {
        val card = ImageView(this)

        card.setImageResource(R.drawable.back)

        card.tag = imageUrl

        card.setOnClickListener {
            onCardClicked(card)
        }
        return card
    }


    private fun flipCard(card: ImageView, showFront: Boolean) {

        val camera = Camera()
        val enlargeXAnimator = ObjectAnimator.ofFloat(card, "scaleX", 1f, 1.5f)
        val enlargeYAnimator = ObjectAnimator.ofFloat(card, "scaleY", 1f, 1.5f)

        val shrinkXAnimator = ObjectAnimator.ofFloat(card, "scaleX", 1.5f, 1f)
        val shrinkYAnimator = ObjectAnimator.ofFloat(card, "scaleY", 1.5f, 1f)

        val rotateToMiddleAnimator = ObjectAnimator.ofFloat(card, "rotationY", 0f, 90f)

        val rotateFromMiddleAnimator = ObjectAnimator.ofFloat(card, "rotationY", 90f, 180f)

        enlargeXAnimator.duration = 300
        enlargeYAnimator.duration = 300
        rotateToMiddleAnimator.duration = 300
        rotateFromMiddleAnimator.duration = 300
        shrinkXAnimator.duration = 300
        shrinkYAnimator.duration = 300

        rotateToMiddleAnimator.doOnEnd {
            if (showFront) {
                val imageUrl = card.tag as String
                Glide.with(this)
                    .load(imageUrl)
                    .into(card)
            } else {
                card.setImageResource(R.drawable.back)
            }
        }

        val firstPhase = AnimatorSet().apply {
            playTogether(enlargeXAnimator, enlargeYAnimator, rotateToMiddleAnimator)
        }

        val secondPhase = AnimatorSet().apply {
            playTogether(rotateFromMiddleAnimator, shrinkXAnimator, shrinkYAnimator)
        }

        firstPhase.doOnEnd {
            secondPhase.start()
        }

        firstPhase.start()
    }



    private fun onCardClicked(card: ImageView) {

        if (isProcessing || card.tag in matchedCards || firstCard == card) return
        playFlipSound()
        flipCard(card, showFront = true)
        if (firstCard == null) {
            firstCard = card
        } else {
            secondCard = card
            checkMatch()
        }
    }

    private fun checkMatch() {
        isProcessing = true

        if (firstCard?.tag == secondCard?.tag) {
            matchCount++
            matchedCards.add(firstCard?.tag as String)
            matchedCards.add(secondCard?.tag as String)
            firstCard = null
            secondCard = null
            isProcessing = false

            matchCountView.text = "Matches: $matchCount"

            if (matchCount == 6) {
                onGameComplete()
            }
        } else {
            Handler().postDelayed({
                flipCard(firstCard!!, showFront = false)
                flipCard(secondCard!!, showFront = false)
                firstCard = null
                secondCard = null
                isProcessing = false
            }, 1500)
        }
    }

    private fun onGameComplete() {
        println("startTime : $startTime")
        val elapsedTime = System.currentTimeMillis() - startTime
        Toast.makeText(this, "Game Complete!, Time: ${elapsedTime / 1000}s", Toast.LENGTH_LONG).show()
        lifecycleScope.launch {
            try {
                val rankingRequest = RankingRequest(
                    userId = getUserIdFromSharedPrefs(),
                    completionTime = formatTimeSpan(elapsedTime / 1000)
                )

                val response = ApiClient.apiService.addRanking(rankingRequest)
                if (response.isSuccessful) {
                    println("Ranking saved successfully!")
                } else {
                    println("Failed to save ranking: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                println("Error saving ranking: ${e.message}")
            }
        }
        stopService(Intent(this, BackgroundMusicService::class.java))
        finish()
        val intent = Intent(this,LeaderBoardActivity::class.java)
        intent.putExtra("completionTime", elapsedTime / 1000)
        println("elapsed $elapsedTime ")
        startActivity(intent)
    }


    override fun onDestroy() {
        adManager.stopAds()
        stopService(Intent(this, BackgroundMusicService::class.java))
        super.onDestroy()
    }

    private fun getUserIdFromSharedPrefs(): Int {
        val sharedPrefs = getSharedPreferences("MemoryGamePrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getInt("userId", -1)
    }

    private fun formatTimeSpan(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun playFlipSound() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.flip_sound)
        mediaPlayer.setOnCompletionListener { mp ->
            mp.release()
        }
        mediaPlayer.start()
    }


}