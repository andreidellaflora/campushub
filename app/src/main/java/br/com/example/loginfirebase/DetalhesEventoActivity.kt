package br.com.example.loginfirebase

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.provider.MediaStore
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.VideoView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class DetalhesEventoActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var containerComentarios: LinearLayout
    private lateinit var edtComentario: EditText
    private lateinit var btnPublicarComentario: Button
    private lateinit var ratingBar: RatingBar
    private lateinit var btnSalvarAvaliacao: Button
    private lateinit var txtMediaAvaliacoes: TextView
    private lateinit var txtSituacaoAvaliacao: TextView
    private lateinit var fotosContainer: LinearLayout
    private lateinit var audiosContainer: LinearLayout
    private lateinit var videosContainer: LinearLayout
    private var uidAtual = ""
    private var mediaPlayer: MediaPlayer? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) salvarBitmapFoto(bitmap) else toast("Foto não capturada")
    }
    private val galeriaFotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) salvarUriMidia(uri, "foto")
    }
    private val selecionarAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) salvarUriMidia(uri, "audio")
    }
    private val gravarAudioLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK && uri != null) salvarUriMidia(uri, "audio")
        else toast("Gravação de áudio cancelada ou não suportada")
    }
    private val selecionarVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) salvarUriMidia(uri, "video")
    }
    private val gravarVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK && uri != null) salvarUriMidia(uri, "video")
        else toast("Gravação de vídeo cancelada")
    }

    private var eventoId = ""
    private var nomeAutor = "Usuário"
    private var processandoFavorito = false
    private var processandoInscricao = false
    private var publicandoComentario = false
    private var salvandoAvaliacao = false
    private var eventoEncerrado = false
    private var usuarioInscrito = false
    private lateinit var btnInscrever: Button
    private lateinit var btnMeuQrCode: Button

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

        btnInscrever = findViewById(R.id.btnInscrever)
        btnMeuQrCode = findViewById(R.id.btnMeuQrCode)
        btnMeuQrCode.visibility = android.view.View.GONE
        val btnFavorito = findViewById<Button>(R.id.btnFavorito)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        val btnVerMapa = findViewById<Button>(R.id.btnVerMapa)

        edtComentario = findViewById(R.id.edtComentario)
        btnPublicarComentario = findViewById(R.id.btnPublicarComentario)
        containerComentarios = findViewById(R.id.comentariosContainer)

        ratingBar = findViewById(R.id.ratingBarAvaliacao)
        btnSalvarAvaliacao = findViewById(R.id.btnSalvarAvaliacao)
        txtMediaAvaliacoes = findViewById(R.id.txtMediaAvaliacoes)
        txtSituacaoAvaliacao = findViewById(R.id.txtSituacaoAvaliacao)
        fotosContainer = findViewById(R.id.fotosContainer)
        audiosContainer = findViewById(R.id.audiosContainer)
        videosContainer = findViewById(R.id.videosContainer)

        val usuario = auth.currentUser

        if (usuario == null) {
            finish()
            return
        }

        eventoId = intent.getStringExtra("eventoId").orEmpty()

        if (eventoId.isBlank()) {
            Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uid = usuario.uid
        uidAtual = uid
        val inscricaoId = "${uid}_${eventoId}"
        val favoritoId = "${uid}_${eventoId}"
        val avaliacaoId = "${uid}_${eventoId}"

        nomeAutor = usuario.displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: usuario.email?.substringBefore("@")
                    ?: "Usuário"

        db.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->
                val nome = documento.getString("nome")?.trim()
                if (!nome.isNullOrBlank()) {
                    nomeAutor = nome
                }
            }

        carregarEvento(
            txtTitulo,
            txtData,
            txtHorario,
            txtLocal,
            txtDescricao
        )

        btnInscrever.isEnabled = false
        btnFavorito.isEnabled = false
        btnSalvarAvaliacao.isEnabled = false

        db.collection("inscricoes")
            .document(inscricaoId)
            .get()
            .addOnSuccessListener { documento ->
                val status = documento.getString("status")
                    ?: if (documento.exists()) "CONFIRMADA" else ""

                usuarioInscrito = status == "CONFIRMADA"
                btnMeuQrCode.visibility = if (usuarioInscrito) android.view.View.VISIBLE else android.view.View.GONE
                if (usuarioInscrito) garantirTokenQr(inscricaoId)

                btnInscrever.text = when (status) {
                    "CONFIRMADA" -> "CANCELAR INSCRIÇÃO"
                    "ESPERA" -> "CANCELAR LISTA DE ESPERA"
                    else -> "INSCREVER-SE"
                }

                btnInscrever.isEnabled = true
                atualizarPermissaoAvaliacao(uid, avaliacaoId)
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

        carregarMediaAvaliacoes()

        btnInscrever.setOnClickListener {
            processarInscricao(uid, inscricaoId, avaliacaoId)
        }

        btnMeuQrCode.setOnClickListener {
            if (usuarioInscrito) mostrarMeuQrCode(inscricaoId)
            else toast("Seu QR Code fica disponível após a confirmação da inscrição.")
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
                                Toast.makeText(
                                    this,
                                    "Evento removido dos favoritos!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Erro ao desfavoritar",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                                Toast.makeText(
                                    this,
                                    "Evento adicionado aos favoritos!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Erro ao favoritar",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                    Toast.makeText(
                        this,
                        "Erro ao verificar favorito",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        btnVerMapa.setOnClickListener {
            val local = txtLocal.text.toString().trim()

            if (local.isBlank() || local == "Sem local") {
                Toast.makeText(
                    this,
                    "Localização não cadastrada",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                abrirMapa(local)
            }
        }

        btnPublicarComentario.setOnClickListener {
            publicarComentario(uid)
        }

        btnSalvarAvaliacao.setOnClickListener {
            salvarAvaliacao(uid, avaliacaoId)
        }

        btnVoltar.setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnTirarFoto).setOnClickListener { cameraLauncher.launch(null) }
        findViewById<Button>(R.id.btnEscolherFoto).setOnClickListener { galeriaFotoLauncher.launch("image/*") }
        findViewById<Button>(R.id.btnGravarAudio).setOnClickListener {
            try { gravarAudioLauncher.launch(Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)) }
            catch (_: Exception) { toast("Não há gravador de áudio disponível neste aparelho") }
        }
        findViewById<Button>(R.id.btnEscolherAudio).setOnClickListener { selecionarAudioLauncher.launch("audio/*") }
        findViewById<Button>(R.id.btnGravarVideo).setOnClickListener {
            val intentVideo = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply { putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30) }
            try { gravarVideoLauncher.launch(intentVideo) }
            catch (_: Exception) { toast("Não foi possível abrir a câmera de vídeo") }
        }
        findViewById<Button>(R.id.btnEscolherVideo).setOnClickListener { selecionarVideoLauncher.launch("video/*") }
        carregarMidiasLocais()
        carregarComentarios(uid)
    }

    private fun processarInscricao(
        uid: String,
        inscricaoId: String,
        avaliacaoId: String
    ) {
        if (processandoInscricao) return

        processandoInscricao = true
        btnInscrever.isEnabled = false

        val eventoRef = db.collection("eventos").document(eventoId)
        val inscricaoRef = db.collection("inscricoes").document(inscricaoId)
        val novoTokenQr = UUID.randomUUID().toString()
        val tokenPromovido = UUID.randomUUID().toString()

        // Primeiro verifica se o aluno já está inscrito.
        inscricaoRef.get()
            .addOnSuccessListener { inscricaoAtual ->
                val statusAtual = inscricaoAtual.getString("status")
                    ?: if (inscricaoAtual.exists()) "CONFIRMADA" else ""

                // Só consulta a lista de espera ao cancelar uma inscrição confirmada.
                fun executarTransacao(proximoRef: com.google.firebase.firestore.DocumentReference?) {
                    db.runTransaction { transacao ->
                        val evento = transacao.get(eventoRef)
                        val inscricao = transacao.get(inscricaoRef)
                        val proximo = proximoRef?.let { transacao.get(it) }

                        if (!evento.exists()) {
                            throw Exception("Evento não encontrado.")
                        }

                        if (!inscricao.exists()) {
                            val capacidade = evento.getLong("capacidade") ?: 0L
                            val ocupadas = evento.getLong("vagasOcupadas") ?: 0L
                            val temVaga = capacidade <= 0L || ocupadas < capacidade

                            if (temVaga) {
                                transacao.set(
                                    inscricaoRef,
                                    hashMapOf(
                                        "usuarioId" to uid,
                                        "eventoId" to eventoId,
                                        "status" to "CONFIRMADA",
                                        "qrToken" to novoTokenQr,
                                        "dataInscricao" to FieldValue.serverTimestamp()
                                    )
                                )

                                if (capacidade > 0L) {
                                    transacao.update(eventoRef, "vagasOcupadas", ocupadas + 1L)
                                }
                                "CONFIRMADA"
                            } else {
                                val ordem = (evento.getLong("proximaOrdemEspera") ?: 0L) + 1L
                                transacao.set(
                                    inscricaoRef,
                                    hashMapOf(
                                        "usuarioId" to uid,
                                        "eventoId" to eventoId,
                                        "status" to "ESPERA",
                                        "ordemEspera" to ordem,
                                        "dataInscricao" to FieldValue.serverTimestamp()
                                    )
                                )
                                transacao.update(eventoRef, "proximaOrdemEspera", ordem)
                                "ESPERA"
                            }
                        } else {
                            val status = inscricao.getString("status") ?: "CONFIRMADA"
                            val ocupadas = evento.getLong("vagasOcupadas") ?: 0L

                            if (status == "CONFIRMADA") {
                                transacao.delete(inscricaoRef)

                                if (proximo != null && proximo.exists() &&
                                    proximo.getString("status") == "ESPERA") {
                                    transacao.update(
                                        proximoRef!!,
                                        mapOf(
                                            "status" to "CONFIRMADA",
                                            "qrToken" to tokenPromovido,
                                            "dataConfirmacao" to FieldValue.serverTimestamp()
                                        )
                                    )
                                } else if (ocupadas > 0L) {
                                    transacao.update(eventoRef, "vagasOcupadas", ocupadas - 1L)
                                }
                                "CANCELADA"
                            } else {
                                transacao.delete(inscricaoRef)
                                "SAIU_ESPERA"
                            }
                        }
                    }.addOnSuccessListener { resultado ->
                        usuarioInscrito = resultado == "CONFIRMADA"
                        btnMeuQrCode.visibility = if (usuarioInscrito) android.view.View.VISIBLE else android.view.View.GONE
                        if (usuarioInscrito) garantirTokenQr(inscricaoId)
                        btnInscrever.text = when (resultado) {
                            "CONFIRMADA" -> "CANCELAR INSCRIÇÃO"
                            "ESPERA" -> "CANCELAR LISTA DE ESPERA"
                            else -> "INSCREVER-SE"
                        }

                        val mensagem = when (resultado) {
                            "CONFIRMADA" -> "Inscrição confirmada!"
                            "ESPERA" -> "Evento lotado! Você entrou na lista de espera."
                            "CANCELADA" -> "Inscrição cancelada. Lista de espera atualizada."
                            "SAIU_ESPERA" -> "Você saiu da lista de espera."
                            else -> "Operação concluída."
                        }
                        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
                        atualizarPermissaoAvaliacao(uid, avaliacaoId)
                    }.addOnFailureListener { erro ->
                        Toast.makeText(
                            this,
                            "Erro na inscrição: ${erro.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }.addOnCompleteListener {
                        processandoInscricao = false
                        btnInscrever.isEnabled = true
                    }
                }

                if (inscricaoAtual.exists() && statusAtual == "CONFIRMADA") {
                    db.collection("inscricoes")
                        .whereEqualTo("eventoId", eventoId)
                        .whereEqualTo("status", "ESPERA")
                        .orderBy("ordemEspera")
                        .limit(1)
                        .get()
                        .addOnSuccessListener { esperaSnapshot ->
                            val proximoRef = esperaSnapshot.documents.firstOrNull()?.reference
                            executarTransacao(proximoRef)
                        }
                        .addOnFailureListener { erro ->
                            processandoInscricao = false
                            btnInscrever.isEnabled = true
                            Toast.makeText(
                                this,
                                "Erro ao consultar lista de espera: ${erro.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    executarTransacao(null)
                }
            }
            .addOnFailureListener { erro ->
                processandoInscricao = false
                btnInscrever.isEnabled = true
                Toast.makeText(
                    this,
                    "Erro ao verificar inscrição: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun garantirTokenQr(inscricaoId: String) {
        val referencia = db.collection("inscricoes").document(inscricaoId)
        referencia.get().addOnSuccessListener { documento ->
            if (!documento.exists() || documento.getString("status").let { it != null && it != "CONFIRMADA" }) {
                btnMeuQrCode.visibility = android.view.View.GONE
                return@addOnSuccessListener
            }
            if (documento.getString("qrToken").isNullOrBlank()) {
                referencia.update("qrToken", UUID.randomUUID().toString())
                    .addOnFailureListener { toast("Não foi possível preparar seu QR Code.") }
            }
        }
    }

    private fun mostrarMeuQrCode(inscricaoId: String) {
        if (!usuarioInscrito) {
            toast("Somente inscrições confirmadas possuem QR Code.")
            return
        }
        btnMeuQrCode.isEnabled = false
        db.collection("inscricoes").document(inscricaoId).get()
            .addOnSuccessListener { documento ->
                val status = documento.getString("status") ?: if (documento.exists()) "CONFIRMADA" else ""
                if (!documento.exists() || status != "CONFIRMADA") {
                    usuarioInscrito = false
                    btnMeuQrCode.visibility = android.view.View.GONE
                    toast("Sua inscrição não está confirmada.")
                    return@addOnSuccessListener
                }
                var token = documento.getString("qrToken")
                if (token.isNullOrBlank()) {
                    token = UUID.randomUUID().toString()
                    val tokenNovo = token
                    documento.reference.update("qrToken", tokenNovo)
                        .addOnSuccessListener { exibirImagemQr(tokenNovo) }
                        .addOnFailureListener { toast("Erro ao gerar QR Code. Tente novamente.") }
                } else {
                    exibirImagemQr(token)
                }
            }
            .addOnFailureListener { toast("Erro ao carregar seu QR Code.") }
            .addOnCompleteListener { btnMeuQrCode.isEnabled = true }
    }

    private fun exibirImagemQr(token: String) {
        try {
            // O conteúdo contém somente um token aleatório; não expõe e-mail ou UID.
            val conteudo = "CAMPUSHUB|$eventoId|$token"
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2
            )
            val matriz = MultiFormatWriter().encode(conteudo, BarcodeFormat.QR_CODE, 800, 800, hints)
            val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
            for (x in 0 until 800) {
                for (y in 0 until 800) {
                    bitmap.setPixel(x, y, if (matriz.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            val imagem = ImageView(this).apply {
                setImageBitmap(bitmap)
                adjustViewBounds = true
                setPadding(24, 24, 24, 24)
            }
            AlertDialog.Builder(this)
                .setTitle("Meu QR Code - CampusHub")
                .setMessage("Apresente este código ao organizador para confirmar sua presença.")
                .setView(imagem)
                .setPositiveButton("FECHAR", null)
                .show()
        } catch (erro: Exception) {
            toast("Não foi possível gerar o QR Code: ${erro.message}")
        }
    }

    // Mídias armazenadas localmente na pasta privada do aplicativo.
    private fun toast(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }

    private fun pastaMidia(tipo: String): File {
        val seguro = eventoId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(filesDir, "campushub/$seguro/$tipo").apply { mkdirs() }
    }

    private fun preferenciasMidia() = getSharedPreferences("campushub_midias", MODE_PRIVATE)
    private fun chaveMidia(tipo: String) = "${tipo}_$eventoId"

    private fun listaMidia(tipo: String): JSONArray = try {
        JSONArray(preferenciasMidia().getString(chaveMidia(tipo), "[]"))
    } catch (_: Exception) { JSONArray() }

    private fun gravarListaMidia(tipo: String, lista: JSONArray) {
        preferenciasMidia().edit().putString(chaveMidia(tipo), lista.toString()).apply()
    }

    private fun salvarBitmapFoto(bitmap: Bitmap) {
        try {
            val arquivo = File(pastaMidia("foto"), "${UUID.randomUUID()}.jpg")
            FileOutputStream(arquivo).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            registrarMidia("foto", arquivo)
        } catch (e: Exception) { toast("Erro ao salvar foto: ${e.message}") }
    }

    private fun salvarUriMidia(uri: Uri, tipo: String) {
        try {
            val extensao = when (tipo) { "foto" -> ".jpg"; "audio" -> ".audio"; else -> ".video" }
            val arquivo = File(pastaMidia(tipo), "${UUID.randomUUID()}$extensao")
            val entrada = contentResolver.openInputStream(uri) ?: throw Exception("Não foi possível ler o arquivo")
            entrada.use { input -> FileOutputStream(arquivo).use { output -> input.copyTo(output) } }
            registrarMidia(tipo, arquivo)
        } catch (e: Exception) { toast("Erro ao salvar arquivo: ${e.message}") }
    }

    private fun registrarMidia(tipo: String, arquivo: File) {
        val lista = listaMidia(tipo)
        lista.put(JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("caminho", arquivo.absolutePath)
            put("uid", uidAtual)
            put("autor", nomeAutor)
            put("data", System.currentTimeMillis())
        })
        gravarListaMidia(tipo, lista)
        carregarMidiasLocais()
        toast("Publicação salva no aparelho")
    }

    private fun carregarMidiasLocais() {
        if (!::fotosContainer.isInitialized) return
        exibirListaMidia("foto", fotosContainer)
        exibirListaMidia("audio", audiosContainer)
        exibirListaMidia("video", videosContainer)
    }

    private fun exibirListaMidia(tipo: String, container: LinearLayout) {
        container.removeAllViews()
        val lista = listaMidia(tipo)
        val itens = (0 until lista.length()).mapNotNull { lista.optJSONObject(it) }
            .sortedByDescending { it.optLong("data") }
        if (itens.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhuma publicação ainda."
                textSize = 14f
                setTextColor(Color.GRAY)
                setPadding(0, 8, 0, 12)
            })
            return
        }
        itens.forEach { adicionarCardMidia(tipo, it, container) }
    }

    private fun adicionarCardMidia(tipo: String, item: JSONObject, container: LinearLayout) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 14, 14, 14)
            setBackgroundColor(Color.WHITE)
        }
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, 14)
        card.layoutParams = params
        card.addView(TextView(this).apply {
            text = item.optString("autor", "Usuário")
            textSize = 15f
            setTextColor(Color.rgb(94, 53, 177))
            setTypeface(null, Typeface.BOLD)
        })
        card.addView(TextView(this).apply {
            text = formatarData(Date(item.optLong("data")))
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 2, 0, 10)
        })
        val arquivo = File(item.optString("caminho"))
        if (arquivo.exists()) {
            when (tipo) {
                "foto" -> card.addView(ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 210)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageBitmap(BitmapFactory.decodeFile(arquivo.absolutePath))
                })
                "audio" -> card.addView(Button(this).apply {
                    text = "▶ REPRODUZIR ÁUDIO"
                    setOnClickListener { reproduzirAudio(arquivo, this) }
                })
                "video" -> {
                    val video = VideoView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 220)
                        setVideoPath(arquivo.absolutePath)
                    }
                    card.addView(video)
                    card.addView(Button(this).apply {
                        text = "▶ ASSISTIR VÍDEO"
                        setOnClickListener { video.start() }
                    })
                }
            }
        } else card.addView(TextView(this).apply {
            text = "Arquivo não encontrado neste aparelho."
            setTextColor(Color.RED)
        })
        if (item.optString("uid") == uidAtual) {
            card.addView(Button(this).apply {
                text = "EXCLUIR MINHA PUBLICAÇÃO"
                setOnClickListener {
                    AlertDialog.Builder(this@DetalhesEventoActivity)
                        .setTitle("Excluir publicação")
                        .setMessage("Deseja excluir este arquivo?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Excluir") { _, _ -> excluirMidia(tipo, item.optString("id")) }
                        .show()
                }
            })
        }
        container.addView(card)
    }

    private fun reproduzirAudio(arquivo: File, botao: Button) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(arquivo.absolutePath)
                setOnCompletionListener { player ->
                    player.release()
                    if (mediaPlayer === player) mediaPlayer = null
                    botao.text = "▶ REPRODUZIR ÁUDIO"
                }
                prepare()
                start()
            }
            botao.text = "▶ ÁUDIO EM REPRODUÇÃO"
        } catch (_: Exception) { toast("Não foi possível reproduzir o áudio") }
    }

    private fun excluirMidia(tipo: String, id: String) {
        val lista = listaMidia(tipo)
        val nova = JSONArray()
        var removido = false
        for (i in 0 until lista.length()) {
            val item = lista.optJSONObject(i) ?: continue
            if (item.optString("id") == id && item.optString("uid") == uidAtual) {
                File(item.optString("caminho")).delete()
                removido = true
            } else nova.put(item)
        }
        gravarListaMidia(tipo, nova)
        carregarMidiasLocais()
        toast(if (removido) "Publicação excluída" else "Você só pode excluir suas próprias publicações")
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    private fun abrirMapa(local: String) {
        val endereco = Uri.encode(local)
        val uri = Uri.parse("geo:0,0?q=$endereco")
        val intentMapa = Intent(Intent.ACTION_VIEW, uri)

        try {
            startActivity(intentMapa)
        } catch (e: Exception) {
            val url = "https://www.google.com/maps/search/?api=1&query=$endereco"
            val intentNavegador = Intent(Intent.ACTION_VIEW, Uri.parse(url))

            try {
                startActivity(intentNavegador)
            } catch (erro: Exception) {
                Toast.makeText(
                    this,
                    "Não foi possível abrir o mapa",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun carregarEvento(
        txtTitulo: TextView,
        txtData: TextView,
        txtHorario: TextView,
        txtLocal: TextView,
        txtDescricao: TextView
    ) {
        db.collection("eventos")
            .document(eventoId)
            .get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    txtTitulo.text = documento.getString("titulo") ?: "Sem título"

                    val data = documento.getString("data") ?: ""
                    val horario = documento.getString("horario") ?: ""

                    txtData.text = data.ifBlank { "Sem data" }
                    txtHorario.text = horario.ifBlank { "Sem horário" }
                    txtLocal.text = documento.getString("local") ?: "Sem local"
                    txtDescricao.text = documento.getString("descricao") ?: "Sem descrição"

                    eventoEncerrado = verificarEventoEncerrado(data, horario)

                    val usuario = auth.currentUser
                    if (usuario != null) {
                        atualizarPermissaoAvaliacao(
                            usuario.uid,
                            "${usuario.uid}_${eventoId}"
                        )
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Evento não encontrado",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Erro ao carregar evento",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun verificarEventoEncerrado(data: String, horario: String): Boolean {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            formato.isLenient = false
            val dataEvento = formato.parse("$data $horario") ?: return false
            dataEvento.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    private fun atualizarPermissaoAvaliacao(uid: String, avaliacaoId: String) {
        if (!::btnSalvarAvaliacao.isInitialized) return

        if (!eventoEncerrado) {
            txtSituacaoAvaliacao.text =
                "A avaliação estará disponível após o encerramento do evento."
            btnSalvarAvaliacao.isEnabled = false
            ratingBar.isEnabled = false
            return
        }

        if (!usuarioInscrito) {
            txtSituacaoAvaliacao.text =
                "Somente alunos inscritos podem avaliar este evento."
            btnSalvarAvaliacao.isEnabled = false
            ratingBar.isEnabled = false
            return
        }

        ratingBar.isEnabled = true
        btnSalvarAvaliacao.isEnabled = true
        txtSituacaoAvaliacao.text = "Você pode avaliar este evento."

        db.collection("avaliacoes")
            .document(avaliacaoId)
            .get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    val nota = documento.getLong("nota")?.toFloat() ?: 0f
                    ratingBar.rating = nota
                    btnSalvarAvaliacao.text = "ALTERAR AVALIAÇÃO"
                } else {
                    ratingBar.rating = 0f
                    btnSalvarAvaliacao.text = "ENVIAR AVALIAÇÃO"
                }
            }
    }

    private fun salvarAvaliacao(uid: String, avaliacaoId: String) {
        if (salvandoAvaliacao) return

        if (!eventoEncerrado || !usuarioInscrito) {
            Toast.makeText(
                this,
                "Você não pode avaliar este evento.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val nota = ratingBar.rating.toInt()

        if (nota !in 1..5) {
            Toast.makeText(
                this,
                "Selecione uma nota de 1 a 5 estrelas.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        salvandoAvaliacao = true
        btnSalvarAvaliacao.isEnabled = false

        val dados = hashMapOf(
            "usuarioId" to uid,
            "eventoId" to eventoId,
            "nota" to nota,
            "dataAvaliacao" to FieldValue.serverTimestamp()
        )

        db.collection("avaliacoes")
            .document(avaliacaoId)
            .set(dados)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Avaliação salva!",
                    Toast.LENGTH_SHORT
                ).show()
                btnSalvarAvaliacao.text = "ALTERAR AVALIAÇÃO"
                carregarMediaAvaliacoes()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(
                    this,
                    "Erro ao avaliar: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnCompleteListener {
                salvandoAvaliacao = false
                btnSalvarAvaliacao.isEnabled = eventoEncerrado && usuarioInscrito
            }
    }

    private fun carregarMediaAvaliacoes() {
        db.collection("avaliacoes")
            .whereEqualTo("eventoId", eventoId)
            .get()
            .addOnSuccessListener { documentos ->
                if (documentos.isEmpty) {
                    txtMediaAvaliacoes.text = "Média: ainda sem avaliações"
                    return@addOnSuccessListener
                }

                val notas = documentos.documents.mapNotNull {
                    it.getLong("nota")?.toDouble()
                }

                if (notas.isEmpty()) {
                    txtMediaAvaliacoes.text = "Média: ainda sem avaliações"
                    return@addOnSuccessListener
                }

                val media = notas.average()
                txtMediaAvaliacoes.text =
                    "Média: ${String.format(Locale("pt", "BR"), "%.1f", media)} / 5 (${notas.size} avaliações)"
            }
            .addOnFailureListener {
                txtMediaAvaliacoes.text = "Não foi possível carregar a média."
            }
    }

    private fun publicarComentario(uid: String) {
        if (publicandoComentario) return

        val texto = edtComentario.text.toString().trim()

        if (texto.isBlank()) {
            edtComentario.error = "Digite seu comentário"
            return
        }

        publicandoComentario = true
        btnPublicarComentario.isEnabled = false

        val dados = hashMapOf(
            "eventoId" to eventoId,
            "usuarioId" to uid,
            "nomeAutor" to nomeAutor,
            "texto" to texto,
            "dataPublicacao" to FieldValue.serverTimestamp()
        )

        db.collection("comentarios")
            .add(dados)
            .addOnSuccessListener {
                edtComentario.text.clear()
                Toast.makeText(
                    this,
                    "Comentário publicado!",
                    Toast.LENGTH_SHORT
                ).show()
                carregarComentarios(uid)
            }
            .addOnFailureListener { erro ->
                Toast.makeText(
                    this,
                    "Erro ao publicar: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnCompleteListener {
                publicandoComentario = false
                btnPublicarComentario.isEnabled = true
            }
    }

    private fun carregarComentarios(uid: String) {
        db.collection("comentarios")
            .whereEqualTo("eventoId", eventoId)
            .get()
            .addOnSuccessListener { documentos ->
                containerComentarios.removeAllViews()

                val comentarios = documentos.documents.sortedByDescending {
                    it.getTimestamp("dataPublicacao")?.toDate()?.time ?: 0L
                }

                if (comentarios.isEmpty()) {
                    val mensagem = TextView(this)
                    mensagem.text = "Nenhum comentário ainda. Seja o primeiro!"
                    mensagem.textSize = 15f
                    mensagem.setTextColor(Color.DKGRAY)
                    containerComentarios.addView(mensagem)
                    return@addOnSuccessListener
                }

                comentarios.forEach { documento ->
                    adicionarComentario(documento, uid)
                }
            }
            .addOnFailureListener { erro ->
                Toast.makeText(
                    this,
                    "Erro ao carregar comentários: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun adicionarComentario(documento: DocumentSnapshot, uid: String) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(18, 16, 18, 16)
        card.setBackgroundColor(Color.WHITE)

        val parametros = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        parametros.setMargins(0, 0, 0, 14)
        card.layoutParams = parametros

        val autor = TextView(this)
        autor.text = documento.getString("nomeAutor") ?: "Usuário"
        autor.textSize = 16f
        autor.setTextColor(Color.rgb(94, 53, 177))
        autor.setTypeface(null, Typeface.BOLD)

        val data = TextView(this)
        val timestamp = documento.getTimestamp("dataPublicacao")
        data.text = timestamp?.toDate()?.let { formatarData(it) } ?: "Publicado agora"
        data.textSize = 12f
        data.setTextColor(Color.GRAY)

        val texto = TextView(this)
        texto.text = documento.getString("texto") ?: ""
        texto.textSize = 15f
        texto.setTextColor(Color.DKGRAY)
        texto.setPadding(0, 10, 0, 10)

        card.addView(autor)
        card.addView(data)
        card.addView(texto)

        if (documento.getString("usuarioId") == uid) {
            val botoes = LinearLayout(this)
            botoes.orientation = LinearLayout.HORIZONTAL

            val btnEditar = Button(this)
            btnEditar.text = "EDITAR"

            val btnExcluir = Button(this)
            btnExcluir.text = "EXCLUIR"

            botoes.addView(
                btnEditar,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
            botoes.addView(
                btnExcluir,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            btnEditar.setOnClickListener {
                editarComentario(documento.id, texto.text.toString(), uid)
            }

            btnExcluir.setOnClickListener {
                confirmarExclusao(documento.id, uid)
            }

            card.addView(botoes)
        }

        containerComentarios.addView(card)
    }

    private fun editarComentario(comentarioId: String, textoAtual: String, uid: String) {
        val campo = EditText(this)
        campo.setText(textoAtual)
        campo.setSelection(campo.text.length)
        campo.minLines = 2
        campo.maxLines = 6

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar comentário")
            .setView(campo)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val novoTexto = campo.text.toString().trim()

                if (novoTexto.isBlank()) {
                    campo.error = "Digite um comentário"
                    return@setOnClickListener
                }

                val referencia = db.collection("comentarios").document(comentarioId)

                referencia.get()
                    .addOnSuccessListener { documento ->
                        if (documento.exists() &&
                            documento.getString("usuarioId") == uid
                        ) {
                            referencia.update("texto", novoTexto)
                                .addOnSuccessListener {
                                    dialog.dismiss()
                                    carregarComentarios(uid)
                                    Toast.makeText(
                                        this,
                                        "Comentário atualizado!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { erro ->
                                    Toast.makeText(
                                        this,
                                        "Erro ao atualizar: ${erro.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "Você só pode editar seus comentários",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Erro ao verificar comentário",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        dialog.show()
    }

    private fun confirmarExclusao(comentarioId: String, uid: String) {
        AlertDialog.Builder(this)
            .setTitle("Excluir comentário")
            .setMessage("Deseja realmente excluir este comentário?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                val referencia = db.collection("comentarios").document(comentarioId)

                referencia.get()
                    .addOnSuccessListener { documento ->
                        if (documento.exists() &&
                            documento.getString("usuarioId") == uid
                        ) {
                            referencia.delete()
                                .addOnSuccessListener {
                                    carregarComentarios(uid)
                                    Toast.makeText(
                                        this,
                                        "Comentário excluído!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { erro ->
                                    Toast.makeText(
                                        this,
                                        "Erro ao excluir: ${erro.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "Você só pode excluir seus comentários",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Erro ao verificar comentário",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .show()
    }

    private fun formatarData(data: Date): String {
        return SimpleDateFormat(
            "dd/MM/yyyy 'às' HH:mm",
            Locale("pt", "BR")
        ).format(data)
    }
}
