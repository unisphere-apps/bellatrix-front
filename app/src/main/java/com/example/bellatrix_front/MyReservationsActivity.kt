package com.example.bellatrix_front

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MyReservationsActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val baseUrl = "http://10.0.2.2/bellatrix-backend/public"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reservations)

        val listView = findViewById<ListView>(R.id.reservationListView)

        // ✅ Récupère les données une seule fois
        val incomingIntent = intent
        val token = incomingIntent.getStringExtra("token") ?: ""
        val userId = incomingIntent.getIntExtra("user_id", -1)
        val roleId = incomingIntent.getIntExtra("role_id", -1)

        val backButton = findViewById<Button>(R.id.backToHomeButton)
        backButton.setOnClickListener {
            val backIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("token", incomingIntent.getStringExtra("token"))
                putExtra("user_id", incomingIntent.getIntExtra("user_id", -1))
                putExtra("role_id", incomingIntent.getIntExtra("role_id", -1))
                putExtra("nom", incomingIntent.getStringExtra("nom"))
                putExtra("prenom", incomingIntent.getStringExtra("prenom"))
                putExtra("etablissement", incomingIntent.getStringExtra("etablissement"))
                putExtra("email", incomingIntent.getStringExtra("email"))
            }
            startActivity(backIntent)
            finish()
        }

        val request = Request.Builder()
            .url("$baseUrl/reservations/user/$userId")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MyReservationsActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val jsonArray = JSONArray(responseBody)
                    val reservationDescriptions = mutableListOf<String>()
                    var pending = jsonArray.length()

                    if (pending == 0) {
                        runOnUiThread {
                            listView.adapter = ArrayAdapter(
                                this@MyReservationsActivity,
                                android.R.layout.simple_list_item_1,
                                listOf("Aucune inscription")
                            )
                        }
                        return
                    }

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val activiteId = obj.getInt("activite_id")

                        fetchActivityById(activiteId, token) { activityDescription ->
                            reservationDescriptions.add(activityDescription)
                            pending--
                            if (pending == 0) {
                                runOnUiThread {
                                    listView.adapter = ArrayAdapter(
                                        this@MyReservationsActivity,
                                        android.R.layout.simple_list_item_1,
                                        reservationDescriptions
                                    )
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun fetchActivityById(id: Int, token: String, callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/activites/$id")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Activité ID: $id — ⚠️ Erreur de chargement")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val json = JSONArray(body).getJSONObject(0) // ✅ ici
                    val titre = json.getString("titre")
                    val lieu = json.getString("lieu")
                    val date = json.getString("date")
                    val description = json.getString("description")
                    val capacite = json.getInt("capacite")

                    val info = """
            🎉 $titre
            📍 Lieu : $lieu
            🗓️ Date : $date
            👥 Capacité : $capacite
            📝 $description
        """.trimIndent()

                    callback(info)
                } else {
                    callback("Activité ID: $id — ❌ Erreur ${response.code}")
                }
            }
        })
    }
}
