
package br.com.example.loginfirebase

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var eventosContainer: LinearLayout
    private lateinit var edtPesquisar: EditText
    private lateinit var spCategoria: Spinner
    private lateinit var spSituacao: Spinner
    private lateinit var btnGerenciarEventos: Button

    private val listaEventos = mutableListOf<Evento>()

    data class Evento(
        val id: String,
        val titulo: String,
        val data: String,
        val horario: String,
        val local: String,
        val categoria: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        eventosContainer = findViewById(R.id.eventosContainer)
        edtPesquisar = findViewById(R.id.edtPesquisar)
        spCategoria = findViewById(R.id.spCategoria)
        spSituacao = findViewById(R.id.spSituacao)

        val btnPerfil = findViewById<Button>(R.id.btnPerfil)
        val btnMeusEventos = findViewById<Button>(R.id.btnMeusEventos)
        val btnMeusFavoritos = findViewById<Button>(R.id.btnMeusFavoritos)
        val btnSair = findViewById<Button>(R.id.btnSair)

        btnGerenciarEventos = findViewById(R.id.btnGerenciarEventos)
        btnGerenciarEventos.visibility = View.GONE

        btnPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        btnMeusEventos.setOnClickListener {
            startActivity(Intent(this, MeusEventosActivity::class.java))
        }

        btnMeusFavoritos.setOnClickListener {
            startActivity(Intent(this, MeusFavoritosActivity::class.java))
        }

        btnGerenciarEventos.setOnClickListener {
            startActivity(
                Intent(this, GerenciarEventosActivity::class.java)
            )
        }

        btnSair.setOnClickListener {
            auth.signOut()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }

        configurarFiltros()
        verificarPermissaoOrganizador()
        carregarEventos()
    }

    private fun verificarPermissaoOrganizador() {
        val usuario = auth.currentUser

        if (usuario == null) {
            btnGerenciarEventos.visibility = View.GONE
            return
        }

        db.collection("usuarios")
            .document(usuario.uid)
            .get()
            .addOnSuccessListener { documento ->

                val tipoUsuario =
                    documento.getString("tipoUsuario")

                btnGerenciarEventos.visibility =
                    if (tipoUsuario == "ORGANIZADOR") {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
            .addOnFailureListener {
                btnGerenciarEventos.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Não foi possível verificar suas permissões.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun configurarFiltros() {

        val situacoes = listOf(
            "Todas as situações",
            "Próximos",
            "Encerrados"
        )

        val adapterSituacao = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            situacoes
        )

        adapterSituacao.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spSituacao.adapter = adapterSituacao

        spSituacao.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    aplicarFiltros()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        spCategoria.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    aplicarFiltros()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        edtPesquisar.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                aplicarFiltros()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun carregarEventos() {

        eventosContainer.removeAllViews()

        db.collection("eventos")
            .get()
            .addOnSuccessListener { documentos ->

                listaEventos.clear()

                for (documento in documentos) {

                    val evento = Evento(
                        id = documento.id,
                        titulo = documento.getString("titulo") ?: "Evento",
                        data = documento.getString("data") ?: "",
                        horario = documento.getString("horario") ?: "",
                        local = documento.getString("local") ?: "",
                        categoria = documento.getString("categoria")
                            ?.takeIf { it.isNotBlank() }
                            ?: "Sem categoria"
                    )

                    listaEventos.add(evento)
                }

                android.util.Log.d(
                    "CampusHub",
                    "Eventos encontrados: ${documentos.size()}"
                )

                carregarCategorias()
                aplicarFiltros()

                if (documentos.isEmpty) {
                    Toast.makeText(
                        this,
                        "A coleção eventos está vazia.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { erro ->

                android.util.Log.e(
                    "CampusHub",
                    "Erro ao carregar eventos",
                    erro
                )

                Toast.makeText(
                    this,
                    "Erro Firebase: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun carregarCategorias() {

        val categorias = mutableListOf("Todas as categorias")

        categorias.addAll(
            listaEventos
                .map { it.categoria }
                .distinct()
                .sorted()
        )

        val adapterCategoria = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categorias
        )

        adapterCategoria.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spCategoria.adapter = adapterCategoria
    }

    private fun aplicarFiltros() {

        if (!::eventosContainer.isInitialized) return

        val pesquisa = edtPesquisar.text.toString().trim()
            .lowercase(Locale.ROOT)

        val categoriaSelecionada =
            spCategoria.selectedItem?.toString()
                ?: "Todas as categorias"

        val situacaoSelecionada =
            spSituacao.selectedItem?.toString()
                ?: "Todas as situações"

        val eventosFiltrados = listaEventos.filter { evento ->

            val correspondeTitulo =
                evento.titulo.lowercase(Locale.ROOT).contains(pesquisa)

            val correspondeCategoria =
                categoriaSelecionada == "Todas as categorias" ||
                        evento.categoria == categoriaSelecionada

            val encerrado = eventoEncerrado(
                evento.data,
                evento.horario
            )

            val correspondeSituacao = when (situacaoSelecionada) {
                "Próximos" -> !encerrado
                "Encerrados" -> encerrado
                else -> true
            }

            correspondeTitulo &&
                    correspondeCategoria &&
                    correspondeSituacao
        }

        mostrarEventos(eventosFiltrados)
    }

    private fun eventoEncerrado(
        data: String,
        horario: String
    ): Boolean {

        return try {

            val formato = SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale("pt", "BR")
            )

            formato.isLenient = false

            val dataEvento = formato.parse("$data $horario")
                ?: return false

            dataEvento.before(Date())

        } catch (e: Exception) {
            false
        }
    }

    private fun mostrarEventos(eventos: List<Evento>) {

        eventosContainer.removeAllViews()

        if (eventos.isEmpty()) {

            val mensagem = TextView(this)
            mensagem.text = "Nenhum evento encontrado."
            mensagem.textSize = 16f
            mensagem.setTextColor(Color.DKGRAY)
            mensagem.setPadding(0, 16, 0, 16)

            eventosContainer.addView(mensagem)
            return
        }

        for (evento in eventos) {

            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.setPadding(20, 20, 20, 20)
            card.setBackgroundColor(Color.WHITE)

            val parametros = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            parametros.setMargins(0, 0, 0, 16)
            card.layoutParams = parametros

            val txtTitulo = TextView(this)
            txtTitulo.text = evento.titulo
            txtTitulo.textSize = 19f
            txtTitulo.setTextColor(Color.rgb(94, 53, 177))

            val txtData = TextView(this)
            txtData.text = "${evento.data} • ${evento.horario}"
            txtData.textSize = 14f
            txtData.setTextColor(Color.DKGRAY)

            val txtLocal = TextView(this)
            txtLocal.text = evento.local
            txtLocal.textSize = 14f
            txtLocal.setTextColor(Color.GRAY)

            val txtCategoria = TextView(this)
            txtCategoria.text = "Categoria: ${evento.categoria}"
            txtCategoria.textSize = 13f
            txtCategoria.setTextColor(Color.GRAY)

            val txtSituacao = TextView(this)

            txtSituacao.text =
                if (eventoEncerrado(evento.data, evento.horario)) {
                    "Evento encerrado"
                } else {
                    "Próximo evento"
                }

            txtSituacao.textSize = 13f
            txtSituacao.setTextColor(Color.rgb(94, 53, 177))

            val btnDetalhes = Button(this)
            btnDetalhes.text = "VER DETALHES"
            btnDetalhes.setTextColor(Color.WHITE)
            btnDetalhes.setBackgroundColor(Color.rgb(106, 75, 188))

            val margemBotao = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            margemBotao.setMargins(0, 14, 0, 0)
            btnDetalhes.layoutParams = margemBotao

            btnDetalhes.setOnClickListener {

                val intent = Intent(
                    this,
                    DetalhesEventoActivity::class.java
                )

                intent.putExtra("eventoId", evento.id)
                startActivity(intent)
            }

            card.addView(txtTitulo)
            card.addView(txtData)
            card.addView(txtLocal)
            card.addView(txtCategoria)
            card.addView(txtSituacao)
            card.addView(btnDetalhes)

            eventosContainer.addView(card)
        }
    }

    override fun onResume() {
        super.onResume()

        if (::db.isInitialized) {
            verificarPermissaoOrganizador()
            carregarEventos()
        }
    }
}
