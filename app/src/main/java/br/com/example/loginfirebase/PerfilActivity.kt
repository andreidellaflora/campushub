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

        val edtNome = findViewById<EditText>(R.id.edtNomePerfil)
        val edtEmail = findViewById<EditText>(R.id.edtEmailPerfil)
        val btnSalvar = findViewById<Button>(R.id.btnSalvarPerfil)
        val btnVoltar = findViewById<Button>(R.id.btnVoltarPerfil)

        // Usuário atualmente logado
        val usuario = auth.currentUser

        if (usuario == null) {
            finish()
            return
        }

        val uid = usuario.uid
        val email = usuario.email

        // E-mail vem do Firebase Authentication
        edtEmail.setText(email)

        // Busca o nome no Firestore
        db.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->

                if (documento.exists()) {
                    val nome = documento.getString("nome")

                    if (nome != null) {
                        edtNome.setText(nome)
                    }
                }
            }

        // Salvar perfil
        btnSalvar.setOnClickListener {

            val nome = edtNome.text.toString().trim()

            if (nome.isEmpty()) {
                Toast.makeText(
                    this,
                    "Digite seu nome",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val dadosUsuario = hashMapOf(
                "nome" to nome,
                "email" to (email ?: "")
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
                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "Erro ao salvar perfil",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // Voltar
        btnVoltar.setOnClickListener {
            finish()
        }
    }
}