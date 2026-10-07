package br.com.example.loginfirebase

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtSenha = findViewById<EditText>(R.id.edtSenha)
        val btnEntrar = findViewById<Button>(R.id.btnEntrar)
        val txtCadastro = findViewById<TextView>(R.id.txtCadastro)
        val txtEsqueciSenha = findViewById<TextView>(R.id.txtEsqueciSenha)

        // LOGIN
        btnEntrar.setOnClickListener {

            val email = edtEmail.text.toString().trim()
            val senha = edtSenha.text.toString().trim()

            if (email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(
                    this,
                    "Preencha e-mail e senha",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {

                        Toast.makeText(
                            this,
                            "E-mail ou senha incorretos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // CADASTRO
        txtCadastro.setOnClickListener {

            val intent = Intent(this, CadastroActivity::class.java)
            startActivity(intent)
        }

        // RECUPERAÇÃO DE SENHA
        txtEsqueciSenha.setOnClickListener {

            val email = edtEmail.text.toString().trim()

            if (email.isEmpty()) {

                Toast.makeText(
                    this,
                    "Digite seu e-mail primeiro",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "E-mail de recuperação enviado!",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "Não foi possível enviar o e-mail.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}