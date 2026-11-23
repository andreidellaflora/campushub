
package br.com.example.loginfirebase

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FormEventoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_evento)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val edtTitulo = findViewById<EditText>(R.id.edtTituloEvento)
        val edtData = findViewById<EditText>(R.id.edtDataEvento)
        val edtHorario = findViewById<EditText>(R.id.edtHorarioEvento)
        val edtLocal = findViewById<EditText>(R.id.edtLocalEvento)
        val edtCategoria = findViewById<EditText>(R.id.edtCategoriaEvento)
        val edtDescricao = findViewById<EditText>(R.id.edtDescricaoEvento)
        val edtCapacidade = findViewById<EditText>(R.id.edtCapacidadeEvento)

        val btnSalvar = findViewById<Button>(R.id.btnSalvarEvento)
        val btnCancelar = findViewById<Button>(R.id.btnCancelarEvento)

        btnCancelar.setOnClickListener {
            finish()
        }

        btnSalvar.setOnClickListener {

            val titulo = edtTitulo.text.toString().trim()
            val data = edtData.text.toString().trim()
            val horario = edtHorario.text.toString().trim()
            val local = edtLocal.text.toString().trim()
            val categoria = edtCategoria.text.toString().trim()
            val descricao = edtDescricao.text.toString().trim()
            val capacidadeTexto = edtCapacidade.text.toString().trim()

            if (
                titulo.isEmpty() ||
                data.isEmpty() ||
                horario.isEmpty() ||
                local.isEmpty() ||
                categoria.isEmpty() ||
                descricao.isEmpty() ||
                capacidadeTexto.isEmpty()
            ) {
                Toast.makeText(
                    this,
                    "Preencha todos os campos.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val capacidade = capacidadeTexto.toIntOrNull()

            if (capacidade == null || capacidade <= 0) {
                Toast.makeText(
                    this,
                    "Informe uma capacidade válida.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            salvarEvento(
                titulo,
                data,
                horario,
                local,
                categoria,
                descricao,
                capacidade
            )
        }
    }

    private fun salvarEvento(
        titulo: String,
        data: String,
        horario: String,
        local: String,
        categoria: String,
        descricao: String,
        capacidade: Int
    ) {

        val usuario = auth.currentUser

        if (usuario == null) {
            Toast.makeText(
                this,
                "Faça login novamente.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        db.collection("usuarios")
            .document(usuario.uid)
            .get()
            .addOnSuccessListener { documento ->

                if (documento.getString("tipoUsuario") != "ORGANIZADOR") {
                    Toast.makeText(
                        this,
                        "Somente organizadores podem cadastrar eventos.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val evento = hashMapOf(
                    "titulo" to titulo,
                    "data" to data,
                    "horario" to horario,
                    "local" to local,
                    "categoria" to categoria,
                    "descricao" to descricao,
                    "capacidade" to capacidade,
                    "vagasOcupadas" to 0,
                    "criadorId" to usuario.uid,
                    "publicado" to true,
                    "encerrado" to false,
                    "dataCriacao" to System.currentTimeMillis()
                )

                db.collection("eventos")
                    .add(evento)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Evento cadastrado com sucesso!",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    }
                    .addOnFailureListener { erro ->
                        Toast.makeText(
                            this,
                            "Erro ao salvar: ${erro.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { erro ->
                Toast.makeText(
                    this,
                    "Erro ao verificar organizador: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
