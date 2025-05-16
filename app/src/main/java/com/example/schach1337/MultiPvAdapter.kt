package com.example.schach1337

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MultiPvAdapter : RecyclerView.Adapter<MultiPvAdapter.MultiPvViewHolder>() {

    private val multipvList = mutableListOf<MultipvInfo>()

    fun updateData(newList: List<MultipvInfo>) {
        multipvList.clear()
        multipvList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiPvViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_multipv, parent, false)
        return MultiPvViewHolder(view)
    }

    override fun onBindViewHolder(holder: MultiPvViewHolder, position: Int) {
        val info = multipvList[position]
        holder.bind(info)
    }

    override fun getItemCount(): Int = multipvList.size

    class MultiPvViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val depthText: TextView = view.findViewById(R.id.textDepth)
        private val evalText: TextView = view.findViewById(R.id.textEval)
        private val pvText: TextView = view.findViewById(R.id.textPv)

        fun bind(info: MultipvInfo) {
            depthText.text = "Depth: ${info.depth}"
            evalText.text = "${info.score}"
            pvText.text = "${info.pv}"
        }
    }
}