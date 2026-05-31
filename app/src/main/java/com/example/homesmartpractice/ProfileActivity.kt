package com.example.homesmartpractice

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnEdit: Button
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button

    private var isEditing = false

    // Инициализация Firestore
    private val db = FirebaseFirestore.getInstance()

    // Ленивая инициализация SharedPreferences
    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val documentId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    private lateinit var userDocRef: DocumentReference

    // Наше строгое регулярное выражение для валидации адреса
    private val addressRegex = Regex(
        """^г\.\s*[^,]+,\s*ул\.\s*[^,]+,\s*д\.\s*\d+[\s-]?[а-яА-Я]?(?:\s*(?:/|к|корп|стр)\.?\s*\d+)?(?:\s*,\s*кв\.\s*\d+[а-яА-Я]?)?$""",
        RegexOption.IGNORE_CASE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Инициализация views
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etAddress = findViewById(R.id.etAddress)
        btnEdit = findViewById(R.id.btnReduct)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout) // Инициализируем кнопку выхода

        // Ссылка на документ пользователя в таблице users
        userDocRef = db.collection("users").document(documentId)

        // Изначально поля заблокированы
        enableEditing(false)

        // Загрузка данных из БД при открытии активности
        loadUserData()

        // Сбрасываем ошибку адреса, когда пользователь начинает вводить текст
        etAddress.doAfterTextChanged {
            etAddress.error = null
        }

        // Кнопка "Назад"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Кнопка "Редактировать"
        btnEdit.setOnClickListener {
            isEditing = true
            enableEditing(true)
        }

        // Кнопка "Сохранить"
        btnSave.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val address = etAddress.text.toString().trim()

            // Передаем адрес в метод валидации
            if (validateData(username, email, address)) {
                saveUserData(username, email, address)
            }
        }

        // Кнопка "Выйти"
        btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    /**
     * Логика выхода из аккаунта
     */
    private fun logoutUser() {
        // 1. Стираем ID пользователя из памяти устройства
        sharedPref.edit().remove("USER_ID").apply()

        // 2. Делаем переход на экран авторизации
        val intent = Intent(this, AuthorisationActivity::class.java)

        // Флаги CLEAR_TASK и NEW_TASK полностью стирают историю прошлых экранов.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // 3. Закрываем текущий экран профиля
        finish()
    }

    /**
     * Загрузка данных из Firestore
     */
    private fun loadUserData() {
        if (documentId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID пользователя отсутствует", Toast.LENGTH_SHORT).show()
            return
        }

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    etUsername.setText(document.getString("login") ?: "")
                    etEmail.setText(document.getString("email") ?: "")
                    etAddress.setText(document.getString("address") ?: "")
                } else {
                    Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Сохранение измененных данных в Firestore
     */
    private fun saveUserData(username: String, email: String, address: String) {
        val updates = hashMapOf<String, Any>(
            "login" to username,
            "email" to email,
            "address" to address
        )

        userDocRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Данные успешно сохранены", Toast.LENGTH_SHORT).show()
                isEditing = false
                enableEditing(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка保存ения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Валидация введенных данных
     */
    private fun validateData(username: String, email: String, address: String): Boolean {
        if (username.isEmpty()) {
            etUsername.error = "Имя пользователя не может быть пустым"
            etUsername.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            etEmail.error = "Email не может быть пустым"
            etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Введите корректный адрес электронной почты"
            etEmail.requestFocus()
            return false
        }

        // Валидация адреса: проверка на пустоту
        if (address.isEmpty()) {
            etAddress.error = "Пожалуйста, введите адрес"
            etAddress.requestFocus()
            return false
        }

        // Валидация адреса: проверка регулярным выражением (квартира опциональна)
        if (!addressRegex.matches(address)) {
            etAddress.error = "Формат: г. Город, ул. Улица, д. Дом (кв. Квартира — при наличии)"
            etAddress.requestFocus()
            return false
        }

        return true
    }

    /**
     * Включение/отключение редактирования полей
     */
    private fun enableEditing(enabled: Boolean) {
        val editTexts = listOf(etUsername, etEmail, etAddress)

        editTexts.forEach { et ->
            et.isEnabled = enabled
            et.isFocusableInTouchMode = enabled
            et.isClickable = enabled
            et.isCursorVisible = enabled

            et.backgroundTintList = if (enabled) {
                ColorStateList.valueOf(Color.BLACK)
            } else {
                ColorStateList.valueOf(Color.parseColor("#CCCCCC"))
            }

            et.setTextColor(if (enabled) Color.BLACK else Color.parseColor("#888888"))
            et.setHintTextColor(if (enabled) Color.BLACK else Color.parseColor("#BBBBBB"))
        }

        btnSave.isEnabled = enabled
    }
}