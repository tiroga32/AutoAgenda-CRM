package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FollowUpQuickSelector(
    currentEpochDay: Long?,
    onSelectEpochDay: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    includeToday: Boolean = false
) {
    val today = DateUtils.todayEpochDay()
    val tomorrow = today + 1
    val in3Days = today + 3
    val in1Week = today + 7
    var showDatePicker by remember { mutableStateOf(false) }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (includeToday) {
            FilterChip(
                selected = currentEpochDay == today,
                onClick = { onSelectEpochDay(today) },
                label = { Text("Hoy") },
                modifier = Modifier.testTag("followup_today_chip")
            )
        }
        FilterChip(
            selected = currentEpochDay == tomorrow,
            onClick = { onSelectEpochDay(tomorrow) },
            label = { Text("Mañana") },
            leadingIcon = {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("followup_tomorrow_chip")
        )
        FilterChip(
            selected = currentEpochDay == in3Days,
            onClick = { onSelectEpochDay(in3Days) },
            label = { Text("En 3 días") },
            modifier = Modifier.testTag("followup_3days_chip")
        )
        FilterChip(
            selected = currentEpochDay == in1Week,
            onClick = { onSelectEpochDay(in1Week) },
            label = { Text("En 1 semana") },
            modifier = Modifier.testTag("followup_1week_chip")
        )
        val isCustomDate = currentEpochDay != null &&
            currentEpochDay != today &&
            currentEpochDay != tomorrow &&
            currentEpochDay != in3Days &&
            currentEpochDay != in1Week
        FilterChip(
            selected = isCustomDate,
            onClick = { showDatePicker = true },
            label = {
                Text(
                    if (isCustomDate && currentEpochDay != null) {
                        DateUtils.formatEpochDayShort(currentEpochDay)
                    } else {
                        "Elegir fecha"
                    }
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("followup_custom_chip")
        )
        if (currentEpochDay != null) {
            FilterChip(
                selected = false,
                onClick = { onSelectEpochDay(null) },
                label = { Text("Quitar") },
                leadingIcon = {
                    Icon(Icons.Rounded.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("followup_clear_chip")
            )
        }
    }

    if (showDatePicker) {
        val initialMillis = DateUtils.epochDayToUtcMillis(currentEpochDay ?: tomorrow)
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onSelectEpochDay(DateUtils.utcMillisToEpochDay(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
