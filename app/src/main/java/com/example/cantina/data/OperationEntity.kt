package com.example.cantina.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operazioni")
data class OperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val data: String, // Formato yyyy/MM/dd per facilitare import
    val tipologiaUva: String,
    val operazione: String,
    val vendemmiaAnno: Int,
    
    // Campi versione 2.0 (Gestiti in base alla tipologia operazione)
    val aggiuntaDi: String? = null,
    val quantita: Double? = null,
    val unMis: String? = null,
    val note: String? = null,
    val foto: String? = null
)
