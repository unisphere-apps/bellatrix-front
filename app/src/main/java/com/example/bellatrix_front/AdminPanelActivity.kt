package com.example.bellatrix_front

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AdminPanelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        val backButton = findViewById<Button>(R.id.backToHomeButton)
        backButton.setOnClickListener {
            val homeIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("token", intent.getStringExtra("token"))
                putExtra("user_id", intent.getIntExtra("user_id", -1))
                putExtra("role_id", intent.getIntExtra("role_id", -1))
            }
            startActivity(homeIntent)
            finish()
        }
    }
}
