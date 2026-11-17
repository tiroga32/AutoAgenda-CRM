package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupPreferences
import com.example.data.local.ClientEntity
import com.example.data.local.ClientNoteEntity
import com.example.data.local.ClientWithNotes
import com.example.data.local.CrmDao
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CrmRepository(
    private val crmDao: CrmDao,
    private val backupManager: BackupManager,
    private val backupPreferences: BackupPreferences
) {
    val allClients: Flow<List<ClientWithNotes>> = crmDao.observeAllClientsWithNotes()
    val lastBackupMillis: Flow<Long?> = backupPreferences.lastBackupMillis
    val lastExportMillis: Flow<Long?> = backupPreferences.lastExportMillis
    val monthlyGoalTarget: Flow<Int> = backupPreferences.monthlyGoalTarget

    suspend fun setMonthlyGoalTarget(target: Int) {
        backupPreferences.setMonthlyGoalTarget(target)
    }

    fun observeClient(clientId: Long): Flow<ClientWithNotes?> =
        crmDao.observeClientWithNotes(clientId)

    suspend fun createClient(client: ClientEntity, initialNote: String? = null): Long {
        val now = System.currentTimeMillis()
        val insertedId = crmDao.insertClient(
            client.copy(
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        if (!initialNote.isNullOrBlank()) {
            crmDao.insertNote(
                ClientNoteEntity(
                    clientId = insertedId,
                    type = NoteType.NOTA,
                    content = initialNote.trim(),
                    createdAtMillis = now
                )
            )
        }
        return insertedId
    }

    suspend fun updateClient(client: ClientEntity) {
        crmDao.updateClient(client.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteClient(clientId: Long) {
        crmDao.deleteClientById(clientId)
    }

    suspend fun addNote(clientId: Long, type: NoteType, content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        val now = System.currentTimeMillis()
        crmDao.insertNote(
            ClientNoteEntity(
                clientId = clientId,
                type = type,
                content = trimmed,
                createdAtMillis = now
            )
        )
        crmDao.getClientById(clientId)?.let { existing ->
            crmDao.updateClient(existing.copy(updatedAtMillis = now))
        }
    }

    suspend fun deleteNote(noteId: Long) {
        crmDao.deleteNoteById(noteId)
    }

    suspend fun updateFollowUp(clientId: Long, epochDay: Long?) {
        val existing = crmDao.getClientById(clientId) ?: return
        val now = System.currentTimeMillis()
        crmDao.updateClient(
            existing.copy(
                nextFollowUpEpochDay = epochDay,
                updatedAtMillis = now
            )
        )
    }

    suspend fun completeTodayFollowUp(clientId: Long) {
        val existing = crmDao.getClientById(clientId) ?: return
        val now = System.currentTimeMillis()
        crmDao.updateClient(
            existing.copy(
                nextFollowUpEpochDay = null,
                updatedAtMillis = now
            )
        )
        crmDao.insertNote(
            ClientNoteEntity(
                clientId = clientId,
                type = NoteType.NOTA,
                content = "Seguimiento de hoy completado",
                createdAtMillis = now
            )
        )
    }

    suspend fun updateActiveStage(clientId: Long, stage: SalesStage) {
        require(!stage.isClosed) { "Usa markSold o markLost para etapas de cierre." }
        val existing = crmDao.getClientById(clientId) ?: return
        if (existing.stage == stage) return
        val now = System.currentTimeMillis()
        crmDao.updateClient(
            existing.copy(
                stage = stage,
                lossReason = null,
                lossDetails = "",
                soldCar = "",
                soldDateEpochDay = null,
                updatedAtMillis = now
            )
        )
        crmDao.insertNote(
            ClientNoteEntity(
                clientId = clientId,
                type = NoteType.NOTA,
                content = "Etapa actualizada a: ${stage.displayName}",
                createdAtMillis = now
            )
        )
    }

    suspend fun markSold(
        clientId: Long,
        soldCar: String,
        soldDateEpochDay: Long,
        extraNote: String
    ) {
        val existing = crmDao.getClientById(clientId) ?: return
        val now = System.currentTimeMillis()
        val finalCar = soldCar.trim().ifBlank { existing.carSummary() }
        crmDao.updateClient(
            existing.copy(
                stage = SalesStage.VENDIDO,
                soldCar = finalCar,
                soldDateEpochDay = soldDateEpochDay,
                lossReason = null,
                lossDetails = "",
                nextFollowUpEpochDay = null,
                updatedAtMillis = now
            )
        )
        val detail = buildString {
            append("¡Venta cerrada! Auto: $finalCar (${DateUtils.formatEpochDay(soldDateEpochDay)})")
            if (extraNote.isNotBlank()) {
                append(" · ${extraNote.trim()}")
            }
        }
        crmDao.insertNote(
            ClientNoteEntity(
                clientId = clientId,
                type = NoteType.NOTA,
                content = detail,
                createdAtMillis = now
            )
        )
    }

    suspend fun markLost(
        clientId: Long,
        reason: LossReason,
        details: String
    ) {
        val existing = crmDao.getClientById(clientId) ?: return
        val now = System.currentTimeMillis()
        crmDao.updateClient(
            existing.copy(
                stage = SalesStage.PERDIDO,
                lossReason = reason,
                lossDetails = details.trim(),
                soldCar = "",
                soldDateEpochDay = null,
                nextFollowUpEpochDay = null,
                updatedAtMillis = now
            )
        )
        val detailText = buildString {
            append("Marcado como Perdido · Motivo: ${reason.displayName}")
            if (details.isNotBlank()) {
                append(" · ${details.trim()}")
            }
        }
        crmDao.insertNote(
            ClientNoteEntity(
                clientId = clientId,
                type = NoteType.NOTA,
                content = detailText,
                createdAtMillis = now
            )
        )
    }

    suspend fun writeCsvToUri(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val all = crmDao.getAllClientsWithNotesOnce()
        val csv = backupManager.buildCsvContent(all)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(csv.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("No se pudo abrir el destino para guardar el archivo CSV.")
        backupPreferences.recordExport()
        all.size
    }

    suspend fun createShareCsvUri(context: Context): Pair<Uri, Int> = withContext(Dispatchers.IO) {
        val all = crmDao.getAllClientsWithNotesOnce()
        val csv = backupManager.buildCsvContent(all)
        val fileName = "AutoAgenda_Clientes_${DateUtils.formatFileTimestamp()}.csv"
        val uri = backupManager.createShareUri(context, fileName, csv)
        backupPreferences.recordExport()
        uri to all.size
    }

    suspend fun writeBackupZipToUri(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val all = crmDao.getAllClientsWithNotesOnce()
        val zipBytes = backupManager.buildZipBackupBytes(all)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(zipBytes)
        } ?: throw IllegalStateException("No se pudo abrir el destino para guardar el respaldo .zip.")
        backupPreferences.recordBackup()
        all.size
    }

    suspend fun createShareBackupZipUri(context: Context): Pair<Uri, Int> = withContext(Dispatchers.IO) {
        val all = crmDao.getAllClientsWithNotesOnce()
        val zipBytes = backupManager.buildZipBackupBytes(all)
        val fileName = "AutoAgenda_Respaldo_${DateUtils.formatFileTimestamp()}.zip"
        val uri = backupManager.createShareBinaryUri(context, fileName, zipBytes)
        backupPreferences.recordBackup()
        uri to all.size
    }

    suspend fun parseBackupFromUri(context: Context, uri: Uri): List<Pair<ClientEntity, List<ClientNoteEntity>>> =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                backupManager.parseBackupFromStream(stream)
            } ?: throw IllegalStateException("No se pudo leer el archivo seleccionado.")
        }

    suspend fun restoreBackup(
        entries: List<Pair<ClientEntity, List<ClientNoteEntity>>>,
        replaceExisting: Boolean
    ): Int = withContext(Dispatchers.IO) {
        if (replaceExisting) {
            crmDao.replaceAllFromBackup(entries)
            entries.size
        } else {
            crmDao.mergeFromBackup(entries)
        }
    }
}
