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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.nezhaedr.R
import xin.ctkqiang.nezhaedr.containment.ThreatLogEntry
import xin.ctkqiang.nezhaedr.data.local.VisitedSiteRecord
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.repository.RemoteRuleCatalogItem
import xin.ctkqiang.nezhaedr.service.telemetry.NezhaVpnService
import xin.ctkqiang.nezhaedr.ui.theme.AppThemeMode
import xin.ctkqiang.nezhaedr.ui.theme.AppleGlassBottom
import xin.ctkqiang.nezhaedr.ui.theme.AppleGlassTop
import xin.ctkqiang.nezhaedr.ui.theme.AppleMutedLabel
import xin.ctkqiang.nezhaedr.ui.theme.AppleOledBlack
import xin.ctkqiang.nezhaedr.ui.theme.AppleRefinedCrimson
import xin.ctkqiang.nezhaedr.ui.theme.AppleSoftEmerald
import xin.ctkqiang.nezhaedr.ui.theme.AppleSpecularBorderBottom
import xin.ctkqiang.nezhaedr.ui.theme.AppleSpecularBorderTop
import xin.ctkqiang.nezhaedr.ui.theme.NezhaEDRTheme
import xin.ctkqiang.nezhaedr.util.EDRLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
            onConfirmDeleteRule = { rule ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.confirmDeleteRule(rule)
            },
            onExecuteDeleteRule = { rule ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.executeDeleteRule(rule) { success ->
                    val msg = if (success) context.resources.getString(R.string.rule_deleted_toast) else "Failed to delete rule"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismissThreatPopup = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.dismissThreatPopup()
            },
            onOpenSyncPicker = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.openSyncPickerDialog()
            },
            onDismissSyncPicker = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.dismissSyncPickerDialog()
            },
            onToggleRemoteRuleSelection = { fileName ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.toggleRemoteRuleSelection(fileName)
            },
            onSelectAllRemoteRules = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.selectAllRemoteRules()
            },
            onDeselectAllRemoteRules = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.deselectAllRemoteRules()
            },
            onSyncSelectedRules = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.syncSelectedRulesFromGithub { success ->
                    val msg = if (success) context.resources.getString(R.string.sync_success_toast) else context.resources.getString(R.string.sync_failed_toast)
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
    onConfirmDeleteRule: (DetectionRule?) -> Unit = {},
    onExecuteDeleteRule: (DetectionRule) -> Unit = {},
    onDismissThreatPopup: () -> Unit = {},
    onOpenSyncPicker: () -> Unit = {},
    onDismissSyncPicker: () -> Unit = {},
    onToggleRemoteRuleSelection: (String) -> Unit = {},
    onSelectAllRemoteRules: () -> Unit = {},
    onDeselectAllRemoteRules: () -> Unit = {},
    onSyncSelectedRules: () -> Unit = {},
    onImportRuleClick: () -> Unit = {},
    onSelectTheme: (AppThemeMode) -> Unit = {},
    onClearData: () -> Unit = {},
    onExportYaml: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onToggleVpn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleOledBlack)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Section 1: Apple-Grade Minimalist Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NEZHA EDR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                    Text(
                        text = "ZERO-TELEMETRY LOCAL DEFENSE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = AppleMutedLabel
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onOpenSyncPicker) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = "Sync Rules",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Main Scrollable Area with Spring Dynamics
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState.selectedTab) {
                    0 -> item {
                        MinimalistDashboardTab(
                            uiState = uiState,
                            onToggleVpn = onToggleVpn,
                            onSelectTab = onSelectTab
                        )
                    }
                    1 -> item {
                        RulesTabContent(
                            uiState = uiState,
                            onSelectRule = onSelectRule,
                            onConfirmDeleteRule = onConfirmDeleteRule,
                            onImportRuleClick = onImportRuleClick,
                            onOpenSyncPicker = onOpenSyncPicker
                        )
                    }
                    2 -> item { ThreatLogsTabContent(uiState = uiState) }
                    3 -> item {
                        HistoryTabContent(
                            uiState = uiState,
                            onExportYaml = onExportYaml,
                            onExportCsv = onExportCsv,
                            onClearData = { showClearDialog = true }
                        )
                    }
                    4 -> item {
                        SettingsTabContent(
                            uiState = uiState,
                            onSelectTheme = onSelectTheme,
                            onOpenSyncPicker = onOpenSyncPicker,
                            onImportRuleClick = onImportRuleClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section 5: Floating Specular Glass Navigation Bar
            AppleFloatingNavigationBar(
                selectedTab = uiState.selectedTab,
                onSelectTab = onSelectTab
            )
        }

        // Modals & Dialogs
        if (uiState.showSyncPickerDialog) {
            SyncRulesPickerDialog(
                availableCatalog = uiState.availableRemoteCatalog,
                selectedFiles = uiState.selectedRemoteFilesToSync,
                onToggleSelection = onToggleRemoteRuleSelection,
                onSelectAll = onSelectAllRemoteRules,
                onDeselectAll = onDeselectAllRemoteRules,
                onSyncSelected = onSyncSelectedRules,
                onDismiss = onDismissSyncPicker
            )
        }

        uiState.deleteRuleConfirm?.let { rule ->
            AlertDialog(
                onDismissRequest = { onConfirmDeleteRule(null) },
                containerColor = Color(0xFF141418),
                title = { Text(stringResource(R.string.delete_rule_title), fontWeight = FontWeight.Bold, color = Color.White) },
                text = { Text(stringResource(R.string.delete_rule_body, rule.id, rule.name), color = AppleMutedLabel) },
                confirmButton = {
                    Button(
                        onClick = { onExecuteDeleteRule(rule) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleRefinedCrimson)
                    ) {
                        Text(stringResource(R.string.btn_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onConfirmDeleteRule(null) }) {
                        Text(stringResource(R.string.btn_cancel), color = Color.White.copy(alpha = 0.7f))
                    }
                }
            )
        }

        if (uiState.isSyncingRules) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = Color(0xFF141418),
                title = { Text(stringResource(R.string.syncing_dialog_title), fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp), color = AppleSoftEmerald)
                        Text(stringResource(R.string.syncing_dialog_body), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
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
                containerColor = Color(0xFF141418),
                title = { Text(stringResource(R.string.confirm_clear_title), fontWeight = FontWeight.Bold, color = Color.White) },
                text = { Text(stringResource(R.string.confirm_clear_body), color = AppleMutedLabel) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            onClearData()
                        }
                    ) {
                        Text(stringResource(R.string.btn_clear_data), color = AppleRefinedCrimson)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(stringResource(R.string.btn_cancel), color = Color.White.copy(alpha = 0.7f))
                    }
                }
            )
        }
    }
}

// Specular Glass Card Container Component
@Composable
fun AppleGlassCard(
    modifier: Modifier = Modifier,
    borderColorTop: Color = AppleSpecularBorderTop,
    borderColorBottom: Color = AppleSpecularBorderBottom,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                colors = listOf(borderColorTop, borderColorBottom)
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AppleGlassTop, AppleGlassBottom)
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        content = content
    )
}

// Section 2: Master Status Control Card
@Composable
fun AppleMasterStatusCard(
    isActive: Boolean,
    blockedAttemptsCount: Int,
    onToggleVpn: () -> Unit
) {
    val stateColor by animateColorAsState(
        targetValue = if (isActive) AppleSoftEmerald else AppleMutedLabel,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "StateColorAnimation"
    )

    AppleGlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
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
                        color = stateColor,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Text(
                        text = if (isActive) "LOCAL DEFENSE ENGAGED" else "LOCAL DEFENSE PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = stateColor
                    )
                }

                // Native High-End Switch
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggleVpn() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AppleSoftEmerald,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Massive Light-Weight Intercept Typography
            Text(
                text = "$blockedAttemptsCount",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 64.sp,
                    lineHeight = 68.sp
                ),
                fontWeight = FontWeight.Light,
                color = if (blockedAttemptsCount > 0) AppleRefinedCrimson else Color.White
            )

            Text(
                text = "INTERCEPTED THREAT ATTEMPTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = AppleMutedLabel
            )
        }
    }
}

// Section 3: YAML Engine & Rule Quick Status Card
@Composable
fun AppleRuleEngineStatusCard(
    activeRulesCount: Int,
    onManageClick: () -> Unit
) {
    AppleGlassCard(
        modifier = Modifier.clickable { onManageClick() }
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
                    text = "YAML EVALUATION ENGINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AppleMutedLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$activeRulesCount Active Rulesets Loaded",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Text(
                text = "Manage →",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Section 4: Threat Intercept Trail Feed Card
@Composable
fun AppleThreatTrailCard(
    blockedSitesTrail: List<VisitedSiteRecord>
) {
    AppleGlassCard(
        borderColorTop = AppleRefinedCrimson.copy(alpha = 0.3f),
        borderColorBottom = AppleSpecularBorderBottom
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTERCEPTED THREAT TRAIL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AppleRefinedCrimson
                )
                Text(
                    text = "REAL-TIME AUDIT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppleRefinedCrimson
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (blockedSitesTrail.isEmpty()) {
                Text(
                    text = "No intercepted banned connection attempts recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleMutedLabel
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    blockedSitesTrail.take(4).forEach { item ->
                        AppleThreatItemRow(record = item)
                    }
                }
            }
        }
    }
}

@Composable
fun AppleThreatItemRow(record: VisitedSiteRecord) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = remember(record.timestamp) { formatter.format(Date(record.timestamp)) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = record.destinationIp,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleRefinedCrimson
                    )
                    if (record.domain.isNotEmpty()) {
                        Text(
                            text = "(${record.domain})",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleMutedLabel
                        )
                    }
                }
                Text(
                    text = "$timeStr • ${record.protocol}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleMutedLabel
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = AppleRefinedCrimson
            ) {
                Text(
                    text = "BLOCKED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// Section 5: Floating Specular Glass Navigation Bar
@Composable
fun AppleFloatingNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                colors = listOf(AppleSpecularBorderTop, AppleSpecularBorderBottom)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AppleGlassTop, AppleGlassBottom)
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                label = { Text(stringResource(R.string.nav_dashboard), style = MaterialTheme.typography.labelSmall) },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_dashboard),
                        contentDescription = stringResource(R.string.nav_dashboard)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.White.copy(alpha = 0.1f)
                )
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                label = { Text(stringResource(R.string.nav_rules), style = MaterialTheme.typography.labelSmall) },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_rules),
                        contentDescription = stringResource(R.string.nav_rules)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.White.copy(alpha = 0.1f)
                )
            )
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { onSelectTab(2) },
                label = { Text(stringResource(R.string.nav_threats), style = MaterialTheme.typography.labelSmall) },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_threats),
                        contentDescription = stringResource(R.string.nav_threats)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppleRefinedCrimson,
                    indicatorColor = AppleRefinedCrimson.copy(alpha = 0.2f)
                )
            )
            NavigationBarItem(
                selected = selectedTab == 3,
                onClick = { onSelectTab(3) },
                label = { Text(stringResource(R.string.nav_history), style = MaterialTheme.typography.labelSmall) },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_history),
                        contentDescription = stringResource(R.string.nav_history)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.White.copy(alpha = 0.1f)
                )
            )
            NavigationBarItem(
                selected = selectedTab == 4,
                onClick = { onSelectTab(4) },
                label = { Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.labelSmall) },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_settings),
                        contentDescription = stringResource(R.string.nav_settings)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
fun SyncRulesPickerDialog(
    availableCatalog: List<RemoteRuleCatalogItem>,
    selectedFiles: Set<String>,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onSyncSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(availableCatalog, searchQuery) {
        if (searchQuery.isBlank()) availableCatalog else {
            val q = searchQuery.lowercase()
            availableCatalog.filter {
                it.name.lowercase().contains(q) || it.ruleId.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141418),
        title = {
            Text(
                text = stringResource(id = R.string.sync_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_rules_placeholder), style = MaterialTheme.typography.bodySmall, color = AppleMutedLabel) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = null,
                            tint = AppleMutedLabel,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSelectAll) {
                        Text(stringResource(R.string.btn_select_all), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = onDeselectAll) {
                        Text(stringResource(R.string.btn_deselect_all), style = MaterialTheme.typography.labelSmall, color = AppleMutedLabel)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSelection(item.fileName) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedFiles.contains(item.fileName),
                                onCheckedChange = { onToggleSelection(item.fileName) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "${item.fileName} • ${item.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Category: ${item.category} | Severity: ${item.severity}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleMutedLabel
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSyncSelected,
                shape = RoundedCornerShape(12.dp),
                enabled = selectedFiles.isNotEmpty()
            ) {
                Text(stringResource(R.string.btn_sync_selected, selectedFiles.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = AppleMutedLabel)
            }
        }
    )
}

@Composable
fun SettingsTabContent(
    uiState: MainUiState,
    onSelectTheme: (AppThemeMode) -> Unit,
    onOpenSyncPicker: () -> Unit,
    onImportRuleClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_header_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Theme Switcher Card
        AppleGlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_theme_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.settings_theme_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleMutedLabel
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.appTheme == AppThemeMode.RED,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTheme(AppThemeMode.RED)
                        },
                        label = { Text("Red (Default)", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = uiState.appTheme == AppThemeMode.BLUE,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTheme(AppThemeMode.BLUE)
                        },
                        label = { Text("Blue", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = uiState.appTheme == AppThemeMode.PINK,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTheme(AppThemeMode.PINK)
                        },
                        label = { Text("Pink", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Rule Engine Sync Card
        AppleGlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_gitops_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.settings_gitops_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleMutedLabel
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenSyncPicker()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.btn_sync_github))
                }
            }
        }

        // Local Custom YAML Rule Import Card
        AppleGlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_custom_rule_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.settings_custom_rule_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleMutedLabel
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onImportRuleClick()
                    },
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

        // Open Source & Privacy Disclaimer Card
        AppleGlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.privacy_disclaimer_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.privacy_disclaimer_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleMutedLabel
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
        containerColor = Color(0xFF141418),
        title = {
            Text(
                text = stringResource(id = R.string.threat_alert_popup_title),
                fontWeight = FontWeight.Bold,
                color = AppleRefinedCrimson
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
                ),
                color = Color.White.copy(alpha = 0.85f)
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppleRefinedCrimson
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
    onToggleVpn: () -> Unit,
    onSelectTab: (Int) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 2: Master Status Control Specular Glass Card
        AppleMasterStatusCard(
            isActive = uiState.isVpnActive,
            blockedAttemptsCount = uiState.blockedAttemptsCount,
            onToggleVpn = onToggleVpn
        )

        // Section 3: YAML Engine Status Card
        AppleRuleEngineStatusCard(
            activeRulesCount = uiState.rulePackage?.rules?.size ?: 0,
            onManageClick = { onSelectTab(1) }
        )

        // Section 4: Threat Intercept Trail Card
        AppleThreatTrailCard(
            blockedSitesTrail = uiState.blockedSitesTrail
        )
    }
}

@Composable
fun RulesTabContent(
    uiState: MainUiState,
    onSelectRule: (DetectionRule) -> Unit,
    onConfirmDeleteRule: (DetectionRule?) -> Unit,
    onImportRuleClick: () -> Unit,
    onOpenSyncPicker: () -> Unit
) {
    val rules = uiState.rulePackage?.rules ?: emptyList()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredRules = remember(rules, searchQuery, selectedCategory) {
        rules.filter { rule ->
            val matchesCategory = (selectedCategory == "ALL" || rule.category.equals(selectedCategory, ignoreCase = true))
            val matchesQuery = searchQuery.isBlank() || (
                rule.name.lowercase().contains(searchQuery.lowercase()) ||
                rule.id.lowercase().contains(searchQuery.lowercase()) ||
                rule.description.lowercase().contains(searchQuery.lowercase())
            )
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.rules_header_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
                Button(
                    onClick = onOpenSyncPicker,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = "Sync",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Sync", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_rules_placeholder), style = MaterialTheme.typography.bodySmall, color = AppleMutedLabel) },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = null,
                    tint = AppleMutedLabel,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("ALL", "NETWORK", "PACKAGE", "PROCESS")) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (filteredRules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No YAML detection rules match search criteria.", color = AppleMutedLabel)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredRules.forEach { rule ->
                    RuleItemCard(
                        rule = rule,
                        onClick = { onSelectRule(rule) },
                        onDeleteClick = { onConfirmDeleteRule(rule) }
                    )
                }
            }
        }
    }
}

@Composable
fun RuleItemCard(
    rule: DetectionRule,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AppleGlassCard(
        modifier = Modifier.clickable { onClick() }
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (rule.severity) {
                            "CRITICAL", "HIGH" -> AppleRefinedCrimson.copy(alpha = 0.2f)
                            else -> Color.White.copy(alpha = 0.1f)
                        }
                    ) {
                        Text(
                            text = rule.severity,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = when (rule.severity) {
                                "CRITICAL", "HIGH" -> AppleRefinedCrimson
                                else -> Color.White.copy(alpha = 0.8f)
                            }
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Delete Rule",
                            tint = AppleRefinedCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = rule.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rule.description,
                style = MaterialTheme.typography.bodySmall,
                color = AppleMutedLabel
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.rule_category_label, rule.category),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleMutedLabel
                )
                Text(
                    text = stringResource(id = R.string.rule_action_label, rule.action.name),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
        containerColor = Color(0xFF141418),
        title = {
            Column {
                Text(text = rule.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
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
                    color = Color(0xFF09090B),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
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
                                "BLOCK", "TERMINATE" -> AppleRefinedCrimson.copy(alpha = 0.2f)
                                else -> Color.White.copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = "Action: ${rule.action.name}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = rule.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleMutedLabel
                    )

                    if (rule.condition.targetIps.isNotEmpty() || rule.condition.targetDomains.isNotEmpty()) {
                        Text(
                            text = stringResource(id = R.string.rule_condition_targets),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        AppleGlassCard {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                rule.condition.targetIps.forEach { ip ->
                                    Text(text = " • IP: $ip", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                                }
                                rule.condition.targetDomains.forEach { domain ->
                                    Text(text = " • Domain: $domain", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                                }
                            }
                        }
                    }

                    if (rule.condition.packagePatterns.isNotEmpty()) {
                        Text(
                            text = stringResource(id = R.string.rule_condition_patterns),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        AppleGlassCard {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                rule.condition.packagePatterns.forEach { pattern ->
                                    Text(text = " • $pattern", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.btn_close), color = Color.White)
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
    var selectedSeverity by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(uiState.threatLogs, selectedSeverity) {
        if (selectedSeverity == "ALL") uiState.threatLogs else {
            uiState.threatLogs.filter { it.severity.equals(selectedSeverity, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.threat_logs_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("ALL", "CRITICAL", "HIGH", "MEDIUM", "LOG")) { sev ->
                FilterChip(
                    selected = selectedSeverity == sev,
                    onClick = { selectedSeverity = sev },
                    label = { Text(sev, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.no_threats_detected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppleMutedLabel
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredLogs.forEach { log ->
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
    var filterBlockedOnly by remember { mutableStateOf(false) }

    val displayList = remember(uiState.visitedSites, filterBlockedOnly) {
        if (filterBlockedOnly) uiState.visitedSites.filter { it.isThreat } else uiState.visitedSites
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.visited_sites_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !filterBlockedOnly,
                    onClick = { filterBlockedOnly = false },
                    label = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = filterBlockedOnly,
                    onClick = { filterBlockedOnly = true },
                    label = { Text(stringResource(R.string.filter_blocked), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

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
                    contentColor = AppleRefinedCrimson
                )
            ) {
                Text(text = stringResource(id = R.string.btn_clear_data), style = MaterialTheme.typography.labelSmall)
            }
        }

        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.no_history_recorded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppleMutedLabel
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayList.forEach { site ->
                    VisitedSiteItemCard(site = site)
                }
            }
        }
    }
}

@Composable
fun VisitedSiteItemCard(site: VisitedSiteRecord) {
    AppleGlassCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = site.destinationIp,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (site.isThreat) "THREAT BLOCKED" else "SAFE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (site.isThreat) AppleRefinedCrimson else AppleSoftEmerald
                )
            }
            if (site.domain.isNotEmpty()) {
                Text(
                    text = site.domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleMutedLabel
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.app_name_label, site.appName),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleMutedLabel
                )
                Text(
                    text = site.protocol,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleMutedLabel
                )
            }
        }
    }
}

@Composable
fun ThreatLogItemCard(log: ThreatLogEntry) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppleRefinedCrimson.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, AppleRefinedCrimson.copy(alpha = 0.3f)),
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
                    color = AppleRefinedCrimson
                )
                Text(
                    text = log.severity,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppleRefinedCrimson
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.app_name_label, log.appName),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (log.packageName.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.package_name_label, log.packageName),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleMutedLabel
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Action: ${log.actionTaken}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppleRefinedCrimson
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
                        ruleId = "c2_threat_block",
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
