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

class MeusEventosActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meus_eventos)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        val container = findViewById<LinearLayout>(R.id.meusEventosContainer)

        val usuario = auth.currentUser

        if (usuario == null) {
            finish()
            return
        }

        btnVoltar.setOnClickListener {
            finish()
        }

        db.collection("inscricoes")
            .whereEqualTo("usuarioId", usuario.uid)
            .get()
            .addOnSuccessListener { inscricoes ->

                container.removeAllViews()

                if (inscricoes.isEmpty) {
                    val mensagem = TextView(this)

                    mensagem.text = "Você ainda não está inscrito em nenhum evento."
                    mensagem.textSize = 16f
                    mensagem.setTextColor(Color.DKGRAY)

                    container.addView(mensagem)

                    return@addOnSuccessListener
                }

                for (inscricao in inscricoes) {

                    val eventoId = inscricao.getString("eventoId")

                    if (eventoId == null) {
                        continue
                    }

                    db.collection("eventos")
                        .document(eventoId)
                        .get()
                        .addOnSuccessListener { evento ->

                            if (!evento.exists()) {
                                return@addOnSuccessListener
                            }

                            val titulo =
                                evento.getString("titulo") ?: "Evento"

                            val data =
                                evento.getString("data") ?: ""

                            val horario =
                                evento.getString("horario") ?: ""

                            val local =
                                evento.getString("local") ?: ""

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
                            txtTitulo.setTextColor(
                                Color.rgb(94, 53, 177)
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

                            btnDetalhes.setOnClickListener {

                                val intent = Intent(
                                    this,
                                    DetalhesEventoActivity::class.java
                                )

                                intent.putExtra(
                                    "eventoId",
                                    eventoId
                                )

                                startActivity(intent)
                            }

                            val btnCancelar = Button(this)

                            btnCancelar.text = "CANCELAR INSCRIÇÃO"
                            btnCancelar.setTextColor(Color.WHITE)
                            btnCancelar.setBackgroundColor(
                                Color.rgb(190, 50, 50)
                            )

                            btnCancelar.setOnClickListener {

                                db.collection("inscricoes")
                                    .document(inscricao.id)
                                    .delete()
                                    .addOnSuccessListener {

                                        Toast.makeText(
                                            this,
                                            "Inscrição cancelada!",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        carregarEventos()
                                    }
                                    .addOnFailureListener { erro ->

                                        Toast.makeText(
                                            this,
                                            "Erro ao cancelar: ${erro.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }

                            card.addView(txtTitulo)
                            card.addView(txtData)
                            card.addView(txtLocal)
                            card.addView(btnDetalhes)
                            card.addView(btnCancelar)

                            container.addView(card)
                        }
                }
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    this,
                    "Erro ao carregar eventos: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun carregarEventos() {
        recreate()
    }
}