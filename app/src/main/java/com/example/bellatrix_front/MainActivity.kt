package com.example.bellatrix_front

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bellatrix_front.ui.theme.BellatrixfrontTheme
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient()
    private val baseUrl = "http://10.0.2.2/bellatrix-backend/public"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = intent.getStringExtra("token") ?: ""
        val userId = intent.getIntExtra("user_id", -1)
        val roleId = intent.getIntExtra("role_id", -1)

        enableEdgeToEdge()

        setContent {
            BellatrixfrontTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    MainScreen(
                        token = token,
                        userId = userId,
                        roleId = roleId,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    @Composable
    fun MainScreen(token: String, userId: Int, roleId: Int, modifier: Modifier = Modifier) {

        var activities by remember { mutableStateOf(listOf<Activity>()) }
        var userReservations by remember { mutableStateOf(listOf<Reservation>()) }
        val context = LocalContext.current

        fun refreshReservations() {
            fetchUserReservations(token, userId) { result ->
                userReservations = result
            }
        }

        LaunchedEffect(true) {
            fetchActivities(token) { result ->
                activities = result
            }

            fetchUserReservations(token, userId) { result ->
                userReservations = result
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✅ Connecté", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = {
                        val intent = android.content.Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Se déconnecter", color = MaterialTheme.colorScheme.onError)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("🔐 Token: $token")
            Text("👤 User ID: $userId")
            Text("🛡️ Role ID: $roleId")

            if (roleId == 2 || roleId == 3) {
                Button(
                    onClick = {
                        val intent = Intent(context, AdminPanelActivity::class.java).apply {
                            putExtra("token", token)
                            putExtra("user_id", userId)
                            putExtra("role_id", roleId)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("🛠️ Panel Admin")
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val intent = android.content.Intent(context, MyReservationsActivity::class.java).apply {
                        putExtra("token", token)
                        putExtra("user_id", userId)
                        putExtra("role_id", roleId)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text("📄 Mes Inscriptions")
            }

            Spacer(modifier = Modifier.height(16.dp))


            Spacer(modifier = Modifier.height(16.dp))
            Text("📚 Activités disponibles:", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(activities) { activity ->
                    ActivityCard(
                        activity = activity,
                        token = token,
                        userId = userId,
                        reservations = userReservations,
                        onReservationChanged = { refreshReservations() }
                    )
                }
            }
        }

    }

    @Composable
    fun ActivityCard(
        activity: Activity,
        token: String,
        userId: Int,
        reservations: List<Reservation>,
        onReservationChanged: () -> Unit
    ) {
        val context = LocalContext.current
        val currentReservation = reservations.find { it.activiteId == activity.id }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = activity.titre, style = MaterialTheme.typography.titleMedium)
                Text(text = "📍 ${activity.lieu} — 🗓️ ${activity.date}")
                Text(text = "👥 ${activity.capacite} places — Statut: ${activity.statut}")
                Text(text = "📝 ${activity.description}")

                Spacer(modifier = Modifier.height(8.dp))

                if (currentReservation != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("✅ Inscrit", color = MaterialTheme.colorScheme.primary)
                        Button(onClick = {
                            deleteReservation(token, currentReservation!!.reservationId) { success ->
                                runOnUiThread {
                                    if (success) {
                                        Toast.makeText(context, "Désinscrit", Toast.LENGTH_SHORT).show()
                                        onReservationChanged()
                                    } else {
                                        Toast.makeText(context, "Erreur désinscription", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Se désinscrire", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                } else {
                    Button(onClick = {
                        inscrireAActivite(token, userId, activity.id) { success ->
                            runOnUiThread {
                                if (success) {
                                    Toast.makeText(context, "Réservé !", Toast.LENGTH_SHORT).show()
                                    onReservationChanged() // 🧠 Recharge les vraies données
                                } else {
                                    Toast.makeText(context, "Erreur lors de l'inscription", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Text("S'inscrire")
                    }
                }
            }
        }
    }

    private fun fetchUserReservations(token: String, userId: Int, callback: (List<Reservation>) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/reservations/user/$userId")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val jsonArray = JSONArray(body)
                    val reservations = mutableListOf<Reservation>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        reservations.add(
                            Reservation(
                                reservationId = obj.getInt("id_reservation"),
                                activiteId = obj.getInt("activite_id")
                            )
                        )
                    }
                    callback(reservations)
                } else {
                    callback(emptyList())
                }
            }
        })
    }

    private fun inscrireAActivite(token: String, userId: Int, activiteId: Int, callback: (Boolean) -> Unit) {
        val json = JSONObject().apply {
            put("utilisateur_id", userId)
            put("activite_id", activiteId)
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$baseUrl/reservations")
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }

    private fun fetchActivities(token: String, callback: (List<Activity>) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/activites")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val jsonArray = JSONArray(body)
                    val activities = mutableListOf<Activity>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        activities.add(
                            Activity(
                                id = obj.getInt("id_activite"),
                                titre = obj.getString("titre"),
                                description = obj.getString("description"),
                                lieu = obj.getString("lieu"),
                                date = obj.getString("date"),
                                capacite = obj.getInt("capacite"),
                                statut = obj.getString("statut"),
                                organisateurId = obj.getInt("organisateur_id")
                            )
                        )
                    }
                    callback(activities)
                } else {
                    callback(emptyList())
                }
            }
        })
    }

    private fun deleteReservation(token: String, reservationId: Int, callback: (Boolean) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/reservations/$reservationId")
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }
}

// 📦 Data class pour une activité
data class Activity(
    val id: Int,
    val titre: String,
    val description: String,
    val lieu: String,
    val date: String,
    val capacite: Int,
    val statut: String,
    val organisateurId: Int
)

data class Reservation(
    val reservationId: Int,
    val activiteId: Int
)
