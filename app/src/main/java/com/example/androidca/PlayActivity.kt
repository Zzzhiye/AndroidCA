package com.example.androidca

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Camera
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
import androidx.cardview.widget.CardView
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


    //存放已翻开的卡牌
    private val matchedCards = mutableSetOf<String>()
    //两张用于比较的牌
    private var firstCard: ImageView? = null
    private var secondCard: ImageView? = null
    //两张牌正在比较中的状态
    private var isProcessing = false

    private var startTime = 0L
    //private var selectImages = listOf<Int>()//图片资源id传入
    private var revealedCards = mutableListOf<View>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        setDynamicGradientBackground()
        startService(Intent(this, BackgroundMusicService::class.java))
        //Initialise and load ads
        MobileAds.initialize(this) {}

        //绑定视图
        matchCountView = findViewById(R.id.mC)
        timerView = findViewById(R.id.timer)
        gameGrid = findViewById(R.id.gG)
        adContainer = findViewById(R.id.adContainer)
        closeButton = findViewById(R.id.closeButton)

        //initialise ads
        adManager = AdManager(this, adContainer)
        adManager.initializeAds()

        val selectedImages = intent.getStringArrayListExtra("selectedImages") ?: ArrayList()

        //计时器启动！
        startTimer()
        //游戏网格初始化
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
        animator.duration = 3000 // 动画持续时间
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
    //游戏卡牌初始化
    private fun initGameGrid(images: List<String>) {
        val shuffledImages = (images + images).shuffled() // 双份图片并随机打乱
        gameGrid.columnCount = 4 // 设置网格布局为 4 列

        for (i in shuffledImages.indices) {
            val card = createCardView(shuffledImages[i])
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = when (i) {
                    0 -> GridLayout.spec(1, 1f) // 第一行第一个卡片，位于第二列
                    1 -> GridLayout.spec(2, 1f) // 第一行第二个卡片，位于第三列
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
                    in 0..1 -> GridLayout.spec(0, 1f) // 第一行
                    in 2..5 -> GridLayout.spec(1, 1f) // 第二行
                    in 6..9 -> GridLayout.spec(2, 1f) // 第三行
                    in 10..11 -> GridLayout.spec(3, 1f) // 第四行
                    else -> GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }

                // 为第一行的卡片增加顶部间距
                if (i in 0..1) {
                    setMargins(16, 64, 16, 16) // 增加顶部间距为 64
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




    //创建卡牌
    private fun createCardView(imageUrl: String): View {
        val card = ImageView(this)

        card.setImageResource(R.drawable.card_back) // 默认显示卡背

        // 使用 tag 保存图片 URL
        card.tag = imageUrl

        card.setOnClickListener {
            onCardClicked(card)
        }
        return card
    }


    //卡片翻转方法
    private fun flipCard(card: ImageView, showFront: Boolean) {

       val camera = Camera()
        // 放大动画
        val enlargeXAnimator = ObjectAnimator.ofFloat(card, "scaleX", 1f, 1.5f)
        val enlargeYAnimator = ObjectAnimator.ofFloat(card, "scaleY", 1f, 1.5f)
        // 缩小动画
        val shrinkXAnimator = ObjectAnimator.ofFloat(card, "scaleX", 1.5f, 1f)
        val shrinkYAnimator = ObjectAnimator.ofFloat(card, "scaleY", 1.5f, 1f)
        // 旋转动画：从0°到90°
        val rotateToMiddleAnimator = ObjectAnimator.ofFloat(card, "rotationY", 0f, 90f)
        // 旋转动画：从90°到0°
        val rotateFromMiddleAnimator = ObjectAnimator.ofFloat(card, "rotationY", 90f, 180f)

        enlargeXAnimator.duration = 300
        enlargeYAnimator.duration = 300
        rotateToMiddleAnimator.duration = 300
        rotateFromMiddleAnimator.duration = 300
        shrinkXAnimator.duration = 300
        shrinkYAnimator.duration = 300

        // 中间状态切换图片
        rotateToMiddleAnimator.doOnEnd {
            if (showFront) {
                // 显示正面图片
                val imageUrl = card.tag as String
                Glide.with(this)
                    .load(imageUrl)
                    .override(593,851)
                    .centerCrop()
                    .into(card)
            } else {
                // 显示卡背
                card.setImageResource(R.drawable.card_back)
            }
        }

        // 动画序列
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



    //游戏内部
    private fun onCardClicked(card: ImageView) {
        // 防止重复点击
        // 或正在处理其他翻牌操作时继续点击
        // 或点击翻转后的牌
        if (isProcessing || card.tag in matchedCards || firstCard == card) return

        // 翻转卡片
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

    //游戏结算
    private fun onGameComplete() {
        println("startTime : $startTime")
        val elapsedTime = System.currentTimeMillis() - startTime
        Toast.makeText(this, "Game Complete!, Time: ${elapsedTime / 1000}s", Toast.LENGTH_LONG).show()
        // 保存成绩到后端并跳转到排行榜界面
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
        //关闭游戏画面
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
        return sharedPrefs.getInt("userId", -1) // 返回 -1 表示未找到
    }

    private fun formatTimeSpan(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }


}