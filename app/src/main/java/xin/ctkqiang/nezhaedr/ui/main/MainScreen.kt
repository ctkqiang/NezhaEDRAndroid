package xin.ctkqiang.nezhaedr.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.nezhaedr.R
import xin.ctkqiang.nezhaedr.containment.SIEMThreatLog
import xin.ctkqiang.nezhaedr.data.local.VisitedSiteRecord
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.service.telemetry.NezhaVpnService
import xin.ctkqiang.nezhaedr.ui.theme.NezhaEDRTheme

private const val CONTRIBUTOR_GITHUB_URL = "https://github.com/placeholder/NezhaEDR-rules"

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = Intent(context, NezhaVpnService::class.java).apply {
                action = NezhaVpnService.ACTION_START_VPN
            }
            context.startService(intent)
        }
    }

    MainScreenContent(
        uiState = uiState,
        onSelectTab = { tab -> viewModel.selectTab(tab) },
        onSelectRule = { rule -> viewModel.selectRule(rule) },
        onClearData = {
            viewModel.clearLocalTelemetryData()
            Toast.makeText(context, context.resources.getString(R.string.data_cleared_toast), Toast.LENGTH_SHORT).show()
        },
        onExportData = {
            val jsonExport = viewModel.exportTelemetryDataJson()
            shareExportData(context, jsonExport)
        },
        onToggleVpn = {
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
        onOpenContributeLink = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(CONTRIBUTOR_GITHUB_URL))
            context.startActivity(intent)
        },
        modifier = modifier
    )
}

private fun shareExportData(context: Context, json: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, json)
        type = "application/json"
    }
    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.btn_export_data))
    context.startActivity(shareIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onSelectTab: (Int) -> Unit = {},
    onSelectRule: (DetectionRule?) -> Unit = {},
    onClearData: () -> Unit = {},
    onExportData: () -> Unit = {},
    onToggleVpn: () -> Unit = {},
    onOpenContributeLink: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.edr_dashboard_title), fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    label = { Text(stringResource(R.string.nav_dashboard)) },
                    icon = { Text("D", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    label = { Text(stringResource(R.string.nav_rules)) },
                    icon = { Text("R", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { onSelectTab(2) },
                    label = { Text(stringResource(R.string.nav_threats)) },
                    icon = { Text("S", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 3,
                    onClick = { onSelectTab(3) },
                    label = { Text(stringResource(R.string.nav_history)) },
                    icon = { Text("H", fontWeight = FontWeight.Bold) }
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
                    0 -> DashboardTabContent(
                        uiState = uiState,
                        onToggleVpn = onToggleVpn,
                        onOpenContributeLink = onOpenContributeLink
                    )
                    1 -> RulesTabContent(
                        uiState = uiState,
                        onSelectRule = onSelectRule
                    )
                    2 -> SiemLogsTabContent(uiState = uiState)
                    3 -> HistoryTabContent(
                        uiState = uiState,
                        onExportData = onExportData,
                        onClearData = { showClearDialog = true }
                    )
                }

                uiState.selectedRule?.let { rule ->
                    RuleDetailDialog(
                        rule = rule,
                        onDismiss = { onSelectRule(null) }
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
fun DashboardTabContent(
    uiState: MainUiState,
    onToggleVpn: () -> Unit,
    onOpenContributeLink: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Open Source & Privacy Guarantee Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.privacy_disclaimer_title),
                    style = MaterialTheme.typography.titleMedium,
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

        // Sentinel VPN Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isVpnActive)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.vpn_service_status),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiState.isVpnActive)
                        stringResource(id = R.string.vpn_status_active)
                    else
                        stringResource(id = R.string.vpn_status_inactive),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onToggleVpn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isVpnActive)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (uiState.isVpnActive)
                            stringResource(id = R.string.btn_disable_vpn)
                        else
                            stringResource(id = R.string.btn_enable_vpn)
                    )
                }
            }
        }

        // Metrics Counters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.active_rules_count),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.rulePackage?.rules?.size ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.total_threats_blocked),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.threatLogs.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Visual Threat Activity Graph Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.threat_activity_graph),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                ThreatActivityGraph(
                    visitedCount = uiState.visitedSites.size,
                    threatCount = uiState.threatLogs.size
                )
            }
        }

        // Contribute Rules Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.contribute_rules_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.contribute_rules_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenContributeLink,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(text = stringResource(id = R.string.btn_contribute))
                }
            }
        }
    }
}

@Composable
fun ThreatActivityGraph(visitedCount: Int, threatCount: Int) {
    val telemetryColor = MaterialTheme.colorScheme.primary
    val threatColor = MaterialTheme.colorScheme.error

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val width = size.width
        val height = size.height
        val barCount = 14
        val barWidth = (width / barCount) - 8.dp.toPx()

        for (i in 0 until barCount) {
            val left = i * (barWidth + 8.dp.toPx())
            val isThreatBar = (i % 5 == 0 && threatCount > 0)
            val barFactor = if (visitedCount > 0) 0.3f + ((i % 4) * 0.15f) else 0.1f
            val barHeight = (height * barFactor).coerceAtMost(height)

            drawRect(
                color = if (isThreatBar) threatColor else telemetryColor,
                topLeft = Offset(left, height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun RulesTabContent(
    uiState: MainUiState,
    onSelectRule: (DetectionRule) -> Unit
) {
    val rules = uiState.rulePackage?.rules ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.rules_header_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (rules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No detection rules loaded.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
    Card(
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
                Text(
                    text = rule.severity,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (rule.severity) {
                        "CRITICAL", "HIGH" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rule.name,
                style = MaterialTheme.typography.titleMedium,
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = rule.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Rule ID: ${rule.id}", fontWeight = FontWeight.SemiBold)
                Text(text = "Category: ${rule.category}")
                Text(text = "Severity: ${rule.severity}")
                Text(text = "Containment Action: ${rule.action.name}")
                Text(text = "Description: ${rule.description}")

                if (rule.condition.targetIps.isNotEmpty() || rule.condition.targetDomains.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.rule_condition_targets),
                        fontWeight = FontWeight.Bold
                    )
                    rule.condition.targetIps.forEach { ip -> Text(text = " • IP: $ip") }
                    rule.condition.targetDomains.forEach { domain -> Text(text = " • Domain: $domain") }
                }

                if (rule.condition.packagePatterns.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.rule_condition_patterns),
                        fontWeight = FontWeight.Bold
                    )
                    rule.condition.packagePatterns.forEach { pattern -> Text(text = " • $pattern") }
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

@Composable
fun SiemLogsTabContent(uiState: MainUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.siem_threat_logs),
            style = MaterialTheme.typography.titleLarge,
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.threatLogs) { log ->
                    SIEMThreatLogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    uiState: MainUiState,
    onExportData: () -> Unit,
    onClearData: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(id = R.string.privacy_disclaimer_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.privacy_disclaimer_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExportData,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(id = R.string.btn_export_data))
            }
            OutlinedButton(
                onClick = onClearData,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(id = R.string.btn_clear_data))
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
    Card(modifier = Modifier.fillMaxWidth()) {
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
fun SIEMThreatLogItem(log: SIEMThreatLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
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
                    SIEMThreatLog(
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
