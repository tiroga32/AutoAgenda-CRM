package com.example.data.backup

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CrmBackupPayload(
    val version: Int = 1,
    val exportedAtMillis: Long,
    val clients: List<BackupClientDto>
)

@JsonClass(generateAdapter = true)
data class BackupClientDto(
    val uuid: String,
    val name: String,
    val phone: String,
    val whatsapp: String = "",
    val email: String = "",
    val origin: String = "AGENCIA",
    val carBrand: String = "",
    val carModel: String = "",
    val carYear: String = "",
    val carCondition: String = "NUEVO",
    val budget: String = "",
    val paymentMethod: String = "CREDITO",
    val hasTradeIn: Boolean = false,
    val tradeInDetails: String = "",
    val stage: String = "NUEVO",
    val nextFollowUpEpochDay: Long? = null,
    val lossReason: String? = null,
    val lossDetails: String = "",
    val soldCar: String = "",
    val soldDateEpochDay: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val notes: List<BackupNoteDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BackupNoteDto(
    val type: String,
    val content: String,
    val createdAtMillis: Long
)
