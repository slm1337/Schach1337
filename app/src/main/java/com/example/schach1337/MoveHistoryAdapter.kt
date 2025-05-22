package com.example.schach1337

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MoveAdapter(
    private var selectedIndex: Int = -1
) : RecyclerView.Adapter<MoveAdapter.MoveViewHolder>() {

    private val moves: MutableList<String> = mutableListOf()
    private val analyses: MutableList<MoveAnalysis?> = mutableListOf()

    class MoveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.moveText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_move, parent, false)
        return MoveViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
        holder.textView.text = moves[position]

        holder.textView.setBackgroundColor(
            if (position == selectedIndex) Color.YELLOW else Color.TRANSPARENT
        )

        val analysis = analyses.getOrNull(position + 1)
        val borderDrawableId = when (analysis?.category) {
            MoveCategory.BEST -> R.drawable.best_border
            MoveCategory.GOOD -> R.drawable.good_border
            MoveCategory.INACCURACY -> R.drawable.inaccuracy_border
            MoveCategory.MISTAKE -> R.drawable.mistake_border
            MoveCategory.BLUNDER -> R.drawable.blunder_border
            else -> 0
        }
        holder.textView.background = if (borderDrawableId != 0) {
            ContextCompat.getDrawable(holder.textView.context, borderDrawableId)
        } else {
            null
        }

        if (position == selectedIndex) {
            holder.textView.setBackgroundColor(Color.YELLOW)
        }
    }

    override fun getItemCount(): Int = moves.size

    fun setSelectedIndex(index: Int) {
        val oldIndex = selectedIndex
        selectedIndex = index
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        notifyItemChanged(selectedIndex)
    }

    fun updateMoves(newMoves: MutableList<String>, newAnalyses: MutableList<MoveAnalysis?>, newSelectedIndex: Int) {
        moves.clear()
        moves.addAll(newMoves)
        analyses.clear()
        analyses.addAll(newAnalyses)
        selectedIndex = newSelectedIndex
        notifyDataSetChanged()
    }
}