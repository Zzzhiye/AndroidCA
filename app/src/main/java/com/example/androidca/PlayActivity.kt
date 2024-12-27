package com.example.androidca

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
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

        for (imageUrl in shuffledImages) {
            val card = createCardView(imageUrl)
            gameGrid.addView(card)
        }
    }

    //创建卡牌
    private fun createCardView(imageUrl: String): View {
        val card = ImageView(this)
        card.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 0
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
        card.setImageResource(R.drawable.back) // 默认显示卡背

        // 使用 tag 保存图片 URL
        card.tag = imageUrl

        card.setOnClickListener {
            onCardClicked(card)
        }
        return card
    }


    //卡片翻转方法
    private fun flipCard(card: ImageView, showFront: Boolean) {
        val animator = ObjectAnimator.ofFloat(card, "rotationY", 0f, 90f)
        animator.duration = 300
        animator.start()
        animator.doOnEnd {
            if (showFront) {
                // 加载正面图片
                val imageUrl = card.tag as String
                Glide.with(this)
                    .load(imageUrl)
                    .into(card)
            } else {
                // 显示卡背
                card.setImageResource(R.drawable.back)
            }

            val reverseAnimator = ObjectAnimator.ofFloat(card, "rotationY", 90f, 0f)
            reverseAnimator.duration = 300
            reverseAnimator.start()
        }
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
            }, 1000)
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
        //关闭游戏画面
        finish()
        val intent = Intent(this,LeaderBoardActivity::class.java)
        intent.putExtra("completionTime", elapsedTime / 1000)
        println("elapsed $elapsedTime ")
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        adManager.stopAds()
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