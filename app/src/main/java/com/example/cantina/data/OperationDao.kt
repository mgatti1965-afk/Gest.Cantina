package com.example.cantina.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationDao {
    // Operazioni principali
    @Query("SELECT * FROM operazioni WHERE vendemmiaAnno = :anno ORDER BY data DESC")
    fun getOperationsByYear(anno: Int): Flow<List<OperationEntity>>

    @Query("SELECT DISTINCT vendemmiaAnno FROM operazioni ORDER BY vendemmiaAnno DESC")
    fun getAvailableYears(): Flow<List<Int>>

    @Query("SELECT * FROM operazioni ORDER BY data DESC")
    suspend fun getAllOperationsSync(): List<OperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: OperationEntity): Long

    @Update
    suspend fun updateOperation(operation: OperationEntity): Int

    @Delete
    suspend fun deleteOperation(operation: OperationEntity): Int

    // Configurazione Uva
    @Query("SELECT * FROM tipologia_uva ORDER BY denominazione ASC")
    fun getAllGrapeTypes(): Flow<List<GrapeTypeEntity>>

    @Query("SELECT * FROM tipologia_uva")
    suspend fun getAllGrapeTypesSync(): List<GrapeTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrapeType(grapeType: GrapeTypeEntity): Long

    @Update
    suspend fun updateGrapeType(grapeType: GrapeTypeEntity): Int

    @Delete
    suspend fun deleteGrapeType(grapeType: GrapeTypeEntity): Int

    // Configurazione Operazioni
    @Query("SELECT * FROM tipologia_operazioni ORDER BY denominazione ASC")
    fun getAllOperationTypes(): Flow<List<OperationTypeEntity>>

    @Query("SELECT * FROM tipologia_operazioni")
    suspend fun getAllOperationTypesSync(): List<OperationTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperationType(operationType: OperationTypeEntity): Long

    @Update
    suspend fun updateOperationType(operationType: OperationTypeEntity): Int

    @Delete
    suspend fun deleteOperationType(operationType: OperationTypeEntity): Int

    // Passaggi
    @Query("SELECT * FROM passaggi ORDER BY id ASC")
    fun getAllSteps(): Flow<List<StepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: StepEntity): Long

    @Update
    suspend fun updateStep(step: StepEntity): Int

    @Delete
    suspend fun deleteStep(step: StepEntity): Int
}
