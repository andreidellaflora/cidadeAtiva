package br.com.example.cidadeativa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        // ==========================================
        // FIREBASE AUTHENTICATION
        // ==========================================

        auth = FirebaseAuth.getInstance()

        // ==========================================
        // COMPONENTES DA TELA
        // ==========================================

        val edtEmailLogin =
            findViewById<EditText>(
                R.id.edtEmailLogin
            )

        val edtSenhaLogin =
            findViewById<EditText>(
                R.id.edtSenhaLogin
            )

        val btnEntrar =
            findViewById<Button>(
                R.id.btnEntrar
            )

        val btnCriarConta =
            findViewById<Button>(
                R.id.btnCriarConta
            )

        val btnVoltarLogin =
            findViewById<Button>(
                R.id.btnVoltarLogin
            )

        // ==========================================
        // BOTÃO ENTRAR
        // ==========================================

        btnEntrar.setOnClickListener {

            val email =
                edtEmailLogin.text
                    .toString()
                    .trim()

            val senha =
                edtSenhaLogin.text
                    .toString()

            // ======================================
            // VALIDAR E-MAIL
            // ======================================

            if (email.isEmpty()) {

                edtEmailLogin.error =
                    "Digite seu e-mail"

                edtEmailLogin.requestFocus()

                return@setOnClickListener
            }

            if (
                !android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            ) {

                edtEmailLogin.error =
                    "Digite um e-mail válido"

                edtEmailLogin.requestFocus()

                return@setOnClickListener
            }

            // ======================================
            // VALIDAR SENHA
            // ======================================

            if (senha.isEmpty()) {

                edtSenhaLogin.error =
                    "Digite sua senha"

                edtSenhaLogin.requestFocus()

                return@setOnClickListener
            }

            // ======================================
            // DESABILITAR BOTÃO
            // ======================================

            btnEntrar.isEnabled =
                false

            btnEntrar.text =
                "ENTRANDO..."

            // ======================================
            // LOGIN NO FIREBASE
            // ======================================

            auth.signInWithEmailAndPassword(
                email,
                senha
            )

                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Login realizado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // ==================================
                    // ABRIR TELA PRINCIPAL
                    // ==================================

                    val intent =
                        Intent(
                            this,
                            MainActivity::class.java
                        )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)

                    finish()
                }

                .addOnFailureListener { erro ->

                    btnEntrar.isEnabled =
                        true

                    btnEntrar.text =
                        "ENTRAR"

                    Toast.makeText(
                        this,
                        traduzirErroLogin(erro),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        // ==========================================
        // BOTÃO CRIAR CONTA
        // ==========================================

        btnCriarConta.setOnClickListener {

            val intent =
                Intent(
                    this,
                    CadastroActivity::class.java
                )

            startActivity(intent)
        }

        // ==========================================
        // BOTÃO VOLTAR
        // ==========================================

        btnVoltarLogin.setOnClickListener {

            finish()
        }
    }

    // ==============================================
    // TRADUZIR ERROS DO FIREBASE
    // ==============================================

    private fun traduzirErroLogin(
        erro: Exception
    ): String {

        val mensagem =
            erro.message ?: ""

        return when {

            mensagem.contains(
                "invalid credential",
                ignoreCase = true
            ) -> {

                "E-mail ou senha incorretos."
            }

            mensagem.contains(
                "password is invalid",
                ignoreCase = true
            ) -> {

                "Senha incorreta."
            }

            mensagem.contains(
                "no user record",
                ignoreCase = true
            ) -> {

                "Usuário não encontrado."
            }

            mensagem.contains(
                "network",
                ignoreCase = true
            ) -> {

                "Verifique sua conexão com a internet."
            }

            else -> {

                "Não foi possível entrar:\n$mensagem"
            }
        }
    }
}