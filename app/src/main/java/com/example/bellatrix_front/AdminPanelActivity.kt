// Dans AdminPanelActivity.kt

package com.example.bellatrix_front

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor

class AdminPanelActivity : AppCompatActivity() {

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        OkHttpClient.Builder().addInterceptor(logging).build()
    }

    private val baseUrl = "http://10.0.2.2/bellatrix-backend/public"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        val token = intent.getStringExtra("token") ?: ""
        val userId = intent.getIntExtra("user_id", -1)

        findViewById<Button>(R.id.backToHomeButton).setOnClickListener { finish() }

        findViewById<Button>(R.id.addActivityButton).setOnClickListener {
            findViewById<Button>(R.id.addActivityButton).setOnClickListener {
                val titre = findViewById<EditText>(R.id.titreEditText).text.toString()
                val description = findViewById<EditText>(R.id.descriptionEditText).text.toString()
                val lieu = findViewById<EditText>(R.id.lieuEditText).text.toString()
                val date = findViewById<EditText>(R.id.dateEditText).text.toString()
                val capacite = findViewById<EditText>(R.id.capaciteEditText).text.toString().toIntOrNull() ?: 0

                val json = JSONObject().apply {
                    put("titre", titre)
                    put("description", description)
                    put("lieu", lieu)
                    put("date", date)
                    put("capacite", capacite)
                    put("statut", "ouverte")
                    put("organisateur_id", userId)
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
                                Toast.makeText(this@AdminPanelActivity, "Erreur lors de l'ajout ❌", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
            }
        }

        findViewById<Button>(R.id.viewAllActivitiesButton).setOnClickListener {
            fetchAllActivities(token)
        }
    }

    private fun fetchAllActivities(token: String) {
        val request = Request.Builder()
            .url("$baseUrl/activites")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AdminPanelActivity, "Erreur chargement", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val jsonArray = JSONArray(body)
                    val activityList = mutableListOf<String>()
                    val idList = mutableListOf<Int>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val title = obj.getString("titre")
                        val id = obj.getInt("id_activite")
                        activityList.add("#${id} - $title")
                        idList.add(id)
                    }

                    runOnUiThread {
                        val listView = ListView(this@AdminPanelActivity)
                        listView.adapter = ArrayAdapter(this@AdminPanelActivity, android.R.layout.simple_list_item_1, activityList)

                        val dialog = AlertDialog.Builder(this@AdminPanelActivity)
                            .setTitle("Toutes les activités")
                            .setView(listView)
                            .setNegativeButton("Fermer", null)
                            .create()

                        listView.setOnItemClickListener { _, _, position, _ ->
                            val selectedId = idList[position]
                            showActivityOptionsDialog(token, selectedId)
                            dialog.dismiss()
                        }

                        dialog.show()
                    }
                }
            }
        })
    }

    private fun showActivityOptionsDialog(token: String, activityId: Int) {
        val options = arrayOf("Voir les inscriptions", "Supprimer l'activité")

        AlertDialog.Builder(this)
            .setTitle("Options pour l'activité #$activityId")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> fetchRegistrationsForActivity(token, activityId)
                    1 -> deleteActivity(token, activityId)
                }
            }
            .show()
    }

    private fun fetchRegistrationsForActivity(token: String, activityId: Int) {
        val request = Request.Builder()
            .url("$baseUrl/reservations/activite/$activityId")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AdminPanelActivity, "Erreur chargement inscriptions", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val jsonArray = JSONArray(body)
                    val entries = mutableListOf<String>()
                    val resIds = mutableListOf<Int>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val resId = obj.getInt("id_reservation")
                        val userId = obj.getInt("utilisateur_id")
                        entries.add("Réservation $resId - Utilisateur $userId")
                        resIds.add(resId)
                    }

                    runOnUiThread {
                        val listView = ListView(this@AdminPanelActivity)
                        listView.adapter = ArrayAdapter(this@AdminPanelActivity, android.R.layout.simple_list_item_1, entries)

                        val dialog = AlertDialog.Builder(this@AdminPanelActivity)
                            .setTitle("Inscriptions pour l'activité #$activityId")
                            .setView(listView)
                            .setNegativeButton("Fermer", null)
                            .create()

                        listView.setOnItemClickListener { _, _, position, _ ->
                            val resId = resIds[position]
                            deleteReservation(token, resId)
                            dialog.dismiss()
                        }

                        dialog.show()
                    }
                }
            }
        })
    }

    private fun deleteActivity(token: String, activityId: Int) {
        val request = Request.Builder()
            .url("$baseUrl/activites/$activityId")
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AdminPanelActivity, "Erreur suppression", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AdminPanelActivity, "Activité + inscriptions supprimées ✅", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AdminPanelActivity, "Erreur lors de la suppression ❌", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun deleteReservation(token: String, reservationId: Int) {
        val request = Request.Builder()
            .url("$baseUrl/reservations/$reservationId")
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AdminPanelActivity, "Erreur suppression inscription", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AdminPanelActivity, "Inscription supprimée", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AdminPanelActivity, "Erreur suppression inscription", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
