package com.example.homesmartpractice.model

import androidx.annotation.DrawableRes

data class DeviceItem(
    val docId: String,             // ID документа в Firestore
    val name: String,              // Отображаемое имя (Тип + Название)
    val type: String,              // ДОБАВИЛИ: тип устройства (например, "Теплый пол")
    @DrawableRes val iconRes: Int, // Иконка
    var isEnabled: Boolean = false // Статус свитча
)