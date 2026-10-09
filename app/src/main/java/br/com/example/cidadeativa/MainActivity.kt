package br.com.example.cidadeativa

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Verifica se o usuário está logado
        if (auth.currentUser == null) {

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )

            finish()

            return
        }

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_main
        )

        // Ajusta a tela para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // ==================================================
        // USUÁRIO LOGADO
        // ==================================================

        val usuario = auth.currentUser

        val nome =
            usuario
                ?.displayName
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?: "usuário"

        // ==================================================
        // SAUDAÇÃO
        // ==================================================

        val txtSaudacao =
            findViewById<TextView>(
                R.id.txtSaudacao
            )

        txtSaudacao.text =
            "Olá, $nome! 👋"

        // ==================================================
        // CARDS
        // ==================================================

        val cardReclamacao =
            findViewById<LinearLayout>(
                R.id.cardReclamacao
            )

        val cardProblemas =
            findViewById<LinearLayout>(
                R.id.cardProblemas
            )

        val cardInformacoes =
            findViewById<LinearLayout>(
                R.id.cardInformacoes
            )

        val cardPerfil =
            findViewById<LinearLayout>(
                R.id.cardPerfil
            )

        // ==================================================
        // RECLAMAÇÃO
        // ==================================================

        cardReclamacao.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ReclamacaoActivity::class.java
                )
            )
        }

        // ==================================================
        // PROBLEMAS DA REGIÃO
        // ==================================================

        cardProblemas.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ProblemasDaRegiaoActivity::class.java
                )
            )
        }

        // ==================================================
        // INFORMAÇÕES
        // ==================================================

        cardInformacoes.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    InformacoesActivity::class.java
                )
            )
        }

        // ==================================================
        // PERFIL
        // ==================================================

        cardPerfil.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PerfilActivity::class.java
                )
            )
        }
    }
}