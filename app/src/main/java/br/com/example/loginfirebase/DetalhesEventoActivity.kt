
package br.com.example.loginfirebase

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private var eventoId = ""
    private var nomeAutor = "Usuário"
    private var processandoFavorito = false
    private var processandoInscricao = false
    private var publicandoComentario = false
    private var salvandoAvaliacao = false
    private var eventoEncerrado = false
    private var usuarioInscrito = false

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

        edtComentario = findViewById(R.id.edtComentario)
        btnPublicarComentario = findViewById(R.id.btnPublicarComentario)
        containerComentarios = findViewById(R.id.comentariosContainer)

        ratingBar = findViewById(R.id.ratingBarAvaliacao)
        btnSalvarAvaliacao = findViewById(R.id.btnSalvarAvaliacao)
        txtMediaAvaliacoes = findViewById(R.id.txtMediaAvaliacoes)
        txtSituacaoAvaliacao = findViewById(R.id.txtSituacaoAvaliacao)

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
                usuarioInscrito = documento.exists()

                btnInscrever.text = if (usuarioInscrito) {
                    "CANCELAR INSCRIÇÃO"
                } else {
                    "INSCREVER-SE"
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
            if (processandoInscricao) return@setOnClickListener

            processandoInscricao = true
            btnInscrever.isEnabled = false

            val referencia = db.collection("inscricoes").document(inscricaoId)

            referencia.get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        referencia.delete()
                            .addOnSuccessListener {
                                usuarioInscrito = false
                                btnInscrever.text = "INSCREVER-SE"
                                atualizarPermissaoAvaliacao(uid, avaliacaoId)
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
                                usuarioInscrito = true
                                btnInscrever.text = "CANCELAR INSCRIÇÃO"
                                atualizarPermissaoAvaliacao(uid, avaliacaoId)
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

        btnPublicarComentario.setOnClickListener {
            publicarComentario(uid)
        }

        btnSalvarAvaliacao.setOnClickListener {
            salvarAvaliacao(uid, avaliacaoId)
        }

        btnVoltar.setOnClickListener {
            finish()
        }

        carregarComentarios(uid)
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
                    Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar evento", Toast.LENGTH_SHORT).show()
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
            txtSituacaoAvaliacao.text = "A avaliação estará disponível após o encerramento do evento."
            btnSalvarAvaliacao.isEnabled = false
            ratingBar.isEnabled = false
            return
        }

        if (!usuarioInscrito) {
            txtSituacaoAvaliacao.text = "Somente alunos inscritos podem avaliar este evento."
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
            Toast.makeText(this, "Você não pode avaliar este evento.", Toast.LENGTH_SHORT).show()
            return
        }

        val nota = ratingBar.rating.toInt()

        if (nota !in 1..5) {
            Toast.makeText(this, "Selecione uma nota de 1 a 5 estrelas.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Avaliação salva!", Toast.LENGTH_SHORT).show()
                btnSalvarAvaliacao.text = "ALTERAR AVALIAÇÃO"
                carregarMediaAvaliacoes()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao avaliar: ${erro.message}", Toast.LENGTH_LONG).show()
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
                Toast.makeText(this, "Comentário publicado!", Toast.LENGTH_SHORT).show()
                carregarComentarios(uid)
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao publicar: ${erro.message}", Toast.LENGTH_LONG).show()
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
                Toast.makeText(this, "Erro ao carregar comentários: ${erro.message}", Toast.LENGTH_LONG).show()
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
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            botoes.addView(
                btnExcluir,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
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
                        if (documento.exists() && documento.getString("usuarioId") == uid) {
                            referencia.update("texto", novoTexto)
                                .addOnSuccessListener {
                                    dialog.dismiss()
                                    carregarComentarios(uid)
                                    Toast.makeText(this, "Comentário atualizado!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { erro ->
                                    Toast.makeText(this, "Erro ao atualizar: ${erro.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Toast.makeText(this, "Você só pode editar seus comentários", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao verificar comentário", Toast.LENGTH_SHORT).show()
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
                        if (documento.exists() && documento.getString("usuarioId") == uid) {
                            referencia.delete()
                                .addOnSuccessListener {
                                    carregarComentarios(uid)
                                    Toast.makeText(this, "Comentário excluído!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { erro ->
                                    Toast.makeText(this, "Erro ao excluir: ${erro.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Toast.makeText(this, "Você só pode excluir seus comentários", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao verificar comentário", Toast.LENGTH_SHORT).show()
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
