package br.com.example.cidadeativa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.ProgressBar
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
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.security.MessageDigest
import android.util.Base64

class ReclamacaoActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var envioEmAndamento = false
    private var imagemSelecionada: Uri? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val FOTO_REQUEST_CODE = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reclamacao)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        // COMPONENTES

        val spinnerTipo = findViewById<Spinner>(R.id.spinnerTipo)
        val edtDescricao = findViewById<EditText>(R.id.edtDescricao)
        val edtCep = findViewById<EditText>(R.id.edtCep)
        val btnBuscarCep = findViewById<Button>(R.id.btnBuscarCep)
        val txtEndereco = findViewById<TextView>(R.id.txtEndereco)
        val btnFoto = findViewById<Button>(R.id.btnFoto)
        val btnLocalizacao = findViewById<Button>(R.id.btnLocalizacao)
        val btnEnderecoManual = findViewById<Button>(R.id.btnEnderecoManual)
        val btnEnviar = findViewById<Button>(R.id.btnEnviar)
        val progressoEnvio = findViewById<ProgressBar>(R.id.progressoEnvio)

        // BOTÃO VOLTAR

        val btnVoltarReclamacao =
            findViewById<TextView>(R.id.btnVoltarReclamacao)

        btnVoltarReclamacao.setOnClickListener {
            finish()
        }

        // TIPOS DE PROBLEMA

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

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tipos
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerTipo.adapter = adapter

        // BUSCAR CEP

        btnBuscarCep.setOnClickListener {
            val cep = edtCep.text.toString()
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

            buscarCep(cep, txtEndereco)
        }

        // ADICIONAR FOTO

        btnFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }

            startActivityForResult(intent, FOTO_REQUEST_CODE)
        }

        // USAR LOCALIZAÇÃO

        btnLocalizacao.setOnClickListener {
            obterLocalizacao(txtEndereco)
        }

        // DIGITAR ENDEREÇO MANUALMENTE

        btnEnderecoManual.setOnClickListener {
            mostrarEnderecoManual(txtEndereco)
        }

        // ENVIAR RECLAMAÇÃO

        btnEnviar.setOnClickListener {
            if (envioEmAndamento) return@setOnClickListener
            val usuario = auth.currentUser

            if (usuario == null) {
                Toast.makeText(
                    this,
                    "Você precisa estar logado para fazer uma reclamação.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val tipo = spinnerTipo.selectedItem.toString()
            val descricao = edtDescricao.text.toString().trim()
            val endereco = txtEndereco.text.toString().trim()
            val cep = edtCep.text.toString().trim()

            if (descricao.isEmpty()) {
                edtDescricao.error = "Obrigatório: descreva o problema."
                edtDescricao.requestFocus()
                return@setOnClickListener
            }

            if (
                endereco.isEmpty() ||
                endereco == "O endereço aparecerá aqui"
            ) {
                Toast.makeText(this, "Obrigatório: informe o endereço pelo CEP, GPS ou manualmente.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val usuarioId = usuario.uid

            val usuarioNome = usuario.displayName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "Usuário"

            val usuarioEmail = usuario.email ?: ""

            val revisaoView = layoutInflater.inflate(
                R.layout.dialog_revisar_reclamacao,
                null
            )
            revisaoView.findViewById<TextView>(R.id.revisaoTipo).text = "TIPO DO PROBLEMA\n$tipo"
            revisaoView.findViewById<TextView>(R.id.revisaoDescricao).text = "DESCRIÇÃO\n$descricao"
            revisaoView.findViewById<TextView>(R.id.revisaoEndereco).text = "LOCALIZAÇÃO\n$endereco"

            val dialogoRevisao = AlertDialog.Builder(this)
                .setView(revisaoView)
                .setNegativeButton("EDITAR", null)
                .setPositiveButton("CONFIRMAR ENVIO", null)
                .create()

            dialogoRevisao.setOnShowListener {
                dialogoRevisao.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                    setTextColor(android.graphics.Color.rgb(34, 34, 34))
                    backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.rgb(255, 211, 42)
                    )
                    setOnClickListener {
                        dialogoRevisao.dismiss()
                    envioEmAndamento = true
                    btnEnviar.isEnabled = false
                    btnEnviar.text = "ENVIANDO..."
                    progressoEnvio.visibility = ProgressBar.VISIBLE
                    salvarReclamacao(
                        tipo = tipo,
                        descricao = descricao,
                        endereco = endereco,
                        cep = cep,
                        usuarioId = usuarioId,
                        usuarioNome = usuarioNome,
                        usuarioEmail = usuarioEmail,
                        chaveEnvio = gerarChaveEnvio(usuarioId, tipo, descricao, endereco),
                        btnEnviar = btnEnviar,
                        progressoEnvio = progressoEnvio,
                        imagemUri = imagemSelecionada,
                        onFinish = { envioEmAndamento = false }
                    )
                    }
                }
                dialogoRevisao.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(android.graphics.Color.rgb(73, 69, 61))
            }
            dialogoRevisao.show()
        }
    }

    // ENDEREÇO MANUAL

    private fun mostrarEnderecoManual(txtEndereco: TextView) {
        val campoEndereco = EditText(this)

        campoEndereco.hint = "Ex.: Rua das Flores, 123, Centro"
        campoEndereco.setPadding(20, 15, 20, 15)
        campoEndereco.minLines = 2
        campoEndereco.maxLines = 4

        AlertDialog.Builder(this)
            .setTitle("Informar endereço")
            .setView(campoEndereco)
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("CONFIRMAR") { _, _ ->
                val endereco = campoEndereco.text.toString().trim()

                if (endereco.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Digite um endereço.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    txtEndereco.text =
                        "Endereço informado\n\n$endereco"
                }
            }
            .show()
    }

    // SALVAR RECLAMAÇÃO NO FIRESTORE

    private fun salvarReclamacao(
        tipo: String,
        descricao: String,
        endereco: String,
        cep: String,
        usuarioId: String,
        usuarioNome: String,
        usuarioEmail: String,
        chaveEnvio: String,
        btnEnviar: Button,
        progressoEnvio: ProgressBar,
        imagemUri: Uri?,
        onFinish: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fotoBase64 = imagemUri?.let { converterImagemParaBase64(it) }
                withContext(Dispatchers.Main) {
                    val reclamacao = hashMapOf<String, Any>(
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
                    if (fotoBase64 != null) reclamacao["fotoBase64"] = fotoBase64

                    db.collection("reclamacoes")
                        .document(chaveEnvio)
                        .set(reclamacao)
                        .addOnSuccessListener {
                            Toast.makeText(this@ReclamacaoActivity, "Reclamação enviada com sucesso!", Toast.LENGTH_LONG).show()
                            finish()
                        }
                        .addOnFailureListener { erro ->
                            Log.e("ReclamacaoActivity", "Falha ao gravar reclamação no Firestore", erro)
                            restaurarBotaoEnvio(btnEnviar, progressoEnvio, onFinish)
                            val mensagem = when {
                                erro.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                                    "Firebase bloqueou o envio. Verifique as regras do Firestore para a coleção reclamacoes."
                                erro.message?.contains("UNAVAILABLE", ignoreCase = true) == true ->
                                    "Sem conexão com o Firebase. Verifique sua internet e tente novamente."
                                else -> "Falha ao enviar: ${erro.localizedMessage ?: erro.javaClass.simpleName}"
                            }
                            Toast.makeText(this@ReclamacaoActivity, mensagem, Toast.LENGTH_LONG).show()
                        }
                }
            } catch (erro: Exception) {
                Log.e("ReclamacaoActivity", "Falha ao preparar imagem para salvar", erro)
                withContext(Dispatchers.Main) {
                    restaurarBotaoEnvio(btnEnviar, progressoEnvio, onFinish)
                    Toast.makeText(this@ReclamacaoActivity, "Não foi possível processar a imagem. Escolha uma foto menor e tente novamente.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun restaurarBotaoEnvio(
        btnEnviar: Button,
        progressoEnvio: ProgressBar,
        onFinish: () -> Unit
    ) {
        onFinish()
        btnEnviar.isEnabled = true
        btnEnviar.text = "ENVIAR RECLAMAÇÃO"
        progressoEnvio.visibility = ProgressBar.GONE
    }

    private fun converterImagemParaBase64(uri: Uri): String {
        val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opcoes) }
        opcoes.inJustDecodeBounds = false
        opcoes.inSampleSize = 1
        while (opcoes.outWidth / opcoes.inSampleSize > 1280 || opcoes.outHeight / opcoes.inSampleSize > 1280) {
            opcoes.inSampleSize *= 2
        }
        var bitmap = contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, opcoes)
        } ?: throw IllegalArgumentException("Não foi possível ler a imagem")

        try {
            var qualidade = 80
            var bytes: ByteArray
            do {
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, qualidade, stream)
                bytes = stream.toByteArray()
                if (bytes.size <= 500 * 1024) break
                if (qualidade > 45) qualidade -= 10
                else {
                    val reduzida = Bitmap.createScaledBitmap(bitmap, bitmap.width * 3 / 4, bitmap.height * 3 / 4, true)
                    if (reduzida !== bitmap) bitmap.recycle()
                    bitmap = reduzida
                    qualidade = 70
                }
            } while (bitmap.width > 240 && bitmap.height > 240)

            if (bytes.size > 700 * 1024) {
                throw IllegalArgumentException("Imagem maior que o limite permitido")
            }
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally {
            bitmap.recycle()
        }
    }

    private fun gerarChaveEnvio(
        usuarioId: String,
        tipo: String,
        descricao: String,
        endereco: String
    ): String {
        val conteudo = listOf(usuarioId, tipo, descricao, endereco)
            .joinToString("|") { it.trim().lowercase(Locale.ROOT) }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(conteudo.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { byte -> "%02x".format(byte) }
    }

    // RESULTADO DA SELEÇÃO DA FOTO

    @Deprecated("Usa a API antiga de seleção de imagens")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode == FOTO_REQUEST_CODE &&
            resultCode == RESULT_OK &&
            data != null
        ) {
            val imagemUri: Uri? = data.data

            if (imagemUri != null) {
                imagemSelecionada = imagemUri
                val imgFoto = findViewById<ImageView>(R.id.imgFoto)

                imgFoto.setImageURI(imagemUri)
                imgFoto.visibility = ImageView.VISIBLE
            }
        }
    }

    // OBTER LOCALIZAÇÃO ATUAL

    private fun obterLocalizacao(txtEndereco: TextView) {
        val permissaoFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val permissaoCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (
            permissaoFine != PackageManager.PERMISSION_GRANTED &&
            permissaoCoarse != PackageManager.PERMISSION_GRANTED
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

        txtEndereco.text = "Obtendo localização atual..."

        val locationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()

        fusedLocationClient
            .getCurrentLocation(locationRequest, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    txtEndereco.text =
                        "Localização encontrada\n\nObtendo endereço..."

                    obterEndereco(
                        location.latitude,
                        location.longitude,
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
                txtEndereco.text = "Erro ao obter localização."

                Toast.makeText(
                    this,
                    "Erro de localização: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // CONVERTER COORDENADAS EM ENDEREÇO

    private fun obterEndereco(
        latitude: Double,
        longitude: Double,
        txtEndereco: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(
                    this@ReclamacaoActivity,
                    Locale("pt", "BR")
                )

                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1
                )

                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        val address: Address = addresses[0]
                        val partes = mutableListOf<String>()

                        val rua = address.thoroughfare.orEmpty()
                        val numero = address.subThoroughfare.orEmpty()
                        val bairro = address.subLocality.orEmpty()
                        val cidade = address.locality.orEmpty()
                        val estado = address.adminArea.orEmpty()
                        val cep = address.postalCode.orEmpty()
                        val pais = address.countryName.orEmpty()

                        if (rua.isNotEmpty()) {
                            partes.add(
                                if (numero.isNotEmpty()) {
                                    "$rua, $numero"
                                } else {
                                    rua
                                }
                            )
                        }

                        if (bairro.isNotEmpty()) {
                            partes.add(bairro)
                        }

                        val municipio = listOf(cidade, estado)
                            .filter { it.isNotEmpty() }
                            .joinToString(" - ")

                        if (municipio.isNotEmpty()) {
                            partes.add(municipio)
                        }

                        if (cep.isNotEmpty()) {
                            partes.add("CEP: $cep")
                        }

                        if (pais.isNotEmpty()) {
                            partes.add(pais)
                        }

                        txtEndereco.text =
                            "Localização encontrada\n\n" +
                                    partes.joinToString("\n").ifEmpty {
                                        "Não foi possível identificar o endereço."
                                    }
                    } else {
                        txtEndereco.text =
                            "Localização encontrada\n\n" +
                                    "Não foi possível identificar o endereço."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    txtEndereco.text =
                        "Localização encontrada\n\n" +
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

    // PERMISSÃO DE LOCALIZAÇÃO

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

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                obterLocalizacao(
                    findViewById(R.id.txtEndereco)
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

    // BUSCAR CEP PELO VIACEP

    private fun buscarCep(
        cep: String,
        txtEndereco: TextView
    ) {
        txtEndereco.text = "Buscando endereço..."

        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null

            try {
                val url = URL("https://viacep.com.br/ws/$cep/json/")

                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val resposta = connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                    val json = JSONObject(resposta)

                    withContext(Dispatchers.Main) {
                        if (json.optBoolean("erro", false)) {
                            txtEndereco.text = "CEP não encontrado."
                            return@withContext
                        }

                        val logradouro = json.optString("logradouro")
                        val complemento = json.optString("complemento")
                        val bairro = json.optString("bairro")
                        val cidade = json.optString("localidade")
                        val uf = json.optString("uf")
                        val cepFormatado = json.optString("cep")

                        val municipio = listOf(cidade, uf)
                            .filter { it.isNotEmpty() }
                            .joinToString(" - ")

                        val partes = listOf(
                            logradouro,
                            complemento,
                            bairro,
                            municipio,
                            if (cepFormatado.isNotEmpty()) {
                                "CEP: $cepFormatado"
                            } else {
                                ""
                            }
                        ).filter { it.isNotEmpty() }

                        txtEndereco.text =
                            "Endereço encontrado\n\n" +
                                    partes.joinToString("\n")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        txtEndereco.text = "Erro ao consultar o CEP."

                        Toast.makeText(
                            this@ReclamacaoActivity,
                            "Servidor do ViaCEP não respondeu.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
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
