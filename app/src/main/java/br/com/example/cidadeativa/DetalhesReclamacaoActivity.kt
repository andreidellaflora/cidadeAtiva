package br.com.example.cidadeativa

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class DetalhesReclamacaoActivity : AppCompatActivity() {

    private val db =
        FirebaseFirestore.getInstance()

    private var documentoId = ""

    private lateinit var txtStatus: TextView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_detalhes_reclamacao
        )

        // ====================================================
        // RECEBE O ID DA RECLAMAÇÃO
        // ====================================================

        documentoId =
            intent.getStringExtra("documentoId")
                ?: ""

        // ====================================================
        // COMPONENTES
        // ====================================================

        val txtTipo =
            findViewById<TextView>(
                R.id.txtTipo
            )

        val txtDescricao =
            findViewById<TextView>(
                R.id.txtDescricao
            )

        val txtEndereco =
            findViewById<TextView>(
                R.id.txtEndereco
            )

        val txtCep =
            findViewById<TextView>(
                R.id.txtCep
            )

        txtStatus =
            findViewById(
                R.id.txtStatus
            )

        val txtData =
            findViewById<TextView>(
                R.id.txtData
            )

        val txtUsuario =
            findViewById<TextView>(
                R.id.txtUsuario
            )

        val btnMapa =
            findViewById<Button>(
                R.id.btnMapa
            )

        val btnStatus =
            findViewById<Button>(
                R.id.btnStatus
            )

        val btnExcluir =
            findViewById<Button>(
                R.id.btnExcluir
            )

        val btnVoltar =
            findViewById<Button>(
                R.id.btnVoltar
            )

        // ====================================================
        // VERIFICA ID
        // ====================================================

        if (documentoId.isBlank()) {

            Toast.makeText(
                this,
                "Não foi possível identificar a reclamação.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }

        // ====================================================
        // BUSCA A RECLAMAÇÃO NO FIREBASE
        // ====================================================

        db.collection("reclamacoes")
            .document(documentoId)
            .get()
            .addOnSuccessListener { documento ->

                if (!documento.exists()) {

                    Toast.makeText(
                        this,
                        "Reclamação não encontrada.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()

                    return@addOnSuccessListener
                }

                // ====================================================
                // DADOS DA RECLAMAÇÃO
                // ====================================================

                val tipo =
                    documento
                        .getString("tipo")
                        ?: "Não informado"

                val descricao =
                    documento
                        .getString("descricao")
                        ?: "Não informado"

                val endereco =
                    documento
                        .getString("endereco")
                        ?: "Não informado"

                val cep =
                    documento
                        .getString("cep")
                        ?: ""

                val status =
                    documento
                        .getString("status")
                        ?: "Pendente"

                // ====================================================
                // USUÁRIO
                // ====================================================

                val usuarioNome =
                    documento
                        .getString("usuarioNome")
                        ?: "Usuário não identificado"

                val usuarioEmail =
                    documento
                        .getString("usuarioEmail")
                        ?: ""

                // ====================================================
                // DATA
                // ====================================================

                val data =
                    documento.get("data")

                val dataFormatada =
                    formatarData(data)

                // ====================================================
                // MOSTRA OS DADOS
                // ====================================================

                txtTipo.text =
                    tipo

                txtDescricao.text =
                    descricao

                txtEndereco.text =
                    endereco

                txtCep.text =
                    if (cep.isBlank()) {

                        "Não informado"

                    } else {

                        cep
                    }

                txtStatus.text =
                    status

                txtData.text =
                    if (dataFormatada.isBlank()) {

                        "Não informada"

                    } else {

                        dataFormatada
                    }

                // ====================================================
                // IDENTIFICAÇÃO DO USUÁRIO
                // ====================================================

                txtUsuario.text =
                    if (usuarioEmail.isBlank()) {

                        "👤 Registrado por: $usuarioNome"

                    } else {

                        "👤 Registrado por: $usuarioNome\n" +
                                "✉️ $usuarioEmail"
                    }

                configurarStatus(
                    status
                )

                // ====================================================
                // GOOGLE MAPS
                // ====================================================

                btnMapa.setOnClickListener {

                    if (
                        endereco.isBlank() ||
                        endereco == "Não informado"
                    ) {

                        Toast.makeText(
                            this,
                            "Esta reclamação não possui endereço.",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    abrirGoogleMaps(
                        endereco
                    )
                }

                // ====================================================
                // ALTERAR STATUS
                // ====================================================

                btnStatus.setOnClickListener {

                    mostrarOpcoesStatus()
                }

                // ====================================================
                // EXCLUIR RECLAMAÇÃO
                // ====================================================

                btnExcluir.setOnClickListener {

                    confirmarExclusao()
                }
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    this,
                    "Erro ao carregar reclamação:\n${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        // ====================================================
        // VOLTAR
        // ====================================================

        btnVoltar.setOnClickListener {

            finish()
        }
    }

    // ========================================================
    // CONFIRMAR EXCLUSÃO
    // ========================================================

    private fun confirmarExclusao() {

        val dialog =
            AlertDialog.Builder(this)

        dialog.setTitle(
            "Excluir reclamação?"
        )

        dialog.setMessage(
            "Esta ação não poderá ser desfeita."
        )

        dialog.setNegativeButton(
            "CANCELAR",
            null
        )

        dialog.setPositiveButton(
            "EXCLUIR"
        ) { _, _ ->

            excluirReclamacao()
        }

        dialog.show()
    }

    // ========================================================
    // EXCLUIR RECLAMAÇÃO DO FIRESTORE
    // ========================================================

    private fun excluirReclamacao() {

        if (documentoId.isBlank()) {

            Toast.makeText(
                this,
                "Não foi possível identificar a reclamação.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        db.collection("reclamacoes")
            .document(documentoId)
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Reclamação excluída com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    this,
                    "Erro ao excluir reclamação:\n${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ========================================================
    // FORMATAR DATA
    // ========================================================

    private fun formatarData(
        valor: Any?
    ): String {

        if (valor == null) {
            return ""
        }

        return try {

            when (valor) {

                is Timestamp -> {

                    val formato =
                        SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            Locale.getDefault()
                        )

                    formato.format(
                        valor.toDate()
                    )
                }

                is java.util.Date -> {

                    val formato =
                        SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            Locale.getDefault()
                        )

                    formato.format(
                        valor
                    )
                }

                is Number -> {

                    val formato =
                        SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            Locale.getDefault()
                        )

                    formato.format(
                        java.util.Date(
                            valor.toLong()
                        )
                    )
                }

                else -> {

                    valor.toString()
                }
            }

        } catch (e: Exception) {

            ""
        }
    }

    // ========================================================
    // MOSTRAR OPÇÕES DE STATUS
    // ========================================================

    private fun mostrarOpcoesStatus() {

        val opcoes =
            arrayOf(
                "Pendente",
                "Em análise",
                "Resolvido"
            )

        val builder =
            AlertDialog.Builder(this)

        builder.setTitle(
            "Alterar status"
        )

        builder.setItems(
            opcoes
        ) { _, escolha ->

            val novoStatus =
                opcoes[escolha]

            atualizarStatus(
                novoStatus
            )
        }

        builder.setNegativeButton(
            "Cancelar",
            null
        )

        builder.show()
    }

    // ========================================================
    // ATUALIZAR STATUS NO FIRESTORE
    // ========================================================

    private fun atualizarStatus(
        novoStatus: String
    ) {

        if (documentoId.isBlank()) {

            Toast.makeText(
                this,
                "Não foi possível identificar a reclamação.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val dados =
            hashMapOf<String, Any>(
                "status" to novoStatus
            )

        db.collection("reclamacoes")
            .document(documentoId)
            .update(dados)
            .addOnSuccessListener {

                txtStatus.text =
                    novoStatus

                configurarStatus(
                    novoStatus
                )

                Toast.makeText(
                    this,
                    "Status atualizado com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { erro ->

                Toast.makeText(
                    this,
                    "Erro ao atualizar status:\n${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ========================================================
    // CONFIGURAÇÃO VISUAL DO STATUS
    // ========================================================

    private fun configurarStatus(
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

        txtStatus.gravity =
            Gravity.CENTER

        txtStatus.setPadding(
            15,
            10,
            15,
            10
        )
    }

    // ========================================================
    // ABRIR GOOGLE MAPS
    // ========================================================

    private fun abrirGoogleMaps(
        endereco: String
    ) {

        val enderecoCodificado =
            Uri.encode(
                endereco
            )

        val uriMaps =
            Uri.parse(
                "geo:0,0?q=$enderecoCodificado"
            )

        val intentMaps =
            Intent(
                Intent.ACTION_VIEW,
                uriMaps
            )

        intentMaps.setPackage(
            "com.google.android.apps.maps"
        )

        try {

            startActivity(
                intentMaps
            )

        } catch (e: Exception) {

            val url =
                "https://www.google.com/maps/search/?api=1&query=$enderecoCodificado"

            val intentNavegador =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )

            try {

                startActivity(
                    intentNavegador
                )

            } catch (erro: Exception) {

                Toast.makeText(
                    this,
                    "Não foi possível abrir o mapa.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}