package com.example.homesmartpractice

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        findViewById<Button>(R.id.btn_next).setOnClickListener {
            startActivity(Intent(this, AddressActivity::class.java))
        }
        findViewById<Button>(R.id.btnExit).setOnClickListener {
            finish()
        }
    }
}