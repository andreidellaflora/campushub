
package br.com.example.loginfirebase

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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

    private var eventoId = ""
    private var nomeAutor = "Usuário"
    private var processandoFavorito = false
    private var processandoInscricao = false
    private var publicandoComentario = false

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
                Toast.makeText(
                    this,
                    "Erro ao verificar inscrição",
                    Toast.LENGTH_SHORT
                ).show()
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
                Toast.makeText(
                    this,
                    "Erro ao verificar favorito",
                    Toast.LENGTH_SHORT
                ).show()
            }

        btnInscrever.setOnClickListener {
            if (processandoInscricao) return@setOnClickListener

            processandoInscricao = true
            btnInscrever.isEnabled = false

            val referencia = db.collection("inscricoes")
                .document(inscricaoId)

            referencia.get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        referencia.delete()
                            .addOnSuccessListener {
                                btnInscrever.text = "INSCREVER-SE"
                                Toast.makeText(
                                    this,
                                    "Inscrição cancelada!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Erro ao cancelar inscrição",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                                Toast.makeText(
                                    this,
                                    "Inscrição realizada!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Erro ao realizar inscrição",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                    Toast.makeText(
                        this,
                        "Erro ao verificar inscrição",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        btnFavorito.setOnClickListener {
            if (processandoFavorito) return@setOnClickListener

            processandoFavorito = true
            btnFavorito.isEnabled = false

            val referencia = db.collection("favoritos")
                .document(favoritoId)

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

        btnPublicarComentario.setOnClickListener {
            publicarComentario(uid)
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
                    txtData.text = documento.getString("data") ?: "Sem data"
                    txtHorario.text = documento.getString("horario") ?: "Sem horário"
                    txtLocal.text = documento.getString("local") ?: "Sem local"
                    txtDescricao.text = documento.getString("descricao") ?: "Sem descrição"
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

    private fun adicionarComentario(
        documento: DocumentSnapshot,
        uid: String
    ) {
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
        data.text = timestamp?.toDate()?.let { formatarData(it) }
            ?: "Publicado agora"
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

    private fun editarComentario(
        comentarioId: String,
        textoAtual: String,
        uid: String
    ) {
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

                val referencia = db.collection("comentarios")
                    .document(comentarioId)

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
                val referencia = db.collection("comentarios")
                    .document(comentarioId)

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
