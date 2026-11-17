package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.LeadOrigin
import com.example.data.LossReason
import com.example.data.PaymentMethod
import com.example.data.SalesStage
import com.example.data.VehicleCondition
import java.util.UUID

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val whatsapp: String = "",
    val email: String = "",
    val origin: LeadOrigin = LeadOrigin.AGENCIA,
    val carBrand: String = "",
    val carModel: String = "",
    val carYear: String = "",
    val carCondition: VehicleCondition = VehicleCondition.NUEVO,
    val budget: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CREDITO,
    val hasTradeIn: Boolean = false,
    val tradeInDetails: String = "",
    val stage: SalesStage = SalesStage.NUEVO,
    val nextFollowUpEpochDay: Long? = null,
    val lossReason: LossReason? = null,
    val lossDetails: String = "",
    val soldCar: String = "",
    val soldDateEpochDay: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun carSummary(): String {
        val parts = listOf(carBrand.trim(), carModel.trim(), carYear.trim()).filter { it.isNotEmpty() }
        return if (parts.isEmpty()) "Sin auto especificado" else parts.joinToString(" ")
    }

    fun effectiveWhatsApp(): String = whatsapp.ifBlank { phone }
}
