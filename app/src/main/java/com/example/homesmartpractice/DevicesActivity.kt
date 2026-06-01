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

    // Списки для хранения актуальных данных из трех таблиц
    private val regularDevices = mutableListOf<DeviceItem>()
    private val termDevices = mutableListOf<DeviceItem>()
    private val tvDevices = mutableListOf<DeviceItem>() // ДОБАВИЛИ список телевизоров

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        roomId = intent.getStringExtra("ROOM_ID") ?: ""
        roomName = intent.getStringExtra("ROOM_NAME") ?: "Устройства"

        tvRoomTitle = findViewById(R.id.tvRoomTitle)
        tvRoomTitle.text = roomName

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            val intent = Intent(this, AddDeviceActivity::class.java)
            intent.putExtra("ROOM_ID", roomId)
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.rvDevices)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Подключаем слушатели для всех трех коллекций
        listenToDevices()
    }

    private fun listenToDevices() {
        if (roomId.isEmpty()) return

        // 1. Слушаем обычные устройства
        db.collection("devices")
            .whereEqualTo("roomID", roomId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Ошибка загрузки устройств: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                regularDevices.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val name = doc.getString("name") ?: ""
                        val type = doc.getString("type") ?: ""
                        val status = doc.getBoolean("status") ?: false
                        val docId = doc.id

                        regularDevices.add(
                            DeviceItem(docId, "$type $name", type, getIconForDeviceType(type), status)
                        )
                    }
                }
                updateRecyclerView()
            }

        // 2. Слушаем теплые полы
        db.collection("term_device")
            .whereEqualTo("roomID", roomId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Ошибка загрузки теплых полов: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                termDevices.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val name = doc.getString("name") ?: ""
                        val type = doc.getString("type") ?: "Теплый пол"
                        val status = doc.getBoolean("status") ?: false
                        val docId = doc.id

                        termDevices.add(
                            DeviceItem(docId, "$type $name", type, getIconForDeviceType(type), status)
                        )
                    }
                }
                updateRecyclerView()
            }

        // 3. ДОБАВИЛИ: Слушаем телевизоры
        db.collection("tv_device")
            .whereEqualTo("roomID", roomId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Ошибка загрузки телевизоров: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                tvDevices.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val name = doc.getString("name") ?: ""
                        val type = doc.getString("type") ?: "Телевизор"
                        val status = doc.getBoolean("status") ?: false
                        val docId = doc.id

                        tvDevices.add(
                            DeviceItem(docId, "$type $name", type, getIconForDeviceType(type), status)
                        )
                    }
                }
                updateRecyclerView()
            }
    }

    /**
     * Объединяет три списка и настраивает адаптер с логикой ветвления
     */
    private fun updateRecyclerView() {
        val allDevices = mutableListOf<DeviceItem>().apply {
            addAll(regularDevices)
            addAll(termDevices)
            addAll(tvDevices) // Добавляем телевизоры в общий список
        }

        recyclerView.adapter = DeviceAdapter(
            allDevices,
            onItemClicked = { clickedItem ->
                // В зависимости от типа перенаправляем на нужную активность управления
                when (clickedItem.type) {
                    "Теплый пол" -> {
                        val intent = Intent(this, TermDeviceActivity::class.java).apply {
                            putExtra("DEVICE_ID", clickedItem.docId)
                        }
                        startActivity(intent)
                    }
                    "Телевизор" -> {
                        // Перенаправляем на экран управления телевизором
                        val intent = Intent(this, TvDeviceActivity::class.java).apply {
                            putExtra("DEVICE_ID", clickedItem.docId)
                        }
                        startActivity(intent)
                    }
                }
            },
            onStatusChanged = { clickedItem, isChecked ->
                // Определяем верное имя коллекции для быстрого изменения статуса (On/Off)
                val collectionName = when (clickedItem.type) {
                    "Теплый пол" -> "term_device"
                    "Телевизор" -> "tv_device"
                    else -> "devices"
                }

                db.collection(collectionName).document(clickedItem.docId)
                    .update("status", isChecked)
                    .addOnFailureListener { err ->
                        Toast.makeText(this, "Ошибка обновления статуса: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        )
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