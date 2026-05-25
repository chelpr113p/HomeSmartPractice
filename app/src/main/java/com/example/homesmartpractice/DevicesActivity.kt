package com.example.homesmartpractice

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homesmartpractice.adapter.DeviceAdapter
import com.example.homesmartpractice.model.DeviceItem

class DevicesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        val roomName = intent.getStringExtra("ROOM_NAME")
        findViewById<TextView>(R.id.tvRoomTitle).text = roomName ?: "Устройства"

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddDeviceActivity::class.java))
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddDeviceActivity::class.java))}

        val recyclerView = findViewById<RecyclerView>(R.id.rvDevices)

        val devices = listOf(
            DeviceItem("Теплый пол", R.drawable.ic_term_blue),
            DeviceItem("Телевизор", R.drawable.ic_tv_blue)
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = DeviceAdapter(devices)

    }
}