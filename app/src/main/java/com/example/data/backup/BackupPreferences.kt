package com.example.data.backup

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backupDataStore by preferencesDataStore(name = "auto_agenda_prefs")

class BackupPreferences(private val context: Context) {
    private val lastBackupKey = longPreferencesKey("last_backup_millis")
    private val lastExportKey = longPreferencesKey("last_export_millis")
    private val monthlyGoalKey = intPreferencesKey("monthly_goal_target")

    val lastBackupMillis: Flow<Long?> = context.backupDataStore.data.map { it[lastBackupKey] }
    val lastExportMillis: Flow<Long?> = context.backupDataStore.data.map { it[lastExportKey] }
    val monthlyGoalTarget: Flow<Int> = context.backupDataStore.data.map { it[monthlyGoalKey] ?: 30 }

    suspend fun recordBackup(timestamp: Long = System.currentTimeMillis()) {
        context.backupDataStore.edit { it[lastBackupKey] = timestamp }
    }

    suspend fun recordExport(timestamp: Long = System.currentTimeMillis()) {
        context.backupDataStore.edit { it[lastExportKey] = timestamp }
    }

    suspend fun setMonthlyGoalTarget(target: Int) {
        context.backupDataStore.edit { it[monthlyGoalKey] = target.coerceIn(1, 999) }
    }
}
