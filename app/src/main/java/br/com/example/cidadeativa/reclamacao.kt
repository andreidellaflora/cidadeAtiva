package br.com.example.cidadeativa

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reclamacoes")
data class Reclamacao(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val tipo: String,

    val descricao: String,

    val endereco: String,

    val cep: String,

    val data: String
)