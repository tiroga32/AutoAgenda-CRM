package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.SalesStage
import com.example.data.VehicleCondition
import com.example.ui.components.ClientCard
import com.example.ui.viewmodel.ClientsUiState

import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup

@Composable
fun ClientsScreen(
    state: ClientsUiState,
    onSearchQueryChange: (String) -> Unit,
    onStageFilterChange: (SalesStage?) -> Unit,
    onConditionFilterChange: (VehicleCondition?) -> Unit,
    onClientClick: (Long) -> Unit,
    onNavigateHome: () -> Unit,
    onOpenBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateHome,
                            modifier = Modifier.testTag("clients_back_home")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Volver al inicio"
                            )
                        }
                        Text(
                            text = "Clients",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${state.filteredClients.size} de ${state.totalClientsCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = onOpenBackup) {
                            Icon(
                                imageVector = Icons.Rounded.Backup,
                                contentDescription = "Respaldo y Exportar CSV"
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar por nombre, teléfono o modelo...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = "Buscar")
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clients_search_input")
                )
            }

            // Stage filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedStage == null,
                        onClick = { onStageFilterChange(null) },
                        label = { Text("Todas (${state.totalClientsCount})") },
                        modifier = Modifier.testTag("filter_stage_all")
                    )
                }
                items(SalesStage.entries) { stage ->
                    val count = state.stageCounts[stage] ?: 0
                    FilterChip(
                        selected = state.selectedStage == stage,
                        onClick = {
                            onStageFilterChange(if (state.selectedStage == stage) null else stage)
                        },
                        label = { Text("${stage.shortName} ($count)") },
                        modifier = Modifier.testTag("filter_stage_${stage.name}")
                    )
                }
            }

            // Vehicle condition filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCondition == null,
                        onClick = { onConditionFilterChange(null) },
                        label = { Text("Todos los autos") }
                    )
                }
                items(VehicleCondition.entries) { condition ->
                    FilterChip(
                        selected = state.selectedCondition == condition,
                        onClick = {
                            onConditionFilterChange(
                                if (state.selectedCondition == condition) null else condition
                            )
                        },
                        label = { Text(condition.displayName) },
                        modifier = Modifier.testTag("filter_condition_${condition.name}")
                    )
                }
            }

            if (state.filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.height(48.dp)
                        )
                        Text(
                            text = if (state.totalClientsCount == 0) {
                                "Aún no tienes clientes registrados"
                            } else {
                                "Sin resultados con esos filtros"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (state.totalClientsCount == 0) {
                                "Toca el botón + para registrar tu primer prospecto."
                            } else {
                                "Prueba borrando la búsqueda o cambiando la etapa."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("clients_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredClients, key = { it.client.id }) { item ->
                        ClientCard(
                            item = item,
                            onClick = { onClientClick(item.client.id) }
                        )
                    }
                }
            }
        }
    }
}
