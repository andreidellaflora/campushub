package br.com.example.loginfirebase

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val edtNome = findViewById<EditText>(R.id.edtNome)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtCurso = findViewById<EditText>(R.id.edtCurso)
        val edtMatricula = findViewById<EditText>(R.id.edtMatricula)
        val edtTelefone = findViewById<EditText>(R.id.edtTelefone)
        val btnSalvar = findViewById<Button>(R.id.btnSalvar)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        val usuario = auth.currentUser

        if (usuario == null) {
            finish()
            return
        }

        val uid = usuario.uid

        edtEmail.setText(usuario.email)

        db.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    edtNome.setText(documento.getString("nome") ?: "")
                    edtEmail.setText(documento.getString("email") ?: usuario.email)
                    edtCurso.setText(documento.getString("curso") ?: "")
                    edtMatricula.setText(documento.getString("matricula") ?: "")
                    edtTelefone.setText(documento.getString("telefone") ?: "")
                }
            }

        btnSalvar.setOnClickListener {

            val dadosUsuario = hashMapOf(
                "nome" to edtNome.text.toString().trim(),
                "email" to edtEmail.text.toString().trim(),
                "curso" to edtCurso.text.toString().trim(),
                "matricula" to edtMatricula.text.toString().trim(),
                "telefone" to edtTelefone.text.toString().trim()
            )

            db.collection("usuarios")
                .document(uid)
                .set(dadosUsuario)
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        "Perfil salvo com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { erro ->
                    Toast.makeText(
                        this,
                        "Erro ao salvar: ${erro.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        btnVoltar.setOnClickListener {
            finish()
        }
    }
}