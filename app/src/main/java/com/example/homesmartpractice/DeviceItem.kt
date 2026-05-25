package com.example.homesmartpractice.model

import androidx.annotation.DrawableRes

data class DeviceItem(
    val name: String,
    @DrawableRes val iconRes: Int,
    var isEnabled: Boolean = false // для переключателя
)