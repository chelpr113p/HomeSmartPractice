package com.example.homesmartpractice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationActivity : AppCompatActivity() {

    // Объявляем переменные для полей ввода
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: Button

    // Инициализируем Firestore
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Инициализируем views по ID из XML
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnLogin = findViewById(R.id.btnLogin)

        // Кнопка "Зарегистрироваться"
        btnRegister.setOnClickListener {
            registerUser()
        }

        // Кнопка "Войти" — возвращает на предыдущий экран (AuthorisationActivity)
        btnLogin.setOnClickListener {
            finish()
        }
    }

    /**
     * Основная логика валидации и проверки пользователя
     */
    private fun registerUser() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // 1. Валидация полей на пустоту и корректность
        if (username.isEmpty()) {
            etUsername.error = "Имя пользователя не может быть пустым"
            etUsername.requestFocus()
            return
        }

        if (email.isEmpty()) {
            etEmail.error = "Email не может быть пустым"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Введите корректный адрес электронной почты"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Пароль не может быть пустым"
            etPassword.requestFocus()
            return
        }

        // Так как в XML у тебя inputType="numberPassword", пароль будет цифровым.
        // Можно добавить проверку на минимальную длину (например, 4 символа)
        if (password.length < 4) {
            etPassword.error = "Пароль должен содержать не менее 4 цифр"
            etPassword.requestFocus()
            return
        }

        // 2. Проверяем в Firestore, занят ли этот email
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Если документ найден, значит email уже используется
                    Toast.makeText(this, "Пользователь с таким email уже существует", Toast.LENGTH_SHORT).show()
                } else {
                    // Если всё чисто — создаем новый аккаунт
                    createNewUser(username, email, password)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка проверки данных: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Запись нового документа в Firebase Firestore
     */
    private fun createNewUser(username: String, email: String, password: String) {
        // Создаем ссылку на новый документ со случайным уникальным ID (как на твоем скриншоте)
        val newUserRef = db.collection("users").document()
        val documentId = newUserRef.id

        // Формируем структуру данных в точности как в твоей консоли Firebase
        val userMap = hashMapOf<String, Any>(
            "id" to documentId,       // Записываем сгенерированный ID внутрь документа
            "login" to username,
            "email" to email,
            "password" to password,
            "address" to ""           // Пустая строка (заполнится на другом экране)
        )

        // Сохраняем map в базу данных
        newUserRef.set(userMap)
            .addOnSuccessListener {
                // Сохраняем ID зарегистрированного пользователя в SharedPreferences для авто-входа
                val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                sharedPref.edit().putString("USER_ID", documentId).apply()

                Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()

                // Переходим на экран создания PIN-кода
                val intent = Intent(this, CreatePinActivity::class.java)
                // Очищаем стек, чтобы пользователь не вернулся назад на экран регистрации
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}