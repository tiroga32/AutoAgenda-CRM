package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.LeadOrigin
import com.example.data.PaymentMethod
import com.example.data.VehicleCondition
import com.example.data.local.ClientEntity
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClientEditorBottomSheet(
    existingClient: ClientEntity? = null,
    onDismiss: () -> Unit,
    onSave: (ClientEntity, initialNote: String?) -> Unit
) {
    val isEditing = existingClient != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(existingClient?.name ?: "") }
    var phone by remember { mutableStateOf(existingClient?.phone ?: "") }
    var sameWhatsApp by remember {
        mutableStateOf(
            existingClient == null ||
                existingClient.whatsapp.isBlank() ||
                existingClient.whatsapp == existingClient.phone
        )
    }
    var whatsapp by remember { mutableStateOf(existingClient?.whatsapp ?: "") }
    var email by remember { mutableStateOf(existingClient?.email ?: "") }
    var origin by remember { mutableStateOf(existingClient?.origin ?: LeadOrigin.AGENCIA) }

    var carBrand by remember { mutableStateOf(existingClient?.carBrand ?: "") }
    var carModel by remember { mutableStateOf(existingClient?.carModel ?: "") }
    var carYear by remember { mutableStateOf(existingClient?.carYear ?: "") }
    var carCondition by remember {
        mutableStateOf(existingClient?.carCondition ?: VehicleCondition.NUEVO)
    }
    var budget by remember { mutableStateOf(existingClient?.budget ?: "") }
    var paymentMethod by remember {
        mutableStateOf(existingClient?.paymentMethod ?: PaymentMethod.CREDITO)
    }
    var hasTradeIn by remember { mutableStateOf(existingClient?.hasTradeIn ?: false) }
    var tradeInDetails by remember { mutableStateOf(existingClient?.tradeInDetails ?: "") }

    var nextFollowUpEpochDay by remember {
        mutableStateOf(
            if (isEditing) existingClient?.nextFollowUpEpochDay else DateUtils.todayEpochDay()
        )
    }
    var initialNote by remember { mutableStateOf("") }

    var showMoreDetails by remember { mutableStateOf(isEditing) }

    val canSave = name.isNotBlank() && phone.isNotBlank()

    fun buildAndSave() {
        if (!canSave) return
        val resolvedWhatsApp = if (sameWhatsApp) phone.trim() else whatsapp.trim()
        val entity = (existingClient ?: ClientEntity(name = "", phone = "")).copy(
            name = name.trim(),
            phone = phone.trim(),
            whatsapp = resolvedWhatsApp,
            email = email.trim(),
            origin = origin,
            carBrand = carBrand.trim(),
            carModel = carModel.trim(),
            carYear = carYear.trim(),
            carCondition = carCondition,
            budget = budget.trim(),
            paymentMethod = paymentMethod,
            hasTradeIn = hasTradeIn,
            tradeInDetails = if (hasTradeIn) tradeInDetails.trim() else "",
            nextFollowUpEpochDay = nextFollowUpEpochDay
        )
        onSave(entity, if (isEditing) null else initialNote.trim().ifBlank { null })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEditing) "Editar cliente" else "Registro rápido",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEditing) {
                            "Actualiza datos de contacto o auto de interés"
                        } else {
                            "Solo nombre y teléfono son obligatorios"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }

            // Mandatory fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre completo *") },
                placeholder = { Text("Ej. Carlos Mendoza") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_client_name")
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono *") },
                placeholder = { Text("Ej. 55 1234 5678") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_client_phone")
            )

            if (!isEditing) {
                Text(
                    text = "Próximo seguimiento",
                    style = MaterialTheme.typography.labelLarge
                )
                FollowUpQuickSelector(
                    currentEpochDay = nextFollowUpEpochDay,
                    onSelectEpochDay = { nextFollowUpEpochDay = it },
                    includeToday = true
                )
            }

            // Fast save button right after mandatory fields!
            Button(
                onClick = ::buildAndSave,
                enabled = canSave,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("save_client_button")
            ) {
                Text(
                    text = if (isEditing) "Guardar cambios" else "Guardar cliente",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Toggle optional details
            TextButton(
                onClick = { showMoreDetails = !showMoreDetails },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toggle_optional_details")
            ) {
                Text(
                    if (showMoreDetails) {
                        "Ocultar datos opcionales"
                    } else {
                        "Completar auto de interés, origen y pago (opcional)"
                    }
                )
                Icon(
                    imageVector = if (showMoreDetails) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = showMoreDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider()

                    Text(
                        text = "Contacto y origen",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = sameWhatsApp,
                            onCheckedChange = { sameWhatsApp = it }
                        )
                        Text(
                            text = "WhatsApp es el mismo número que el teléfono",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (!sameWhatsApp) {
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("WhatsApp") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Origen del cliente", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LeadOrigin.entries.forEach { item ->
                            FilterChip(
                                selected = origin == item,
                                onClick = { origin = item },
                                label = { Text(item.displayName) }
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Auto de interés",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = carBrand,
                            onValueChange = { carBrand = it },
                            label = { Text("Marca") },
                            placeholder = { Text("Nissan") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = carModel,
                            onValueChange = { carModel = it },
                            label = { Text("Modelo") },
                            placeholder = { Text("Versa") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = carYear,
                            onValueChange = { carYear = it },
                            label = { Text("Año") },
                            placeholder = { Text("2024") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f)
                        )
                        OutlinedTextField(
                            value = budget,
                            onValueChange = { budget = it },
                            label = { Text("Presupuesto") },
                            placeholder = { Text("$350,000") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Text("Tipo de auto", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VehicleCondition.entries.forEach { cond ->
                            FilterChip(
                                selected = carCondition == cond,
                                onClick = { carCondition = cond },
                                label = { Text(cond.displayName) }
                            )
                        }
                    }

                    Text("Forma de pago", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method.displayName) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Tiene auto a cuenta?",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = hasTradeIn,
                            onCheckedChange = { hasTradeIn = it }
                        )
                    }

                    if (hasTradeIn) {
                        OutlinedTextField(
                            value = tradeInDetails,
                            onValueChange = { tradeInDetails = it },
                            label = { Text("Auto a cuenta (modelo/año)") },
                            placeholder = { Text("Ej. Aveo 2019") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (!isEditing) {
                        OutlinedTextField(
                            value = initialNote,
                            onValueChange = { initialNote = it },
                            label = { Text("Nota inicial (opcional)") },
                            placeholder = { Text("Ej. Preguntó por promoción a 48 meses") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = ::buildAndSave,
                        enabled = canSave,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                    ) {
                        Text(if (isEditing) "Guardar cambios" else "Guardar cliente completo")
                    }
                }
            }
        }
    }
}
