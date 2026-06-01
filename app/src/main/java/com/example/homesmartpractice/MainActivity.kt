package com.example.homesmartpractice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homesmartpractice.adapter.RoomAdapter
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvAddress: TextView

    private val db = FirebaseFirestore.getInstance()

    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val currentUserId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.RoomsList)
        tvAddress = findViewById(R.id.tvAddress)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // 1. Изначально список комнат пуст. Запускаем слушатель реального времени
        listenToRooms()

        findViewById<ImageButton>(R.id.Settings_btn).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddRoomActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserAddress()
    }
    /**
     * Получение адреса пользователя из коллекции "users"
     */
    private fun loadUserAddress() {
        if (currentUserId.isNotEmpty()) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val address = document.getString("address")
                        if (!address.isNullOrEmpty()) {
                            tvAddress.text = address
                        }
                    }
                }
        }
    }

    /**
     * Слушатель изменений в коллекции комнат текущего пользователя
     */
    /**
     * Слушатель изменений в коллекции комнат текущего пользователя
     */
    private fun listenToRooms() {
        if (currentUserId.isEmpty()) return

        // Выбираем только комнаты, где userID совпадает с текущим пользователем
        db.collection("rooms")
            .whereEqualTo("userID", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Ошибка получения списка комнат: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val roomItems = mutableListOf<RoomItem>()

                if (snapshots != null) {
                    for (doc in snapshots) {
                        val name = doc.getString("name") ?: ""
                        val type = doc.getString("type") ?: ""

                        // Склеиваем тип и название комнаты, как настраивали ранее
                        val displayName = "$type $name".trim()

                        // Определяем синюю иконку по типу
                        val iconRes = getIconForType(type)

                        // Берем системный ID документа в Firestore
                        val roomId = doc.id

                        // Передаем в модель все 3 параметра по порядку
                        roomItems.add(RoomItem(roomId, displayName, iconRes))
                    }
                }

                // Передаем обновленный список в адаптер
                recyclerView.adapter = RoomAdapter(roomItems) { clickedItem ->
                    val intent = Intent(this, DevicesActivity::class.java)

                    // Используем clickedItem, так как переменные из цикла здесь недоступны
                    intent.putExtra("ROOM_ID", clickedItem.id)
                    intent.putExtra("ROOM_NAME", clickedItem.name)
                    startActivity(intent)
                }
            } // Здесь корректно закрывается addSnapshotListener
    } // Здесь корректно закрывается listenToRooms

    /**
     * Сопоставление строкового типа комнаты с синими ресурсами иконок для списка
     */
    private fun getIconForType(type: String): Int {
        return when (type) {
            "Кухня" -> R.drawable.ic_kitchen_blue
            "Гостиная" -> R.drawable.ic_living_room_blue
            "Ванная" -> R.drawable.ic_bathroom_blue
            "Кабинет" -> R.drawable.ic_office_blue
            "Спальня" -> R.drawable.ic_bedroom_blue
            "Зал" -> R.drawable.ic_tv_blue
            else -> R.drawable.ic_plug_blue // Дефолтная заглушка, если совпадений нет
        }
    }
}