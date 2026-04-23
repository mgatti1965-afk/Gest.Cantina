package com.example.cantina.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tipologia_uva")
data class GrapeTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val denominazione: String
)
