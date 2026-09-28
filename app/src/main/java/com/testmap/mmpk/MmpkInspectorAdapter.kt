package com.testmap.mmpk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.testmap.mmpk.databinding.ItemLayerInfoBinding
import com.testmap.mmpk.model.LayerInfoItem

class MmpkInspectorAdapter : RecyclerView.Adapter<MmpkInspectorAdapter.LayerViewHolder>() {

    private val items = mutableListOf<LayerInfoItem>()

    fun submitList(newItems: List<LayerInfoItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val binding = ItemLayerInfoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LayerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class LayerViewHolder(private val binding: ItemLayerInfoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LayerInfoItem) {
            binding.tvLayerName.text = item.name
            binding.tvLayerType.text = item.type

            if (item.isRoutingDataset) {
                binding.tvLayerIcon.text = "🛣️"
                binding.tvLayerStatus.text = "Routing Active"
                binding.tvLayerStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.badge_success)
                )
            } else {
                binding.tvLayerIcon.text = "🗺️"
                binding.tvLayerStatus.text = "Layer Map"
                binding.tvLayerStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_secondary)
                )
            }
        }
    }
}
