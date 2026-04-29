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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
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

    // --- NOTIFICHE ---
    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    fun showMessage(text: String) {
        viewModelScope.launch { _message.emit(text) }
    }

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
        .map { yearsFromDb ->
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            // Range: ultimi 5 anni + il prossimo
            val range = (currentYear - 5 .. currentYear + 1).toList()
            (yearsFromDb + range).distinct().sortedDescending()
        }
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

        // Recuperiamo tutte le operazioni (senza filtro anno per il backup completo) e ordiniamo per ID
        val allOps = operationDao.getAllOperationsSync().sortedBy { it.id }
        
        val header = "id;vendemmiaAnno;data;tipologiaUva;operazione;aggiuntaDi;quantita;unMis;note;foto"
        val csv = StringBuilder(header).append("\n")
        
        allOps.forEach { op ->
            csv.append("${op.id};")
                .append("${op.vendemmiaAnno};")
                .append("${op.data};")
                .append("${escapeCsv(op.tipologiaUva)};")
                .append("${escapeCsv(op.operazione)};")
                .append("${escapeCsv(op.aggiuntaDi ?: "")};")
                .append("${op.quantita?.toString()?.replace(".", ",") ?: ""};")
                .append("${escapeCsv(op.unMis ?: "")};")
                .append("${escapeCsv(op.note ?: "")};")
                .append("${escapeCsv(op.foto ?: "")}")
                .append("\n")
        }
        
        return try {
            file.writeText(csv.toString())
            loadExportedFiles(exportsDir)
            showMessage("Esportate ${allOps.size} operazioni con successo.")
            file
        } catch (e: Exception) {
            showMessage("Errore durante l'esportazione.")
            null
        }
    }

    // Funzione per importare operazioni da una stringa CSV
    suspend fun importOperationsFromCsv(csvData: String): Int {
        var importedCount = 0
        val lines = csvData.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return 0
        
        // Rilevamento header e mappatura colonne
        val firstLine = lines.first()
        val delimiter = if (firstLine.contains(";")) ";" else ","
        val headers = firstLine.split(delimiter).map { it.trim().lowercase() }
        
        val hasHeader = headers.contains("operazione") || headers.contains("data")
        val dataLines = if (hasHeader) lines.drop(1) else lines

        // Mappatura indici basata sui nomi delle colonne (default su posizione se non trova header)
        val idxId = headers.indexOf("id").takeIf { it >= 0 } ?: 0
        val idxAnno = headers.indexOf("vendemmiaanno").takeIf { it >= 0 } ?: 1
        val idxData = headers.indexOf("data").takeIf { it >= 0 } ?: 2
        val idxUva = headers.indexOf("tipologiauva").takeIf { it >= 0 } ?: 3
        val idxOp = headers.indexOf("operazione").takeIf { it >= 0 } ?: 4
        val idxAgg = headers.indexOf("aggiuntadi").takeIf { it >= 0 } ?: 5
        val idxQta = headers.indexOf("quantita").takeIf { it >= 0 } ?: 6
        val idxUm = headers.indexOf("unmis").takeIf { it >= 0 } ?: 7
        val idxNote = headers.indexOf("note").takeIf { it >= 0 } ?: 8
        val idxFoto = headers.indexOf("foto").takeIf { it >= 0 } ?: 9

        dataLines.forEach { line ->
            val parts = line.split(delimiter)
            if (parts.size > idxOp) {
                try {
                    val dataRaw = if (parts.size > idxData) parts[idxData].trim() else ""
                    if (dataRaw.isBlank()) return@forEach

                    // Calcolo anno: se presente nel CSV lo usiamo, altrimenti lo deduciamo dalla data
                    var anno = if (parts.size > idxAnno) parts[idxAnno].toIntOrNull() else null
                    if (anno == null) {
                        anno = getHarvestYearFromDate(dataRaw)
                    }

                    val op = OperationEntity(
                        id = if (parts.size > idxId) parts[idxId].toLongOrNull() ?: 0L else 0L,
                        vendemmiaAnno = anno,
                        data = dataRaw,
                        tipologiaUva = unescapeCsv(if (parts.size > idxUva) parts[idxUva] else ""),
                        operazione = unescapeCsv(if (parts.size > idxOp) parts[idxOp] else ""),
                        aggiuntaDi = unescapeCsv(if (parts.size > idxAgg) parts[idxAgg] else "").takeIf { it.isNotBlank() },
                        quantita = if (parts.size > idxQta) parts[idxQta].replace(",", ".").toDoubleOrNull() else null,
                        unMis = unescapeCsv(if (parts.size > idxUm) parts[idxUm] else "").takeIf { it.isNotBlank() },
                        note = unescapeCsv(if (parts.size > idxNote) parts[idxNote] else "").takeIf { it.isNotBlank() },
                        foto = unescapeCsv(if (parts.size > idxFoto) parts[idxFoto] else "").takeIf { it.isNotBlank() }
                    )
                    operationDao.insertOperation(op)
                    importedCount++
                } catch (e: Exception) {
                    // Salta riga corrotta
                }
            }
        }
        showMessage("Importazione completata: $importedCount operazioni caricate.")
        return importedCount
    }

    private fun getHarvestYearFromDate(dateStr: String): Int {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ITALY)
        return try {
            val date = sdf.parse(dateStr)
            val cal = Calendar.getInstance()
            cal.time = date!!
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) // 0-indexed, 0=Jan, 6=July
            // Regola: se prima di Luglio fa parte della vendemmia dell'anno precedente
            if (month < 6) year - 1 else year
        } catch (e: Exception) {
            _selectedYear.value
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
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
