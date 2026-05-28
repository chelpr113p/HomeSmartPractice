package com.example.homesmartpractice

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homesmartpractice.adapter.DeviceAdapter
import com.example.homesmartpractice.model.DeviceItem
import com.google.firebase.firestore.FirebaseFirestore

class DevicesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvRoomTitle: TextView
    private val db = FirebaseFirestore.getInstance()

    private var roomId: String = ""
    private var roomName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        // Получаем данные выбранной комнаты
        roomId = intent.getStringExtra("ROOM_ID") ?: ""
        roomName = intent.getStringExtra("ROOM_NAME") ?: "Устройства"

        tvRoomTitle = findViewById(R.id.tvRoomTitle)
        tvRoomTitle.text = roomName

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Переходим на экран добавления устройства и прокидываем ID комнаты
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            val intent = Intent(this, AddDeviceActivity::class.java)
            intent.putExtra("ROOM_ID", roomId)
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.rvDevices)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Подключаем слушатель базы данных
        listenToDevices()
    }

    private fun listenToDevices() {
        if (roomId.isEmpty()) return

        // Выбираем только те устройства, которые принадлежат текущей комнате
        db.collection("devices")
            .whereEqualTo("roomID", roomId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val deviceItems = mutableListOf<DeviceItem>()

                if (snapshots != null) {
                    for (doc in snapshots) {
                        val name = doc.getString("name") ?: ""
                        val type = doc.getString("type") ?: ""
                        val status = doc.getBoolean("status") ?: false
                        val docId = doc.id // Берем уникальный системный ID документа

                        // Формируем строку: Тип + Название устройства
                        val displayName = "$type $name"
                        val iconRes = getIconForDeviceType(type)

                        deviceItems.add(DeviceItem(docId, displayName, iconRes, status))
                    }
                }

                // Сетим адаптер и передаем логику изменения статуса в БД
                recyclerView.adapter = DeviceAdapter(deviceItems) { clickedItem, isChecked ->
                    db.collection("devices").document(clickedItem.docId)
                        .update("status", isChecked)
                        .addOnFailureListener { err ->
                            Toast.makeText(this, "Ошибка обновления статуса: ${err.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun getIconForDeviceType(type: String): Int {
        return when (type) {
            "Свет" -> R.drawable.ic_light_blue
            "Кондиционер" -> R.drawable.ic_condi_blue
            "Вытяжка" -> R.drawable.ic_hood_blue
            "Теплый пол" -> R.drawable.ic_term_blue
            "Вентилятор" -> R.drawable.ic_fan_blue
            "Телевизор" -> R.drawable.ic_tv_blue
            else -> R.drawable.ic_term_blue
        }
    }
}