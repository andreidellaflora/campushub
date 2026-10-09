package br.com.example.loginfirebase

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MeusFavoritosActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var container: LinearLayout

    private val eventosExibidos = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meus_favoritos)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        container = findViewById(R.id.meusFavoritosContainer)

        findViewById<Button>(R.id.btnVoltar).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        carregarFavoritos()
    }

    private fun carregarFavoritos() {
        val usuario = auth.currentUser

        if (usuario == null) {
            finish()
            return
        }

        container.removeAllViews()
        eventosExibidos.clear()

        db.collection("favoritos")
            .whereEqualTo("usuarioId", usuario.uid)
            .get()
            .addOnSuccessListener { documentos ->

                if (documentos.isEmpty) {
                    mostrarMensagem("Você ainda não possui eventos favoritos.")
                    return@addOnSuccessListener
                }

                val favoritosUnicos = documentos.documents
                    .mapNotNull { documento ->
                        documento.getString("eventoId")?.let { eventoId ->
                            eventoId to documento.id
                        }
                    }
                    .distinctBy { it.first }

                for ((eventoId, favoritoId) in favoritosUnicos) {
                    carregarEvento(eventoId, favoritoId)
                }
            }
            .addOnFailureListener {
                mostrarMensagem("Erro ao carregar favoritos.")
            }
    }

    private fun mostrarMensagem(texto: String) {
        val mensagem = TextView(this)
        mensagem.text = texto
        mensagem.textSize = 16f
        mensagem.setTextColor(Color.DKGRAY)
        container.addView(mensagem)
    }

    private fun carregarEvento(eventoId: String, favoritoId: String) {
        if (!eventosExibidos.add(eventoId)) {
            return
        }

        db.collection("eventos")
            .document(eventoId)
            .get()
            .addOnSuccessListener { evento ->

                if (!evento.exists()) {
                    return@addOnSuccessListener
                }

                val card = LinearLayout(this)
                card.orientation = LinearLayout.VERTICAL
                card.setPadding(20, 20, 20, 20)
                card.setBackgroundColor(Color.WHITE)

                val parametros = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                parametros.setMargins(0, 0, 0, 16)
                card.layoutParams = parametros

                val titulo = TextView(this)
                titulo.text = evento.getString("titulo") ?: "Evento"
                titulo.textSize = 19f
                titulo.setTypeface(null, Typeface.BOLD)
                titulo.setTextColor(Color.rgb(94, 53, 177))

                val data = TextView(this)
                data.text = "${evento.getString("data") ?: ""} • ${evento.getString("horario") ?: ""}"
                data.textSize = 14f
                data.setTextColor(Color.DKGRAY)

                val local = TextView(this)
                local.text = evento.getString("local") ?: ""
                local.textSize = 14f
                local.setTextColor(Color.GRAY)

                val btnDetalhes = Button(this)
                btnDetalhes.text = "VER DETALHES"
                btnDetalhes.setOnClickListener {
                    val intent = Intent(this, DetalhesEventoActivity::class.java)
                    intent.putExtra("eventoId", eventoId)
                    startActivity(intent)
                }

                val btnDesfavoritar = Button(this)
                btnDesfavoritar.text = "DESFAVORITAR"

                btnDesfavoritar.setOnClickListener {
                    btnDesfavoritar.isEnabled = false

                    db.collection("favoritos")
                        .document(favoritoId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Evento removido dos favoritos!",
                                Toast.LENGTH_SHORT
                            ).show()
                            carregarFavoritos()
                        }
                        .addOnFailureListener {
                            btnDesfavoritar.isEnabled = true
                            Toast.makeText(
                                this,
                                "Erro ao remover favorito.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                card.addView(titulo)
                card.addView(data)
                card.addView(local)
                card.addView(btnDetalhes)
                card.addView(btnDesfavoritar)

                container.addView(card)
            }
            .addOnFailureListener {
                eventosExibidos.remove(eventoId)
            }
    }
}