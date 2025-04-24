package com.example.bellatrix_front

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // 🔁 Récupération des données passées via l’intent
        val nom = intent.getStringExtra("nom") ?: ""
        val prenom = intent.getStringExtra("prenom") ?: ""
        val etablissement = intent.getStringExtra("etablissement") ?: ""
        val email = intent.getStringExtra("email") ?: ""

        val token = intent.getStringExtra("token") ?: ""
        val userId = intent.getIntExtra("user_id", -1)
        val roleId = intent.getIntExtra("role_id", -1)

        // 🔤 Assignation dans les vues
        findViewById<TextView>(R.id.nomTextView).text = "Nom : $nom"
        findViewById<TextView>(R.id.prenomTextView).text = "Prénom : $prenom"
        findViewById<TextView>(R.id.etablissementTextView).text = "Établissement : $etablissement"
        findViewById<TextView>(R.id.emailTextView).text = "Email : $email"

        // 🔙 Bouton retour à l’accueil
        findViewById<Button>(R.id.backToHomeButton).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("token", token)
                putExtra("user_id", userId)
                putExtra("role_id", roleId)
                putExtra("nom", nom)
                putExtra("prenom", prenom)
                putExtra("etablissement", etablissement)
                putExtra("email", email)
            }
            startActivity(intent)
            finish()
        }
    }
}
