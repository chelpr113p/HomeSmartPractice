package com.example.homesmartpractice

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import com.google.firebase.firestore.FirebaseFirestore

class AddRoomActivity : AppCompatActivity() {

    private var selectedRoomId: Int? = null
    private lateinit var etRoomName: EditText
    private lateinit var btnSave: Button
    private lateinit var gridRooms: GridLayout

    private val db = FirebaseFirestore.getInstance()
    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val currentUserId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    // Список для хранения динамически созданных View, чтобы удалять их при обновлении данных
    private val dynamicViews = mutableListOf<View>()

    private val customRoomLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Если добавили новый тип комнаты, перерисовываем динамические карточки
            loadCustomRoomTypes()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_room)

        etRoomName = findViewById(R.id.et_room_name)
        btnSave = findViewById(R.id.btnSave)
        gridRooms = findViewById(R.id.grid_rooms)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Статические стандартные комнаты (без layout_custom, её обработаем отдельно)
        val staticRoomLayouts = listOf(
            findViewById<LinearLayout>(R.id.layout_living_room),
            findViewById<LinearLayout>(R.id.layout_kitchen),
            findViewById<LinearLayout>(R.id.layout_office),
            findViewById<LinearLayout>(R.id.layout_bedroom),
            findViewById<LinearLayout>(R.id.layout_hall)
        )

        // Вешаем обработчик кликов на базовые комнаты
        staticRoomLayouts.forEach { layout ->
            layout.setOnClickListener { clickedView ->
                selectRoomCard(clickedView)
            }
        }

        // Логика для кнопки "Другая"
        findViewById<LinearLayout>(R.id.layout_custom).setOnClickListener {
            val intent = Intent(this, AddCustomRoomActivity::class.java)
            customRoomLauncher.launch(intent)
        }

        // Загружаем кастомные типы комнат из Firestore
        loadCustomRoomTypes()

        btnSave.setOnClickListener {
            saveRoomToDatabase()
        }
    }

    /**
     * Загрузка кастомных типов комнат текущего пользователя из БД
     */
    private fun loadCustomRoomTypes() {
        if (currentUserId.isEmpty()) return

        // Очищаем старые динамические карточки, если они были отрисованы ранее
        dynamicViews.forEach { gridRooms.removeView(it) }
        dynamicViews.clear()

        // Сбрасываем выбор, если была выбрана кастомная карточка, которую сейчас пересоздадим
        selectedRoomId = null

        db.collection("room_types")
            .whereEqualTo("userID", currentUserId)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null) {
                    val inflater = LayoutInflater.from(this)
                    val layoutCustom = findViewById<LinearLayout>(R.id.layout_custom)

                    for (doc in snapshots) {
                        val customType = doc.getString("type") ?: ""

                        // ДОБАВЛЕНО: приведение типа 'as LinearLayout' в конце строки
                        val customCard = inflater.inflate(R.layout.item_room_type, gridRooms, false) as LinearLayout
                        customCard.id = View.generateViewId()

                        // Теперь эта строка сработает без ошибок!
                        val tvText = customCard.findTextViewInside()
                        tvText?.text = customType

                        // Реализуем клик-интерфейс аналогичный статическим карточкам
                        customCard.setOnClickListener { clickedView ->
                            selectRoomCard(clickedView)
                        }

                        // Находим индекс карточки "Другая", чтобы вставить новую карточку СТРОГО перед ней
                        val layoutCustom = findViewById<LinearLayout>(R.id.layout_custom)
                        val targetIndex = gridRooms.indexOfChild(layoutCustom)
                        gridRooms.addView(customCard, targetIndex)

                        // Сохраняем ссылку на view
                        dynamicViews.add(customCard)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки типов: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Универсальная функция визуального выделения карточки
     */
    private fun selectRoomCard(clickedView: View) {
        val clickedId = clickedView.id
        if (selectedRoomId == clickedId) return

        // Снимаем выделение со старой карты
        selectedRoomId?.let { oldId ->
            val oldLayout = findViewById<View>(oldId)
            oldLayout?.isSelected = false
            val oldTextView = (oldLayout as? LinearLayout)?.findTextViewInside()
            oldTextView?.setTextColor(Color.parseColor("#94949B"))
        }

        // Активируем выделение на новой карте
        clickedView.isSelected = true
        val currentTextView = (clickedView as LinearLayout).findTextViewInside()
        currentTextView?.setTextColor(Color.parseColor("#0B50A0"))

        selectedRoomId = clickedId
    }

    private fun saveRoomToDatabase() {
        val roomName = etRoomName.text.toString().trim()

        if (roomName.isEmpty()) {
            etRoomName.error = "Введите название комнаты"
            etRoomName.requestFocus()
            return
        }

        if (selectedRoomId == null) {
            Toast.makeText(this, "Пожалуйста, выберите тип комнаты", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedLayout = findViewById<LinearLayout>(selectedRoomId!!)
        val roomType = selectedLayout?.findTextViewInside()?.text.toString()

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        val uniqueNumericId = System.currentTimeMillis().toString() + (100..999).random().toString()

        val roomMap = hashMapOf(
            "id" to uniqueNumericId,
            "name" to roomName,
            "type" to roomType,
            "userID" to currentUserId
        )

        db.collection("rooms").document()
            .set(roomMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Комната успешно добавлена!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun LinearLayout.findTextViewInside(): TextView? {
        for (i in 0 until this.childCount) {
            val child = this.getChildAt(i)
            if (child is TextView) {
                return child
            }
        }
        return null
    }
}