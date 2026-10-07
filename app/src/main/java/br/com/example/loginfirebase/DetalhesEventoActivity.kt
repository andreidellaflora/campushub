package br.com.example.loginfirebase

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class DetalhesEventoActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_evento)

        db = FirebaseFirestore.getInstance()

        val txtTitulo = findViewById<TextView>(R.id.txtTituloEvento)
        val txtData = findViewById<TextView>(R.id.txtDataEvento)
        val txtHorario = findViewById<TextView>(R.id.txtHorarioEvento)
        val txtLocal = findViewById<TextView>(R.id.txtLocalEvento)
        val txtDescricao = findViewById<TextView>(R.id.txtDescricaoEvento)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        val eventoId = intent.getStringExtra("eventoId")

        if (eventoId == null) {
            Toast.makeText(
                this,
                "Evento não encontrado",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

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

        btnVoltar.setOnClickListener {
            finish()
        }
    }
}