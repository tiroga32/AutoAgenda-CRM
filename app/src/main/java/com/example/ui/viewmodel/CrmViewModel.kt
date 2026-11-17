package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CrmRepository
import com.example.data.LossReason
import com.example.data.NoteType
import com.example.data.SalesStage
import com.example.data.VehicleCondition
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupPreferences
import com.example.data.local.ClientEntity
import com.example.data.local.ClientNoteEntity
import com.example.data.local.ClientWithNotes
import com.example.data.local.CrmDatabase
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val todayEpochDay: Long = DateUtils.todayEpochDay(),
    val overdueClients: List<ClientWithNotes> = emptyList(),
    val dueTodayClients: List<ClientWithNotes> = emptyList(),
    val recentRequests: List<ClientWithNotes> = emptyList(),
    val monthlyRequestsCount: Int = 0,
    val monthlyGoalTarget: Int = 30,
    val upcomingClientsCount: Int = 0,
    val activeClientsCount: Int = 0,
    val isLoaded: Boolean = false
)

data class ClientsUiState(
    val searchQuery: String = "",
    val selectedStage: SalesStage? = null,
    val selectedCondition: VehicleCondition? = null,
    val filteredClients: List<ClientWithNotes> = emptyList(),
    val stageCounts: Map<SalesStage, Int> = emptyMap(),
    val totalClientsCount: Int = 0
)

data class StageMetric(
    val stage: SalesStage,
    val count: Int,
    val percentage: Float
)

data class LossReasonMetric(
    val reason: LossReason,
    val count: Int,
    val percentage: Float
)

data class PipelineUiState(
    val stageMetrics: List<StageMetric> = emptyList(),
    val lossReasonMetrics: List<LossReasonMetric> = emptyList(),
    val totalClients: Int = 0,
    val activeClients: Int = 0,
    val soldCount: Int = 0,
    val lostCount: Int = 0,
    val overdueCount: Int = 0,
    val bottleneckTitle: String = "",
    val bottleneckDescription: String = "",
    val recentSales: List<ClientWithNotes> = emptyList()
)

data class PendingRestorePreview(
    val entries: List<Pair<ClientEntity, List<ClientNoteEntity>>>,
    val clientCount: Int,
    val noteCount: Int
)

data class BackupUiState(
    val totalClients: Int = 0,
    val totalNotes: Int = 0,
    val lastBackupMillis: Long? = null,
    val lastExportMillis: Long? = null,
    val isBusy: Boolean = false,
    val pendingRestore: PendingRestorePreview? = null
)

data class ShareFileRequest(
    val uri: Uri,
    val mimeType: String,
    val title: String
)

class CrmViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CrmRepository by lazy {
        val db = CrmDatabase.getInstance(application)
        CrmRepository(
            crmDao = db.crmDao(),
            backupManager = BackupManager(),
            backupPreferences = BackupPreferences(application)
        )
    }

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStage = MutableStateFlow<SalesStage?>(null)
    private val _selectedCondition = MutableStateFlow<VehicleCondition?>(null)
    private val _isBackupBusy = MutableStateFlow(false)
    private val _pendingRestore = MutableStateFlow<PendingRestorePreview?>(null)

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _shareRequests = MutableSharedFlow<ShareFileRequest>(extraBufferCapacity = 4)
    val shareRequests: SharedFlow<ShareFileRequest> = _shareRequests.asSharedFlow()

    val allClients: StateFlow<List<ClientWithNotes>> = repository.allClients
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayUiState: StateFlow<TodayUiState> = combine(
        repository.allClients,
        repository.monthlyGoalTarget
    ) { list, goalTarget ->
        val today = DateUtils.todayEpochDay()
        val active = list.filter { !it.client.stage.isClosed }
        val overdue = active
            .filter {
                val f = it.client.nextFollowUpEpochDay
                f != null && f < today
            }
            .sortedBy { it.client.nextFollowUpEpochDay }
        val dueToday = active
            .filter { it.client.nextFollowUpEpochDay == today }
            .sortedByDescending { it.client.updatedAtMillis }
        val upcoming = active.count {
            val f = it.client.nextFollowUpEpochDay
            f != null && f > today
        }
        val monthlyCount = list.count { DateUtils.isCurrentMonth(it.client.createdAtMillis) }
        val recent = list.sortedByDescending { it.client.createdAtMillis }
        TodayUiState(
            todayEpochDay = today,
            overdueClients = overdue,
            dueTodayClients = dueToday,
            recentRequests = recent,
            monthlyRequestsCount = monthlyCount,
            monthlyGoalTarget = goalTarget,
            upcomingClientsCount = upcoming,
            activeClientsCount = active.size,
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    val clientsUiState: StateFlow<ClientsUiState> = combine(
        repository.allClients,
        _searchQuery,
        _selectedStage,
        _selectedCondition
    ) { list, query, stageFilter, conditionFilter ->
        val trimmed = query.trim().lowercase()
        val stageCounts = SalesStage.entries.associateWith { stage ->
            list.count { it.client.stage == stage }
        }
        val filtered = list.filter { item ->
            val c = item.client
            val matchesStage = stageFilter == null || c.stage == stageFilter
            val matchesCondition = conditionFilter == null || c.carCondition == conditionFilter
            val matchesQuery = trimmed.isEmpty() ||
                c.name.lowercase().contains(trimmed) ||
                c.phone.lowercase().contains(trimmed) ||
                c.whatsapp.lowercase().contains(trimmed) ||
                c.carModel.lowercase().contains(trimmed) ||
                c.carBrand.lowercase().contains(trimmed)
            matchesStage && matchesCondition && matchesQuery
        }.sortedWith(
            compareBy<ClientWithNotes> { it.client.stage.isClosed }
                .thenBy { it.client.nextFollowUpEpochDay ?: Long.MAX_VALUE }
                .thenByDescending { it.client.updatedAtMillis }
        )

        ClientsUiState(
            searchQuery = query,
            selectedStage = stageFilter,
            selectedCondition = conditionFilter,
            filteredClients = filtered,
            stageCounts = stageCounts,
            totalClientsCount = list.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClientsUiState()
    )

    val pipelineUiState: StateFlow<PipelineUiState> = repository.allClients
        .map { list ->
            val total = list.size
            val today = DateUtils.todayEpochDay()
            val stageMetrics = SalesStage.entries.map { stage ->
                val count = list.count { it.client.stage == stage }
                StageMetric(
                    stage = stage,
                    count = count,
                    percentage = if (total > 0) count.toFloat() / total else 0f
                )
            }
            val lostClients = list.filter { it.client.stage == SalesStage.PERDIDO }
            val lostCount = lostClients.size
            val lossMetrics = LossReason.entries.map { reason ->
                val count = lostClients.count { it.client.lossReason == reason }
                LossReasonMetric(
                    reason = reason,
                    count = count,
                    percentage = if (lostCount > 0) count.toFloat() / lostCount else 0f
                )
            }
            val active = list.filter { !it.client.stage.isClosed }
            val soldClients = list
                .filter { it.client.stage == SalesStage.VENDIDO }
                .sortedByDescending { it.client.soldDateEpochDay ?: it.client.updatedAtMillis }
            val overdueCount = active.count {
                val f = it.client.nextFollowUpEpochDay
                f != null && f < today
            }

            val topActiveMetric = stageMetrics
                .filter { !it.stage.isClosed && it.count > 0 }
                .maxByOrNull { it.count }

            val (insightTitle, insightDesc) = when {
                total == 0 -> {
                    "Tu embudo está listo" to "Agrega tu primer cliente con el botón + para empezar a medir tus etapas."
                }
                overdueCount > 0 -> {
                    "$overdueCount seguimiento(s) vencido(s)" to "Tienes clientes activos esperando respuesta. Revisa la pestaña Hoy para llamarlos."
                }
                topActiveMetric != null -> {
                    "Mayor concentración en ${topActiveMetric.stage.displayName}" to
                        "${topActiveMetric.count} de ${active.size} clientes activos están en esta etapa. Revisa si necesitan seguimiento."
                }
                else -> {
                    "Sin clientes activos pendientes" to "Todos tus registros actuales están cerrados como Vendido o Perdido."
                }
            }

            PipelineUiState(
                stageMetrics = stageMetrics,
                lossReasonMetrics = lossMetrics,
                totalClients = total,
                activeClients = active.size,
                soldCount = soldClients.size,
                lostCount = lostCount,
                overdueCount = overdueCount,
                bottleneckTitle = insightTitle,
                bottleneckDescription = insightDesc,
                recentSales = soldClients.take(5)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PipelineUiState()
        )

    val backupUiState: StateFlow<BackupUiState> = combine(
        repository.allClients,
        repository.lastBackupMillis,
        repository.lastExportMillis,
        _isBackupBusy,
        _pendingRestore
    ) { list, lastBackup, lastExport, busy, pending ->
        BackupUiState(
            totalClients = list.size,
            totalNotes = list.sumOf { it.notes.size },
            lastBackupMillis = lastBackup,
            lastExportMillis = lastExport,
            isBusy = busy,
            pendingRestore = pending
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BackupUiState()
    )

    fun observeClient(clientId: Long): Flow<ClientWithNotes?> = repository.observeClient(clientId)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStageFilterChange(stage: SalesStage?) {
        _selectedStage.value = stage
    }

    fun onConditionFilterChange(condition: VehicleCondition?) {
        _selectedCondition.value = condition
    }

    fun createClient(
        client: ClientEntity,
        initialNote: String? = null,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.createClient(client, initialNote)
            _snackbarMessages.tryEmit("Cliente guardado en etapa Nuevo")
            onCreated(id)
        }
    }

    fun updateClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.updateClient(client)
            _snackbarMessages.tryEmit("Datos del cliente actualizados")
        }
    }

    fun deleteClient(clientId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteClient(clientId)
            _snackbarMessages.tryEmit("Cliente eliminado")
            onDeleted()
        }
    }

    fun addNote(clientId: Long, type: NoteType, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.addNote(clientId, type, content)
            _snackbarMessages.tryEmit("Nota agregada al historial")
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            _snackbarMessages.tryEmit("Nota eliminada")
        }
    }

    fun updateFollowUp(clientId: Long, epochDay: Long?) {
        viewModelScope.launch {
            repository.updateFollowUp(clientId, epochDay)
            val msg = if (epochDay == null) {
                "Recordatorio eliminado"
            } else {
                "Seguimiento programado: ${DateUtils.formatFollowUpRelative(epochDay)}"
            }
            _snackbarMessages.tryEmit(msg)
        }
    }

    fun completeTodayTask(clientId: Long) {
        viewModelScope.launch {
            repository.completeTodayFollowUp(clientId)
            _snackbarMessages.tryEmit("Actividad completada")
        }
    }

    fun setMonthlyGoalTarget(target: Int) {
        viewModelScope.launch {
            repository.setMonthlyGoalTarget(target)
            _snackbarMessages.tryEmit("Meta del mes actualizada a $target")
        }
    }

    fun updateActiveStage(clientId: Long, stage: SalesStage) {
        viewModelScope.launch {
            repository.updateActiveStage(clientId, stage)
            _snackbarMessages.tryEmit("Etapa cambiada a ${stage.displayName}")
        }
    }

    fun markSold(
        clientId: Long,
        soldCar: String,
        soldDateEpochDay: Long,
        extraNote: String
    ) {
        viewModelScope.launch {
            repository.markSold(clientId, soldCar, soldDateEpochDay, extraNote)
            _snackbarMessages.tryEmit("¡Felicidades! Cliente marcado como Vendido")
        }
    }

    fun markLost(
        clientId: Long,
        reason: LossReason,
        details: String
    ) {
        viewModelScope.launch {
            repository.markLost(clientId, reason, details)
            _snackbarMessages.tryEmit("Cliente marcado como Perdido")
        }
    }

    fun exportCsvToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            runCatching { repository.writeCsvToUri(context, uri) }
                .onSuccess { count ->
                    _snackbarMessages.tryEmit("CSV exportado con $count clientes")
                }
                .onFailure { e ->
                    _snackbarMessages.tryEmit(e.localizedMessage ?: "Error al exportar CSV")
                }
            _isBackupBusy.value = false
        }
    }

    fun shareCsv(context: Context) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            runCatching { repository.createShareCsvUri(context) }
                .onSuccess { (uri, _) ->
                    _shareRequests.tryEmit(
                        ShareFileRequest(
                            uri = uri,
                            mimeType = "text/csv",
                            title = "Compartir reporte CSV de clientes"
                        )
                    )
                }
                .onFailure { e ->
                    _snackbarMessages.tryEmit(e.localizedMessage ?: "Error al preparar CSV")
                }
            _isBackupBusy.value = false
        }
    }

    fun saveBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            runCatching { repository.writeBackupZipToUri(context, uri) }
                .onSuccess { count ->
                    _snackbarMessages.tryEmit("Respaldo .zip guardado ($count clientes)")
                }
                .onFailure { e ->
                    _snackbarMessages.tryEmit(e.localizedMessage ?: "Error al guardar respaldo")
                }
            _isBackupBusy.value = false
        }
    }

    fun shareBackup(context: Context) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            runCatching { repository.createShareBackupZipUri(context) }
                .onSuccess { (uri, _) ->
                    _shareRequests.tryEmit(
                        ShareFileRequest(
                            uri = uri,
                            mimeType = "application/zip",
                            title = "Compartir respaldo .zip AutoAgenda"
                        )
                    )
                }
                .onFailure { e ->
                    _snackbarMessages.tryEmit(e.localizedMessage ?: "Error al preparar respaldo")
                }
            _isBackupBusy.value = false
        }
    }

    fun prepareRestoreFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            runCatching { repository.parseBackupFromUri(context, uri) }
                .onSuccess { entries ->
                    if (entries.isEmpty()) {
                        _snackbarMessages.tryEmit("El archivo de respaldo no contiene clientes.")
                    } else {
                        _pendingRestore.value = PendingRestorePreview(
                            entries = entries,
                            clientCount = entries.size,
                            noteCount = entries.sumOf { it.second.size }
                        )
                    }
                }
                .onFailure { e ->
                    _snackbarMessages.tryEmit(
                        "Archivo no válido: ${e.localizedMessage ?: "formato incorrecto"}"
                    )
                }
            _isBackupBusy.value = false
        }
    }

    fun confirmRestore(replaceExisting: Boolean) {
        val pending = _pendingRestore.value ?: return
        viewModelScope.launch {
            _isBackupBusy.value = true
            _pendingRestore.value = null
            runCatching { repository.restoreBackup(pending.entries, replaceExisting) }
                .onSuccess { count ->
                    val msg = if (replaceExisting) {
                        "Respaldo restaurado: $count clientes cargados"
                    } else {
                        "Se agregaron $count clientes nuevos del respaldo"
                    }
                    _snackbarMessages.tryEmit(msg)
                }
                .onFailure { e ->
                    _snackbarMessages.tryEmit(e.localizedMessage ?: "Error al restaurar respaldo")
                }
            _isBackupBusy.value = false
        }
    }

    fun dismissRestorePreview() {
        _pendingRestore.value = null
    }
}
