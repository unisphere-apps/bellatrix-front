package com.example.bellatrix_front

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class LoginActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val baseUrl = "http://10.0.2.2/bellatrix-backend/public" // localhost pour Android Emulator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Remplis tous les champs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$baseUrl/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val token = jsonResponse.getString("token")
                    val userId = jsonResponse.getInt("user_id")
                    val roleId = jsonResponse.getInt("role_id")

                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Connecté !", Toast.LENGTH_SHORT).show()

                        // Passer les données à MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("token", token)
                            putExtra("user_id", userId)
                            putExtra("role_id", roleId)
                        }

                        startActivity(intent)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Identifiants invalides", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
