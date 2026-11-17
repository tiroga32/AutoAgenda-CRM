package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.ClientEditorBottomSheet
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.ClientDetailScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.PipelineBoardScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.AutoAgendaTheme
import com.example.ui.viewmodel.CrmViewModel

private const val ROUTE_TODAY = "today"
private const val ROUTE_CLIENTS = "clients"
private const val ROUTE_PIPELINE = "pipeline"
private const val ROUTE_BACKUP = "backup"
private const val ROUTE_CLIENT_DETAIL = "client_detail/{clientId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoAgendaTheme {
                AutoAgendaApp()
            }
        }
    }
}

@Composable
fun AutoAgendaApp(
    viewModel: CrmViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val todayState by viewModel.todayUiState.collectAsStateWithLifecycle()
    val clientsState by viewModel.clientsUiState.collectAsStateWithLifecycle()
    val pipelineState by viewModel.pipelineUiState.collectAsStateWithLifecycle()
    val backupState by viewModel.backupUiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showQuickAddSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareRequests.collect { request ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = request.mimeType
                putExtra(Intent.EXTRA_STREAM, request.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, request.title))
        }
    }

    val showBottomBar = currentRoute in listOf(ROUTE_TODAY, ROUTE_CLIENTS, ROUTE_PIPELINE)

    fun navigateTop(route: String) {
        if (currentRoute == route) {
            navController.popBackStack(ROUTE_TODAY, inclusive = false)
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateHome() {
        navController.popBackStack(ROUTE_TODAY, inclusive = false)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                CrmThreeColumnBottomBar(
                    currentRoute = currentRoute,
                    onClientsClick = { navigateTop(ROUTE_CLIENTS) },
                    onAddProspectClick = { showQuickAddSheet = true },
                    onMetricsClick = { navigateTop(ROUTE_PIPELINE) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_TODAY
            ) {
                composable(ROUTE_TODAY) {
                    TodayScreen(
                        state = todayState,
                        onClientClick = { id -> navController.navigate("client_detail/$id") },
                        onCompleteTask = viewModel::completeTodayTask,
                        onUpdateGoalTarget = viewModel::setMonthlyGoalTarget,
                        onOpenBackup = { navController.navigate(ROUTE_BACKUP) },
                        onAddClientClick = { showQuickAddSheet = true }
                    )
                }
                composable(ROUTE_CLIENTS) {
                    ClientsScreen(
                        state = clientsState,
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        onStageFilterChange = viewModel::onStageFilterChange,
                        onConditionFilterChange = viewModel::onConditionFilterChange,
                        onClientClick = { id -> navController.navigate("client_detail/$id") },
                        onNavigateHome = ::navigateHome,
                        onOpenBackup = { navController.navigate(ROUTE_BACKUP) }
                    )
                }
                composable(ROUTE_PIPELINE) {
                    PipelineBoardScreen(
                        state = pipelineState,
                        onStageClick = { stage ->
                            viewModel.onStageFilterChange(stage)
                            navigateTop(ROUTE_CLIENTS)
                        },
                        onClientClick = { id -> navController.navigate("client_detail/$id") },
                        onNavigateHome = ::navigateHome,
                        onOpenBackup = { navController.navigate(ROUTE_BACKUP) }
                    )
                }
                composable(ROUTE_BACKUP) {
                    BackupScreen(
                        state = backupState,
                        onExportCsvToUri = { uri -> viewModel.exportCsvToUri(context, uri) },
                        onShareCsv = { viewModel.shareCsv(context) },
                        onSaveBackupToUri = { uri -> viewModel.saveBackupToUri(context, uri) },
                        onShareBackup = { viewModel.shareBackup(context) },
                        onRestoreFromUri = { uri -> viewModel.prepareRestoreFromUri(context, uri) },
                        onConfirmRestore = viewModel::confirmRestore,
                        onDismissRestore = viewModel::dismissRestorePreview,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = ROUTE_CLIENT_DETAIL,
                    arguments = listOf(navArgument("clientId") { type = NavType.LongType })
                ) { entry ->
                    val clientId = entry.arguments?.getLong("clientId") ?: 0L
                    val clientWithNotes by remember(clientId) {
                        viewModel.observeClient(clientId)
                    }.collectAsStateWithLifecycle(initialValue = null)

                    ClientDetailScreen(
                        clientWithNotes = clientWithNotes,
                        onNavigateBack = { navController.popBackStack() },
                        onUpdateClient = viewModel::updateClient,
                        onDeleteClient = { id ->
                            viewModel.deleteClient(id) {
                                navController.popBackStack()
                            }
                        },
                        onAddNote = viewModel::addNote,
                        onChangeActiveStage = viewModel::updateActiveStage,
                        onMarkSold = viewModel::markSold,
                        onMarkLost = viewModel::markLost,
                        onUpdateFollowUp = viewModel::updateFollowUp
                    )
                }
            }
        }
    }

    if (showQuickAddSheet) {
        ClientEditorBottomSheet(
            existingClient = null,
            onDismiss = { showQuickAddSheet = false },
            onSave = { newClient, initialNote ->
                viewModel.createClient(newClient, initialNote) {
                    showQuickAddSheet = false
                }
            }
        )
    }
}

@Composable
private fun CrmThreeColumnBottomBar(
    currentRoute: String?,
    onClientsClick: () -> Unit,
    onAddProspectClick: () -> Unit,
    onMetricsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Left column: Clients
                BottomTabColumn(
                    label = "Clients",
                    selected = currentRoute == ROUTE_CLIENTS,
                    selectedIcon = Icons.Rounded.Group,
                    unselectedIcon = Icons.Outlined.Group,
                    onClick = onClientsClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_clients")
                )

                // Center column: Prominent elevated circular "+" button for "Add Prospect"
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Surface(
                        onClick = onAddProspectClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(52.dp)
                            .offset(y = (-6).dp)
                            .testTag("fab_add_client")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add Prospect",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "Add Prospect",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                }

                // Right column: Metrics
                BottomTabColumn(
                    label = "Metrics",
                    selected = currentRoute == ROUTE_PIPELINE,
                    selectedIcon = Icons.Rounded.BarChart,
                    unselectedIcon = Icons.Outlined.BarChart,
                    onClick = onMetricsClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_pipeline")
                )
            }
        }
    }
}

@Composable
private fun BottomTabColumn(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint
        )
    }
}
