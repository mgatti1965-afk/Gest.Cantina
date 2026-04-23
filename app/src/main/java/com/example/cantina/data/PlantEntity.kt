package com.example.cantina.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "impianti")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val tipo: String, // es. "Vigneto", "Vasca", "Botte"
    val descrizione: String = ""
)
