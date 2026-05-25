package com.example.homesmartpractice.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.example.homesmartpractice.R
import com.example.homesmartpractice.model.DeviceItem

class DeviceAdapter(
    private val items: List<DeviceItem>,
    private val onItemClick: ((DeviceItem) -> Unit)? = null
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.deviceIcon)
        val name: TextView = view.findViewById(R.id.tvDeviceName)
        val switch: SwitchMaterial = view.findViewById(R.id.switchStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.icon.setImageResource(item.iconRes)
        holder.switch.isChecked = item.isEnabled

        if (onItemClick != null) {
            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        } else {

            holder.itemView.isClickable = false
        }

        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            item.isEnabled = isChecked
        }
    }

    override fun getItemCount() = items.size
}