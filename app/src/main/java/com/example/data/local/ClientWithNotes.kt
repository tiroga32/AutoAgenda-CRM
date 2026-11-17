package com.example.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ClientWithNotes(
    @Embedded val client: ClientEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "clientId"
    )
    val notes: List<ClientNoteEntity>
) {
    val chronologicalNotes: List<ClientNoteEntity>
        get() = notes.sortedByDescending { it.createdAtMillis }
}
