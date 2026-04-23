package com.example.cantina.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passaggi")
data class StepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titolo: String,
    val descrizione: String = ""
)
