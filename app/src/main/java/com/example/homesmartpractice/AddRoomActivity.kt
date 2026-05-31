package com.example.homesmartpractice

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AddRoomActivity : AppCompatActivity() {

    private var selectedRoomId: Int? = null
    private lateinit var etRoomName: EditText
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()

    private val sharedPref by lazy { getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
    private val currentUserId by lazy { sharedPref.getString("USER_ID", "") ?: "" }

    // Регистрируем лаунчер для ожидания результата от AddCustomRoomActivity
    private val customRoomLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Если в AddCustomRoomActivity сохранение прошло успешно,
            // закрываем и эту активити тоже
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_room)

        etRoomName = findViewById(R.id.et_room_name)
        btnSave = findViewById(R.id.btnSave)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val roomLayouts = listOf(
            findViewById<LinearLayout>(R.id.layout_living_room),
            findViewById<LinearLayout>(R.id.layout_kitchen),
            findViewById<LinearLayout>(R.id.layout_custom), // Это наш layout_custom ("Другая")
            findViewById<LinearLayout>(R.id.layout_office),
            findViewById<LinearLayout>(R.id.layout_bedroom),
            findViewById<LinearLayout>(R.id.layout_hall)
        )

        roomLayouts.forEach { layout ->
            layout.setOnClickListener { clickedView ->
                val clickedId = clickedView.id

                // ПРОВЕРКА: Если нажали на "Другая" (layout_custom)
                if (clickedId == R.id.layout_custom) {
                    val intent = Intent(this, AddCustomRoomActivity::class.java)
                    customRoomLauncher.launch(intent)
                    return@setOnClickListener
                }

                if (selectedRoomId == clickedId) return@setOnClickListener

                selectedRoomId?.let { oldId ->
                    val oldLayout = findViewById<LinearLayout>(oldId)
                    oldLayout?.isSelected = false
                    val oldTextView = oldLayout?.findTextViewInside()
                    oldTextView?.setTextColor(Color.parseColor("#94949B"))
                }

                clickedView.isSelected = true
                val currentTextView = (clickedView as LinearLayout).findTextViewInside()
                currentTextView?.setTextColor(Color.parseColor("#0B50A0"))

                selectedRoomId = clickedId
            }
        }

        btnSave.setOnClickListener {
            saveRoomToDatabase()
        }
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