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

    private var eventoId: String? = null
    private var inscrito = false

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
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        val usuario = auth.currentUser

        if (usuario == null) {
            finish()
            return
        }

        eventoId = intent.getStringExtra("eventoId")

        if (eventoId == null) {
            Toast.makeText(
                this,
                "Evento não encontrado",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        val idEvento = eventoId!!
        val uid = usuario.uid
        val inscricaoId = "${uid}_${idEvento}"

        db.collection("eventos")
            .document(idEvento)
            .get()
            .addOnSuccessListener { documento ->

                if (documento.exists()) {
                    txtTitulo.text =
                        documento.getString("titulo") ?: "Sem título"

                    txtData.text =
                        documento.getString("data") ?: "Sem data"

                    txtHorario.text =
                        documento.getString("horario") ?: "Sem horário"

                    txtLocal.text =
                        documento.getString("local") ?: "Sem local"

                    txtDescricao.text =
                        documento.getString("descricao") ?: "Sem descrição"
                } else {
                    Toast.makeText(
                        this,
                        "Evento não encontrado no Firestore",
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

        db.collection("inscricoes")
            .document(inscricaoId)
            .get()
            .addOnSuccessListener { documento ->

                if (documento.exists()) {
                    inscrito = true
                    btnInscrever.text = "CANCELAR INSCRIÇÃO"
                } else {
                    inscrito = false
                    btnInscrever.text = "INSCREVER-SE"
                }
            }

        btnInscrever.setOnClickListener {

            if (inscrito) {

                db.collection("inscricoes")
                    .document(inscricaoId)
                    .delete()
                    .addOnSuccessListener {
                        inscrito = false
                        btnInscrever.text = "INSCREVER-SE"

                        Toast.makeText(
                            this,
                            "Inscrição cancelada com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { erro ->
                        Toast.makeText(
                            this,
                            "Erro ao cancelar: ${erro.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            } else {

                val dadosInscricao = hashMapOf(
                    "usuarioId" to uid,
                    "eventoId" to idEvento,
                    "dataInscricao" to FieldValue.serverTimestamp()
                )

                db.collection("inscricoes")
                    .document(inscricaoId)
                    .set(dadosInscricao)
                    .addOnSuccessListener {
                        inscrito = true
                        btnInscrever.text = "CANCELAR INSCRIÇÃO"

                        Toast.makeText(
                            this,
                            "Inscrição realizada com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { erro ->
                        Toast.makeText(
                            this,
                            "Erro ao realizar inscrição: ${erro.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        btnVoltar.setOnClickListener {
            finish()
        }
    }
}