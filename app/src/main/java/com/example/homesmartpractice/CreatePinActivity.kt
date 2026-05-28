package com.example.homesmartpractice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CreatePinActivity : AppCompatActivity() {

    private lateinit var llPinDots: LinearLayout
    private lateinit var glKeyboard: GridLayout

    // Строка для динамической сборки пин-кода
    private val pinCode = StringBuilder()
    private val maxPinLength = 4

    // Шаред-преференсы для вытаскивания USER_ID и сохранения PIN
    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val documentId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_pin)

        // Инициализируем контейнеры
        llPinDots = findViewById(R.id.llPinDots)
        glKeyboard = findViewById(R.id.glKeyboard)

        // Настраиваем клавиатуру и начальное состояние точек
        initKeyboard()
        updatePinDots()
    }

    /**
     * Динамически находим все кнопки внутри GridLayout и вешаем на них один листенер
     */
    private fun initKeyboard() {
        for (i in 0 until glKeyboard.childCount) {
            val child = glKeyboard.getChildAt(i)
            if (child is Button) {
                child.setOnClickListener {
                    val buttonText = child.text.toString()
                    handleKeyPress(buttonText)
                }
            }
        }
    }

    /**
     * Обработка нажатий на кнопки клавиатуры
     */
    private fun handleKeyPress(key: String) {
        when (key) {
            "С" -> {
                pinCode.setLength(0) // Полностью очищаем строку пин-кода
            }
            "⌫" -> {
                if (pinCode.isNotEmpty()) {
                    pinCode.deleteCharAt(pinCode.length - 1) // Удаляем последний символ
                }
            }
            else -> {
                // Если нажата цифра и длина меньше 4 — добавляем её
                if (pinCode.length < maxPinLength) {
                    pinCode.append(key)
                }
            }
        }

        // Обновляем визуальное состояние точек
        updatePinDots()

        // Как только длина пин-кода стала равна 4 — сохраняем и идем дальше
        if (pinCode.length == maxPinLength) {
            savePinAndNavigate()
        }
    }

    /**
     * Обновление состояния точек на экране
     */
    private fun updatePinDots() {
        for (i in 0 until llPinDots.childCount) {
            val dot = llPinDots.getChildAt(i)
            // Если индекс точки меньше текущей длины пин-кода — делаем её видимой (1.0f)
            // Если цифра еще не введена — делаем точку полупрозрачной (0.3f)
            dot.alpha = if (i < pinCode.length) 1.0f else 0.3f
        }
    }

    /**
     * Сохранение PIN-кода для конкретного пользователя и переход на экран адреса
     */
// Находим этот метод в CreatePinActivity.kt и меняем его содержимое:
    private fun savePinAndNavigate() {
        if (documentId.isEmpty()) {
            Toast.makeText(this, "Ошибка: пользователь не определен", Toast.LENGTH_SHORT).show()
            return
        }

        sharedPref.edit().putString("PIN_$documentId", pinCode.toString()).apply()

        // Идем на экран SignInActivity для подтверждения
        val intent = Intent(this, SignInActivity::class.java)
        intent.putExtra("FROM_REGISTRATION", true) // Передаем, что мы после регистрации!
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}