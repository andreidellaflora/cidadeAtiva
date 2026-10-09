package br.com.example.cidadeativa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class ReclamacaoActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    private val db =
        FirebaseFirestore.getInstance()

    private val auth =
        FirebaseAuth.getInstance()

    companion object {

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        private const val FOTO_REQUEST_CODE = 2001
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_reclamacao
        )

        // =====================================================
        // SERVIÇO DE LOCALIZAÇÃO
        // =====================================================

        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(
                    this
                )

        // =====================================================
        // COMPONENTES
        // =====================================================

        val spinnerTipo =
            findViewById<Spinner>(
                R.id.spinnerTipo
            )

        val edtDescricao =
            findViewById<EditText>(
                R.id.edtDescricao
            )

        val edtCep =
            findViewById<EditText>(
                R.id.edtCep
            )

        val btnBuscarCep =
            findViewById<Button>(
                R.id.btnBuscarCep
            )

        val txtEndereco =
            findViewById<TextView>(
                R.id.txtEndereco
            )

        val btnFoto =
            findViewById<Button>(
                R.id.btnFoto
            )

        val imgFoto =
            findViewById<ImageView>(
                R.id.imgFoto
            )

        val btnLocalizacao =
            findViewById<Button>(
                R.id.btnLocalizacao
            )

        val btnEnderecoManual =
            findViewById<Button>(
                R.id.btnEnderecoManual
            )

        val btnEnviar =
            findViewById<Button>(
                R.id.btnEnviar
            )

        // =====================================================
        // TIPOS DE PROBLEMA
        // =====================================================

        val tipos = arrayOf(

            "Buraco na rua",

            "Iluminação pública",

            "Lixo acumulado",

            "Calçada danificada",

            "Problema de trânsito",

            "Água ou esgoto",

            "Árvore ou vegetação",

            "Outro"
        )

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                tipos
            )

        adapter.setDropDownViewResource(
            android.R.layout
                .simple_spinner_dropdown_item
        )

        spinnerTipo.adapter =
            adapter

        // =====================================================
        // BUSCAR CEP
        // =====================================================

        btnBuscarCep.setOnClickListener {

            val cep =
                edtCep.text
                    .toString()
                    .replace("-", "")
                    .trim()

            if (cep.length != 8) {

                Toast.makeText(
                    this,
                    "Digite um CEP válido com 8 números.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            buscarCep(
                cep,
                txtEndereco
            )
        }

        // =====================================================
        // ADICIONAR FOTO
        // =====================================================

        btnFoto.setOnClickListener {

            val intent =
                Intent(
                    Intent.ACTION_PICK
                )

            intent.type =
                "image/*"

            startActivityForResult(
                intent,
                FOTO_REQUEST_CODE
            )
        }

        // =====================================================
        // LOCALIZAÇÃO
        // =====================================================

        btnLocalizacao.setOnClickListener {

            obterLocalizacao(
                txtEndereco
            )
        }

        // =====================================================
        // ENDEREÇO MANUAL
        // =====================================================

        btnEnderecoManual.setOnClickListener {

            mostrarEnderecoManual(
                txtEndereco
            )
        }

        // =====================================================
        // ENVIAR RECLAMAÇÃO
        // =====================================================

        btnEnviar.setOnClickListener {

            val usuario =
                auth.currentUser

            // -------------------------------------------------
            // VERIFICA LOGIN
            // -------------------------------------------------

            if (usuario == null) {

                Toast.makeText(
                    this,
                    "Você precisa estar logado para fazer uma reclamação.",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            val tipo =
                spinnerTipo
                    .selectedItem
                    .toString()

            val descricao =
                edtDescricao.text
                    .toString()
                    .trim()

            val endereco =
                txtEndereco.text
                    .toString()
                    .trim()

            val cep =
                edtCep.text
                    .toString()
                    .trim()

            // -------------------------------------------------
            // VALIDA DESCRIÇÃO
            // -------------------------------------------------

            if (descricao.isEmpty()) {

                Toast.makeText(
                    this,
                    "Digite uma descrição do problema.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // -------------------------------------------------
            // VALIDA LOCALIZAÇÃO
            // -------------------------------------------------

            if (
                endereco.isEmpty() ||
                endereco ==
                "O endereço aparecerá aqui"
            ) {

                Toast.makeText(
                    this,
                    "Informe a localização do problema.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // -------------------------------------------------
            // DESABILITA BOTÃO
            // -------------------------------------------------

            btnEnviar.isEnabled =
                false

            btnEnviar.text =
                "ENVIANDO..."

            // -------------------------------------------------
            // DADOS DO USUÁRIO
            // -------------------------------------------------

            val usuarioId =
                usuario.uid

            val usuarioNome =
                usuario.displayName
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }
                    ?: "Usuário"

            val usuarioEmail =
                usuario.email ?: ""

            // -------------------------------------------------
            // SALVA A RECLAMAÇÃO
            // -------------------------------------------------

            salvarReclamacao(
                tipo = tipo,
                descricao = descricao,
                endereco = endereco,
                cep = cep,
                usuarioId = usuarioId,
                usuarioNome = usuarioNome,
                usuarioEmail = usuarioEmail,
                btnEnviar = btnEnviar
            )
        }
    }

    // =====================================================
    // ENDEREÇO MANUAL
    // =====================================================

    private fun mostrarEnderecoManual(
        txtEndereco: TextView
    ) {

        val campoEndereco =
            EditText(this)

        campoEndereco.hint =
            "Ex.: Rua das Flores, 123, Centro"

        campoEndereco.setPadding(
            20,
            15,
            20,
            15
        )

        campoEndereco.minLines =
            2

        campoEndereco.maxLines =
            4

        val dialog =
            AlertDialog.Builder(this)

        dialog.setTitle(
            "Informar endereço"
        )

        dialog.setView(
            campoEndereco
        )

        dialog.setNegativeButton(
            "CANCELAR",
            null
        )

        dialog.setPositiveButton(
            "CONFIRMAR"
        ) { _, _ ->

            val endereco =
                campoEndereco.text
                    .toString()
                    .trim()

            if (endereco.isEmpty()) {

                Toast.makeText(
                    this,
                    "Digite um endereço.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                txtEndereco.text =
                    "✏️ Endereço informado\n\n$endereco"
            }
        }

        dialog.show()
    }

    // =====================================================
    // SALVAR RECLAMAÇÃO
    // =====================================================

    private fun salvarReclamacao(

        tipo: String,

        descricao: String,

        endereco: String,

        cep: String,

        usuarioId: String,

        usuarioNome: String,

        usuarioEmail: String,

        btnEnviar: Button

    ) {

        val reclamacao =
            hashMapOf(

                "tipo" to tipo,

                "descricao" to descricao,

                "endereco" to endereco,

                "cep" to cep,

                "status" to "Pendente",

                "data" to System.currentTimeMillis(),

                "usuarioId" to usuarioId,

                "usuarioNome" to usuarioNome,

                "usuarioEmail" to usuarioEmail
            )

        db.collection("reclamacoes")
            .add(reclamacao)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Reclamação enviada com sucesso!",
                    Toast.LENGTH_LONG
                ).show()

                btnEnviar.isEnabled =
                    true

                btnEnviar.text =
                    "ENVIAR RECLAMAÇÃO"

                finish()
            }
            .addOnFailureListener { erro ->

                btnEnviar.isEnabled =
                    true

                btnEnviar.text =
                    "ENVIAR RECLAMAÇÃO"

                Toast.makeText(
                    this,
                    "Erro ao enviar reclamação:\n${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =====================================================
    // RESULTADO DA FOTO
    // =====================================================

    override fun onActivityResult(

        requestCode: Int,

        resultCode: Int,

        data: Intent?

    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode ==
            FOTO_REQUEST_CODE &&
            resultCode ==
            RESULT_OK &&
            data != null
        ) {

            val imagemUri: Uri? =
                data.data

            if (imagemUri != null) {

                val imgFoto =
                    findViewById<ImageView>(
                        R.id.imgFoto
                    )

                imgFoto.setImageURI(
                    imagemUri
                )

                imgFoto.visibility =
                    ImageView.VISIBLE
            }
        }
    }

    // =====================================================
    // OBTER LOCALIZAÇÃO
    // =====================================================

    private fun obterLocalizacao(
        txtEndereco: TextView
    ) {

        val permissaoFine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val permissaoCoarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        if (
            permissaoFine !=
            PackageManager.PERMISSION_GRANTED &&
            permissaoCoarse !=
            PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(

                this,

                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),

                LOCATION_PERMISSION_REQUEST_CODE
            )

            return
        }

        txtEndereco.text =
            "📍 Obtendo localização atual..."

        val locationRequest =
            CurrentLocationRequest.Builder()
                .setPriority(
                    Priority.PRIORITY_HIGH_ACCURACY
                )
                .setMaxUpdateAgeMillis(0)
                .build()

        fusedLocationClient
            .getCurrentLocation(
                locationRequest,
                null
            )
            .addOnSuccessListener { location: Location? ->

                if (location != null) {

                    val latitude =
                        location.latitude

                    val longitude =
                        location.longitude

                    txtEndereco.text =
                        "📍 Localização encontrada\n\n" +
                                "Obtendo endereço..."

                    obterEndereco(
                        latitude,
                        longitude,
                        txtEndereco
                    )

                } else {

                    txtEndereco.text =
                        "Não foi possível obter sua localização."

                    Toast.makeText(
                        this,
                        "Verifique o GPS e a localização do aparelho.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { erro ->

                txtEndereco.text =
                    "Erro ao obter localização."

                Toast.makeText(
                    this,
                    "Erro de localização: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =====================================================
    // COORDENADAS → ENDEREÇO
    // =====================================================

    private fun obterEndereco(

        latitude: Double,

        longitude: Double,

        txtEndereco: TextView

    ) {

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val geocoder =
                    Geocoder(
                        this@ReclamacaoActivity,
                        Locale("pt", "BR")
                    )

                @Suppress("DEPRECATION")
                val addresses =
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    if (
                        !addresses.isNullOrEmpty()
                    ) {

                        val address =
                            addresses[0]

                        val rua =
                            address.thoroughfare ?: ""

                        val numero =
                            address.subThoroughfare ?: ""

                        val bairro =
                            address.subLocality ?: ""

                        val cidade =
                            address.locality ?: ""

                        val estado =
                            address.adminArea ?: ""

                        val cep =
                            address.postalCode ?: ""

                        val pais =
                            address.countryName ?: ""

                        val endereco =
                            StringBuilder()

                        endereco.append(
                            "📍 Localização encontrada\n\n"
                        )

                        if (
                            rua.isNotEmpty()
                        ) {

                            endereco.append(
                                rua
                            )

                            if (
                                numero.isNotEmpty()
                            ) {

                                endereco.append(
                                    ", "
                                )

                                endereco.append(
                                    numero
                                )
                            }

                            endereco.append(
                                "\n"
                            )
                        }

                        if (
                            bairro.isNotEmpty()
                        ) {

                            endereco.append(
                                bairro
                            )

                            endereco.append(
                                "\n"
                            )
                        }

                        if (
                            cidade.isNotEmpty()
                        ) {

                            endereco.append(
                                cidade
                            )

                            if (
                                estado.isNotEmpty()
                            ) {

                                endereco.append(
                                    " - "
                                )

                                endereco.append(
                                    estado
                                )
                            }

                            endereco.append(
                                "\n"
                            )
                        }

                        if (
                            cep.isNotEmpty()
                        ) {

                            endereco.append(
                                "CEP: "
                            )

                            endereco.append(
                                cep
                            )

                            endereco.append(
                                "\n"
                            )
                        }

                        if (
                            pais.isNotEmpty()
                        ) {

                            endereco.append(
                                pais
                            )
                        }

                        txtEndereco.text =
                            endereco.toString()

                    } else {

                        txtEndereco.text =
                            "📍 Localização encontrada\n\n" +
                                    "Não foi possível identificar o endereço."
                    }
                }

            } catch (e: Exception) {

                withContext(
                    Dispatchers.Main
                ) {

                    txtEndereco.text =
                        "📍 Localização encontrada\n\n" +
                                "Não foi possível identificar o endereço."

                    Toast.makeText(
                        this@ReclamacaoActivity,
                        "Erro ao identificar o endereço.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // =====================================================
    // PERMISSÃO DE LOCALIZAÇÃO
    // =====================================================

    override fun onRequestPermissionsResult(

        requestCode: Int,

        permissions: Array<out String>,

        grantResults: IntArray

    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            LOCATION_PERMISSION_REQUEST_CODE
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults.any {
                    it ==
                            PackageManager.PERMISSION_GRANTED
                }
            ) {

                val txtEndereco =
                    findViewById<TextView>(
                        R.id.txtEndereco
                    )

                obterLocalizacao(
                    txtEndereco
                )

            } else {

                Toast.makeText(
                    this,
                    "Permissão de localização negada.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =====================================================
    // BUSCAR CEP - VIACEP
    // =====================================================

    private fun buscarCep(

        cep: String,

        txtEndereco: TextView

    ) {

        txtEndereco.text =
            "🔎 Buscando endereço..."

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            var connection:
                    HttpURLConnection? = null

            try {

                val url =
                    URL(
                        "https://viacep.com.br/ws/$cep/json/"
                    )

                connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    10000

                connection.readTimeout =
                    10000

                val responseCode =
                    connection.responseCode

                if (
                    responseCode ==
                    HttpURLConnection.HTTP_OK
                ) {

                    val resposta =
                        connection
                            .inputStream
                            .bufferedReader()
                            .use {
                                it.readText()
                            }

                    val json =
                        JSONObject(
                            resposta
                        )

                    withContext(
                        Dispatchers.Main
                    ) {

                        if (
                            json.optBoolean(
                                "erro",
                                false
                            )
                        ) {

                            txtEndereco.text =
                                "CEP não encontrado."

                            return@withContext
                        }

                        val logradouro =
                            json.optString(
                                "logradouro"
                            )

                        val complemento =
                            json.optString(
                                "complemento"
                            )

                        val bairro =
                            json.optString(
                                "bairro"
                            )

                        val cidade =
                            json.optString(
                                "localidade"
                            )

                        val uf =
                            json.optString(
                                "uf"
                            )

                        val cepFormatado =
                            json.optString(
                                "cep"
                            )

                        val endereco =
                            StringBuilder()

                        endereco.append(
                            "📍 Endereço encontrado\n\n"
                        )

                        if (
                            logradouro.isNotEmpty()
                        ) {

                            endereco.append(
                                logradouro
                            )

                            endereco.append(
                                "\n"
                            )
                        }

                        if (
                            complemento.isNotEmpty()
                        ) {

                            endereco.append(
                                complemento
                            )

                            endereco.append(
                                "\n"
                            )
                        }

                        if (
                            bairro.isNotEmpty()
                        ) {

                            endereco.append(
                                bairro
                            )

                            endereco.append(
                                "\n"
                            )
                        }

                        if (
                            cidade.isNotEmpty()
                        ) {

                            endereco.append(
                                cidade
                            )

                            if (
                                uf.isNotEmpty()
                            ) {

                                endereco.append(
                                    " - "
                                )

                                endereco.append(
                                    uf
                                )
                            }

                            endereco.append(
                                "\n"
                            )
                        }

                        if (
                            cepFormatado.isNotEmpty()
                        ) {

                            endereco.append(
                                "CEP: "
                            )

                            endereco.append(
                                cepFormatado
                            )
                        }

                        txtEndereco.text =
                            endereco.toString()
                    }

                } else {

                    withContext(
                        Dispatchers.Main
                    ) {

                        txtEndereco.text =
                            "Erro ao consultar o CEP."

                        Toast.makeText(
                            this@ReclamacaoActivity,
                            "Servidor do ViaCEP não respondeu.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {

                withContext(
                    Dispatchers.Main
                ) {

                    txtEndereco.text =
                        "Não foi possível consultar o CEP."

                    Toast.makeText(
                        this@ReclamacaoActivity,
                        "Verifique sua conexão com a internet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } finally {

                connection?.disconnect()
            }
        }
    }
}