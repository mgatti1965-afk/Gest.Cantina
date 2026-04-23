package com.example.cantina.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tipologia_operazioni")
data class OperationTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val denominazione: String,
    val hasAggiuntaDi: Boolean = false,
    val hasQuantita: Boolean = false,
    val hasUnMis: Boolean = false,
    val hasNote: Boolean = false,
    val hasFoto: Boolean = false
)
