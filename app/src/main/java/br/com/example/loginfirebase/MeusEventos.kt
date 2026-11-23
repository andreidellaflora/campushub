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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentReference

class MeusEventosActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var container: LinearLayout
    private var uidAtual: String = ""
    private val inscricoesEmProcessamento = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meus_eventos)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        container = findViewById(R.id.meusEventosContainer)

        val usuario = auth.currentUser
        if (usuario == null) {
            finish()
            return
        }
        uidAtual = usuario.uid

        btnVoltar.setOnClickListener { finish() }
        carregarEventos()
    }

    override fun onResume() {
        super.onResume()
        if (::container.isInitialized && uidAtual.isNotBlank()) {
            carregarEventos()
        }
    }

    private fun carregarEventos() {
        if (!::container.isInitialized || uidAtual.isBlank()) return

        container.removeAllViews()
        val carregando = TextView(this).apply {
            text = "Carregando suas inscrições..."
            textSize = 15f
            setTextColor(Color.DKGRAY)
        }
        container.addView(carregando)

        db.collection("inscricoes")
            .whereEqualTo("usuarioId", uidAtual)
            .get()
            .addOnSuccessListener { inscricoes ->
                container.removeAllViews()

                if (inscricoes.isEmpty) {
                    mostrarMensagem("Você ainda não está inscrito em nenhum evento.")
                    return@addOnSuccessListener
                }

                for (inscricao in inscricoes) {
                    val eventoId = inscricao.getString("eventoId")
                    if (eventoId.isNullOrBlank()) continue

                    val status = inscricao.getString("status") ?: "CONFIRMADA"
                    carregarCardEvento(inscricao.id, eventoId, status)
                }
            }
            .addOnFailureListener { erro ->
                container.removeAllViews()
                mostrarMensagem("Não foi possível carregar suas inscrições.")
                Toast.makeText(this, "Erro ao carregar eventos: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun carregarCardEvento(inscricaoId: String, eventoId: String, statusInscricao: String) {
        db.collection("eventos")
            .document(eventoId)
            .get()
            .addOnSuccessListener { evento ->
                if (!evento.exists()) return@addOnSuccessListener

                val titulo = evento.getString("titulo") ?: "Evento"
                val data = evento.getString("data") ?: ""
                val horario = evento.getString("horario") ?: ""
                val local = evento.getString("local") ?: ""
                val confirmado = statusInscricao == "CONFIRMADA"

                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 20, 20, 20)
                    setBackgroundColor(Color.WHITE)
                    elevation = 3f
                }
                val parametros = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
                card.layoutParams = parametros

                val txtTitulo = TextView(this).apply {
                    text = titulo
                    textSize = 19f
                    setTextColor(Color.rgb(94, 53, 177))
                }
                val txtData = TextView(this).apply {
                    text = "$data • $horario"
                    textSize = 14f
                    setTextColor(Color.DKGRAY)
                }
                val txtLocal = TextView(this).apply {
                    text = local
                    textSize = 14f
                    setTextColor(Color.GRAY)
                }
                val txtStatus = TextView(this).apply {
                    text = if (confirmado) "INSCRIÇÃO CONFIRMADA" else "AGUARDANDO VAGA — LISTA DE ESPERA"
                    textSize = 14f
                    setTextColor(if (confirmado) Color.rgb(46, 125, 50) else Color.rgb(230, 126, 34))
                    setPadding(0, 10, 0, 6)
                }

                val btnDetalhes = Button(this).apply {
                    text = "VER DETALHES"
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.rgb(106, 75, 188))
                    setOnClickListener {
                        val intent = Intent(this@MeusEventosActivity, DetalhesEventoActivity::class.java)
                        intent.putExtra("eventoId", eventoId)
                        startActivity(intent)
                    }
                }

                val btnCancelar = Button(this).apply {
                    text = if (confirmado) "CANCELAR INSCRIÇÃO" else "SAIR DA LISTA DE ESPERA"
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.rgb(190, 50, 50))
                    setOnClickListener {
                        cancelarInscricao(inscricaoId, eventoId, this)
                    }
                }

                card.addView(txtTitulo)
                card.addView(txtData)
                card.addView(txtLocal)
                card.addView(txtStatus)
                card.addView(btnDetalhes)
                card.addView(btnCancelar)
                container.addView(card)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar os dados de um evento.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cancelarInscricao(inscricaoId: String, eventoId: String, botao: Button) {
        if (!inscricoesEmProcessamento.add(inscricaoId)) return

        botao.isEnabled = false
        val inscricaoRef = db.collection("inscricoes").document(inscricaoId)
        val eventoRef = db.collection("eventos").document(eventoId)

        // Busca o primeiro da fila fora da transação. A transação confirma novamente
        // se esse documento ainda existe e continua aguardando vaga antes de promovê-lo.
        db.collection("inscricoes")
            .whereEqualTo("eventoId", eventoId)
            .whereEqualTo("status", "ESPERA")
            .orderBy("ordemEspera", Query.Direction.ASCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { fila ->
                val proximoRef: DocumentReference? = fila.documents.firstOrNull()?.reference

                db.runTransaction { transacao ->
                    val evento = transacao.get(eventoRef)
                    val inscricao = transacao.get(inscricaoRef)
                    val proximo = proximoRef?.let { transacao.get(it) }

                    if (!evento.exists()) throw Exception("Evento não encontrado.")
                    if (!inscricao.exists()) return@runTransaction "JA_CANCELADA"

                    val status = inscricao.getString("status") ?: "CONFIRMADA"
                    val ocupadas = evento.getLong("vagasOcupadas") ?: 0L

                    if (status == "CONFIRMADA") {
                        transacao.delete(inscricaoRef)

                        if (proximoRef != null && proximo != null && proximo.exists() &&
                            proximo.getString("status") == "ESPERA") {
                            transacao.update(
                                proximoRef,
                                mapOf(
                                    "status" to "CONFIRMADA",
                                    "dataConfirmacao" to FieldValue.serverTimestamp()
                                )
                            )
                            "CANCELADA_PROMOVIDO"
                        } else {
                            if (ocupadas > 0L) {
                                transacao.update(eventoRef, "vagasOcupadas", ocupadas - 1L)
                            }
                            "CANCELADA"
                        }
                    } else {
                        transacao.delete(inscricaoRef)
                        "SAIU_ESPERA"
                    }
                }.addOnSuccessListener { resultado ->
                    val mensagem = when (resultado) {
                        "CANCELADA_PROMOVIDO" -> "Inscrição cancelada! O próximo aluno da lista foi confirmado."
                        "CANCELADA" -> "Inscrição cancelada e vaga liberada."
                        "SAIU_ESPERA" -> "Você saiu da lista de espera."
                        else -> "Essa inscrição já havia sido cancelada."
                    }
                    Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
                    carregarEventos()
                }.addOnFailureListener { erro ->
                    botao.isEnabled = true
                    Toast.makeText(this, "Erro ao cancelar: ${erro.message}", Toast.LENGTH_LONG).show()
                }.addOnCompleteListener {
                    inscricoesEmProcessamento.remove(inscricaoId)
                }
            }
            .addOnFailureListener { erro ->
                inscricoesEmProcessamento.remove(inscricaoId)
                botao.isEnabled = true
                Toast.makeText(this, "Erro ao consultar a lista de espera: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarMensagem(mensagem: String) {
        val texto = TextView(this).apply {
            text = mensagem
            textSize = 16f
            setTextColor(Color.DKGRAY)
        }
        container.addView(texto)
    }
}
