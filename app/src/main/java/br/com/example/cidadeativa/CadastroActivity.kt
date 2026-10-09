package br.com.example.cidadeativa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class CadastroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_cadastro
        )

        // ==========================================
        // FIREBASE
        // ==========================================

        auth = FirebaseAuth.getInstance()

        // ==========================================
        // CAMPOS
        // ==========================================

        val edtNome =
            findViewById<EditText>(
                R.id.edtNome
            )

        val edtEmail =
            findViewById<EditText>(
                R.id.edtEmail
            )

        val edtSenha =
            findViewById<EditText>(
                R.id.edtSenha
            )

        val edtConfirmarSenha =
            findViewById<EditText>(
                R.id.edtConfirmarSenha
            )

        val btnCadastrar =
            findViewById<Button>(
                R.id.btnCadastrar
            )

        val btnVoltar =
            findViewById<Button>(
                R.id.btnVoltar
            )

        // ==========================================
        // CADASTRAR
        // ==========================================

        btnCadastrar.setOnClickListener {

            val nome =
                edtNome.text
                    .toString()
                    .trim()

            val email =
                edtEmail.text
                    .toString()
                    .trim()

            val senha =
                edtSenha.text
                    .toString()

            val confirmarSenha =
                edtConfirmarSenha.text
                    .toString()

            // ======================================
            // VALIDAÇÕES
            // ======================================

            if (nome.isEmpty()) {

                edtNome.error =
                    "Digite seu nome"

                edtNome.requestFocus()

                return@setOnClickListener
            }

            if (email.isEmpty()) {

                edtEmail.error =
                    "Digite seu e-mail"

                edtEmail.requestFocus()

                return@setOnClickListener
            }

            if (senha.isEmpty()) {

                edtSenha.error =
                    "Digite uma senha"

                edtSenha.requestFocus()

                return@setOnClickListener
            }

            if (senha.length < 6) {

                edtSenha.error =
                    "A senha deve ter pelo menos 6 caracteres"

                edtSenha.requestFocus()

                return@setOnClickListener
            }

            if (confirmarSenha.isEmpty()) {

                edtConfirmarSenha.error =
                    "Confirme sua senha"

                edtConfirmarSenha.requestFocus()

                return@setOnClickListener
            }

            if (senha != confirmarSenha) {

                edtConfirmarSenha.error =
                    "As senhas não são iguais"

                edtConfirmarSenha.requestFocus()

                return@setOnClickListener
            }

            // ==========================================
            // CRIAR CONTA NO FIREBASE
            // ==========================================

            btnCadastrar.isEnabled =
                false

            auth.createUserWithEmailAndPassword(
                email,
                senha
            )
                .addOnCompleteListener { tarefa ->

                    if (tarefa.isSuccessful) {

                        val usuario =
                            auth.currentUser

                        if (usuario != null) {

                            // ======================================
                            // SALVAR NOME NO PERFIL DO FIREBASE
                            // ======================================

                            val perfil =
                                UserProfileChangeRequest
                                    .Builder()
                                    .setDisplayName(nome)
                                    .build()

                            usuario
                                .updateProfile(perfil)
                                .addOnCompleteListener {

                                    Toast.makeText(
                                        this,
                                        "Conta criada com sucesso!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // ==================================
                                    // SAI DA CONTA RECÉM-CRIADA
                                    // ==================================

                                    auth.signOut()

                                    // ==================================
                                    // VOLTA PARA O LOGIN
                                    // ==================================

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

                        }

                    } else {

                        btnCadastrar.isEnabled =
                            true

                        Toast.makeText(
                            this,
                            traduzirErro(
                                tarefa.exception?.message
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // ==========================================
        // VOLTAR
        // ==========================================

        btnVoltar.setOnClickListener {

            finish()
        }
    }

    // =================================================
    // TRADUZIR ERROS DO FIREBASE
    // =================================================

    private fun traduzirErro(
        mensagem: String?
    ): String {

        if (mensagem == null) {
            return "Não foi possível criar a conta."
        }

        return when {

            mensagem.contains(
                "email address is already in use",
                ignoreCase = true
            ) -> {

                "Este e-mail já está cadastrado."
            }

            mensagem.contains(
                "badly formatted",
                ignoreCase = true
            ) -> {

                "Digite um e-mail válido."
            }

            mensagem.contains(
                "password is invalid",
                ignoreCase = true
            ) -> {

                "A senha informada é inválida."
            }

            mensagem.contains(
                "network",
                ignoreCase = true
            ) -> {

                "Verifique sua conexão com a internet."
            }

            else -> {

                "Não foi possível criar a conta."
            }
        }
    }
}