package com.example.homesmartpractice

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AddCustomRoomActivity : AppCompatActivity() {

    private lateinit var etCustomRoomName: EditText
    private lateinit var etCustomRoomType: EditText
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()

    // Достаем ID текущего пользователя из SharedPreferences
    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val currentUserId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_room)

        // Инициализация UI элементов
        etCustomRoomName = findViewById(R.id.etCustomRoomName)
        etCustomRoomType = findViewById(R.id.etCustomRoomType)
        btnSave = findViewById(R.id.btnSave)

        // Кнопка назад просто закрывает текущий экран
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Логика кнопки "Сохранить"
        btnSave.setOnClickListener {
            saveCustomRoomToDatabase()
        }
    }

    private fun saveCustomRoomToDatabase() {
        val roomName = etCustomRoomName.text.toString().trim()
        val roomType = etCustomRoomType.text.toString().trim()

        // 1. Валидация названия комнаты
        if (roomName.isEmpty()) {
            etCustomRoomName.error = "Введите название комнаты"
            etCustomRoomName.requestFocus()
            return
        }

        // 2. Валидация типа комнаты
        if (roomType.isEmpty()) {
            etCustomRoomType.error = "Введите тип комнаты"
            etCustomRoomType.requestFocus()
            return
        }

        // 3. Проверка авторизации
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        // Генерируем уникальный ID
        val uniqueNumericId = System.currentTimeMillis().toString() + (100..999).random().toString()

        // Формируем структуру документа (тип берется из EditText)
        val roomMap = hashMapOf(
            "id" to uniqueNumericId,
            "name" to roomName,
            "type" to roomType,
            "userID" to currentUserId
        )

        // Сохраняем в Firebase Firestore
        db.collection("rooms").document()
            .set(roomMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Своя комната успешно добавлена!", Toast.LENGTH_SHORT).show()

                // Устанавливаем результат OK, чтобы AddRoomActivity поняла, что нужно закрыться
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}