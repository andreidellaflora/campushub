package br.com.example.loginfirebase

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnPerfil = findViewById<Button>(R.id.btnPerfil)

        btnPerfil.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            startActivity(intent)
        }

        val btnSair = findViewById<Button>(R.id.btnSair)

        btnSair.setOnClickListener {
            auth.signOut()

            val intent = Intent(this, MainActivity::class.java)

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }

        carregarEventos()
    }

    private fun carregarEventos() {

        val eventosContainer =
            findViewById<LinearLayout>(R.id.eventosContainer)

        db.collection("eventos")
            .get()
            .addOnSuccessListener { documentos ->

                eventosContainer.removeAllViews()

                if (documentos.isEmpty) {

                    val mensagem = TextView(this)

                    mensagem.text = "Nenhum evento cadastrado."
                    mensagem.textSize = 16f
                    mensagem.setTextColor(Color.GRAY)

                    eventosContainer.addView(mensagem)

                    return@addOnSuccessListener
                }

                for (documento in documentos) {

                    val titulo =
                        documento.getString("titulo") ?: "Sem título"

                    val data =
                        documento.getString("data") ?: "Sem data"

                    val horario =
                        documento.getString("horario") ?: "Sem horário"

                    val local =
                        documento.getString("local") ?: "Sem local"

                    val card = LinearLayout(this)

                    card.orientation = LinearLayout.VERTICAL
                    card.setPadding(20, 20, 20, 20)
                    card.setBackgroundColor(Color.WHITE)

                    val parametros =
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )

                    parametros.setMargins(0, 0, 0, 16)

                    card.layoutParams = parametros

                    val txtTitulo = TextView(this)

                    txtTitulo.text = titulo
                    txtTitulo.textSize = 19f
                    txtTitulo.setTextColor(Color.rgb(94, 53, 177))
                    txtTitulo.setTypeface(
                        null,
                        android.graphics.Typeface.BOLD
                    )

                    val txtData = TextView(this)

                    txtData.text = "$data • $horario"
                    txtData.textSize = 14f
                    txtData.setTextColor(Color.DKGRAY)

                    val txtLocal = TextView(this)

                    txtLocal.text = local
                    txtLocal.textSize = 14f
                    txtLocal.setTextColor(Color.GRAY)

                    val btnDetalhes = Button(this)

                    btnDetalhes.text = "VER DETALHES"
                    btnDetalhes.setTextColor(Color.WHITE)
                    btnDetalhes.setBackgroundColor(
                        Color.rgb(106, 75, 188)
                    )

                    val eventoId = documento.id

                    btnDetalhes.setOnClickListener {

                        val intent =
                            Intent(
                                this,
                                DetalhesEventoActivity::class.java
                            )

                        intent.putExtra(
                            "eventoId",
                            eventoId
                        )

                        startActivity(intent)
                    }

                    card.addView(txtTitulo)
                    card.addView(txtData)
                    card.addView(txtLocal)
                    card.addView(btnDetalhes)

                    eventosContainer.addView(card)
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Erro ao carregar eventos",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}