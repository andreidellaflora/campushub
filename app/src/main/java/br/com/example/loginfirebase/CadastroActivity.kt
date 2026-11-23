
package br.com.example.loginfirebase

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CadastroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val edtEmail = findViewById<EditText>(R.id.edtEmailCadastro)
        val edtSenha = findViewById<EditText>(R.id.edtSenhaCadastro)
        val edtConfirmarSenha = findViewById<EditText>(R.id.edtConfirmarSenha)
        val btnCadastrar = findViewById<Button>(R.id.btnCadastrar)

        btnCadastrar.setOnClickListener {

            val email = edtEmail.text.toString().trim()
            val senha = edtSenha.text.toString().trim()
            val confirmarSenha = edtConfirmarSenha.text.toString().trim()

            if (email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (senha != confirmarSenha) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        val usuario = auth.currentUser

                        if (usuario == null) {
                            Toast.makeText(this, "Erro ao identificar usuário.", Toast.LENGTH_LONG).show()
                            return@addOnCompleteListener
                        }

                        val dadosUsuario = hashMapOf(
                            "uid" to usuario.uid,
                            "email" to email,
                            "tipoUsuario" to "ALUNO",
                            "dataCadastro" to System.currentTimeMillis()
                        )

                        db.collection("usuarios")
                            .document(usuario.uid)
                            .set(dadosUsuario)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Conta criada com sucesso!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                auth.signOut()
                                finish()
                            }
                            .addOnFailureListener { erro ->
                                Toast.makeText(
                                    this,
                                    "Conta criada, mas houve erro ao salvar o perfil: ${erro.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Erro ao criar conta",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
