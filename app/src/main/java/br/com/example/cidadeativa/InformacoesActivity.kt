package br.com.example.cidadeativa

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class InformacoesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_informacoes
        )

        val btnVoltar =
            findViewById<Button>(
                R.id.btnVoltarInformacoes
            )

        btnVoltar.setOnClickListener {

            finish()
        }
    }
}