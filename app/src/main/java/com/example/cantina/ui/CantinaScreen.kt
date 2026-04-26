package com.example.cantina.ui

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.cantina.data.OperationEntity
import com.example.cantina.data.OperationTypeEntity
import com.example.cantina.data.GrapeTypeEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import java.io.File
import androidx.compose.material3.HorizontalDivider as Divider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CantinaScreen(viewModel: CantinaViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val operations by viewModel.operations.collectAsState(initial = emptyList())
    val grapeTypes by viewModel.grapeTypes.collectAsState(initial = emptyList())
    val operationTypes by viewModel.operationTypes.collectAsState(initial = emptyList())
    val selectedYear by viewModel.selectedYear.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    var showYearSelector by remember { mutableStateOf(false) }
    
    var showOperationDialog by remember { mutableStateOf(false) }
    var operationToEdit by remember { mutableStateOf<OperationEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<OperationEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("GestCantina", fontWeight = FontWeight.Bold)
                        if (selectedTab == 0) { // Mostra l'anno solo nel tab Operazioni
                            Spacer(Modifier.width(12.dp))
                            Surface(
                                onClick = { showYearSelector = true },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedYear.toString(), style = MaterialTheme.typography.titleMedium)
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                },
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.List, contentDescription = "Ordina")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Data (Recenti)") },
                                onClick = { viewModel.setSortOrder(SortOrder.DATE_DESC); showSortMenu = false },
                                leadingIcon = { Icon(Icons.Default.DateRange, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Data + Uva") },
                                onClick = { viewModel.setSortOrder(SortOrder.DATE_GRAPE); showSortMenu = false },
                                leadingIcon = { Icon(Icons.Default.DateRange, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Uva + Data") },
                                onClick = { viewModel.setSortOrder(SortOrder.GRAPE_DATE); showSortMenu = false },
                                leadingIcon = { Icon(Icons.Default.Info, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Build, null) },
                    label = { Text("Operazioni") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, null) },
                    label = { Text("Passaggi") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Impianto") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Share, null) },
                    label = { Text("Varie") }
                )
            }
        }
    ) { padding ->
        // Usiamo Box come contenitore principale per gestire il padding dello Scaffold
        // e weight(1f) per far sì che il contenuto occupi tutto lo spazio tra topBar e bottomBar
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> OperationsTab(
                        operations = operations,
                        onAddClick = { operationToEdit = null; showOperationDialog = true },
                        onEditClick = { operationToEdit = it; showOperationDialog = true },
                        onDeleteClick = { showDeleteConfirm = it }
                    )
                    1 -> Box(Modifier.fillMaxSize())
                    2 -> ImpiantoTab(
                        grapeTypes = grapeTypes,
                        operationTypes = operationTypes,
                        onAddGrape = { viewModel.addGrapeType(it) },
                        onUpdateGrape = { viewModel.updateGrapeType(it) },
                        onDeleteGrape = { viewModel.deleteGrapeType(it) },
                        onAddOp = { den, add, qta, um, nt, ft -> viewModel.addOperationType(den, add, qta, um, nt, ft) },
                        onUpdateOp = { viewModel.updateOperationType(it) },
                        onDeleteOp = { viewModel.deleteOperationType(it) }
                    )
                    3 -> VarieTab(viewModel)
                }
            }
        }
    }

    if (showYearSelector) {
        YearSelectorDialog(
            years = availableYears,
            selectedYear = selectedYear,
            onYearSelected = { viewModel.setYear(it); showYearSelector = false },
            onDismiss = { showYearSelector = false }
        )
    }

    if (showOperationDialog) {
        OperationDialog(
            initialOperation = operationToEdit,
            uvaOptions = grapeTypes.map { it.denominazione },
            operazioneOptions = operationTypes,
            onDismiss = { showOperationDialog = false },
            onConfirm = { uva, op, data, aggiunta, qta, um, note, foto ->
                if (operationToEdit == null) {
                    viewModel.addOperation(data, uva, op, aggiunta, qta, um, note, foto)
                } else {
                    viewModel.updateOperation(operationToEdit!!.copy(
                        data = data, tipologiaUva = uva, operazione = op, 
                        aggiuntaDi = aggiunta, quantita = qta, unMis = um, note = note, foto = foto
                    ))
                }
                showOperationDialog = false
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Conferma eliminazione") },
            text = { Text("Sei sicuro di voler eliminare questa operazione?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteOperation(showDeleteConfirm!!)
                    showDeleteConfirm = null
                }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Annulla") }
            }
        )
    }
}

@Composable
fun OperationsTab(
    operations: List<OperationEntity>,
    onAddClick: () -> Unit,
    onEditClick: (OperationEntity) -> Unit,
    onDeleteClick: (OperationEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Nuova Operazione")
        }

        LazyColumn(
            modifier = Modifier.weight(1f), // Usiamo weight invece di fillMaxSize per lo scroll corretto
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(operations) { operation ->
                OperationCard(operation, onEdit = { onEditClick(operation) }, onDelete = { onDeleteClick(operation) })
            }
        }
    }
}

@Composable
fun VarieTab(viewModel: CantinaViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportsDir = File(context.getExternalFilesDir(null), "exports")
    val files by viewModel.exportedFiles.collectAsState(initial = emptyList())

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                content?.let { csv -> viewModel.importOperationsFromCsv(csv) }
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.loadExportedFiles(exportsDir) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gestione Dati (CSV)", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { viewModel.exportAllOperationsToCsv(exportsDir) } }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Send, null); Spacer(Modifier.width(4.dp)); Text("Esporta")
            }
            Button(onClick = { importLauncher.launch("*/*") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Importa")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Storico Export:", style = MaterialTheme.typography.titleSmall)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(files) { file ->
                ListItem(
                    headlineContent = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { 
                        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                        Text(date) 
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = {
                                try {
                                    val authority = "${context.packageName}.fileprovider"
                                    val uri = FileProvider.getUriForFile(context, authority, file)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    val chooser = android.content.Intent.createChooser(intent, "Condividi CSV")
                                    chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(chooser)
                                } catch (e: Exception) {
                                    // Log o feedback all'utente se necessario
                                }
                            }) { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onClick = { viewModel.deleteExportedFile(file) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ImpiantoTab(
    grapeTypes: List<GrapeTypeEntity>,
    operationTypes: List<OperationTypeEntity>,
    onAddGrape: (String) -> Unit,
    onUpdateGrape: (GrapeTypeEntity) -> Unit,
    onDeleteGrape: (GrapeTypeEntity) -> Unit,
    onAddOp: (String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onUpdateOp: (OperationTypeEntity) -> Unit,
    onDeleteOp: (OperationTypeEntity) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showGrapeDialog by remember { mutableStateOf(false) }
    var editingGrape by remember { mutableStateOf<GrapeTypeEntity?>(null) }
    var showOpDialog by remember { mutableStateOf(false) }
    var editingOp by remember { mutableStateOf<OperationTypeEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) { // fillMaxSize invece di fillMaxHeight
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }) { Text("Uva", Modifier.padding(16.dp)) }
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }) { Text("Operazioni", Modifier.padding(16.dp)) }
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }) { Text("Calcolo zucchero", Modifier.padding(16.dp)) }
            Tab(selected = selectedSubTab == 3, onClick = { selectedSubTab = 3 }) { Text("Tabella di comparazione", Modifier.padding(16.dp)) }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSubTab) {
                0 -> GrapeTypesList(grapeTypes, onAdd = { editingGrape = null; showGrapeDialog = true }, onEdit = { editingGrape = it; showGrapeDialog = true }, onDelete = onDeleteGrape)
                1 -> OperationTypesList(operationTypes, onAdd = { editingOp = null; showOpDialog = true }, onEdit = { editingOp = it; showOpDialog = true }, onDelete = onDeleteOp)
                2 -> CalcoloZuccheroTab()
                3 -> TabellaComparazioneTab()
            }
        }
    }

    if (showGrapeDialog) {
        GrapeTypeDialog(editingGrape, onDismiss = { showGrapeDialog = false }, onConfirm = { den ->
            if (editingGrape == null) onAddGrape(den) else onUpdateGrape(editingGrape!!.copy(denominazione = den))
            showGrapeDialog = false
        })
    }
    if (showOpDialog) {
        OperationTypeDialog(editingOp, onDismiss = { showOpDialog = false }, onConfirm = { den, add, qta, um, nt, ft ->
            if (editingOp == null) onAddOp(den, add, qta, um, nt, ft) else onUpdateOp(editingOp!!.copy(denominazione = den, hasAggiuntaDi = add, hasQuantita = qta, hasUnMis = um, hasNote = nt, hasFoto = ft))
            showOpDialog = false
        })
    }
}

@Composable
fun GrapeTypesList(types: List<GrapeTypeEntity>, onAdd: () -> Unit, onEdit: (GrapeTypeEntity) -> Unit, onDelete: (GrapeTypeEntity) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Button(onClick = onAdd, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Aggiungi Uva") } }
        items(types) { type ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🍇 ${type.denominazione}", Modifier.weight(1f))
                    IconButton(onClick = { onEdit(type) }) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { onDelete(type) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
fun OperationTypesList(types: List<OperationTypeEntity>, onAdd: () -> Unit, onEdit: (OperationTypeEntity) -> Unit, onDelete: (OperationTypeEntity) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Button(onClick = onAdd, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Nuovo Tipo Operazione") } }
        items(types) { type ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(type.denominazione, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusIndicator("Agg", type.hasAggiuntaDi)
                            StatusIndicator("Qta", type.hasQuantita)
                            StatusIndicator("UM", type.hasUnMis)
                            StatusIndicator("Note", type.hasNote)
                            StatusIndicator("Foto", type.hasFoto)
                        }
                    }
                    IconButton(onClick = { onEdit(type) }) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { onDelete(type) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(label: String, active: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun OperationCard(operation: OperationEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val displayDate = try {
        val parser = SimpleDateFormat("yyyy/MM/dd", Locale.ITALY)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        parser.parse(operation.data)?.let { formatter.format(it) } ?: operation.data
    } catch (e: Exception) { operation.data }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Riga superiore: Data e Uva
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = operation.tipologiaUva,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Titolo Operazione
                Text(
                    text = operation.operazione,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                // Dettaglio e Quantità
                if (!operation.aggiuntaDi.isNullOrBlank() || operation.quantita != null) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!operation.aggiuntaDi.isNullOrBlank()) {
                            Text(
                                text = operation.aggiuntaDi,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (operation.quantita != null) {
                            val qtaFormatted = String.format(Locale.ITALY, "%.2f", operation.quantita)
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "$qtaFormatted ${operation.unMis ?: ""}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Note e Foto
                if (!operation.note.isNullOrBlank()) {
                    Text(
                        text = "Note: ${operation.note}",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (!operation.foto.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Foto allegata",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Pulsanti Azione
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun YearSelectorDialog(years: List<Int>, selectedYear: Int, onYearSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Seleziona Anno", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(years) { year ->
                        Card(onClick = { onYearSelected(year) }, Modifier.fillMaxWidth(), 
                            colors = CardDefaults.cardColors(containerColor = if (year == selectedYear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)) {
                            Box(Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(year.toString(), color = if (year == selectedYear) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OperationDialog(initialOperation: OperationEntity?, uvaOptions: List<String>, operazioneOptions: List<OperationTypeEntity>, onDismiss: () -> Unit, onConfirm: (String, String, String, String?, Double?, String?, String?, String?) -> Unit) {
    var uva by remember { mutableStateOf(initialOperation?.tipologiaUva ?: "") }
    var operazione by remember { mutableStateOf(initialOperation?.operazione ?: "") }
    var aggiuntaDi by remember { mutableStateOf(initialOperation?.aggiuntaDi ?: "") }
    var quantitaStr by remember { mutableStateOf(initialOperation?.quantita?.toString()?.replace(".", ",") ?: "") }
    var unMis by remember { mutableStateOf(initialOperation?.unMis ?: "") }
    var note by remember { mutableStateOf(initialOperation?.note ?: "") }
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ITALY)
    var selectedDate by remember { mutableStateOf(initialOperation?.data ?: sdf.format(Date())) }
    val context = LocalContext.current
    val selectedOpType = operazioneOptions.find { it.denominazione == operazione }
    val umOptions = listOf("gr/hl", "ql", "hl", "Kg/hl", "°Babo", "°Brix", "I°", "II°", "III°", "IV°")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialOperation == null) "Nuova Operazione" else "Modifica Operazione") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    val cal = Calendar.getInstance().apply { try { time = sdf.parse(selectedDate)!! } catch(e:Exception){} }
                    DatePickerDialog(context, { _, y, m, d ->
                        val nCal = Calendar.getInstance().apply { set(y, m, d) }
                        selectedDate = sdf.format(nCal.time)
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }, Modifier.fillMaxWidth()) { Text("Data: $selectedDate") }
                
                DropdownSelector("Uva", uvaOptions, uva) { uva = it }
                DropdownSelector("Operazione", operazioneOptions.map { it.denominazione }, operazione) { operazione = it }
                
                // Campo 1: Aggiunta di (Opzioni fisse)
                if (selectedOpType?.hasAggiuntaDi == true) {
                    val opzioniAggiunta = listOf("*", "Bisolfito", "Ac.Tartarico", "Ac.Citrico", "Bentonite", "Zucchero", "Marzemino", "Merlot", "Lambrusco")
                    Text("Dettaglio (Aggiunta di):", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        opzioniAggiunta.forEach { opzione ->
                            val isSelected = aggiuntaDi == opzione
                            FilterChip(
                                selected = isSelected,
                                onClick = { aggiuntaDi = if (isSelected) "" else opzione },
                                label = { Text(opzione) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
                
                // Campo 2 e 3: Quantità (Numerico) e U.M. (Dropdown)
                if (selectedOpType?.hasQuantita == true || selectedOpType?.hasUnMis == true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (selectedOpType.hasQuantita) {
                            OutlinedTextField(
                                value = quantitaStr, 
                                onValueChange = { 
                                    // Accetta solo numeri con virgola o punto e max 2 decimali
                                    if (it.isEmpty() || it.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
                                        quantitaStr = it
                                    } 
                                }, 
                                label = { Text("Qta") }, 
                                modifier = Modifier.weight(1f), 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        if (selectedOpType.hasUnMis) {
                            Box(modifier = Modifier.weight(1f)) {
                                DropdownSelector("U.M.", umOptions, unMis) { unMis = it }
                            }
                        }
                    }
                }
                
                // Campo 4: Note (Stringa)
                if (selectedOpType?.hasNote == true) {
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
                
                // Campo 5: Foto (Stringa - placeholder)
                if (selectedOpType?.hasFoto == true) {
                    OutlinedTextField(
                        value = "", 
                        onValueChange = { }, 
                        label = { Text("Foto") }, 
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Gestione foto (prossimamente)") },
                        enabled = false
                    )
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = { 
                    val qtaValue = quantitaStr.replace(",", ".").toDoubleOrNull()
                    onConfirm(uva, operazione, selectedDate, aggiuntaDi, qtaValue, unMis, note, null) 
                }, 
                enabled = uva.isNotBlank() && operazione.isNotBlank()
            ) { Text("Salva") } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var exp by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
            options.forEach { opt -> 
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelected(opt); exp = false }
                )
            }
        }
    }
}

@Composable
fun CalcoloZuccheroTab() {
    var gradoAttuale by remember { mutableStateOf("") }
    var litri by remember { mutableStateOf("") }
    var gradoObiettivo by remember { mutableStateOf("") }

    val babo = gradoAttuale.toDoubleOrNull() ?: 0.0
    val l = litri.toDoubleOrNull() ?: 0.0
    val obj = gradoObiettivo.toDoubleOrNull() ?: 0.0

    // Formule
    val brix = babo * 1.58 // Approssimazione comune
    val alcol = babo * 0.65
    val zuccheroNecessario = if (obj > babo && l > 0) (obj - babo) * 1.7 * (l / 100.0) else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Calcolatore Arricchimento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = gradoAttuale,
            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) gradoAttuale = it },
            label = { Text("Grado attuale (°Babo)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = litri,
            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) litri = it },
            label = { Text("Litri totali") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = gradoObiettivo,
            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) gradoObiettivo = it },
            label = { Text("Grado obiettivo (°Babo)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Divider()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Risultati Conversione", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Grado Brix stimato: ${String.format("%.2f", brix)} °Bx")
                Text("Alcol potenziale: ${String.format("%.2f", alcol)} % vol")
                
                if (zuccheroNecessario > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Zucchero da aggiungere:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${String.format("%.2f", zuccheroNecessario)} Kg",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Text(
            "Nota: I calcoli sono basati su coefficienti standard (1.7kg/hl per 1°Babo). Verificare sempre con strumenti professionali.",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
fun TabellaComparazioneTab() {
    val range = (120..250).step(5) // Esempio da 12.0 a 25.0 Babo

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tabella di Comparazione", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(8.dp)) {
            Text("°Babo", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
            Text("°Brix", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
            Text("% Alcol", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
        }

        LazyColumn(Modifier.weight(1f)) {
            items(range.toList()) { value ->
                val b = value / 10.0
                val bx = b * 1.58
                val alc = b * 0.65
                
                Row(Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(String.format(Locale.ITALY, "%.1f", b), Modifier.weight(1f))
                    Text(String.format(Locale.ITALY, "%.1f", bx), Modifier.weight(1f))
                    Text(String.format(Locale.ITALY, "%.2f", alc), Modifier.weight(1f))
                }
                Divider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun GrapeTypeDialog(initial: GrapeTypeEntity?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var den by remember { mutableStateOf(initial?.denominazione ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Uva") }, text = { OutlinedTextField(value = den, onValueChange = { den = it }, label = { Text("Nome Uva") }) },
        confirmButton = { Button(onClick = { onConfirm(den) }, enabled = den.isNotBlank()) { Text("Salva") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } })
}

@Composable
fun OperationTypeDialog(initial: OperationTypeEntity?, onDismiss: () -> Unit, onConfirm: (String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit) {
    var den by remember { mutableStateOf(initial?.denominazione ?: "") }
    var add by remember { mutableStateOf(initial?.hasAggiuntaDi ?: false) }
    var qta by remember { mutableStateOf(initial?.hasQuantita ?: false) }
    var um by remember { mutableStateOf(initial?.hasUnMis ?: false) }
    var nt by remember { mutableStateOf(initial?.hasNote ?: false) }
    var ft by remember { mutableStateOf(initial?.hasFoto ?: false) }
    
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Tipo Operazione") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = den, onValueChange = { den = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = add, onCheckedChange = { add = it }); Text("Aggiunta di.") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = qta, onCheckedChange = { qta = it }); Text("Quantità.") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = um, onCheckedChange = { um = it }); Text("Un.Mis.") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = nt, onCheckedChange = { nt = it }); Text("Note.") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = ft, onCheckedChange = { ft = it }); Text("Foto.") }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(den, add, qta, um, nt, ft) }, enabled = den.isNotBlank()) { Text("Salva") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } })
}
