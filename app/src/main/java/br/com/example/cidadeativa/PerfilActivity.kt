package br.com.example.cidadeativa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class PerfilActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_perfil
        )

        // ==========================================
        // FIREBASE
        // ==========================================

        auth = FirebaseAuth.getInstance()

        // ==========================================
        // COMPONENTES
        // ==========================================

        val imgPerfil =
            findViewById<ImageView>(
                R.id.imgPerfil
            )

        val txtNome =
            findViewById<TextView>(
                R.id.txtNome
            )

        val txtEmail =
            findViewById<TextView>(
                R.id.txtEmail
            )

        val btnMinhasReclamacoes =
            findViewById<Button>(
                R.id.btnMinhasReclamacoes
            )

        val btnSair =
            findViewById<Button>(
                R.id.btnSair
            )

        val btnVoltar =
            findViewById<Button>(
                R.id.btnVoltar
            )

        // ==========================================
        // USUÁRIO LOGADO
        // ==========================================

        val usuario =
            auth.currentUser

        if (usuario != null) {

            val nome =
                usuario.displayName
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }
                    ?: "Usuário"

            val email =
                usuario.email
                    ?: "E-mail não informado"

            txtNome.text =
                nome

            txtEmail.text =
                email

        } else {

            txtNome.text =
                "Usuário"

            txtEmail.text =
                "E-mail não informado"
        }

        // ==========================================
        // MINHAS RECLAMAÇÕES
        // ==========================================

        btnMinhasReclamacoes.setOnClickListener {

            val intent =
                Intent(
                    this,
                    MinhasReclamacoesActivity::class.java
                )

            startActivity(intent)
        }

        // ==========================================
        // SAIR DA CONTA
        // ==========================================

        btnSair.setOnClickListener {

            auth.signOut()

            val intent =
                Intent(
                    this,
                    LoginActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
        }

        // ==========================================
        // VOLTAR
        // ==========================================

        btnVoltar.setOnClickListener {

            finish()
        }
    }
}