package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.LeadOrigin
import com.example.data.LossReason
import com.example.data.NoteType
import com.example.data.PaymentMethod
import com.example.data.SalesStage
import com.example.data.VehicleCondition

class CrmConverters {
    @TypeConverter
    fun fromSalesStage(value: SalesStage): String = value.name

    @TypeConverter
    fun toSalesStage(value: String): SalesStage =
        runCatching { SalesStage.valueOf(value) }.getOrDefault(SalesStage.NUEVO)

    @TypeConverter
    fun fromLeadOrigin(value: LeadOrigin): String = value.name

    @TypeConverter
    fun toLeadOrigin(value: String): LeadOrigin =
        runCatching { LeadOrigin.valueOf(value) }.getOrDefault(LeadOrigin.AGENCIA)

    @TypeConverter
    fun fromVehicleCondition(value: VehicleCondition): String = value.name

    @TypeConverter
    fun toVehicleCondition(value: String): VehicleCondition =
        runCatching { VehicleCondition.valueOf(value) }.getOrDefault(VehicleCondition.NUEVO)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod =
        runCatching { PaymentMethod.valueOf(value) }.getOrDefault(PaymentMethod.CREDITO)

    @TypeConverter
    fun fromLossReason(value: LossReason?): String? = value?.name

    @TypeConverter
    fun toLossReason(value: String?): LossReason? =
        value?.let { runCatching { LossReason.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromNoteType(value: NoteType): String = value.name

    @TypeConverter
    fun toNoteType(value: String): NoteType =
        runCatching { NoteType.valueOf(value) }.getOrDefault(NoteType.NOTA)
}
