package com.aistudio.dexter.ui.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aistudio.dexter.R
import com.aistudio.dexter.data.model.SegmentInfo

class SegmentsAdapter(
    private var segments: List<SegmentInfo>,
    private val onSegmentClick: (SegmentInfo) -> Unit
) : RecyclerView.Adapter<SegmentsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvSegmentIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvSegmentTitle)
        val tvCategory: TextView = view.findViewById(R.id.tvSegmentCategory)
        val tvCount: TextView = view.findViewById(R.id.tvSegmentCount)
        val tvDesc: TextView = view.findViewById(R.id.tvSegmentDesc)
        val tvTables: TextView = view.findViewById(R.id.tvTablesList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_segment_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val seg = segments[position]
        holder.tvIcon.text = seg.icon
        holder.tvTitle.text = "${seg.segmentNumber}. ${seg.title}"
        holder.tvCategory.text = seg.category
        holder.tvCount.text = "${seg.itemCount} items"
        holder.tvDesc.text = seg.description
        holder.tvTables.text = "Tables: ${seg.allTables.joinToString(", ")}"

        holder.itemView.setOnClickListener {
            onSegmentClick(seg)
        }
    }

    override fun getItemCount(): Int = segments.size

    fun updateList(newList: List<SegmentInfo>) {
        segments = newList
        notifyDataSetChanged()
    }
}
