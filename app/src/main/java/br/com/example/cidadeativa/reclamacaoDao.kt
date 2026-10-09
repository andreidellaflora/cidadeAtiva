package br.com.example.cidadeativa

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReclamacaoDao {

    @Insert
    suspend fun inserir(reclamacao: Reclamacao)

    @Query("SELECT * FROM reclamacoes ORDER BY id DESC")
    suspend fun listarTodas(): List<Reclamacao>
}