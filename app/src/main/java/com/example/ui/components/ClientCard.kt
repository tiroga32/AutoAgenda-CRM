package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SalesStage
import com.example.data.local.ClientWithNotes
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.OverdueRedTextDark
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenContainer
import com.example.util.ContactLauncher
import com.example.util.DateUtils

@Composable
fun ClientCard(
    item: ClientWithNotes,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    todayEpochDay: Long = DateUtils.todayEpochDay()
) {
    val context = LocalContext.current
    val client = item.client
    val dark = isSystemInDarkTheme()
    val isOverdue = !client.stage.isClosed &&
        client.nextFollowUpEpochDay != null &&
        client.nextFollowUpEpochDay < todayEpochDay

    val borderColor = if (isOverdue) {
        (if (dark) OverdueRedTextDark else OverdueRed).copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.outline
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("client_card_${client.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(if (isOverdue) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOverdue) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${client.phone} · ${client.origin.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { ContactLauncher.dialPhone(context, client.phone) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .testTag("quick_call_${client.id}"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Llamar a ${client.name}",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            ContactLauncher.openWhatsApp(context, client.effectiveWhatsApp(), client.name)
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .testTag("quick_whatsapp_${client.id}"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (dark) WhatsAppGreen.copy(alpha = 0.22f) else WhatsAppGreenContainer,
                            contentColor = WhatsAppGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = "WhatsApp a ${client.name}",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Car & payment summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(5.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    val displayCar = if (client.stage == SalesStage.VENDIDO && client.soldCar.isNotBlank()) {
                        "Vendido: ${client.soldCar}"
                    } else {
                        client.carSummary()
                    }
                    Text(
                        text = displayCar,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val metaParts = buildList {
                        add(client.carCondition.displayName)
                        add(client.paymentMethod.displayName)
                        if (client.budget.isNotBlank()) add(client.budget)
                        if (client.hasTradeIn) add("Auto a cuenta")
                    }
                    Text(
                        text = metaParts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stage + Follow-up badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StagePill(stage = client.stage, compact = true)
                FollowUpBadge(
                    epochDay = client.nextFollowUpEpochDay,
                    isClosed = client.stage.isClosed,
                    todayEpochDay = todayEpochDay
                )
            }

            if (client.stage == SalesStage.PERDIDO && client.lossReason != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Motivo: ${client.lossReason.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                item.chronologicalNotes.firstOrNull()?.let { latestNote ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${latestNote.type.displayName}: ${latestNote.content}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
