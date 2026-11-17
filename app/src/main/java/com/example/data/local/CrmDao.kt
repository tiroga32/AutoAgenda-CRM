package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CrmDao {
    @Transaction
    @Query("SELECT * FROM clients ORDER BY updatedAtMillis DESC")
    fun observeAllClientsWithNotes(): Flow<List<ClientWithNotes>>

    @Transaction
    @Query("SELECT * FROM clients WHERE id = :clientId")
    fun observeClientWithNotes(clientId: Long): Flow<ClientWithNotes?>

    @Transaction
    @Query("SELECT * FROM clients ORDER BY createdAtMillis ASC")
    suspend fun getAllClientsWithNotesOnce(): List<ClientWithNotes>

    @Query("SELECT * FROM clients WHERE id = :clientId LIMIT 1")
    suspend fun getClientById(clientId: Long): ClientEntity?

    @Query("SELECT * FROM clients WHERE uuid = :uuid LIMIT 1")
    suspend fun getClientByUuid(uuid: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClientById(clientId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ClientNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<ClientNoteEntity>)

    @Query("DELETE FROM client_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()

    @Transaction
    suspend fun replaceAllFromBackup(entries: List<Pair<ClientEntity, List<ClientNoteEntity>>>) {
        deleteAllClients()
        entries.forEach { (client, notes) ->
            val newId = insertClient(client.copy(id = 0))
            if (notes.isNotEmpty()) {
                insertNotes(notes.map { it.copy(id = 0, clientId = newId) })
            }
        }
    }

    @Transaction
    suspend fun mergeFromBackup(entries: List<Pair<ClientEntity, List<ClientNoteEntity>>>): Int {
        var addedCount = 0
        entries.forEach { (client, notes) ->
            val existing = getClientByUuid(client.uuid)
            if (existing == null) {
                val newId = insertClient(client.copy(id = 0))
                if (notes.isNotEmpty()) {
                    insertNotes(notes.map { it.copy(id = 0, clientId = newId) })
                }
                addedCount++
            }
        }
        return addedCount
    }
}
