package br.com.example.loginfirebase

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DetalhesEventoActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var processandoFavorito = false
    private var processandoInscricao = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_evento)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val txtTitulo = findViewById<TextView>(R.id.txtTituloEvento)
        val txtData = findViewById<TextView>(R.id.txtDataEvento)
        val txtHorario = findViewById<TextView>(R.id.txtHorarioEvento)
        val txtLocal = findViewById<TextView>(R.id.txtLocalEvento)
        val txtDescricao = findViewById<TextView>(R.id.txtDescricaoEvento)
        val btnInscrever = findViewById<Button>(R.id.btnInscrever)
        val btnFavorito = findViewById<Button>(R.id.btnFavorito)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        val usuario = auth.currentUser

        if (usuario == null) {
            finish()
            return
        }

        val eventoId = intent.getStringExtra("eventoId")

        if (eventoId.isNullOrBlank()) {
            Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uid = usuario.uid
        val inscricaoId = "${uid}_${eventoId}"
        val favoritoId = "${uid}_${eventoId}"

        btnInscrever.isEnabled = false
        btnFavorito.isEnabled = false

        db.collection("eventos")
            .document(eventoId)
            .get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    txtTitulo.text = documento.getString("titulo") ?: "Sem título"
                    txtData.text = documento.getString("data") ?: "Sem data"
                    txtHorario.text = documento.getString("horario") ?: "Sem horário"
                    txtLocal.text = documento.getString("local") ?: "Sem local"
                    txtDescricao.text = documento.getString("descricao") ?: "Sem descrição"
                } else {
                    Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar evento", Toast.LENGTH_SHORT).show()
            }

        db.collection("inscricoes")
            .document(inscricaoId)
            .get()
            .addOnSuccessListener { documento ->
                btnInscrever.text = if (documento.exists()) {
                    "CANCELAR INSCRIÇÃO"
                } else {
                    "INSCREVER-SE"
                }
                btnInscrever.isEnabled = true
            }
            .addOnFailureListener {
                btnInscrever.isEnabled = true
                Toast.makeText(this, "Erro ao verificar inscrição", Toast.LENGTH_SHORT).show()
            }

        db.collection("favoritos")
            .document(favoritoId)
            .get()
            .addOnSuccessListener { documento ->
                btnFavorito.text = if (documento.exists()) {
                    "★ DESFAVORITAR"
                } else {
                    "☆ FAVORITAR"
                }
                btnFavorito.isEnabled = true
            }
            .addOnFailureListener {
                btnFavorito.isEnabled = true
                Toast.makeText(this, "Erro ao verificar favorito", Toast.LENGTH_SHORT).show()
            }

        btnInscrever.setOnClickListener {
            if (processandoInscricao) return@setOnClickListener

            processandoInscricao = true
            btnInscrever.isEnabled = false

            val referencia = db.collection("inscricoes").document(inscricaoId)

            referencia.get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        referencia.delete()
                            .addOnSuccessListener {
                                btnInscrever.text = "INSCREVER-SE"
                                Toast.makeText(this, "Inscrição cancelada!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Erro ao cancelar inscrição", Toast.LENGTH_SHORT).show()
                            }
                            .addOnCompleteListener {
                                processandoInscricao = false
                                btnInscrever.isEnabled = true
                            }
                    } else {
                        val dados = hashMapOf(
                            "usuarioId" to uid,
                            "eventoId" to eventoId,
                            "dataInscricao" to FieldValue.serverTimestamp()
                        )

                        referencia.set(dados)
                            .addOnSuccessListener {
                                btnInscrever.text = "CANCELAR INSCRIÇÃO"
                                Toast.makeText(this, "Inscrição realizada!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Erro ao realizar inscrição", Toast.LENGTH_SHORT).show()
                            }
                            .addOnCompleteListener {
                                processandoInscricao = false
                                btnInscrever.isEnabled = true
                            }
                    }
                }
                .addOnFailureListener {
                    processandoInscricao = false
                    btnInscrever.isEnabled = true
                    Toast.makeText(this, "Erro ao verificar inscrição", Toast.LENGTH_SHORT).show()
                }
        }

        btnFavorito.setOnClickListener {
            if (processandoFavorito) return@setOnClickListener

            processandoFavorito = true
            btnFavorito.isEnabled = false

            val referencia = db.collection("favoritos").document(favoritoId)

            referencia.get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        referencia.delete()
                            .addOnSuccessListener {
                                btnFavorito.text = "☆ FAVORITAR"
                                Toast.makeText(this, "Evento removido dos favoritos!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Erro ao desfavoritar", Toast.LENGTH_SHORT).show()
                            }
                            .addOnCompleteListener {
                                processandoFavorito = false
                                btnFavorito.isEnabled = true
                            }
                    } else {
                        val dados = hashMapOf(
                            "usuarioId" to uid,
                            "eventoId" to eventoId,
                            "dataFavorito" to FieldValue.serverTimestamp()
                        )

                        referencia.set(dados)
                            .addOnSuccessListener {
                                btnFavorito.text = "★ DESFAVORITAR"
                                Toast.makeText(this, "Evento adicionado aos favoritos!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Erro ao favoritar", Toast.LENGTH_SHORT).show()
                            }
                            .addOnCompleteListener {
                                processandoFavorito = false
                                btnFavorito.isEnabled = true
                            }
                    }
                }
                .addOnFailureListener {
                    processandoFavorito = false
                    btnFavorito.isEnabled = true
                    Toast.makeText(this, "Erro ao verificar favorito", Toast.LENGTH_SHORT).show()
                }
        }

        btnVoltar.setOnClickListener {
            finish()
        }
    }
}