package com.example.schach1337

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MoveAdapter(
    private var selectedIndex: Int = -1
) : RecyclerView.Adapter<MoveAdapter.MoveViewHolder>() {

    private val items: MutableList<String> = mutableListOf()

    class MoveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.moveText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_move, parent, false)
        return MoveViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
        holder.textView.text = items[position]
        holder.textView.setBackgroundColor(
            if (position == selectedIndex) Color.YELLOW else Color.TRANSPARENT
        )
    }

    override fun getItemCount(): Int = items.size

    fun setSelectedIndex(index: Int) {
        val oldIndex = selectedIndex
        selectedIndex = index
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        notifyItemChanged(selectedIndex)
    }

    fun getSelectedIndex(): Int {
        return selectedIndex
    }

    fun updateMoves(newMoves: MutableList<String>, newSelectedIndex: Int) {
        items.clear()
        items.addAll(newMoves)
        selectedIndex = newSelectedIndex
        notifyDataSetChanged()
    }
}