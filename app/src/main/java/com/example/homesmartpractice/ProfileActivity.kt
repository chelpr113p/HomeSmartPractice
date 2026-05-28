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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnEdit: Button
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button // Переменная для кнопки выхода

    private var isEditing = false

    // Инициализация Firestore
    private val db = FirebaseFirestore.getInstance()

    // Ленивая инициализация SharedPreferences
    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val documentId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    private lateinit var userDocRef: DocumentReference

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

            if (validateData(username, email)) {
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
        // Пользователь не сможет вернуться в профиль, нажав кнопку "Назад".
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
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Валидация введенных данных
     */
    private fun validateData(username: String, email: String): Boolean {
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