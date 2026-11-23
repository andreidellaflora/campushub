
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

class GerenciarEventosActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var eventosContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gerenciar_eventos)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        eventosContainer = findViewById(R.id.eventosContainer)

        val btnNovoEvento = findViewById<Button>(R.id.btnNovoEvento)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        btnVoltar.setOnClickListener {
            finish()
        }

        btnNovoEvento.setOnClickListener {
            startActivity(
                Intent(this, FormEventoActivity::class.java)
            )
        }

        verificarOrganizador()
    }

    private fun verificarOrganizador() {
        val usuario = auth.currentUser

        if (usuario == null) {
            finish()
            return
        }

        db.collection("usuarios")
            .document(usuario.uid)
            .get()
            .addOnSuccessListener { documento ->

                val tipo = documento.getString("tipoUsuario")

                if (tipo != "ORGANIZADOR") {
                    Toast.makeText(
                        this,
                        "Acesso permitido somente para organizadores.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                } else {
                    carregarEventos()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Erro ao verificar permissões.",
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

                    val titulo =
                        documento.getString("titulo") ?: "Evento sem título"

                    val data =
                        documento.getString("data") ?: ""

                    val horario =
                        documento.getString("horario") ?: ""

                    val local =
                        documento.getString("local") ?: ""

                    val publicado =
                        documento.getBoolean("publicado") ?: false

                    val encerrado =
                        documento.getBoolean("encerrado") ?: false

                    criarCardEvento(
                        titulo,
                        data,
                        horario,
                        local,
                        publicado,
                        encerrado
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
        titulo: String,
        data: String,
        horario: String,
        local: String,
        publicado: Boolean,
        encerrado: Boolean
    ) {

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(24, 20, 24, 20)
        card.setBackgroundColor(Color.WHITE)

        val parametros = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        parametros.setMargins(0, 0, 0, 20)
        card.layoutParams = parametros

        val txtTitulo = TextView(this)
        txtTitulo.text = titulo
        txtTitulo.textSize = 19f
        txtTitulo.setTextColor(Color.rgb(94, 53, 177))
        txtTitulo.setTypeface(null, android.graphics.Typeface.BOLD)

        val txtData = TextView(this)
        txtData.text = "$data • $horario"
        txtData.textSize = 14f
        txtData.setTextColor(Color.DKGRAY)

        val txtLocal = TextView(this)
        txtLocal.text = local
        txtLocal.textSize = 14f
        txtLocal.setTextColor(Color.GRAY)

        val txtStatus = TextView(this)
        txtStatus.text = when {
            encerrado -> "Status: Encerrado"
            publicado -> "Status: Publicado"
            else -> "Status: Não publicado"
        }
        txtStatus.textSize = 14f
        txtStatus.setTextColor(Color.rgb(94, 53, 177))
        txtStatus.setPadding(0, 8, 0, 8)

        card.addView(txtTitulo)
        card.addView(txtData)
        card.addView(txtLocal)
        card.addView(txtStatus)

        eventosContainer.addView(card)
    }

    private fun mostrarMensagem(mensagem: String) {
        val texto = TextView(this)
        texto.text = mensagem
        texto.textSize = 16f
        texto.setTextColor(Color.DKGRAY)
        texto.setPadding(8, 20, 8, 20)

        eventosContainer.addView(texto)
    }

    override fun onResume() {
        super.onResume()

        if (::db.isInitialized && auth.currentUser != null) {
            carregarEventos()
        }
    }
}
