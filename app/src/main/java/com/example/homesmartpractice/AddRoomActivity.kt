package com.example.homesmartpractice

import android.content.Context
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

class AddRoomActivity : AppCompatActivity() {

    private var selectedRoomId: Int? = null

    // Объявляем элементы интерфейса
    private lateinit var etRoomName: EditText
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()

    // Достаем ID текущего пользователя из SharedPreferences
    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val currentUserId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_room)

        // Инициализация
        etRoomName = findViewById(R.id.et_room_name)
        btnSave = findViewById(R.id.btnSave)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val roomLayouts = listOf(
            findViewById<LinearLayout>(R.id.layout_living_room),
            findViewById<LinearLayout>(R.id.layout_kitchen),
            findViewById<LinearLayout>(R.id.layout_bathroom),
            findViewById<LinearLayout>(R.id.layout_office),
            findViewById<LinearLayout>(R.id.layout_bedroom),
            findViewById<LinearLayout>(R.id.layout_hall)
        )

        roomLayouts.forEach { layout ->
            layout.setOnClickListener { clickedView ->
                val clickedId = clickedView.id

                if (selectedRoomId == clickedId) return@setOnClickListener

                selectedRoomId?.let { oldId ->
                    val oldLayout = findViewById<LinearLayout>(oldId)
                    oldLayout?.isSelected = false
                    val oldTextView = oldLayout?.findTextViewInside()
                    oldTextView?.setTextColor(Color.parseColor("#94949B"))
                }

                clickedView.isSelected = true
                val currentTextView = (clickedView as LinearLayout).findTextViewInside()
                currentTextView?.setTextColor(Color.parseColor("#0B50A0"))

                selectedRoomId = clickedId
            }
        }

        // Логика кнопки "Сохранить"
        btnSave.setOnClickListener {
            saveRoomToDatabase()
        }
    }

    /**
     * Валидация полей и отправка данных в Firestore
     */
    private fun saveRoomToDatabase() {
        val roomName = etRoomName.text.toString().trim()

        // 1. Проверка: введено ли имя комнаты
        if (roomName.isEmpty()) {
            etRoomName.error = "Введите название комнаты"
            etRoomName.requestFocus()
            return
        }

        // 2. Проверка: выбран ли тип комнаты (контейнер)
        if (selectedRoomId == null) {
            Toast.makeText(this, "Пожалуйста, выберите тип комнаты", Toast.LENGTH_SHORT).show()
            return
        }

        // Получаем текст выбранного типа комнаты (например: "Гостиная", "Кухня")
        val selectedLayout = findViewById<LinearLayout>(selectedRoomId!!)
        val roomType = selectedLayout?.findTextViewInside()?.text.toString()

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        // Генерируем длинный числовой ID в виде строки, как в твоем примере ("13451345306689789")
        val uniqueNumericId = System.currentTimeMillis().toString() + (100..999).random().toString()

        // Формируем структуру документа комнаты
        val roomMap = hashMapOf(
            "id" to uniqueNumericId,
            "name" to roomName,  // Кастомное имя из EditText
            "type" to roomType,  // Тип ("Кухня", "Спальня" и т.д.)
            "userID" to currentUserId
        )

        // Сохраняем в коллекцию "rooms" со случайным ID документа
        db.collection("rooms").document()
            .set(roomMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Комната успешно добавлена!", Toast.LENGTH_SHORT).show()
                // Закрываем активити и автоматически возвращаемся на MainActivity
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
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
}