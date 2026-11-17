package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SalesStage
import com.example.data.VehicleCondition
import com.example.ui.theme.*
import com.example.util.DateUtils

data class StageVisualColors(
    val container: Color,
    val content: Color
)

@Composable
fun stageVisualColors(stage: SalesStage): StageVisualColors {
    val dark = isSystemInDarkTheme()
    return when (stage) {
        SalesStage.NUEVO -> if (dark) StageVisualColors(StageNuevoBgDark, StageNuevoFgDark)
        else StageVisualColors(StageNuevoBgLight, StageNuevoFgLight)

        SalesStage.CONTACTADO -> if (dark) StageVisualColors(StageContactadoBgDark, StageContactadoFgDark)
        else StageVisualColors(StageContactadoBgLight, StageContactadoFgLight)

        SalesStage.CITA_PRUEBA -> if (dark) StageVisualColors(StageCitaBgDark, StageCitaFgDark)
        else StageVisualColors(StageCitaBgLight, StageCitaFgLight)

        SalesStage.COTIZACION -> if (dark) StageVisualColors(StageCotizacionBgDark, StageCotizacionFgDark)
        else StageVisualColors(StageCotizacionBgLight, StageCotizacionFgLight)

        SalesStage.NEGOCIACION -> if (dark) StageVisualColors(StageNegociacionBgDark, StageNegociacionFgDark)
        else StageVisualColors(StageNegociacionBgLight, StageNegociacionFgLight)

        SalesStage.VENDIDO -> if (dark) StageVisualColors(StageVendidoBgDark, StageVendidoFgDark)
        else StageVisualColors(StageVendidoBgLight, StageVendidoFgLight)

        SalesStage.PERDIDO -> if (dark) StageVisualColors(StagePerdidoBgDark, StagePerdidoFgDark)
        else StageVisualColors(StagePerdidoBgLight, StagePerdidoFgLight)
    }
}

@Composable
fun StagePill(
    stage: SalesStage,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = stageVisualColors(stage)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = colors.container
    ) {
        Text(
            text = if (compact) stage.shortName else stage.displayName,
            color = colors.content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun FollowUpBadge(
    epochDay: Long?,
    isClosed: Boolean,
    modifier: Modifier = Modifier,
    todayEpochDay: Long = DateUtils.todayEpochDay()
) {
    if (epochDay == null || isClosed) return
    val isOverdue = epochDay < todayEpochDay
    val isToday = epochDay == todayEpochDay
    val dark = isSystemInDarkTheme()

    val bgColor = when {
        isOverdue -> if (dark) OverdueRedContainerDark else OverdueRedContainerLight
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fgColor = when {
        isOverdue -> if (dark) OverdueRedTextDark else OverdueRed
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border = if (isOverdue) BorderStroke(1.dp, fgColor.copy(alpha = 0.5f)) else null

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isOverdue) Icons.Rounded.WarningAmber else Icons.Rounded.CalendarToday,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = DateUtils.formatFollowUpRelative(epochDay, todayEpochDay),
                color = fgColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isOverdue || isToday) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun VehicleConditionPill(
    condition: VehicleCondition,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = condition.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun RequestStatusBadge(
    stage: SalesStage,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg) = when (stage) {
        SalesStage.VENDIDO -> Triple("Approved", StatusApprovedBg, StatusApprovedFg)
        SalesStage.CONTACTADO,
        SalesStage.CITA_PRUEBA,
        SalesStage.COTIZACION,
        SalesStage.NEGOCIACION -> Triple("In Progress", StatusInProgressBg, StatusInProgressFg)
        SalesStage.NUEVO,
        SalesStage.PERDIDO -> Triple("Pending", StatusPendingBg, StatusPendingFg)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
