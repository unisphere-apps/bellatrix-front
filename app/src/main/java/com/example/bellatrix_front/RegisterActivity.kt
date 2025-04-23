package com.example.bellatrix_front

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class RegisterActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val baseUrl = "http://10.0.2.2/bellatrix-backend/public"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nomEditText = findViewById<EditText>(R.id.nomEditText)
        val prenomEditText = findViewById<EditText>(R.id.prenomEditText)
        val etablissementEditText = findViewById<EditText>(R.id.etablissementEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val nom = nomEditText.text.toString().trim()
            val prenom = prenomEditText.text.toString().trim()
            val etablissement = etablissementEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (nom.isEmpty() || prenom.isEmpty() || etablissement.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(nom, prenom, etablissement, email, password)
        }

        val loginLink = findViewById<TextView>(R.id.linkToLogin)
        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

    }

    private fun registerUser(nom: String, prenom: String, etablissement: String, email: String, password: String) {
        val json = JSONObject().apply {
            put("nom", nom)
            put("prenom", prenom)
            put("etablissement", etablissement)
            put("email", email)
            put("password", password)
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$baseUrl/register")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                runOnUiThread {
                    when (code) {
                        201 -> {
                            Toast.makeText(this@RegisterActivity, "Inscription réussie 🎉", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                            finish()
                        }
                        409 -> Toast.makeText(this@RegisterActivity, "Email déjà utilisé", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(this@RegisterActivity, "Erreur ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
