package com.example.homesmartpractice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AuthorisationActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ПРОВЕРКА: Автоматический вход, если пользователь уже был авторизован
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val savedUserId = sharedPref.getString("USER_ID", null)

        if (savedUserId != null) {
            // Вместо goToMain() вызываем переход на ПИН-код
            goToPinVerification()
            return
        }

        setContentView(R.layout.activity_authorization)

        // Инициализация полей
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        // Кнопка Входа
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            loginUser()
        }

        // Кнопка Регистрации
        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Базовая валидация на пустые поля
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        // Ищем пользователя в БД
        db.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("password", password)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Пользователь найден! Берем первый (и единственный) совпавший документ
                    val userDoc = documents.documents[0]
                    val documentId = userDoc.id

                    // ЗАПОМИНАЕМ ПОЛЬЗОВАТЕЛЯ в SharedPreferences
                    val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                    sharedPref.edit().putString("USER_ID", documentId).apply()

                    Toast.makeText(this, "Успешный вход", Toast.LENGTH_SHORT).show()
                    goToPinVerification()
                } else {
                    // Совпадений нет
                    Toast.makeText(this, "Неверный email или пароль", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка подключения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToPinVerification() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.putExtra("FROM_REGISTRATION", false) // Обычный авторизованный пользователь
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}