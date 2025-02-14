package com.example.schach1337

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.example.schach1337.databinding.GameOverMenuLayoutBinding

class GameOverMenu(context : Context) : Dialog(context, R.style.GameOverMenu) {
    private var binding : GameOverMenuLayoutBinding? = null

    init{
        create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val li = LayoutInflater.from(context)
        binding = GameOverMenuLayoutBinding.inflate(li)
        setContentView(binding!!.root)
    }

    fun setWinnerText(winnerText: String){
        binding!!.winnerText.text = winnerText
    }

    fun setReasonText(reasonText: String){
        binding!!.reasonText.text = reasonText
    }

    fun setRestart(onClick: RestartClick){
        binding!!.restartBtn.setOnClickListener{
            onClick.onRestartClick()
        }
    }

    fun setAnalyze(onClick: AnalyzeClick){
        binding!!.analyzeBtn.setOnClickListener{
            onClick.onAnalyzeClick()
        }
    }

    fun setClose(onClick: CloseClick){
        binding!!.closeBtn.setOnClickListener{
            onClick.onCloseClick()
        }
    }

    interface RestartClick{
        fun onRestartClick()
    }

    interface AnalyzeClick{
        fun onAnalyzeClick()
    }

    interface CloseClick{
        fun onCloseClick()
    }

}