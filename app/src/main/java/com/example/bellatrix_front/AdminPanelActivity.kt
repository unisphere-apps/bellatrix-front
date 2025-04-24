package com.example.bellatrix_front

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log

class AdminPanelActivity : AppCompatActivity() {

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    private val baseUrl = "http://10.0.2.2/bellatrix-backend/public"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        val token = intent.getStringExtra("token") ?: ""
        val organisateurId = intent.getIntExtra("user_id", -1)

        val titreEditText = findViewById<EditText>(R.id.titreEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val lieuEditText = findViewById<EditText>(R.id.lieuEditText)
        val dateEditText = findViewById<EditText>(R.id.dateEditText)
        val capaciteEditText = findViewById<EditText>(R.id.capaciteEditText)
        val addActivityButton = findViewById<Button>(R.id.addActivityButton)
        val backToHomeButton = findViewById<Button>(R.id.backToHomeButton)

        addActivityButton.setOnClickListener {
            val titre = titreEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val lieu = lieuEditText.text.toString()
            val date = dateEditText.text.toString()
            val capacite = capaciteEditText.text.toString().toIntOrNull() ?: 0

            Log.d("ADD_ACTIVITY", "titre=$titre, desc=$description, lieu=$lieu, date=$date, capacite=$capacite, organisateur_id=$organisateurId")

            val json = JSONObject().apply {
                put("titre", titre)
                put("description", description)
                put("lieu", lieu)
                put("date", date)
                put("capacite", capacite)
                put("statut", "ouverte")
                put("organisateur_id", organisateurId)
            }

            val body = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()
                .url("$baseUrl/activites")
                .post(body)
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@AdminPanelActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@AdminPanelActivity, "Activité ajoutée ✅", Toast.LENGTH_SHORT).show()
                        } else {
                            val errorBody = response.body?.string()
                            Toast.makeText(this@AdminPanelActivity, "Erreur ajout ❌ : ${response.code}\n$errorBody", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }

        backToHomeButton.setOnClickListener {
            finish()
        }
    }
}
