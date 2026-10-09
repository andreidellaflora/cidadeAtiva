package br.com.example.cidadeativa

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProblemasDaRegiaoActivity : AppCompatActivity() {

    private lateinit var listaReclamacoes: LinearLayout

    private lateinit var btnTodos: Button
    private lateinit var btnBuracos: Button
    private lateinit var btnIluminacao: Button
    private lateinit var btnLixo: Button

    private val db =
        FirebaseFirestore.getInstance()

    private var listenerReclamacoes:
            ListenerRegistration? = null

    private var documentosAtuais:
            List<DocumentSnapshot> = emptyList()

    private var filtroAtual =
        "Todos"

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_problemas_da_regiao
        )

        // ==========================================
        // COMPONENTES
        // ==========================================

        listaReclamacoes =
            findViewById(
                R.id.listaReclamacoesRegiao
            )

        btnTodos =
            findViewById(
                R.id.btnTodosRegiao
            )

        btnBuracos =
            findViewById(
                R.id.btnBuracosRegiao
            )

        btnIluminacao =
            findViewById(
                R.id.btnIluminacaoRegiao
            )

        btnLixo =
            findViewById(
                R.id.btnLixoRegiao
            )

        // ==========================================
        // FILTRO TODOS
        // ==========================================

        btnTodos.setOnClickListener {

            filtroAtual =
                "Todos"

            atualizarBotoes()

            mostrarReclamacoes()
        }

        // ==========================================
        // FILTRO BURACOS
        // ==========================================

        btnBuracos.setOnClickListener {

            filtroAtual =
                "Buraco na rua"

            atualizarBotoes()

            mostrarReclamacoes()
        }

        // ==========================================
        // FILTRO ILUMINAÇÃO
        // ==========================================

        btnIluminacao.setOnClickListener {

            filtroAtual =
                "Iluminação pública"

            atualizarBotoes()

            mostrarReclamacoes()
        }

        // ==========================================
        // FILTRO LIXO
        // ==========================================

        btnLixo.setOnClickListener {

            filtroAtual =
                "Lixo acumulado"

            atualizarBotoes()

            mostrarReclamacoes()
        }

        atualizarBotoes()

        // ==========================================
        // BUSCAR TODAS AS RECLAMAÇÕES
        // ==========================================

        observarProblemasDaRegiao()
    }

    // =================================================
    // BUSCAR TODAS AS RECLAMAÇÕES
    // =================================================

    private fun observarProblemasDaRegiao() {

        listenerReclamacoes?.remove()

        listenerReclamacoes =
            db.collection("reclamacoes")
                .addSnapshotListener {

                        documentos,
                        erro ->

                    if (erro != null) {

                        listaReclamacoes
                            .removeAllViews()

                        val texto =
                            TextView(this)

                        texto.text =
                            "Erro ao carregar os problemas:\n\n" +
                                    erro.message

                        texto.textSize =
                            16f

                        texto.setTextColor(
                            Color.RED
                        )

                        texto.gravity =
                            Gravity.CENTER

                        texto.setPadding(
                            25,
                            50,
                            25,
                            50
                        )

                        listaReclamacoes
                            .addView(
                                texto
                            )

                        return@addSnapshotListener
                    }

                    if (
                        documentos == null
                    ) {

                        return@addSnapshotListener
                    }

                    documentosAtuais =
                        documentos.documents

                    mostrarReclamacoes()
                }
    }

    // =================================================
    // MOSTRAR RECLAMAÇÕES
    // =================================================

    private fun mostrarReclamacoes() {

        listaReclamacoes
            .removeAllViews()

        val documentosFiltrados =
            if (
                filtroAtual ==
                "Todos"
            ) {

                documentosAtuais

            } else {

                documentosAtuais.filter {
                        documento ->

                    val tipo =
                        documento
                            .getString("tipo")
                            ?.trim()
                            ?: ""

                    tipo.equals(
                        filtroAtual,
                        ignoreCase = true
                    )
                }
            }

        if (
            documentosFiltrados.isEmpty()
        ) {

            val vazio =
                TextView(this)

            vazio.text =
                if (
                    filtroAtual ==
                    "Todos"
                ) {

                    "Nenhum problema registrado na região."

                } else {

                    "Nenhum problema encontrado nesta categoria."
                }

            vazio.textSize =
                17f

            vazio.setTextColor(
                Color.DKGRAY
            )

            vazio.gravity =
                Gravity.CENTER

            vazio.setPadding(
                20,
                80,
                20,
                80
            )

            listaReclamacoes
                .addView(
                    vazio
                )

            return
        }

        for (
        documento in documentosFiltrados
        ) {

            criarCartao(
                documento
            )
        }
    }

    // =================================================
    // CRIAR CARTÃO
    // =================================================

    private fun criarCartao(
        documento: DocumentSnapshot
    ) {

        val tipo =
            documento
                .getString("tipo")
                ?: "Problema não informado"

        val descricao =
            documento
                .getString("descricao")
                ?: "Sem descrição"

        val endereco =
            documento
                .getString("endereco")
                ?: "Localização não informada"

        val cep =
            documento
                .getString("cep")
                ?: ""

        val status =
            documento
                .getString("status")
                ?: "Pendente"

        val usuarioNome =
            documento
                .getString("usuarioNome")
                ?: "Usuário"

        val usuarioEmail =
            documento
                .getString("usuarioEmail")
                ?: ""

        val data =
            documento.get("data")

        val dataFormatada =
            formatarData(data)

        // ==========================================
        // CARTÃO
        // ==========================================

        val cartao =
            LinearLayout(this)

        cartao.orientation =
            LinearLayout.VERTICAL

        cartao.setPadding(
            18,
            18,
            18,
            18
        )

        val fundo =
            GradientDrawable()

        fundo.setColor(
            Color.WHITE
        )

        fundo.cornerRadius =
            22f

        fundo.setStroke(
            1,
            Color.rgb(
                225,
                220,
                230
            )
        )

        cartao.background =
            fundo

        val parametros =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametros.setMargins(
            0,
            0,
            0,
            16
        )

        cartao.layoutParams =
            parametros

        // ==========================================
        // TIPO
        // ==========================================

        val txtTipo =
            TextView(this)



        txtTipo.setTextColor(
            Color.rgb(
                55,
                55,
                55
            )
        )

        txtTipo.setTypeface(
            null,
            Typeface.BOLD
        )

        cartao.addView(
            txtTipo
        )

        // ==========================================
        // USUÁRIO
        // ==========================================

        val txtUsuario =
            TextView(this)

        txtUsuario.text =
            "👤 Relatado por: $usuarioNome"

        txtUsuario.textSize =
            13f

        txtUsuario.setTextColor(
            Color.GRAY
        )

        txtUsuario.setPadding(
            0,
            6,
            0,
            8
        )

        cartao.addView(
            txtUsuario
        )

        // ==========================================
        // DESCRIÇÃO
        // ==========================================

        val txtDescricao =
            TextView(this)

        txtDescricao.text =
            "📝  $descricao"

        txtDescricao.textSize =
            15f

        txtDescricao.setTextColor(
            Color.DKGRAY
        )

        txtDescricao.setPadding(
            0,
            5,
            0,
            8
        )

        cartao.addView(
            txtDescricao
        )

        // ==========================================
        // ENDEREÇO
        // ==========================================

        val txtEndereco =
            TextView(this)

        txtEndereco.text =
            "📍  $endereco"

        txtEndereco.textSize =
            14f

        txtEndereco.setTextColor(
            Color.DKGRAY
        )

        cartao.addView(
            txtEndereco
        )

        // ==========================================
        // CEP
        // ==========================================

        if (
            cep.isNotBlank()
        ) {

            val txtCep =
                TextView(this)

            txtCep.text =
                "📮 CEP: $cep"

            txtCep.textSize =
                14f

            txtCep.setTextColor(
                Color.DKGRAY
            )

            txtCep.setPadding(
                0,
                5,
                0,
                5
            )

            cartao.addView(
                txtCep
            )
        }

        // ==========================================
        // DATA
        // ==========================================

        if (
            dataFormatada.isNotBlank()
        ) {

            val txtData =
                TextView(this)

            txtData.text =
                "📅 $dataFormatada"

            txtData.textSize =
                13f

            txtData.setTextColor(
                Color.GRAY
            )

            txtData.setPadding(
                0,
                5,
                0,
                5
            )

            cartao.addView(
                txtData
            )
        }

        // ==========================================
        // STATUS
        // ==========================================

        val txtStatus =
            TextView(this)

        txtStatus.text =
            "Status: $status"

        txtStatus.textSize =
            14f

        txtStatus.setTypeface(
            null,
            Typeface.BOLD
        )

        configurarStatus(
            txtStatus,
            status
        )

        cartao.addView(
            txtStatus
        )

        // ==========================================
        // CLIQUE
        // ==========================================

        cartao.setOnClickListener {

            val intent =
                Intent(
                    this,
                    DetalhesReclamacaoActivity::class.java
                )

            intent.putExtra(
                "tipo",
                tipo
            )

            intent.putExtra(
                "descricao",
                descricao
            )

            intent.putExtra(
                "endereco",
                endereco
            )

            intent.putExtra(
                "cep",
                cep
            )

            intent.putExtra(
                "status",
                status
            )

            intent.putExtra(
                "documentoId",
                documento.id
            )

            intent.putExtra(
                "usuarioNome",
                usuarioNome
            )

            intent.putExtra(
                "usuarioEmail",
                usuarioEmail
            )

            intent.putExtra(
                "documentoId",
                documento.id
            )

            startActivity(
                intent
            )
        }

        listaReclamacoes
            .addView(
                cartao
            )
    }

    // =================================================
    // CONFIGURAR STATUS
    // =================================================

    private fun configurarStatus(

        txtStatus: TextView,

        status: String

    ) {

        val fundo =
            GradientDrawable()

        fundo.cornerRadius =
            20f

        when {

            status.equals(
                "Resolvido",
                ignoreCase = true
            ) -> {

                fundo.setColor(
                    Color.rgb(
                        215,
                        245,
                        220
                    )
                )

                txtStatus.setTextColor(
                    Color.rgb(
                        30,
                        130,
                        50
                    )
                )
            }

            status.equals(
                "Em análise",
                ignoreCase = true
            ) ||
                    status.equals(
                        "Em analise",
                        ignoreCase = true
                    ) -> {

                fundo.setColor(
                    Color.rgb(
                        215,
                        232,
                        255
                    )
                )

                txtStatus.setTextColor(
                    Color.rgb(
                        30,
                        90,
                        180
                    )
                )
            }

            else -> {

                fundo.setColor(
                    Color.rgb(
                        255,
                        237,
                        200
                    )
                )

                txtStatus.setTextColor(
                    Color.rgb(
                        180,
                        110,
                        0
                    )
                )
            }
        }

        txtStatus.background =
            fundo

        txtStatus.setPadding(
            12,
            9,
            12,
            9
        )

        txtStatus.gravity =
            Gravity.CENTER
    }

    // =================================================
    // FORMATAR DATA
    // =================================================

    private fun formatarData(
        valor: Any?
    ): String {

        if (
            valor == null
        ) {

            return ""
        }

        return try {

            val milissegundos =
                when (valor) {

                    is Number ->
                        valor.toLong()

                    else ->
                        null
                }

            if (
                milissegundos != null
            ) {

                val data =
                    Date(
                        milissegundos
                    )

                val formato =
                    SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                    )

                formato.format(
                    data
                )

            } else {

                valor.toString()
            }

        } catch (
            e: Exception
        ) {

            valor.toString()
        }
    }

    // =================================================
    // ATUALIZAR FILTROS
    // =================================================

    private fun atualizarBotoes() {

        btnTodos.alpha =
            if (
                filtroAtual ==
                "Todos"
            ) {

                1.0f

            } else {

                0.65f
            }

        btnBuracos.alpha =
            if (
                filtroAtual ==
                "Buraco na rua"
            ) {

                1.0f

            } else {

                0.65f
            }

        btnIluminacao.alpha =
            if (
                filtroAtual ==
                "Iluminação pública"
            ) {

                1.0f

            } else {

                0.65f
            }

        btnLixo.alpha =
            if (
                filtroAtual ==
                "Lixo acumulado"
            ) {

                1.0f

            } else {

                0.65f
            }
    }

    // =================================================
    // DESTRUIR LISTENER
    // =================================================

    override fun onDestroy() {

        super.onDestroy()

        listenerReclamacoes?.remove()

        listenerReclamacoes =
            null
    }
}