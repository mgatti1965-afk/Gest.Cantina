package com.example.cantina.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cantina.data.OperationDao
import com.example.cantina.data.OperationEntity
import com.example.cantina.data.GrapeTypeEntity
import com.example.cantina.data.OperationTypeEntity
import com.example.cantina.data.StepEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlinx.coroutines.flow.combine
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class SortOrder {
    DATE_DESC, DATE_GRAPE, GRAPE_DATE
}

@OptIn(ExperimentalCoroutinesApi::class)
class CantinaViewModel(private val operationDao: OperationDao) : ViewModel() {

    // --- CONFIGURAZIONE IMPIANTO ---
    val grapeTypes: StateFlow<List<GrapeTypeEntity>> = operationDao.getAllGrapeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val operationTypes: StateFlow<List<OperationTypeEntity>> = operationDao.getAllOperationTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val steps: StateFlow<List<StepEntity>> = operationDao.getAllSteps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateGrapeType(grapeType: GrapeTypeEntity) {
        viewModelScope.launch { operationDao.updateGrapeType(grapeType) }
    }

    fun addGrapeType(denominazione: String) {
        viewModelScope.launch { operationDao.insertGrapeType(GrapeTypeEntity(denominazione = denominazione)) }
    }

    fun deleteGrapeType(grapeType: GrapeTypeEntity) {
        viewModelScope.launch { operationDao.deleteGrapeType(grapeType) }
    }

    fun updateOperationType(operationType: OperationTypeEntity) {
        viewModelScope.launch { operationDao.updateOperationType(operationType) }
    }

    fun addOperationType(denominazione: String, add: Boolean = false, qta: Boolean = false, um: Boolean = false, nt: Boolean = false, ft: Boolean = false) {
        viewModelScope.launch { 
            operationDao.insertOperationType(OperationTypeEntity(
                denominazione = denominazione,
                hasAggiuntaDi = add,
                hasQuantita = qta,
                hasUnMis = um,
                hasNote = nt,
                hasFoto = ft
            ))
        }
    }

    fun deleteOperationType(operationType: OperationTypeEntity) {
        viewModelScope.launch { operationDao.deleteOperationType(operationType) }
    }

    // --- LOGICA ANNO E TAB ---
    private fun getDefaultHarvestYear(): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-indexed, July is 6
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        return if (currentMonth < 6) {
            currentYear - 1
        } else {
            currentYear
        }
    }

    private val _selectedYear = MutableStateFlow(getDefaultHarvestYear())
    val selectedYear: StateFlow<Int> = _selectedYear

    val availableYears: StateFlow<List<Int>> = operationDao.getAvailableYears()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(getDefaultHarvestYear()))

    // Current navigation tab
    private val _activeTab = MutableStateFlow("Operazione")
    val activeTab: StateFlow<String> = _activeTab

    // --- GESTIONE FILE EXPORT ---
    private val _exportedFiles = MutableStateFlow<List<File>>(emptyList())
    val exportedFiles = _exportedFiles.asStateFlow()

    fun loadExportedFiles(exportsDir: File) {
        if (!exportsDir.exists()) exportsDir.mkdirs()
        val files = exportsDir.listFiles { file -> file.extension == "csv" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        _exportedFiles.value = files
    }

    fun deleteExportedFile(file: File) {
        if (file.exists()) {
            file.delete()
            _exportedFiles.value = _exportedFiles.value.filter { it != file }
        }
    }

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    // Operations flow based on selected year and sort order
    val operations: StateFlow<List<OperationEntity>> = combine(
        _selectedYear.flatMapLatest { year -> operationDao.getOperationsByYear(year) },
        _sortOrder
    ) { ops, order ->
        when (order) {
            SortOrder.DATE_DESC -> ops.sortedByDescending { it.data }
            SortOrder.DATE_GRAPE -> ops.sortedWith(compareByDescending<OperationEntity> { it.data }.thenBy { it.tipologiaUva })
            SortOrder.GRAPE_DATE -> ops.sortedWith(compareBy<OperationEntity> { it.tipologiaUva }.thenByDescending { it.data })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setYear(year: Int) {
        _selectedYear.value = year
    }

    fun setTab(tab: String) {
        _activeTab.value = tab
    }

    fun addOperation(
        data: String,
        uva: String,
        operazione: String,
        aggiuntaDi: String? = null,
        quantita: Double? = null,
        unMis: String? = null,
        note: String? = null,
        foto: String? = null
    ) {
        viewModelScope.launch {
            operationDao.insertOperation(
                OperationEntity(
                    data = data,
                    tipologiaUva = uva,
                    operazione = operazione,
                    vendemmiaAnno = _selectedYear.value,
                    aggiuntaDi = aggiuntaDi,
                    quantita = quantita,
                    unMis = unMis,
                    note = note,
                    foto = foto
                )
            )
        }
    }

    fun updateOperation(operation: OperationEntity) {
        viewModelScope.launch {
            operationDao.updateOperation(operation)
        }
    }

    fun deleteOperation(operation: OperationEntity) {
        viewModelScope.launch {
            operationDao.deleteOperation(operation)
        }
    }

    // --- LOGICA PASSAGGI ---
    fun addStep(titolo: String, descrizione: String) {
        viewModelScope.launch {
            operationDao.insertStep(StepEntity(titolo = titolo, descrizione = descrizione))
        }
    }

    fun updateStep(step: StepEntity) {
        viewModelScope.launch {
            operationDao.updateStep(step)
        }
    }

    fun deleteStep(step: StepEntity) {
        viewModelScope.launch {
            operationDao.deleteStep(step)
        }
    }

    // --- IMPORT / EXPORT CSV ---
    
    // Funzione per esportare tutte le operazioni in formato CSV
    suspend fun exportAllOperationsToCsv(exportsDir: File): File? {
        if (!exportsDir.exists()) exportsDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "export_cantina_$timestamp.csv"
        val file = File(exportsDir, fileName)

        // Recuperiamo tutte le operazioni (senza filtro anno per il backup completo)
        val allOps = operationDao.getAllOperationsSync()
        
        val header = "id,vendemmiaAnno,data,tipologiaUva,operazione,aggiuntaDi,quantita,unMis,note,foto"
        val csv = StringBuilder(header).append("\n")
        
        allOps.forEach { op ->
            csv.append("${op.id},")
                .append("${op.vendemmiaAnno},")
                .append("${op.data},")
                .append("${escapeCsv(op.tipologiaUva)},")
                .append("${escapeCsv(op.operazione)},")
                .append("${escapeCsv(op.aggiuntaDi ?: "")},")
                .append("${op.quantita ?: ""},")
                .append("${escapeCsv(op.unMis ?: "")},")
                .append("${escapeCsv(op.note ?: "")},")
                .append("${escapeCsv(op.foto ?: "")}")
                .append("\n")
        }
        
        return try {
            file.writeText(csv.toString())
            loadExportedFiles(exportsDir)
            file
        } catch (e: Exception) {
            null
        }
    }

    // Funzione per importare operazioni da una stringa CSV
    suspend fun importOperationsFromCsv(csvData: String): Int {
        var importedCount = 0
        val lines = csvData.lines()
        if (lines.isEmpty()) return 0
        
        // Carico i nomi esistenti per evitare duplicati e gestire l'auto-popolamento
        val existingGrapes = operationDao.getAllGrapeTypesSync().map { it.denominazione }.toMutableSet()
        val existingOpTypes = operationDao.getAllOperationTypesSync().map { it.denominazione }.toMutableSet()

        // Salta l'header
        val dataLines = lines.drop(1).filter { it.isNotBlank() }
        
        dataLines.forEach { line ->
            // Rilevamento automatico del delimitatore (virgola o punto e virgola)
            val delimiter = if (line.contains(";")) ";" else ","
            val parts = line.split(delimiter)
            
            if (parts.size >= 10) {
                try {
                    val id = parts[0].toLongOrNull() ?: 0L
                    val anno = parts[1].toIntOrNull() ?: _selectedYear.value
                    val data = parts[2]
                    val uva = unescapeCsv(parts[3])
                    val operazione = unescapeCsv(parts[4])

                    // 1. Se l'uva non esiste, la aggiungo automaticamente
                    if (uva !in existingGrapes && uva.isNotBlank()) {
                        operationDao.insertGrapeType(GrapeTypeEntity(denominazione = uva))
                        existingGrapes.add(uva)
                    }

                    // 2. Se l'operazione non esiste, la aggiungo con tutti i campi attivi
                    if (operazione !in existingOpTypes && operazione.isNotBlank()) {
                        operationDao.insertOperationType(OperationTypeEntity(
                            denominazione = operazione,
                            hasAggiuntaDi = true,
                            hasQuantita = true,
                            hasUnMis = true,
                            hasNote = true,
                            hasFoto = true
                        ))
                        existingOpTypes.add(operazione)
                    }

                    val op = OperationEntity(
                        id = id,
                        vendemmiaAnno = anno,
                        data = data,
                        tipologiaUva = uva,
                        operazione = operazione,
                        aggiuntaDi = unescapeCsv(parts[5]).takeIf { it.isNotBlank() },
                        quantita = parts[6].toDoubleOrNull(),
                        unMis = unescapeCsv(parts[7]).takeIf { it.isNotBlank() },
                        note = unescapeCsv(parts[8]).takeIf { it.isNotBlank() },
                        foto = unescapeCsv(parts[9]).takeIf { it.isNotBlank() }
                    )
                    operationDao.insertOperation(op)
                    importedCount++
                } catch (e: Exception) {
                    // Log error or skip invalid line
                }
            }
        }
        return importedCount
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun unescapeCsv(value: String): String {
        var result = value.trim()
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length - 1).replace("\"\"", "\"")
        }
        return result
    }
}
