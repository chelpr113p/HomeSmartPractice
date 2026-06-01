package com.example.homesmartpractice

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class TermDeviceActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var deviceId: String = ""

    private lateinit var tvAcName: TextView
    private lateinit var switchPower: SwitchMaterial
    private lateinit var tvTempValue: TextView
    private lateinit var sbTemperature: SeekBar
    private lateinit var switchTime: SwitchMaterial
    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndTime: TextInputEditText
    private lateinit var btnBack: ImageButton
    private lateinit var groupTermControls: Group

    // Теперь храним время как строки
    private var currentStartTime: String = "22:00"
    private var currentEndTime: String = "06:00"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp)

        deviceId = intent.getStringExtra("DEVICE_ID") ?: ""

        if (deviceId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID устройства не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()

        btnBack.setOnClickListener { finish() }

        listenToTermDevice()
        setupUiListeners()
    }

    private fun initViews() {
        tvAcName = findViewById(R.id.tvAcName)
        switchPower = findViewById(R.id.switchAc)
        tvTempValue = findViewById(R.id.tvTempValue)
        sbTemperature = findViewById(R.id.sbTemperature)
        switchTime = findViewById(R.id.switchTime)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        btnBack = findViewById(R.id.btnBack)
        groupTermControls = findViewById(R.id.groupTermControls)
    }

    private fun listenToTermDevice() {
        db.collection("term_device").document(deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Ошибка чтения данных: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val name = snapshot.getString("name") ?: "Теплый пол"
                    val status = snapshot.getBoolean("status") ?: false
                    val systemTemp = snapshot.getLong("system_temp")?.toInt() ?: 20
                    val offtime = snapshot.getBoolean("offtime") ?: false

                    // Считываем время из Firestore как строки
                    currentStartTime = snapshot.getString("time1") ?: "22:00"
                    currentEndTime = snapshot.getString("time2") ?: "06:00"

                    tvAcName.text = name
                    switchPower.isChecked = status

                    groupTermControls.visibility = if (status) View.VISIBLE else View.GONE

                    sbTemperature.progress = systemTemp
                    tvTempValue.text = "$systemTemp°C"

                    switchTime.isChecked = offtime

                    // Устанавливаем строки напрямую в UI
                    etStartTime.setText(currentStartTime)
                    etEndTime.setText(currentEndTime)

                    updateTimeFieldsColor(offtime)
                }
            }
    }

    private fun setupUiListeners() {
        val docRef = db.collection("term_device").document(deviceId)

        // 1. Главный выключатель
        switchPower.setOnCheckedChangeListener { _, isChecked ->
            groupTermControls.visibility = if (isChecked) View.VISIBLE else View.GONE
            docRef.update("status", isChecked)
        }

        // 2. Ползунок температуры
        sbTemperature.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTempValue.text = "$progress°C"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { docRef.update("system_temp", it.progress) }
            }
        })

        // 3. Переключатель времени отключения
        switchTime.setOnCheckedChangeListener { _, isChecked ->
            updateTimeFieldsColor(isChecked)
            docRef.update("offtime", isChecked)
        }

        // 4. Клик на "Время с"
        etStartTime.setOnClickListener {
            showTimePickerDialog(currentStartTime, isStartTime = true)
        }

        // 5. Клик на "Время до"
        etEndTime.setOnClickListener {
            showTimePickerDialog(currentEndTime, isStartTime = false)
        }
    }

    /**
     * Парсит текущую строку времени, открывает диалог и сохраняет результат в формате "ЧЧ:ММ"
     */
    private fun showTimePickerDialog(currentTimeStr: String, isStartTime: Boolean) {
        // Разбиваем строку "22:15" на составляющие: часы и минуты
        val parts = currentTimeStr.split(":")
        val startHour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val startMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                // Форматируем результат в красивую строку "ЧЧ:ММ" (например, 9:5 -> "09:05")
                val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                val fieldName = if (isStartTime) "time1" else "time2"

                // Сохраняем строку в Firestore
                db.collection("term_device").document(deviceId)
                    .update(fieldName, formattedTime)
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка сохранения времени: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            startHour,   // Час для открытия диалога
            startMinute, // Минута для открытия диалога
            true         // 24-часовой формат
        )
        timePickerDialog.show()
    }

    private fun updateTimeFieldsColor(isTimeOffActive: Boolean) {
        val color = if (isTimeOffActive) {
            Color.parseColor("#0B50A0")
        } else {
            Color.parseColor("#94949B")
        }
        etStartTime.setTextColor(color)
        etEndTime.setTextColor(color)
    }
}