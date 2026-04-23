package com.example.cantina.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        OperationEntity::class,
        PlantEntity::class,
        StepEntity::class,
        GrapeTypeEntity::class,
        OperationTypeEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun operationDao(): OperationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cantina_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Il popolamento verrà gestito da onOpen per coerenza
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Controllo e ripristino ad OGNI apertura dell'app
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultData(database.operationDao())
                    }
                }
            }

            private suspend fun populateDefaultData(dao: OperationDao) {
                // Recupero i dati esistenti per non sovrascrivere o duplicare
                val existingGrapes = dao.getAllGrapeTypesSync().map { it.denominazione }
                val existingOpTypes = dao.getAllOperationTypesSync().map { it.denominazione }

                // Default Uve - Ripristino solo se mancanti
                listOf("Lambrusco", "Marzemino", "Merlot", "Pinot Grigio").forEach { grape ->
                    if (grape !in existingGrapes) {
                        dao.insertGrapeType(GrapeTypeEntity(denominazione = grape))
                    }
                }

                // Default Operazioni - Ripristino solo se mancanti con i relativi checkbox
                val defaultOps = listOf(
                    OperationTypeEntity(denominazione = "*"),
                    OperationTypeEntity(denominazione = "Aggiunta di", hasAggiuntaDi = true, hasQuantita = true, hasUnMis = true, hasNote = true),
                    OperationTypeEntity(denominazione = "Analisi", hasQuantita = true, hasNote = true, hasFoto = true),
                    OperationTypeEntity(denominazione = "Ferm.Inizio"),
                    OperationTypeEntity(denominazione = "Ferm.Fine"),
                    OperationTypeEntity(denominazione = "Gradazione", hasQuantita = true, hasUnMis = true, hasNote = true),
                    OperationTypeEntity(denominazione = "Note - Foto", hasNote = true, hasFoto = true),
                    OperationTypeEntity(denominazione = "Temperatura", hasQuantita = true, hasUnMis = true, hasNote = true),
                    OperationTypeEntity(denominazione = "Travaso", hasAggiuntaDi = true, hasQuantita = true, hasUnMis = true, hasNote = true),
                    OperationTypeEntity(denominazione = "Vendemmiato", hasQuantita = true, hasUnMis = true, hasNote = true),
                    OperationTypeEntity(denominazione = "Vinificazione", hasAggiuntaDi = true, hasQuantita = true, hasUnMis = true, hasNote = true)
                )

                defaultOps.forEach { opType ->
                    if (opType.denominazione !in existingOpTypes) {
                        dao.insertOperationType(opType)
                    }
                }
            }
        }
    }
}
