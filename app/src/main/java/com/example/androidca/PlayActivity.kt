package com.example.androidca

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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PlayActivity : AppCompatActivity() {
    private lateinit var  matchCountView:TextView
    private lateinit var  timerView:TextView
    private lateinit var  gameGrid:GridLayout
    private lateinit var adContainer:FrameLayout
    private var matchCount = 0
    private var startTime = 0L
    private var selectImages = listOf<Int>()//图片资源id传入
    private var revealedCards = mutableListOf<View>()
    private var selectedImages = listOf(
        R.drawable.text1,
        R.drawable.text2,
        R.drawable.text4
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        //绑定视图
        matchCountView = findViewById(R.id.mC)
        timerView = findViewById(R.id.timer)
        gameGrid = findViewById(R.id.gG)
        adContainer = findViewById(R.id.aC)
        //计时器启动！
        startTimer()
        //游戏网格初始化
        initGamerGrid()
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
        card.setImageResource(R.drawable.card_back)//刚开始是牌背
        card.setOnClickListener{onCardClicked(card,imageId)}
        return card
    }
    //游戏内部
    private fun onCardClicked(card: ImageView, imageId: Int) {
        if (revealedCards.contains(card)) return // 防止重复点击

        // 翻转卡片
        card.setImageResource(imageId)
        revealedCards.add(card)

        if (revealedCards.size == 2) {
            // 检查是否匹配
            val firstCard = revealedCards[0] as ImageView
            val secondCard = revealedCards[1] as ImageView

            if (firstCard.drawable.constantState == secondCard.drawable.constantState) {
                // 匹配成功
                matchCount++
                matchCountView.text = "Matches: $matchCount"
                revealedCards.clear()
            } else {
                // 延时隐藏
                Handler(Looper.getMainLooper()).postDelayed({
                    firstCard.setImageResource(R.drawable.card_back)
                    secondCard.setImageResource(R.drawable.card_back)
                    revealedCards.clear()
                }, 1000)
            }
        }

        // 检查游戏是否完成
        if (matchCount == 3) {
            onGameComplete()

        }


    }
    //游戏结算
    private fun onGameComplete() {
        println("startTime : $startTime")
        val elapsedTime = System.currentTimeMillis() - startTime
        Toast.makeText(this, "Game Complete! Time: ${elapsedTime / 1000}s", Toast.LENGTH_LONG).show()
        // 保存成绩到后端并跳转到排行榜界面

        val intent = Intent(this,LeaderBoardActivity::class.java)
        intent.putExtra("completionTime", elapsedTime / 1000)
        println("elapsed $elapsedTime ")
        startActivity(intent)
    }


}