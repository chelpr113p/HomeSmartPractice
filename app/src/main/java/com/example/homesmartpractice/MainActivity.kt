package com.example.homesmartpractice

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homesmartpractice.adapter.RoomAdapter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.RoomsList)

        val dummyItems = listOf(
            RoomItem("Кухня", R.drawable.ic_kitchen_blue),
            RoomItem("Гостиная", R.drawable.ic_living_room_blue)
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RoomAdapter(dummyItems) { clickedItem ->
            val intent = Intent(this, DevicesActivity::class.java)
            startActivity(intent)
        }
        findViewById<ImageButton>(R.id.Settings_btn).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddRoomActivity::class.java))
        }
    }
}