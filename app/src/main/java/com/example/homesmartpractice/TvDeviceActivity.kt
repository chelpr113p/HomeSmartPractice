package com.example.homesmartpractice

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo // НЕОБХОДИМО ДОБАВИТЬ
import android.widget.Button
import android.widget.EditText // ИЗМЕНИЛИ С TextView НА EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.firestore.FirebaseFirestore

class TvDeviceActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var deviceId: String = ""

    private lateinit var tvAcName: TextView
    private lateinit var switchPower: SwitchMaterial
    private lateinit var tvVolumeValue: TextView
    private lateinit var sbVolume: SeekBar
    private lateinit var tvBrightnessValue: TextView
    private lateinit var sbBrightness: SeekBar
    private lateinit var switchNightmode: SwitchMaterial

    // Изменили тип с TextView на EditText
    private lateinit var tvChannelValue: EditText

    private lateinit var btnChannelMinus: Button
    private lateinit var btnChannelPlus: Button
    private lateinit var btnBack: ImageButton
    private lateinit var groupTvControls: Group

    private var currentChannel: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv)

        deviceId = intent.getStringExtra("DEVICE_ID") ?: ""

        if (deviceId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID устройства не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()

        btnBack.setOnClickListener { finish() }

        listenToTvDevice()
        setupUiListeners()
    }

    private fun initViews() {
        tvAcName = findViewById(R.id.tvAcName)
        switchPower = findViewById(R.id.switchAc)
        tvVolumeValue = findViewById(R.id.tvVolumeValue)
        sbVolume = findViewById(R.id.sbTemperature)
        tvBrightnessValue = findViewById(R.id.tvBrightnessValue)
        sbBrightness = findViewById(R.id.sbTemperature1)
        switchNightmode = findViewById(R.id.switchNightmode)

        // Поиск EditText в разметке
        tvChannelValue = findViewById(R.id.tvChannelValue)

        btnChannelMinus = findViewById(R.id.btnTempMinus)
        btnChannelPlus = findViewById(R.id.btnTempPlus)
        btnBack = findViewById(R.id.btnBack)
        groupTvControls = findViewById(R.id.groupTvControls)
    }

    private fun listenToTvDevice() {
        db.collection("tv_device").document(deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Ошибка чтения данных: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val name = snapshot.getString("name") ?: "Телевизор"
                    val status = snapshot.getBoolean("status") ?: false
                    val volume = snapshot.getLong("volume")?.toInt() ?: 0
                    val brightness = snapshot.getLong("brightness")?.toInt() ?: 0
                    val nightmode = snapshot.getBoolean("nightmode") ?: false
                    currentChannel = snapshot.getLong("channel")?.toInt() ?: 1

                    tvAcName.text = name
                    switchPower.isChecked = status

                    groupTvControls.visibility = if (status) View.VISIBLE else View.GONE

                    sbVolume.progress = volume
                    tvVolumeValue.text = "$volume%"

                    sbBrightness.progress = brightness
                    tvBrightnessValue.text = "$brightness%"

                    switchNightmode.isChecked = nightmode

                    // Обновляем текст в поле ввода, только если пользователь сам сейчас его не редактирует
                    if (!tvChannelValue.isFocused) {
                        tvChannelValue.setText(currentChannel.toString())
                    }
                }
            }
    }

    private fun setupUiListeners() {
        val docRef = db.collection("tv_device").document(deviceId)

        // 1. Главный выключатель (Status)
        switchPower.setOnCheckedChangeListener { _, isChecked ->
            groupTvControls.visibility = if (isChecked) View.VISIBLE else View.GONE
            docRef.update("status", isChecked)
        }

        // 2. Ползунок Громкости
        sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (switchNightmode.isChecked && progress > 30) {
                    seekBar?.progress = 30
                    tvVolumeValue.text = "30%"
                } else {
                    tvVolumeValue.text = "$progress%"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { docRef.update("volume", it.progress) }
            }
        })

        // 3. Ползунок Яркости
        sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (switchNightmode.isChecked && progress > 50) {
                    seekBar?.progress = 50
                    tvBrightnessValue.text = "50%"
                } else {
                    tvBrightnessValue.text = "$progress%"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { docRef.update("brightness", it.progress) }
            }
        })

        // 4. Переключатель Ночного Режима
        switchNightmode.setOnCheckedChangeListener { _, isChecked ->
            val updates = mutableMapOf<String, Any>("nightmode" to isChecked)
            if (isChecked) {
                val currentBrightness = sbBrightness.progress
                val currentVolume = sbVolume.progress
                updates["brightness"] = if (currentBrightness > 50) 50 else currentBrightness
                updates["volume"] = if (currentVolume > 30) 30 else currentVolume
            }
            docRef.update(updates).addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка обновления режима: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Переключение каналов (Минус)
        btnChannelMinus.setOnClickListener {
            if (currentChannel > 1) {
                docRef.update("channel", currentChannel - 1)
            }
        }

        // 6. Переключение каналов (Плюс)
        btnChannelPlus.setOnClickListener {
            docRef.update("channel", currentChannel + 1)
        }

        // 7. СЛУШАТЕЛЬ РУЧНОГО ВВОДА КАНАЛА
        tvChannelValue.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val inputText = tvChannelValue.text.toString()

                if (inputText.isNotEmpty()) {
                    val enteredChannel = inputText.toInt()

                    if (enteredChannel > 0) {
                        docRef.update("channel", enteredChannel)
                    } else {
                        // Если ввели 0 или меньше, возвращаем старый канал
                        tvChannelValue.setText(currentChannel.toString())
                        Toast.makeText(this, "Номер канала должен быть больше 0", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Если поле оставили пустым, возвращаем старое значение
                    tvChannelValue.setText(currentChannel.toString())
                }

                tvChannelValue.clearFocus() // Снимаем фокус с поля ввода
                false // Скрывает клавиатуру автоматически
            } else {
                false
            }
        }
    }
}