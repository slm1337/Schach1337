package com.example.schach1337

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.schach1337.databinding.PawnPromotionsMenuLayoutBinding
import com.example.schach1337.logic.PieceType

class PawnPromotionsMenu(context : Context) : Dialog(context){
    private var binding : PawnPromotionsMenuLayoutBinding? = null
    private var listener: ((PieceType) -> Unit)? = null

    init {
        create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val li = LayoutInflater.from(context)
        binding = PawnPromotionsMenuLayoutBinding.inflate(li)
        setContentView(binding!!.root)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding!!.promotionsQueen.setOnClickListener { listener?.invoke(PieceType.Queen); dismiss() }
        binding!!.promotionsRook.setOnClickListener { listener?.invoke(PieceType.Rook); dismiss() }
        binding!!.promotionsBishop.setOnClickListener { listener?.invoke(PieceType.Bishop); dismiss() }
        binding!!.promotionsKnight.setOnClickListener { listener?.invoke(PieceType.Knight); dismiss() }
    }

    fun setOnPromotionSelected(listener: (PieceType) -> Unit) {
        this.listener = listener
    }


}