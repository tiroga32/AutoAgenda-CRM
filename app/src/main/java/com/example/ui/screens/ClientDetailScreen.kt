package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.PersonPinCircle
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LossReason
import com.example.data.NoteType
import com.example.data.SalesStage
import com.example.data.local.ClientEntity
import com.example.data.local.ClientNoteEntity
import com.example.data.local.ClientWithNotes
import com.example.ui.components.ClientEditorBottomSheet
import com.example.ui.components.DirectContactBar
import com.example.ui.components.FollowUpBadge
import com.example.ui.components.FollowUpQuickSelector
import com.example.ui.components.MarkLostDialog
import com.example.ui.components.MarkSoldDialog
import com.example.ui.components.StagePill
import com.example.ui.components.VehicleConditionPill
import com.example.ui.components.stageVisualColors
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.OverdueRedContainerDark
import com.example.ui.theme.OverdueRedContainerLight
import com.example.ui.theme.OverdueRedTextDark
import com.example.ui.theme.WhatsAppGreen
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClientDetailScreen(
    clientWithNotes: ClientWithNotes?,
    onNavigateBack: () -> Unit,
    onUpdateClient: (ClientEntity) -> Unit,
    onDeleteClient: (Long) -> Unit,
    onAddNote: (Long, NoteType, String) -> Unit,
    onChangeActiveStage: (Long, SalesStage) -> Unit,
    onMarkSold: (Long, String, Long, String) -> Unit,
    onMarkLost: (Long, LossReason, String) -> Unit,
    onUpdateFollowUp: (Long, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSoldDialog by remember { mutableStateOf(false) }
    var showLostDialog by remember { mutableStateOf(false) }

    var selectedNoteType by remember { mutableStateOf(NoteType.LLAMADA) }
    var noteDraft by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = clientWithNotes?.client?.name ?: "Ficha de cliente",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (clientWithNotes != null) {
                        IconButton(
                            onClick = { showEditSheet = true },
                            modifier = Modifier.testTag("detail_edit_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Editar cliente")
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar cliente")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (clientWithNotes == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando ficha del cliente...")
            }
            return@Scaffold
        }

        val client = clientWithNotes.client
        val notes = clientWithNotes.chronologicalNotes
        val today = DateUtils.todayEpochDay()
        val isOverdue = !client.stage.isClosed &&
            client.nextFollowUpEpochDay != null &&
            client.nextFollowUpEpochDay < today
        val dark = isSystemInDarkTheme()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Summary + Direct Contact
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StagePill(stage = client.stage)
                                VehicleConditionPill(condition = client.carCondition)
                                FollowUpBadge(
                                    epochDay = client.nextFollowUpEpochDay,
                                    isClosed = client.stage.isClosed,
                                    todayEpochDay = today
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (client.stage == SalesStage.VENDIDO && client.soldCar.isNotBlank()) {
                                        "Auto vendido: ${client.soldCar}"
                                    } else {
                                        client.carSummary()
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val subtitleParts = buildList {
                                    add(client.phone)
                                    add("Origen: ${client.origin.displayName}")
                                    if (client.budget.isNotBlank()) add("Presupuesto: ${client.budget}")
                                }
                                Text(
                                    text = subtitleParts.joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DirectContactBar(
                                phone = client.phone,
                                whatsAppNumber = client.effectiveWhatsApp(),
                                clientName = client.name
                            )
                        }
                    }
                }

                // Outcome Banner if Sold or Lost
                if (client.stage == SalesStage.VENDIDO) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WhatsAppGreen.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = WhatsAppGreen
                                )
                                Column {
                                    Text(
                                        text = "Venta cerrada: ${client.soldCar}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = WhatsAppGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    client.soldDateEpochDay?.let {
                                        Text(
                                            text = "Fecha de venta: ${DateUtils.formatEpochDay(it)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (client.stage == SalesStage.PERDIDO) {
                    item {
                        val bg = if (dark) OverdueRedContainerDark else OverdueRedContainerLight
                        val fg = if (dark) OverdueRedTextDark else OverdueRed
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = bg,
                            border = BorderStroke(1.dp, fg.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = fg
                                )
                                Column {
                                    Text(
                                        text = "Motivo de pérdida: ${client.lossReason?.displayName ?: "No especificado"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = fg,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (client.lossDetails.isNotBlank()) {
                                        Text(
                                            text = client.lossDetails,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action 1: Quick Note Composer
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "1. Registrar nota o actividad",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(NoteType.entries) { type ->
                                    FilterChip(
                                        selected = selectedNoteType == type,
                                        onClick = { selectedNoteType = type },
                                        label = { Text(type.displayName) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = noteDraft,
                                onValueChange = { noteDraft = it },
                                placeholder = {
                                    Text("Ej. Quiere Versa 2024, le interesa crédito a 48 meses...")
                                },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("note_input")
                            )
                            Button(
                                onClick = {
                                    onAddNote(client.id, selectedNoteType, noteDraft)
                                    noteDraft = ""
                                },
                                enabled = noteDraft.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("save_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("  Guardar nota con fecha y hora")
                            }
                        }
                    }
                }

                // Action 2: Stage Selector
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "2. Cambiar etapa de venta",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SalesStage.entries.forEach { stage ->
                                    val colors = stageVisualColors(stage)
                                    val isSelected = client.stage == stage
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            when (stage) {
                                                SalesStage.VENDIDO -> showSoldDialog = true
                                                SalesStage.PERDIDO -> showLostDialog = true
                                                else -> onChangeActiveStage(client.id, stage)
                                            }
                                        },
                                        label = { Text(stage.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.container,
                                            selectedLabelColor = colors.content
                                        ),
                                        modifier = Modifier.testTag("detail_stage_${stage.name}")
                                    )
                                }
                            }
                        }
                    }
                }

                // Action 3: Next Follow-Up Scheduler
                item {
                    val overdueBg = if (dark) OverdueRedContainerDark else OverdueRedContainerLight
                    val overdueFg = if (dark) OverdueRedTextDark else OverdueRed
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOverdue) overdueBg.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isOverdue) 1.5.dp else 1.dp,
                            color = if (isOverdue) overdueFg.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "3. Programar siguiente seguimiento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = client.nextFollowUpEpochDay?.let {
                                    DateUtils.formatFollowUpRelative(it, today)
                                } ?: "Sin recordatorio programado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOverdue) overdueFg else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                            )
                            FollowUpQuickSelector(
                                currentEpochDay = client.nextFollowUpEpochDay,
                                onSelectEpochDay = { onUpdateFollowUp(client.id, it) }
                            )
                        }
                    }
                }

                // Complete Client & Vehicle Specs Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Detalles del cliente y auto",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { showEditSheet = true }) {
                                    Text("Editar")
                                }
                            }
                            DetailInfoRow("Teléfono", client.phone)
                            DetailInfoRow("WhatsApp", client.effectiveWhatsApp())
                            if (client.email.isNotBlank()) DetailInfoRow("Correo", client.email)
                            DetailInfoRow("Origen", client.origin.displayName)
                            DetailInfoRow("Auto de interés", client.carSummary())
                            DetailInfoRow("Tipo", client.carCondition.displayName)
                            DetailInfoRow("Forma de pago", client.paymentMethod.displayName)
                            if (client.budget.isNotBlank()) DetailInfoRow("Presupuesto", client.budget)
                            DetailInfoRow(
                                "Auto a cuenta",
                                if (client.hasTradeIn) {
                                    client.tradeInDetails.ifBlank { "Sí" }
                                } else {
                                    "No"
                                }
                            )
                        }
                    }
                }

                // Chronological History Header
                item {
                    Text(
                        text = "Historial cronológico (${notes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (notes.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Aún no hay notas registradas. Escribe arriba lo que platicaste con el cliente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(notes, key = { it.id }) { note ->
                        NoteTimelineItem(note = note)
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showEditSheet && clientWithNotes != null) {
        ClientEditorBottomSheet(
            existingClient = clientWithNotes.client,
            onDismiss = { showEditSheet = false },
            onSave = { updated, _ ->
                onUpdateClient(updated)
                showEditSheet = false
            }
        )
    }

    if (showSoldDialog && clientWithNotes != null) {
        MarkSoldDialog(
            initialCar = clientWithNotes.client.carSummary(),
            onDismiss = { showSoldDialog = false },
            onConfirm = { soldCar, soldDateEpochDay, extraNote ->
                onMarkSold(clientWithNotes.client.id, soldCar, soldDateEpochDay, extraNote)
                showSoldDialog = false
            }
        )
    }

    if (showLostDialog && clientWithNotes != null) {
        MarkLostDialog(
            onDismiss = { showLostDialog = false },
            onConfirm = { reason, details ->
                onMarkLost(clientWithNotes.client.id, reason, details)
                showLostDialog = false
            }
        )
    }

    if (showDeleteConfirm && clientWithNotes != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar cliente") },
            text = {
                Text("¿Seguro que quieres eliminar a ${clientWithNotes.client.name} y todo su historial de notas? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClient(clientWithNotes.client.id)
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NoteTimelineItem(note: ClientNoteEntity) {
    val icon: ImageVector = when (note.type) {
        NoteType.LLAMADA -> Icons.Rounded.Call
        NoteType.VISITA -> Icons.Rounded.PersonPinCircle
        NoteType.COTIZACION -> Icons.Rounded.RequestQuote
        NoteType.PRUEBA_MANEJO -> Icons.Rounded.Speed
        NoteType.WHATSAPP -> Icons.AutoMirrored.Rounded.Chat
        NoteType.NOTA -> Icons.Rounded.EventNote
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = note.type.displayName,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.type.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = DateUtils.formatDateTimeMillis(note.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
