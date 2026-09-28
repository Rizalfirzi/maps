package com.testmap.mmpk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.testmap.mmpk.databinding.ItemDirectionBinding
import com.testmap.mmpk.model.DirectionStep
import java.util.Locale

class RouteDirectionAdapter : RecyclerView.Adapter<RouteDirectionAdapter.DirectionViewHolder>() {

    private val items = mutableListOf<DirectionStep>()

    fun submitList(newItems: List<DirectionStep>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectionViewHolder {
        val binding = ItemDirectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DirectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DirectionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class DirectionViewHolder(private val binding: ItemDirectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DirectionStep) {
            binding.tvStepNumber.text = item.index.toString()
            binding.tvDirectionText.text = item.text

            val distFormatted = if (item.distanceMeters >= 1000) {
                String.format(Locale.getDefault(), "%.2f km", item.distanceMeters / 1000.0)
            } else {
                String.format(Locale.getDefault(), "%.0f m", item.distanceMeters)
            }

            val timeFormatted = if (item.estimatedMinutes >= 1.0) {
                String.format(Locale.getDefault(), "%.1f min", item.estimatedMinutes)
            } else {
                "< 1 min"
            }

            binding.tvDirectionDetail.text = "$distFormatted • ~$timeFormatted"
        }
    }
}
