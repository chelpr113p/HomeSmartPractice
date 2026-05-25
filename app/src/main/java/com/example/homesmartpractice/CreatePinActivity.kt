package com.example.homesmartpractice

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CreatePinActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_pin)

        findViewById<Button>(R.id.btn_sec).setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }
}