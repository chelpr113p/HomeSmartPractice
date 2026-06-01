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

    private lateinit var etCustomRoomType: EditText
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()
    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val currentUserId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_room) // Убедитесь, что имя файла совпадает

        etCustomRoomType = findViewById(R.id.etCustomRoomType)
        btnSave = findViewById(R.id.btnSave)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveCustomTypeToDatabase()
        }
    }

    private fun saveCustomTypeToDatabase() {
        val typeName = etCustomRoomType.text.toString().trim()

        if (typeName.isEmpty()) {
            etCustomRoomType.error = "Введите тип комнаты"
            etCustomRoomType.requestFocus()
            return
        }

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        val uniqueId = System.currentTimeMillis().toString() + (100..999).random().toString()

        // Формируем структуру документа согласно вашему скриншоту
        val roomTypeMap = hashMapOf(
            "id" to uniqueId,
            "type" to typeName,
            "userID" to currentUserId
        )

        db.collection("room_types").document(uniqueId)
            .set(roomTypeMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Тип комнаты успешно добавлен!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK) // Передаем успешный статус в AddRoomActivity
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}