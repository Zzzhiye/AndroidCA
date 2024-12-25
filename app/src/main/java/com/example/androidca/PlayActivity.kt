package com.example.androidca

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds


class PlayActivity : AppCompatActivity() {
    private lateinit var  matchCountView:TextView
    private lateinit var  timerView:TextView
    private lateinit var  gameGrid:GridLayout
    private lateinit var adContainer:FrameLayout
    private var matchCount = 0

    //for ads display usage
    private lateinit var adView: AdView
    private val refreshInterval = 30000L
    private val handler = Handler(Looper.getMainLooper())

    //存放已翻开的卡牌
    private val matchedCards = mutableSetOf<Int>()
    //两张用于比较的牌
    private var firstCard: ImageView? = null
    private var secondCard: ImageView? = null
    //两张牌正在比较中的状态
    private var isProcessing = false

    private var startTime = 0L
    private var selectImages = listOf<Int>()//图片资源id传入
    private var revealedCards = mutableListOf<View>()
    private var selectedImages = listOf(
        R.drawable.br,
        R.drawable.che,
        R.drawable.mc,
        R.drawable.mu,
        R.drawable.ncs,
        R.drawable.w
    )

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

        //initialise ads
        adView = AdView(this)
        adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER)
        adView.adUnitId = "ca-app-pub-6677345918902926/4778437587"
        adContainer.addView(adView)

        //计时器启动！
        startTimer()
        //游戏网格初始化
        initGamerGrid()

        startAdRefreshing()
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
    private fun initGamerGrid(){
        val shuffledImages = (selectedImages + selectedImages).shuffled() //6zu对应

        gameGrid.columnCount = 4 //形成3行4列布局

        for(imageId in shuffledImages){
            val card = createCardView(imageId)
            gameGrid.addView(card)
        }
    }
    //创建卡牌
    private fun createCardView(imageId:Int): View{
        val card = ImageView(this)
        card.layoutParams = GridLayout.LayoutParams().apply{
            width = 0
            height = 0
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED,1f)
            rowSpec = GridLayout.spec(GridLayout.UNDEFINED,1f)
        }
        card.setImageResource(R.drawable.back)//刚开始是牌背

        //后面用来图片配对
        card.tag = imageId

        card.setOnClickListener{onCardClicked(card,imageId)}
        return card
    }

    //卡片翻转方法
    private fun flipCard(card: ImageView, showFront: Boolean) {
        val animator = ObjectAnimator.ofFloat(card, "rotationY", 0f, 90f)
        animator.duration = 300
        animator.start()
        animator.doOnEnd {
            card.setImageResource(if (showFront) card.tag as Int else R.drawable.back)
            val reverseAnimator = ObjectAnimator.ofFloat(card, "rotationY", 90f, 0f)
            reverseAnimator.duration = 300
            reverseAnimator.start()
        }
    }

    //游戏内部
    private fun onCardClicked(card: ImageView, imageId: Int) {
        // 防止重复点击
        // 或正在处理其他翻牌操作时继续点击
        // 或点击翻转后的牌
        if (isProcessing || card.tag in matchedCards || card.tag in matchedCards) return

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
            matchedCards.add(firstCard?.tag as Int)
            matchedCards.add(secondCard?.tag as Int)
            firstCard = null
            secondCard = null
            isProcessing = false

            matchCountView.text = "Matches: $matchCount"

            if (matchCount == selectedImages.size) {
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

        //关闭游戏画面
        finish()
        val intent = Intent(this,LeaderBoardActivity::class.java)
        intent.putExtra("completionTime", elapsedTime / 1000)
        println("elapsed $elapsedTime ")
        startActivity(intent)
    }

    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun startAdRefreshing() {
        loadAd()

        handler.postDelayed(object: Runnable {
            override fun run() {
                loadAd()
                handler.postDelayed(this, refreshInterval)
            }
        }, refreshInterval)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }


}