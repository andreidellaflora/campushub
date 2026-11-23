package br.com.example.loginfirebase

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class GerenciarEventosActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var eventosContainer: LinearLayout

    private var permissaoVerificada = false
    private var eventoSelecionadoQr = ""
    private var tituloEventoSelecionadoQr = ""

    // API atual do JourneyApps: substitui IntentIntegrator/onActivityResult.
    private lateinit var qrScannerLauncher: ActivityResultLauncher<ScanOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gerenciar_eventos)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        eventosContainer = findViewById(R.id.eventosContainer)

        qrScannerLauncher = registerForActivityResult(ScanContract()) { resultado ->
            val conteudo = resultado.contents
            if (conteudo.isNullOrBlank()) {
                Toast.makeText(this, "Leitura cancelada.", Toast.LENGTH_SHORT).show()
            } else {
                validarQrERegistrarPresenca(conteudo)
            }
        }

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnNovoEvento).setOnClickListener {
            startActivity(Intent(this, FormEventoActivity::class.java))
        }

        verificarOrganizador()
    }

    private fun verificarOrganizador() {
        val usuario = auth.currentUser
        if (usuario == null) {
            finish()
            return
        }

        db.collection("usuarios").document(usuario.uid).get()
            .addOnSuccessListener { documento ->
                if (documento.getString("tipoUsuario") != "ORGANIZADOR") {
                    Toast.makeText(
                        this,
                        "Acesso permitido somente para organizadores.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    permissaoVerificada = true
                    carregarEventos()
                }
            }
            .addOnFailureListener { erro ->
                Toast.makeText(
                    this,
                    "Erro ao verificar permissões: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
    }

    private fun carregarEventos() {
        val usuario = auth.currentUser ?: return
        eventosContainer.removeAllViews()

        db.collection("eventos")
            .whereEqualTo("criadorId", usuario.uid)
            .get()
            .addOnSuccessListener { documentos ->
                eventosContainer.removeAllViews()
                if (documentos.isEmpty) {
                    mostrarMensagem("Você ainda não cadastrou eventos.")
                    return@addOnSuccessListener
                }

                for (documento in documentos) {
                    criarCardEvento(
                        eventoId = documento.id,
                        titulo = documento.getString("titulo") ?: "Evento sem título",
                        data = documento.getString("data") ?: "",
                        horario = documento.getString("horario") ?: "",
                        local = documento.getString("local") ?: "",
                        capacidade = documento.getLong("capacidade") ?: 0L,
                        publicado = documento.getBoolean("publicado") ?: false,
                        encerrado = documento.getBoolean("encerrado") ?: false
                    )
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

    private fun criarCardEvento(
        eventoId: String,
        titulo: String,
        data: String,
        horario: String,
        local: String,
        capacidade: Long,
        publicado: Boolean,
        encerrado: Boolean
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(Color.WHITE)
        }

        val parametros = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 20) }
        card.layoutParams = parametros

        val txtTitulo = TextView(this).apply {
            text = titulo
            textSize = 19f
            setTextColor(Color.rgb(94, 53, 177))
            setTypeface(null, Typeface.BOLD)
        }

        val txtData = TextView(this).apply {
            text = "$data • $horario"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 6, 0, 0)
        }

        val txtLocal = TextView(this).apply {
            text = local
            textSize = 14f
            setTextColor(Color.GRAY)
            setPadding(0, 4, 0, 0)
        }

        val txtStatus = TextView(this).apply {
            text = when {
                encerrado -> "Status: Encerrado"
                publicado -> "Status: Publicado"
                else -> "Status: Não publicado"
            }
            textSize = 14f
            setTextColor(Color.rgb(94, 53, 177))
            setPadding(0, 8, 0, 8)
        }

        val txtInscricoes = TextView(this).apply {
            text = "Carregando inscrições..."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 8, 0, 12)
        }

        val btnParticipantes = Button(this).apply {
            text = "VER PARTICIPANTES"
            isEnabled = false
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.rgb(106, 75, 188)
            )
            setTextColor(Color.WHITE)
        }

        val btnLerQrCode = Button(this).apply {
            text = "LER QR CODE / REGISTRAR PRESENÇA"
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.rgb(76, 175, 80)
            )
            setTextColor(Color.WHITE)
            isEnabled = !encerrado
        }

        card.addView(txtTitulo)
        card.addView(txtData)
        card.addView(txtLocal)
        card.addView(txtStatus)
        card.addView(txtInscricoes)
        card.addView(btnParticipantes)
        card.addView(btnLerQrCode)
        eventosContainer.addView(card)

        db.collection("inscricoes")
            .whereEqualTo("eventoId", eventoId)
            .get()
            .addOnSuccessListener { inscricoes ->
                val confirmadas = inscricoes.documents.count {
                    (it.getString("status") ?: "CONFIRMADA") == "CONFIRMADA"
                }
                val espera = inscricoes.documents.count {
                    it.getString("status") == "ESPERA"
                }
                val disponiveis = if (capacidade > 0L) {
                    (capacidade - confirmadas).coerceAtLeast(0L)
                } else {
                    -1L
                }
                val textoVagas = if (capacidade > 0L) {
                    "Vagas: $confirmadas/$capacidade ocupadas\nDisponíveis: $disponiveis"
                } else {
                    "Vagas ocupadas: $confirmadas\nCapacidade ilimitada"
                }
                txtInscricoes.text = "$textoVagas\nLista de espera: $espera aluno(s)"
                btnParticipantes.isEnabled = true
            }
            .addOnFailureListener { erro ->
                txtInscricoes.text = "Não foi possível carregar as inscrições."
                Toast.makeText(
                    this,
                    "Erro ao consultar inscrições: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        btnParticipantes.setOnClickListener { mostrarParticipantes(eventoId, titulo) }
        btnLerQrCode.setOnClickListener { iniciarLeituraQr(eventoId, titulo) }
    }

    private fun iniciarLeituraQr(eventoId: String, titulo: String) {
        eventoSelecionadoQr = eventoId
        tituloEventoSelecionadoQr = titulo

        val opcoes = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Leia o QR Code do participante de: $titulo")
            setBeepEnabled(false)
            setOrientationLocked(false)
            setCameraId(0)
        }
        qrScannerLauncher.launch(opcoes)
    }

    private fun validarQrERegistrarPresenca(conteudo: String) {
        val partes = conteudo.split("|")
        if (partes.size != 3 || partes[0] != "CAMPUSHUB") {
            mostrarResultadoCheckIn(false, "QR Code inválido do CampusHub.")
            return
        }

        val eventoDoQr = partes[1]
        val token = partes[2]
        val usuarioAtual = auth.currentUser ?: run {
            finish()
            return
        }

        if (eventoDoQr != eventoSelecionadoQr || token.isBlank()) {
            mostrarResultadoCheckIn(false, "Este QR Code não pertence ao evento selecionado.")
            return
        }

        Toast.makeText(this, "Validando inscrição...", Toast.LENGTH_SHORT).show()
        val eventoRef = db.collection("eventos").document(eventoDoQr)

        db.collection("inscricoes")
            .whereEqualTo("eventoId", eventoDoQr)
            .whereEqualTo("qrToken", token)
            .limit(1)
            .get()
            .addOnSuccessListener { resultado ->
                val inscricaoDoc = resultado.documents.firstOrNull()
                if (inscricaoDoc == null) {
                    mostrarResultadoCheckIn(
                        false,
                        "QR Code não encontrado. Confira se a inscrição é válida."
                    )
                    return@addOnSuccessListener
                }

                val inscricaoRef = inscricaoDoc.reference
                db.runTransaction { transacao ->
                    val evento = transacao.get(eventoRef)
                    val inscricao = transacao.get(inscricaoRef)

                    if (!evento.exists()) throw Exception("Evento não encontrado.")
                    if (evento.getString("criadorId") != usuarioAtual.uid) {
                        throw Exception("Você não é o organizador deste evento.")
                    }
                    if (!inscricao.exists()) throw Exception("Inscrição não encontrada.")
                    if (inscricao.getString("eventoId") != eventoDoQr ||
                        inscricao.getString("qrToken") != token
                    ) {
                        throw Exception("QR Code inválido.")
                    }

                    val status = inscricao.getString("status") ?: "CONFIRMADA"
                    if (status != "CONFIRMADA") {
                        throw Exception("Participante não possui inscrição confirmada.")
                    }
                    if (inscricao.getBoolean("checkInRealizado") == true) {
                        throw Exception("Este participante já teve a presença registrada.")
                    }

                    transacao.update(
                        inscricaoRef,
                        mapOf(
                            "checkInRealizado" to true,
                            "dataCheckIn" to FieldValue.serverTimestamp(),
                            "checkInPor" to usuarioAtual.uid
                        )
                    )
                    "OK"
                }.addOnSuccessListener {
                    val uidAluno = inscricaoDoc.getString("usuarioId").orEmpty()
                    if (uidAluno.isBlank()) {
                        mostrarResultadoCheckIn(
                            true,
                            "Presença confirmada!\nEvento: $tituloEventoSelecionadoQr"
                        )
                    } else {
                        db.collection("usuarios").document(uidAluno).get()
                            .addOnSuccessListener { aluno ->
                                val nome = aluno.getString("nome") ?: "Participante"
                                mostrarResultadoCheckIn(
                                    true,
                                    "Presença confirmada!\n$nome\nEvento: $tituloEventoSelecionadoQr"
                                )
                            }
                            .addOnFailureListener {
                                mostrarResultadoCheckIn(
                                    true,
                                    "Presença confirmada!\nEvento: $tituloEventoSelecionadoQr"
                                )
                            }
                    }
                }.addOnFailureListener { erro ->
                    mostrarResultadoCheckIn(
                        false,
                        erro.message ?: "Não foi possível validar o QR Code."
                    )
                }
            }
            .addOnFailureListener { erro ->
                mostrarResultadoCheckIn(
                    false,
                    "Erro ao consultar QR Code: ${erro.message}"
                )
            }
    }

    private fun mostrarResultadoCheckIn(sucesso: Boolean, mensagem: String) {
        AlertDialog.Builder(this)
            .setTitle(if (sucesso) "PRESENÇA REGISTRADA" else "NÃO FOI POSSÍVEL REGISTRAR")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun mostrarParticipantes(eventoId: String, tituloEvento: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 12, 24, 12)
        }
        val scroll = ScrollView(this)
        scroll.addView(layout)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Participantes: $tituloEvento")
            .setView(scroll)
            .setPositiveButton("FECHAR", null)
            .create()

        layout.addView(TextView(this).apply {
            text = "Carregando participantes..."
            textSize = 15f
            setTextColor(Color.DKGRAY)
        })
        dialog.show()

        db.collection("inscricoes")
            .whereEqualTo("eventoId", eventoId)
            .get()
            .addOnSuccessListener { resultado ->
                layout.removeAllViews()

                val confirmadas = resultado.documents.filter {
                    (it.getString("status") ?: "CONFIRMADA") == "CONFIRMADA"
                }
                val espera = resultado.documents.filter {
                    it.getString("status") == "ESPERA"
                }.sortedBy { it.getLong("ordemEspera") ?: Long.MAX_VALUE }

                adicionarTituloLista(layout, "INSCRIÇÕES CONFIRMADAS (${confirmadas.size})")
                if (confirmadas.isEmpty()) {
                    adicionarTextoLista(layout, "Nenhum aluno confirmado.")
                } else {
                    confirmadas.forEachIndexed { index, inscricao ->
                        carregarDadosAluno(
                            inscricao.getString("usuarioId").orEmpty(),
                            index + 1,
                            "Confirmado",
                            layout
                        )
                    }
                }

                adicionarTituloLista(layout, "LISTA DE ESPERA (${espera.size})")
                if (espera.isEmpty()) {
                    adicionarTextoLista(layout, "Ninguém na lista de espera.")
                } else {
                    espera.forEachIndexed { index, inscricao ->
                        carregarDadosAluno(
                            inscricao.getString("usuarioId").orEmpty(),
                            index + 1,
                            "Aguardando vaga",
                            layout
                        )
                    }
                }
            }
            .addOnFailureListener { erro ->
                layout.removeAllViews()
                adicionarTextoLista(layout, "Erro ao carregar participantes: ${erro.message}")
            }
    }

    private fun carregarDadosAluno(
        uid: String,
        numero: Int,
        situacao: String,
        layout: LinearLayout
    ) {
        if (uid.isBlank()) {
            adicionarTextoLista(layout, "$numero. Aluno não identificado — $situacao")
            return
        }

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { usuario ->
                val nome = usuario.getString("nome")
                    ?.takeIf { it.isNotBlank() } ?: "Aluno"
                val email = usuario.getString("email")
                    ?.takeIf { it.isNotBlank() } ?: "E-mail não cadastrado"
                adicionarTextoLista(layout, "$numero. $nome\n$email\nSituação: $situacao")
            }
            .addOnFailureListener {
                adicionarTextoLista(
                    layout,
                    "$numero. Não foi possível carregar o aluno\nSituação: $situacao"
                )
            }
    }

    private fun adicionarTituloLista(layout: LinearLayout, titulo: String) {
        layout.addView(TextView(this).apply {
            text = titulo
            textSize = 16f
            setTextColor(Color.rgb(94, 53, 177))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 18, 0, 10)
        })
    }

    private fun adicionarTextoLista(layout: LinearLayout, texto: String) {
        layout.addView(TextView(this).apply {
            text = texto
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(8, 10, 8, 10)
        })
    }

    private fun mostrarMensagem(mensagem: String) {
        eventosContainer.addView(TextView(this).apply {
            text = mensagem
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(8, 20, 8, 20)
        })
    }

    override fun onResume() {
        super.onResume()
        if (::db.isInitialized && permissaoVerificada && auth.currentUser != null) {
            carregarEventos()
        }
    }
}
