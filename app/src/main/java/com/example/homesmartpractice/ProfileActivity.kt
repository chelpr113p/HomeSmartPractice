package com.example.homesmartpractice

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton

import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import com.google.android.material.imageview.ShapeableImageView
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnEdit: Button
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button

    // НАСТРОЙКА АВАТАРКИ: Элементы интерфейса и переменная для хранения временного Uri
    private lateinit var avatarContainer: FrameLayout
    private lateinit var ivAvatar: ShapeableImageView
    private var selectedImageUri: Uri? = null

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

    // НАСТРОЙКА АВАТАРКИ: Регистрация Photo Picker (выбор картинки из галереи)
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivAvatar.setImageURI(uri) // Временно отображаем выбранную картинку
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Инициализация views
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etAddress = findViewById(R.id.etAddress)
        btnEdit = findViewById(R.id.btnReduct)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)

        // НАСТРОЙКА АВАТАРКИ: Инициализация view аватарки
        avatarContainer = findViewById(R.id.avatarContainer)
        ivAvatar = findViewById(R.id.ivAvatar)

        // Ссылка на документ пользователя в таблице users
        userDocRef = db.collection("users").document(documentId)

        // Изначально поля заблокированы
        enableEditing(false)

        // Загрузка данных из БД при открытии активности
        loadUserData()

        // НАСТРОЙКА АВАТАРКИ: Загружаем аватарку локально
        loadLocalAvatar()

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
                // НАСТРОЙКА АВАТАРКИ: Если была выбрана новая аватарка, сохраняем её локально
                selectedImageUri?.let { uri ->
                    saveAvatarToInternalStorage(uri)
                }
                saveUserData(username, email, address)
            }
        }

        // Кнопка "Выйти"
        btnLogout.setOnClickListener {
            logoutUser()
        }

        // НАСТРОЙКА АВАТАРКИ: Клик по аватарке запускает выбор фото (только в режиме редактирования)
        avatarContainer.setOnClickListener {
            if (isEditing) {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }

    /**
     * Логика выхода из аккаунта
     */
    private fun logoutUser() {
        // Стираем ID пользователя из памяти устройства
        sharedPref.edit().remove("USER_ID").apply()

        // Переход на экран авторизации
        val intent = Intent(this, AuthorisationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * НАСТРОЙКА АВАТАРКИ: Сохранение изображения во внутреннюю память приложения
     */
    private fun saveAvatarToInternalStorage(uri: Uri) {
        try {
            // Создаем уникальное имя файла для текущего пользователя
            val fileName = "avatar_$documentId.jpg"
            val file = File(filesDir, fileName)

            // Копируем картинку через поток данных
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Сохраняем локальный путь к файлу в SharedPreferences
            sharedPref.edit().putString("AVATAR_PATH_$documentId", file.absolutePath).apply()
            selectedImageUri = null // Сбрасываем временный URI
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Не удалось сохранить аватарку местно", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * НАСТРОЙКА АВАТАРКИ: Локальная загрузка аватарки при старте экрана
     */
    private fun loadLocalAvatar() {
        val savedPath = sharedPref.getString("AVATAR_PATH_$documentId", null)
        if (savedPath != null) {
            val file = File(savedPath)
            if (file.exists()) {
                ivAvatar.setImageURI(Uri.fromFile(file))
            }
        }
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

        if (address.isEmpty()) {
            etAddress.error = "Пожалуйста, введите адрес"
            etAddress.requestFocus()
            return false
        }

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

        // НАСТРОЙКА АВАТАРКИ: Делаем контейнер аватарки кликабельным визуально (опционально можно добавить альфу)
        avatarContainer.alpha = if (enabled) 0.8f else 1.0f
    }
}