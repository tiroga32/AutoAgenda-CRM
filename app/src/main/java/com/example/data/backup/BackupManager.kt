package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.LeadOrigin
import com.example.data.LossReason
import com.example.data.NoteType
import com.example.data.PaymentMethod
import com.example.data.SalesStage
import com.example.data.VehicleCondition
import com.example.data.local.ClientEntity
import com.example.data.local.ClientNoteEntity
import com.example.data.local.ClientWithNotes
import com.example.util.DateUtils
import com.squareup.moshi.Moshi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager {
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(CrmBackupPayload::class.java).indent("  ")

    fun buildCsvContent(items: List<ClientWithNotes>): String {
        val sb = StringBuilder()
        // UTF-8 BOM for proper Spanish characters in Microsoft Excel
        sb.append('\uFEFF')
        sb.appendLine(
            listOf(
                "Nombre",
                "Teléfono",
                "WhatsApp",
                "Correo",
                "Origen",
                "Marca",
                "Modelo",
                "Año",
                "Tipo de auto",
                "Presupuesto",
                "Forma de pago",
                "Auto a cuenta",
                "Detalle auto a cuenta",
                "Etapa",
                "Próximo seguimiento",
                "Motivo de pérdida",
                "Detalle pérdida",
                "Auto vendido",
                "Fecha de venta",
                "Fecha de registro",
                "Notas e historial"
            ).joinToString(",") { escapeCsv(it) }
        )

        items.forEach { item ->
            val c = item.client
            val notesSummary = item.chronologicalNotes.joinToString(" | ") { note ->
                "[${DateUtils.formatDateTimeMillis(note.createdAtMillis)} - ${note.type.displayName}] ${note.content}"
            }
            val row = listOf(
                c.name,
                c.phone,
                c.whatsapp,
                c.email,
                c.origin.displayName,
                c.carBrand,
                c.carModel,
                c.carYear,
                c.carCondition.displayName,
                c.budget,
                c.paymentMethod.displayName,
                if (c.hasTradeIn) "Sí" else "No",
                c.tradeInDetails,
                c.stage.displayName,
                c.nextFollowUpEpochDay?.let { DateUtils.formatEpochDay(it) } ?: "",
                c.lossReason?.displayName ?: "",
                c.lossDetails,
                c.soldCar,
                c.soldDateEpochDay?.let { DateUtils.formatEpochDay(it) } ?: "",
                DateUtils.formatDateTimeMillis(c.createdAtMillis),
                notesSummary
            )
            sb.appendLine(row.joinToString(",") { escapeCsv(it) })
        }
        return sb.toString()
    }

    fun buildJsonBackup(items: List<ClientWithNotes>): String {
        val payload = CrmBackupPayload(
            version = 1,
            exportedAtMillis = System.currentTimeMillis(),
            clients = items.map { item ->
                val c = item.client
                BackupClientDto(
                    uuid = c.uuid,
                    name = c.name,
                    phone = c.phone,
                    whatsapp = c.whatsapp,
                    email = c.email,
                    origin = c.origin.name,
                    carBrand = c.carBrand,
                    carModel = c.carModel,
                    carYear = c.carYear,
                    carCondition = c.carCondition.name,
                    budget = c.budget,
                    paymentMethod = c.paymentMethod.name,
                    hasTradeIn = c.hasTradeIn,
                    tradeInDetails = c.tradeInDetails,
                    stage = c.stage.name,
                    nextFollowUpEpochDay = c.nextFollowUpEpochDay,
                    lossReason = c.lossReason?.name,
                    lossDetails = c.lossDetails,
                    soldCar = c.soldCar,
                    soldDateEpochDay = c.soldDateEpochDay,
                    createdAtMillis = c.createdAtMillis,
                    updatedAtMillis = c.updatedAtMillis,
                    notes = item.notes.map { n ->
                        BackupNoteDto(
                            type = n.type.name,
                            content = n.content,
                            createdAtMillis = n.createdAtMillis
                        )
                    }
                )
            }
        )
        return adapter.toJson(payload)
    }

    fun buildZipBackupBytes(items: List<ClientWithNotes>): ByteArray {
        val jsonBytes = buildJsonBackup(items).toByteArray(Charsets.UTF_8)
        val csvBytes = buildCsvContent(items).toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("autoagenda_respaldo.json"))
            zip.write(jsonBytes)
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("autoagenda_clientes.csv"))
            zip.write(csvBytes)
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    fun parseBackupFromStream(inputStream: InputStream): List<Pair<ClientEntity, List<ClientNoteEntity>>> {
        val bytes = inputStream.readBytes()
        if (bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()
        ) {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".json", ignoreCase = true)) {
                        val jsonText = zip.readBytes().toString(Charsets.UTF_8)
                        return parseJsonBackup(jsonText)
                    }
                    entry = zip.nextEntry
                }
            }
            throw IllegalArgumentException("El archivo .zip no contiene un respaldo válido (.json).")
        }
        return parseJsonBackup(bytes.toString(Charsets.UTF_8))
    }

    fun parseJsonBackup(rawJson: String): List<Pair<ClientEntity, List<ClientNoteEntity>>> {
        val payload = adapter.fromJson(rawJson)
            ?: throw IllegalArgumentException("El archivo de respaldo está vacío o no tiene formato válido.")

        return payload.clients.map { dto ->
            val entity = ClientEntity(
                uuid = dto.uuid.ifBlank { UUID.randomUUID().toString() },
                name = dto.name.trim(),
                phone = dto.phone.trim(),
                whatsapp = dto.whatsapp.trim(),
                email = dto.email.trim(),
                origin = runCatching { LeadOrigin.valueOf(dto.origin) }.getOrDefault(LeadOrigin.AGENCIA),
                carBrand = dto.carBrand.trim(),
                carModel = dto.carModel.trim(),
                carYear = dto.carYear.trim(),
                carCondition = runCatching { VehicleCondition.valueOf(dto.carCondition) }
                    .getOrDefault(VehicleCondition.NUEVO),
                budget = dto.budget.trim(),
                paymentMethod = runCatching { PaymentMethod.valueOf(dto.paymentMethod) }
                    .getOrDefault(PaymentMethod.CREDITO),
                hasTradeIn = dto.hasTradeIn,
                tradeInDetails = dto.tradeInDetails.trim(),
                stage = runCatching { SalesStage.valueOf(dto.stage) }.getOrDefault(SalesStage.NUEVO),
                nextFollowUpEpochDay = dto.nextFollowUpEpochDay,
                lossReason = dto.lossReason?.let { runCatching { LossReason.valueOf(it) }.getOrNull() },
                lossDetails = dto.lossDetails.trim(),
                soldCar = dto.soldCar.trim(),
                soldDateEpochDay = dto.soldDateEpochDay,
                createdAtMillis = dto.createdAtMillis,
                updatedAtMillis = dto.updatedAtMillis
            )
            val notes = dto.notes.map { n ->
                ClientNoteEntity(
                    clientId = 0,
                    type = runCatching { NoteType.valueOf(n.type) }.getOrDefault(NoteType.NOTA),
                    content = n.content.trim(),
                    createdAtMillis = n.createdAtMillis
                )
            }
            entity to notes
        }
    }

    fun createShareUri(context: Context, fileName: String, content: String): Uri {
        return createShareBinaryUri(context, fileName, content.toByteArray(Charsets.UTF_8))
    }

    fun createShareBinaryUri(context: Context, fileName: String, bytes: ByteArray): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val target = File(dir, fileName)
        target.writeBytes(bytes)
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            target
        )
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ')
        return if (clean.contains(',') || clean.contains('"') || clean.contains('\n')) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }
}
