
package br.com.example.loginfirebase

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class CadastroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        auth = FirebaseAuth.getInstance()

        val edtEmail = findViewById<EditText>(R.id.edtEmailCadastro)
        val edtSenha = findViewById<EditText>(R.id.edtSenhaCadastro)
        val edtConfirmarSenha = findViewById<EditText>(R.id.edtConfirmarSenha)
        val btnCadastrar = findViewById<Button>(R.id.btnCadastrar)

        btnCadastrar.setOnClickListener {

            val email = edtEmail.text.toString().trim()
            val senha = edtSenha.text.toString().trim()
            val confirmarSenha = edtConfirmarSenha.text.toString().trim()

            if (email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                Toast.makeText(
                    this,
                    "Preencha todos os campos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (senha != confirmarSenha) {
                Toast.makeText(
                    this,
                    "As senhas não coincidem",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Conta criada com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

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

