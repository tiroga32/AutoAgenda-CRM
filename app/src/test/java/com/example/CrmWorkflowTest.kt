package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CrmRepository
import com.example.data.LeadOrigin
import com.example.data.LossReason
import com.example.data.NoteType
import com.example.data.PaymentMethod
import com.example.data.SalesStage
import com.example.data.VehicleCondition
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupPreferences
import com.example.data.local.ClientEntity
import com.example.data.local.CrmDatabase
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CrmWorkflowTest {
    private lateinit var database: CrmDatabase
    private lateinit var repository: CrmRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CrmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupManager = BackupManager()
        repository = CrmRepository(
            crmDao = database.crmDao(),
            backupManager = backupManager,
            backupPreferences = BackupPreferences(context)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `create client adds initial note and supports stage transitions and backup roundtrip`() = runTest {
        val today = DateUtils.todayEpochDay()
        val clientId = repository.createClient(
            client = ClientEntity(
                name = "Laura Jiménez",
                phone = "5511223344",
                origin = LeadOrigin.FACEBOOK,
                carBrand = "Nissan",
                carModel = "Versa",
                carYear = "2024",
                carCondition = VehicleCondition.NUEVO,
                paymentMethod = PaymentMethod.CREDITO,
                nextFollowUpEpochDay = today - 1
            ),
            initialNote = "Preguntó por financiamiento a 48 meses"
        )

        repository.addNote(clientId, NoteType.LLAMADA, "Confirmó cita para prueba de manejo")
        repository.updateActiveStage(clientId, SalesStage.CITA_PRUEBA)
        repository.updateFollowUp(clientId, today + 3)

        val updated = repository.observeClient(clientId).first()!!
        assertEquals(SalesStage.CITA_PRUEBA, updated.client.stage)
        assertEquals(today + 3, updated.client.nextFollowUpEpochDay)
        assertEquals(3, updated.notes.size)

        // Export CSV & JSON backup
        val all = repository.allClients.first()
        val csv = backupManager.buildCsvContent(all)
        assertTrue(csv.contains("Laura Jiménez"))
        assertTrue(csv.contains("Nissan"))
        assertTrue(csv.contains("Cita o prueba de manejo"))

        val json = backupManager.buildJsonBackup(all)
        val parsed = backupManager.parseJsonBackup(json)
        assertEquals(1, parsed.size)
        assertEquals("Laura Jiménez", parsed.first().first.name)
        assertEquals(3, parsed.first().second.size)

        val zipBytes = backupManager.buildZipBackupBytes(all)
        val parsedFromZip = backupManager.parseBackupFromStream(zipBytes.inputStream())
        assertEquals(1, parsedFromZip.size)
        assertEquals("Laura Jiménez", parsedFromZip.first().first.name)
        assertEquals(3, parsedFromZip.first().second.size)

        // Mark lost clears follow-up and stores reason
        repository.markLost(clientId, LossReason.PRECIO, "Prefirió otra agencia")
        val lostClient = repository.observeClient(clientId).first()!!
        assertEquals(SalesStage.PERDIDO, lostClient.client.stage)
        assertEquals(LossReason.PRECIO, lostClient.client.lossReason)
        assertNull(lostClient.client.nextFollowUpEpochDay)

        // Restore from backup brings back CITA_PRUEBA state
        val restoredCount = repository.restoreBackup(parsed, replaceExisting = true)
        assertEquals(1, restoredCount)
        val restoredAll = repository.allClients.first()
        assertEquals(SalesStage.CITA_PRUEBA, restoredAll.first().client.stage)
    }
}
