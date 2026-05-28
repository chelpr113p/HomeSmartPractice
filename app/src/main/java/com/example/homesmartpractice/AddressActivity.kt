package com.example.homesmartpractice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class AddressActivity : AppCompatActivity() {

    private lateinit var etAddress: TextInputEditText
    private lateinit var btnSave: Button

    // Инициализация Firestore
    private val db = FirebaseFirestore.getInstance()

    // Ленивое получение ID пользователя, сохраненного при регистрации
    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val documentId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address)

        // Инициализируем элементы интерфейса
        etAddress = findViewById(R.id.etAddress)
        btnSave = findViewById(R.id.btnSave)

        // Обработка нажатия на кнопку "Сохранить"
        btnSave.setOnClickListener {
            saveAddressToFirestore()
        }
    }

    /**
     * Валидация и сохранение адреса в БД
     */
    private fun saveAddressToFirestore() {
        val address = etAddress.text.toString().trim()

        // 1. Валидация: поле не должно быть пустым
        if (address.isEmpty()) {
            etAddress.error = "Пожалуйста, введите адрес"
            etAddress.requestFocus()
            return
        }

        // Подстраховка: проверяем, за залогинен ли пользователь в SharedPreferences
        if (documentId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Обновляем только поле "address" в существующем документе пользователя
        db.collection("users").document(documentId)
            .update("address", address)
            .addOnSuccessListener {
                Toast.makeText(this, "Адрес успешно сохранен!", Toast.LENGTH_SHORT).show()
                // 3. Переходим на главный экран
                goToMain()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Переход на главный экран с очисткой стека
     */
    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        // Флаги сбросят стек, чтобы по кнопке "Назад" нельзя было вернуться к экрану ввода адреса
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}