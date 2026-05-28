package com.example.homesmartpractice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignInActivity : AppCompatActivity() {

    private lateinit var tvPinTitle: TextView
    private lateinit var llPinDots: LinearLayout
    private lateinit var glKeyboard: GridLayout

    private val pinCode = StringBuilder()
    private val maxPinLength = 4

    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val documentId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    // Получаем сохраненный правильный пин-код для этого пользователя
    private val savedPinCode by lazy { sharedPref.getString("PIN_$documentId", "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Используем макет экрана пин-кода
        setContentView(R.layout.activity_create_pin)

        llPinDots = findViewById(R.id.llPinDots)
        glKeyboard = findViewById(R.id.glKeyboard)
        tvPinTitle = findViewById(R.id.tvPinTitle)

        // Меняем текст заголовка, так как макет используется повторно
        tvPinTitle.text = "Введите PIN-код"

        initKeyboard()
        updatePinDots()
    }

    private fun initKeyboard() {
        for (i in 0 until glKeyboard.childCount) {
            val child = glKeyboard.getChildAt(i)
            if (child is Button) {
                child.setOnClickListener {
                    handleKeyPress(child.text.toString())
                }
            }
        }
    }

    private fun handleKeyPress(key: String) {
        when (key) {
            "С" -> pinCode.setLength(0)
            "⌫" -> if (pinCode.isNotEmpty()) pinCode.deleteCharAt(pinCode.length - 1)
            else -> if (pinCode.length < maxPinLength) pinCode.append(key)
        }

        updatePinDots()

        // Когда ввели все 4 цифры — проверяем
        if (pinCode.length == maxPinLength) {
            verifyPin()
        }
    }

    private fun updatePinDots() {
        for (i in 0 until llPinDots.childCount) {
            llPinDots.getChildAt(i).alpha = if (i < pinCode.length) 1.0f else 0.3f
        }
    }

    /**
     * Проверка введенного PIN-кода
     */
    private fun verifyPin() {
        if (pinCode.toString() == savedPinCode) {
            // ПИН-код верный! Проверяем, откуда пришел пользователь
            val fromRegistration = intent.getBooleanExtra("FROM_REGISTRATION", false)

            if (fromRegistration) {
                // Если только что зарегистрировался -> на экран ввода адреса
                val intent = Intent(this, AddressActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                // Если это обычный вход авторизованного пользователя -> на главный экран
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            finish()
        } else {
            // ПИН-код неверный — сбрасываем ввод и уведомляем
            Toast.makeText(this, "Неверный PIN-код, попробуйте еще раз", Toast.LENGTH_SHORT).show()
            pinCode.setLength(0)
            updatePinDots()
        }
    }
}