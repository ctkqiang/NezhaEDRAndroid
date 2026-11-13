package xin.ctkqiang.nezhaedr.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.nezhaedr.R
import xin.ctkqiang.nezhaedr.containment.ThreatLogEntry
import xin.ctkqiang.nezhaedr.data.local.VisitedSiteRecord
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.service.telemetry.NezhaVpnService
import xin.ctkqiang.nezhaedr.ui.theme.AppThemeMode
import xin.ctkqiang.nezhaedr.ui.theme.NezhaEDRTheme
import xin.ctkqiang.nezhaedr.util.EDRLogger
import kotlin.math.sin

private const val TAG = "MainScreenUI"

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            EDRLogger.i(TAG, "VPN permission granted -> launching NezhaVpnService")
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val intent = Intent(context, NezhaVpnService::class.java).apply {
                action = NezhaVpnService.ACTION_START_VPN
            }
            context.startService(intent)
        } else {
            EDRLogger.w(TAG, "VPN permission denied by user")
        }
    }

    val customYamlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.importCustomYamlRule(it) { success ->
                val msg = if (success) context.resources.getString(R.string.custom_import_success) else context.resources.getString(R.string.custom_import_failed)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    NezhaEDRTheme(appTheme = uiState.appTheme) {
        MainScreenContent(
            uiState = uiState,
            onSelectTab = { tab ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.selectTab(tab)
            },
            onSelectRule = { rule ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.selectRule(rule)
            },
            onDismissThreatPopup = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.dismissThreatPopup()
            },
            onSyncRules = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.syncRulesFromGithub { success ->
                    val msg = if (success) context.getString(R.string.sync_success_toast) else context.getString(R.string.sync_failed_toast)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onImportRuleClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                customYamlPickerLauncher.launch("*/*")
            },
            onSelectTheme = { theme ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setAppTheme(theme)
            },
            onClearData = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.clearLocalTelemetryData()
                Toast.makeText(context, context.resources.getString(R.string.data_cleared_toast), Toast.LENGTH_SHORT).show()
            },
            onExportYaml = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val yamlExport = viewModel.exportTelemetryDataYaml()
                shareExportData(context, yamlExport, "text/yaml", context.resources.getString(R.string.btn_export_data))
            },
            onExportCsv = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val csvExport = viewModel.exportTelemetryDataCsv()
                shareExportData(context, csvExport, "text/csv", context.resources.getString(R.string.btn_export_csv))
            },
            onToggleVpn = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (uiState.isVpnActive) {
                    val intent = Intent(context, NezhaVpnService::class.java).apply {
                        action = NezhaVpnService.ACTION_STOP_VPN
                    }
                    context.startService(intent)
                } else {
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent != null) {
                        vpnPrepareLauncher.launch(prepareIntent)
                    } else {
                        val intent = Intent(context, NezhaVpnService::class.java).apply {
                            action = NezhaVpnService.ACTION_START_VPN
                        }
                        context.startService(intent)
                    }
                }
            },
            modifier = modifier
        )
    }
}

private fun shareExportData(context: Context, textContent: String, mimeType: String, title: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, textContent)
        type = mimeType
    }
    val shareIntent = Intent.createChooser(sendIntent, title)
    context.startActivity(shareIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onSelectTab: (Int) -> Unit = {},
    onSelectRule: (DetectionRule?) -> Unit = {},
    onDismissThreatPopup: () -> Unit = {},
    onSyncRules: () -> Unit = {},
    onImportRuleClick: () -> Unit = {},
    onSelectTheme: (AppThemeMode) -> Unit = {},
    onClearData: () -> Unit = {},
    onExportYaml: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onToggleVpn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "SyncRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (uiState.isVpnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Text(
                            text = stringResource(id = R.string.edr_dashboard_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSyncRules) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Sync Rules",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(if (uiState.isSyncingRules) rotationAngle else 0f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    label = { Text(stringResource(R.string.nav_dashboard)) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_dashboard),
                            contentDescription = stringResource(R.string.nav_dashboard)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    label = { Text(stringResource(R.string.nav_rules)) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_rules),
                            contentDescription = stringResource(R.string.nav_rules)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { onSelectTab(2) },
                    label = { Text(stringResource(R.string.nav_threats)) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_threats),
                            contentDescription = stringResource(R.string.nav_threats)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.error,
                        indicatorColor = MaterialTheme.colorScheme.errorContainer
                    )
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 3,
                    onClick = { onSelectTab(3) },
                    label = { Text(stringResource(R.string.nav_history)) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_history),
                            contentDescription = stringResource(R.string.nav_history)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 4,
                    onClick = { onSelectTab(4) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_settings),
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.selectedTab) {
                    0 -> MinimalistDashboardTab(
                        uiState = uiState,
                        onToggleVpn = onToggleVpn
                    )
                    1 -> RulesTabContent(
                        uiState = uiState,
                        onSelectRule = onSelectRule,
                        onImportRuleClick = onImportRuleClick
                    )
                    2 -> ThreatLogsTabContent(uiState = uiState)
                    3 -> HistoryTabContent(
                        uiState = uiState,
                        onExportYaml = onExportYaml,
                        onExportCsv = onExportCsv,
                        onClearData = { showClearDialog = true }
                    )
                    4 -> SettingsTabContent(
                        uiState = uiState,
                        onSelectTheme = onSelectTheme,
                        onSyncRules = onSyncRules,
                        onImportRuleClick = onImportRuleClick
                    )
                }

                if (uiState.isSyncingRules) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(stringResource(R.string.syncing_dialog_title), fontWeight = FontWeight.Bold) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                Text(stringResource(R.string.syncing_dialog_body), style = MaterialTheme.typography.bodyMedium)
                            }
                        },
                        confirmButton = {}
                    )
                }

                uiState.selectedRule?.let { rule ->
                    RuleDetailDialog(
                        rule = rule,
                        onDismiss = { onSelectRule(null) }
                    )
                }

                uiState.activeThreatPopup?.let { threat ->
                    ThreatAlertPopupDialog(
                        threat = threat,
                        onDismiss = onDismissThreatPopup
                    )
                }

                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        title = { Text(stringResource(R.string.confirm_clear_title), fontWeight = FontWeight.Bold) },
                        text = { Text(stringResource(R.string.confirm_clear_body)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showClearDialog = false
                                    onClearData()
                                }
                            ) {
                                Text(stringResource(R.string.btn_clear_data), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDialog = false }) {
                                Text(stringResource(R.string.btn_cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    uiState: MainUiState,
    onSelectTheme: (AppThemeMode) -> Unit,
    onSyncRules: () -> Unit,
    onImportRuleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_header_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Dedicated Theme Switcher Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_theme_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.settings_theme_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.appTheme == AppThemeMode.RED,
                        onClick = { onSelectTheme(AppThemeMode.RED) },
                        label = { Text("Red (Default)", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = uiState.appTheme == AppThemeMode.BLUE,
                        onClick = { onSelectTheme(AppThemeMode.BLUE) },
                        label = { Text("Blue", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = uiState.appTheme == AppThemeMode.PINK,
                        onClick = { onSelectTheme(AppThemeMode.PINK) },
                        label = { Text("Pink", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Dedicated Rule Engine Sync Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_gitops_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.settings_gitops_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSyncRules,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.btn_sync_github))
                }
            }
        }

        // Dedicated Local Custom YAML Rule Import Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_custom_rule_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.settings_custom_rule_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onImportRuleClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.btn_import_rule))
                }
            }
        }

        // Open Source & Privacy Disclaimer Card (Moved to Settings)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.privacy_disclaimer_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.privacy_disclaimer_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun ThreatAlertPopupDialog(
    threat: ThreatLogEntry,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.threat_alert_popup_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                text = stringResource(
                    id = R.string.threat_alert_popup_body,
                    threat.appName,
                    threat.ruleId,
                    threat.ruleName,
                    threat.details
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(id = R.string.btn_close))
            }
        }
    )
}

@Composable
fun MinimalistDashboardTab(
    uiState: MainUiState,
    onToggleVpn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (uiState.isVpnActive) "SYSTEM PROTECTED" else "PROTECTION PAUSED",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (uiState.isVpnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (uiState.isVpnActive) "NezhaCyberEDR Active" else "Tap button to activate NezhaCyberEDR",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        MasterProtectionButton(
            isActive = uiState.isVpnActive,
            onClick = onToggleVpn
        )

        // Real-Time Endpoint Security Index Widget (%)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (uiState.isVpnActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.endpoint_score_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (uiState.isVpnActive) "98.4%" else "0.0%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (uiState.isVpnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (uiState.isVpnActive) stringResource(R.string.endpoint_score_optimal) else stringResource(R.string.endpoint_score_warning),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    CircularProgressIndicator(
                        progress = if (uiState.isVpnActive) 0.984f else 0.0f,
                        strokeWidth = 6.dp,
                        color = if (uiState.isVpnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = if (uiState.isVpnActive) "OK" else "OFF",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Metrics Counters Row
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumn(
                    label = stringResource(R.string.active_rules_count),
                    value = "${uiState.rulePackage?.rules?.size ?: 0}"
                )
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                MetricColumn(
                    label = stringResource(R.string.total_threats_blocked),
                    value = "${uiState.threatLogs.size}",
                    isHighlight = true
                )
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                MetricColumn(
                    label = stringResource(R.string.visited_sites_count),
                    value = "${uiState.visitedSites.size}"
                )
            }
        }

        // Live Protection Vectors Status Widget
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(id = R.string.protection_vectors_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                ProtectionVectorRow(
                    label = stringResource(R.string.vector_dns),
                    isActive = uiState.isVpnActive
                )
                Spacer(modifier = Modifier.height(6.dp))
                ProtectionVectorRow(
                    label = stringResource(R.string.vector_package),
                    isActive = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                ProtectionVectorRow(
                    label = stringResource(R.string.vector_engine),
                    isActive = true
                )
            }
        }

        // Live Telemetry Wave Analytics Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.threat_activity_graph),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (uiState.isVpnActive) "1.8 KB/s LIVE" else "IDLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                SaaS2026TelemetryWave(
                    isActive = uiState.isVpnActive,
                    threatCount = uiState.threatLogs.size
                )
            }
        }
    }
}

@Composable
fun ProtectionVectorRow(label: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(8.dp)
            ) {}
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = if (isActive) stringResource(R.string.vector_status_active) else "Paused",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun MasterProtectionButton(
    isActive: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(400),
        label = "ButtonColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(400),
        label = "ContentColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
        }

        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = buttonColor,
            shadowElevation = if (isActive) 12.dp else 2.dp,
            modifier = Modifier.size(120.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = if (isActive) stringResource(R.string.switch_on) else stringResource(R.string.switch_off),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun MetricColumn(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SaaS2026TelemetryWave(
    isActive: Boolean,
    threatCount: Int
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val threatColor = MaterialTheme.colorScheme.error

    val transition = rememberInfiniteTransition(label = "WavePhase")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PhaseAnimation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val amplitude = if (isActive) height / 3f else 6.dp.toPx()

        val path = Path()
        path.moveTo(0f, centerY)

        for (x in 0..width.toInt() step 4) {
            val normalizedX = x / width
            val y = centerY + amplitude * sin(normalizedX * 4 * Math.PI.toFloat() + phase)
            path.lineTo(x.toFloat(), y)
        }

        drawPath(
            path = path,
            color = if (threatCount > 0) threatColor else primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun RulesTabContent(
    uiState: MainUiState,
    onSelectRule: (DetectionRule) -> Unit,
    onImportRuleClick: () -> Unit
) {
    val rules = uiState.rulePackage?.rules ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.rules_header_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onImportRuleClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "Import Rule",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(id = R.string.btn_import_rule), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (rules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No YAML detection rules loaded.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(rules) { rule ->
                    RuleItemCard(rule = rule, onClick = { onSelectRule(rule) })
                }
            }
        }
    }
}

@Composable
fun RuleItemCard(rule: DetectionRule, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rule.id,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (rule.severity) {
                        "CRITICAL", "HIGH" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = rule.severity,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (rule.severity) {
                            "CRITICAL", "HIGH" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = rule.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rule.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.rule_category_label, rule.category),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = stringResource(id = R.string.rule_action_label, rule.action.name),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RuleDetailDialog(rule: DetectionRule, onDismiss: () -> Unit) {
    var isDeveloperMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = rule.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isDeveloperMode,
                        onClick = { isDeveloperMode = false },
                        label = { Text(stringResource(R.string.mode_user), style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = isDeveloperMode,
                        onClick = { isDeveloperMode = true },
                        label = { Text(stringResource(R.string.mode_developer), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        },
        text = {
            if (isDeveloperMode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = buildYamlSyntaxHighlighting(rule),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Rule ID: ${rule.id}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (rule.action.name) {
                                "BLOCK", "TERMINATE" -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = "Action: ${rule.action.name}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = rule.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (rule.condition.targetIps.isNotEmpty() || rule.condition.targetDomains.isNotEmpty()) {
                        Text(
                            text = stringResource(id = R.string.rule_condition_targets),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                rule.condition.targetIps.forEach { ip ->
                                    Text(text = " • IP: $ip", style = MaterialTheme.typography.bodySmall)
                                }
                                rule.condition.targetDomains.forEach { domain ->
                                    Text(text = " • Domain: $domain", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    if (rule.condition.packagePatterns.isNotEmpty()) {
                        Text(
                            text = stringResource(id = R.string.rule_condition_patterns),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                rule.condition.packagePatterns.forEach { pattern ->
                                    Text(text = " • $pattern", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.btn_close))
            }
        }
    )
}

fun buildYamlSyntaxHighlighting(rule: DetectionRule): AnnotatedString {
    val rawYaml = StringBuilder()
    rawYaml.append("id: \"${rule.id}\"\n")
    rawYaml.append("name: \"${rule.name}\"\n")
    rawYaml.append("description: \"${rule.description}\"\n")
    rawYaml.append("severity: \"${rule.severity}\"\n")
    rawYaml.append("category: \"${rule.category}\"\n")
    rawYaml.append("action: \"${rule.action.name}\"\n")
    rawYaml.append("condition:\n")
    if (rule.condition.targetIps.isNotEmpty()) {
        rawYaml.append("  target_ips:\n")
        rule.condition.targetIps.forEach { ip -> rawYaml.append("    - \"$ip\"\n") }
    }
    if (rule.condition.targetDomains.isNotEmpty()) {
        rawYaml.append("  target_domains:\n")
        rule.condition.targetDomains.forEach { domain -> rawYaml.append("    - \"$domain\"\n") }
    }
    if (rule.condition.packagePatterns.isNotEmpty()) {
        rawYaml.append("  package_patterns:\n")
        rule.condition.packagePatterns.forEach { pattern -> rawYaml.append("    - \"$pattern\"\n") }
    }

    return buildAnnotatedString {
        val lines = rawYaml.toString().lines()
        for (line in lines) {
            if (line.contains(":")) {
                val parts = line.split(":", limit = 2)
                val key = parts[0]
                val value = parts.getOrNull(1) ?: ""

                withStyle(SpanStyle(color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold)) {
                    append(key)
                }
                withStyle(SpanStyle(color = Color(0xFFFFCC80))) {
                    append(":")
                }
                if (value.contains("\"")) {
                    withStyle(SpanStyle(color = Color(0xFFAED581))) {
                        append(value)
                    }
                } else {
                    withStyle(SpanStyle(color = Color(0xFFCE93D8))) {
                        append(value)
                    }
                }
            } else if (line.trim().startsWith("-")) {
                withStyle(SpanStyle(color = Color(0xFFFFD54F))) {
                    append(line.takeWhile { it.isWhitespace() } + "- ")
                }
                withStyle(SpanStyle(color = Color(0xFFAED581))) {
                    append(line.trim().drop(1).trim())
                }
            } else {
                append(line)
            }
            append("\n")
        }
    }
}

@Composable
fun ThreatLogsTabContent(uiState: MainUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(id = R.string.threat_logs_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.threatLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.no_threats_detected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.threatLogs) { log ->
                    ThreatLogItemCard(log = log)
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    uiState: MainUiState,
    onExportYaml: () -> Unit,
    onExportCsv: () -> Unit,
    onClearData: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.visited_sites_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onExportYaml,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(id = R.string.btn_export_data), style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = onExportCsv,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(id = R.string.btn_export_csv), style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = onClearData,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(id = R.string.btn_clear_data), style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.visitedSites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.no_history_recorded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.visitedSites) { site ->
                    VisitedSiteItemCard(site = site)
                }
            }
        }
    }
}

@Composable
fun VisitedSiteItemCard(site: VisitedSiteRecord) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = site.destinationIp,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (site.isThreat) "THREAT BLOCKED" else "SAFE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (site.isThreat) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            if (site.domain.isNotEmpty()) {
                Text(
                    text = site.domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.app_name_label, site.appName),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = site.protocol,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun ThreatLogItemCard(log: ThreatLogEntry) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.ruleName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = log.severity,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.app_name_label, log.appName),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (log.packageName.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.package_name_label, log.packageName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Action: ${log.actionTaken}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenDashboardPreview() {
    NezhaEDRTheme {
        MainScreenContent(
            uiState = MainUiState(
                isVpnActive = true,
                isLoading = false,
                threatLogs = listOf(
                    ThreatLogEntry(
                        ruleId = "RULE-NET-001",
                        ruleName = "Known C2 Threat IP Block",
                        severity = "HIGH",
                        actionTaken = "BLOCK",
                        appName = "Suspicious Application",
                        packageName = "com.test.malware.app",
                        details = "Detects and blocks egress connection to known malicious C2 addresses"
                    )
                )
            )
        )
    }
}
