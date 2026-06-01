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
    private val onItemClicked: (DeviceItem) -> Unit,          // ДОБАВИЛИ: клик по элементу
    private val onStatusChanged: (DeviceItem, Boolean) -> Unit // Переключение свитча
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

        // Клик по всей плашке устройства (кроме свитча)
        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }

        // Сначала убираем слушатель, чтобы избежать багов ресайклинга
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.setChecked(item.isEnabled)

        // Ставим актуальный слушатель изменения статуса
        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            item.isEnabled = isChecked
            onStatusChanged(item, isChecked)
        }
    }

    override fun getItemCount() = items.size
}