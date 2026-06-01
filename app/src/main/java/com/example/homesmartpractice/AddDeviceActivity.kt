package com.example.homesmartpractice

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AddDeviceActivity : AppCompatActivity() {

    private var selectedId: Int? = null
    private lateinit var etDeviceName: EditText
    private lateinit var etDeviceId: EditText
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()
    private var roomId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        roomId = intent.getStringExtra("ROOM_ID") ?: ""

        etDeviceName = findViewById(R.id.et_device_name)
        etDeviceId = findViewById(R.id.et_device_ID)
        btnSave = findViewById(R.id.btnSave)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val deviceLayouts = listOf(
            findViewById<LinearLayout>(R.id.layout_living_room),
            findViewById<LinearLayout>(R.id.layout_kitchen),
            findViewById<LinearLayout>(R.id.layout_bathroom),
            findViewById<LinearLayout>(R.id.layout_office),
            findViewById<LinearLayout>(R.id.layout_bedroom),
            findViewById<LinearLayout>(R.id.layout_hall)
        )

        deviceLayouts.forEach { layout ->
            layout.setOnClickListener { clickedView ->
                val clickedId = clickedView.id
                if (selectedId == clickedId) return@setOnClickListener

                selectedId?.let { oldId ->
                    val oldLayout = findViewById<LinearLayout>(oldId)
                    oldLayout?.isSelected = false
                    val oldTextView = oldLayout?.findTextViewInside()
                    oldTextView?.setTextColor(Color.parseColor("#94949B"))
                }

                clickedView.isSelected = true
                val currentTextView = (clickedView as LinearLayout).findTextViewInside()
                currentTextView?.setTextColor(Color.parseColor("#0B50A0"))

                selectedId = clickedId
            }
        }

        btnSave.setOnClickListener {
            saveDeviceToDatabase()
        }
    }

    private fun saveDeviceToDatabase() {
        val deviceName = etDeviceName.text.toString().trim()
        val customId = etDeviceId.text.toString().trim()

        if (deviceName.isEmpty()) {
            etDeviceName.error = "Введите название устройства"
            return
        }

        if (selectedId == null) {
            Toast.makeText(this, "Пожалуйста, выберите тип устройства", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedLayout = findViewById<LinearLayout>(selectedId!!)
        val deviceType = selectedLayout?.findTextViewInside()?.text.toString()
        val finalId = if (customId.isEmpty()) System.currentTimeMillis().toString() else customId

        when (deviceType) {
            "Теплый пол" -> {
                // Явно указываем <String, Any> для hashMapOf
                val termDeviceMap = hashMapOf<String, Any>(
                    "id" to finalId,
                    "name" to deviceName,
                    "roomID" to roomId,
                    "status" to false,
                    "system_temp" to 25,
                    "type" to deviceType,
                    "time1" to "12:00",
                    "time2" to "13:00"
                )
                saveToCollection("term_device", termDeviceMap, "Теплый пол успешно добавлен!")
            }
            "Телевизор" -> {
                // Явно указываем <String, Any> для hashMapOf
                val tvDeviceMap = hashMapOf<String, Any>(
                    "id" to finalId,
                    "name" to deviceName,
                    "roomID" to roomId,
                    "type" to deviceType,
                    "status" to false,
                    "brightness" to 100,
                    "channel" to 1,
                    "nightmode" to false,
                    "volume" to 0
                )
                saveToCollection("tv_device", tvDeviceMap, "Телевизор успешно добавлен!")
            }
            else -> {
                // Явно указываем <String, Any> для hashMapOf
                val deviceMap = hashMapOf<String, Any>(
                    "id" to finalId,
                    "name" to deviceName,
                    "type" to deviceType,
                    "roomID" to roomId,
                    "status" to false
                )
                saveToCollection("devices", deviceMap, "Устройство успешно добавлено!")
            }
        }
    }

    // Изменили тип аргумента data с HashMap на более универсальный Map
    private fun saveToCollection(collection: String, data: Map<String, Any>, successMessage: String) {
        db.collection(collection).document()
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

    private fun LinearLayout.findTextViewInside(): TextView? {
        for (i in 0 until this.childCount) {
            val child = this.getChildAt(i)
            if (child is TextView) {
                return child
            }
        }
        return null
    }
