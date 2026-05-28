package com.example.homesmartpractice.model

import androidx.annotation.DrawableRes

data class DeviceItem(
    val docId: String,       // ID документа в Firestore (добавили это поле)
    val name: String,        // Название (тип + имя)
    @DrawableRes val iconRes: Int, // Иконка
    var isEnabled: Boolean = false // Статус свитча
)