package xin.ctkqiang.nezhaedr.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezhaedr.containment.ContainmentManager
import xin.ctkqiang.nezhaedr.data.local.TelemetryDatabaseHelper
import xin.ctkqiang.nezhaedr.data.local.VisitedSiteRecord
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.repository.RuleRepository
import xin.ctkqiang.nezhaedr.data.repository.RuleRepositoryImpl
import xin.ctkqiang.nezhaedr.engine.EDREvaluationEngine
import xin.ctkqiang.nezhaedr.service.telemetry.NezhaVpnService
import xin.ctkqiang.nezhaedr.service.telemetry.PackageTelemetryReceiver
import xin.ctkqiang.nezhaedr.util.EDRLogger

class MainViewModel(
    application: Application,
    private val ruleRepository: RuleRepository = RuleRepositoryImpl(),
    private val evaluationEngine: EDREvaluationEngine = EDREvaluationEngine(),
    private val containmentManager: ContainmentManager = ContainmentManager(application),
    private val dbHelper: TelemetryDatabaseHelper = TelemetryDatabaseHelper(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState(isLoading = true))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        EDRLogger.i(TAG, "MainViewModel: Initializing state observers and loading rules")
        observeVpnState()
        observeTelemetry()
        observeThreatLogs()
        loadRules()
        loadVisitedSitesFromDb()
    }

    fun selectTab(tabIndex: Int) {
        EDRLogger.d(TAG, "selectTab: User selected tab index $tabIndex")
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectRule(rule: DetectionRule?) {
        EDRLogger.d(TAG, "selectRule: Selected rule '${rule?.id ?: "none"}'")
        _uiState.update { it.copy(selectedRule = rule) }
    }

    fun dismissThreatPopup() {
        _uiState.update { it.copy(activeThreatPopup = null) }
    }

    fun clearLocalTelemetryData() {
        EDRLogger.w(TAG, "clearLocalTelemetryData: Clearing local SQLite telemetry history")
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.clearAllVisitedSites()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(visitedSites = emptyList()) }
                EDRLogger.i(TAG, "clearLocalTelemetryData: Telemetry state cleared successfully")
            }
        }
    }

    fun exportTelemetryDataYaml(): String {
        EDRLogger.i(TAG, "exportTelemetryDataYaml: Exporting telemetry data to YAML")
        return dbHelper.exportVisitedSitesToYaml()
    }

    fun exportTelemetryDataCsv(): String {
        EDRLogger.i(TAG, "exportTelemetryDataCsv: Exporting telemetry data to CSV")
        return dbHelper.exportVisitedSitesToCsv()
    }

    private fun loadVisitedSitesFromDb() {
        EDRLogger.d(TAG, "loadVisitedSitesFromDb: Querying SQLite database")
        viewModelScope.launch(Dispatchers.IO) {
            val sites = dbHelper.getAllVisitedSites()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(visitedSites = sites) }
                EDRLogger.i(TAG, "loadVisitedSitesFromDb: Loaded ${sites.size} records into state")
            }
        }
    }

    private fun recordVisitedSite(record: VisitedSiteRecord) {
        EDRLogger.v(TAG, "recordVisitedSite: Recording IP '${record.destinationIp}' into SQLite")
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.insertVisitedSite(record)
            val updatedList = dbHelper.getAllVisitedSites()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(visitedSites = updatedList) }
            }
        }
    }

    private fun loadRules() {
        EDRLogger.i(TAG, "loadRules: Fetching rulesets from repository")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = ruleRepository.fetchRules()
            result.getOrNull()?.let { rules ->
                evaluationEngine.updateRules(rules)
                _uiState.update { it.copy(rulePackage = rules, isLoading = false) }
                EDRLogger.i(TAG, "loadRules: Successfully loaded ${rules.rules.size} rules into engine")
            } ?: run {
                EDRLogger.e(TAG, "loadRules: Failed to load rules package")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun observeVpnState() {
        viewModelScope.launch {
            NezhaVpnService.vpnState.collect { isActive ->
                EDRLogger.i(TAG, "observeVpnState: VPN active state changed to $isActive")
                _uiState.update { it.copy(isVpnActive = isActive) }
            }
        }
    }

    private fun observeTelemetry() {
        viewModelScope.launch {
            NezhaVpnService.telemetryFlow.collect { netEvent ->
                EDRLogger.v(TAG, "observeTelemetry: Captured network event for IP '${netEvent.destinationIp}'")
                val match = evaluationEngine.evaluateNetworkEvent(netEvent)
                val isThreat = match != null
                if (match != null) {
                    EDRLogger.w(TAG, "observeTelemetry: Threat match detected for IP '${netEvent.destinationIp}' -> Rule ${match.rule.id}")
                    containmentManager.handleRuleMatch(match)
                }

                recordVisitedSite(
                    VisitedSiteRecord(
                        destinationIp = netEvent.destinationIp,
                        protocol = netEvent.protocol,
                        isThreat = isThreat,
                        appName = "Network Egress Telemetry"
                    )
                )
            }
        }
        viewModelScope.launch {
            PackageTelemetryReceiver.packageEvents.collect { pkgEvent ->
                EDRLogger.d(TAG, "observeTelemetry: Captured package event for '${pkgEvent.packageName}'")
                val match = evaluationEngine.evaluatePackageEvent(pkgEvent)
                if (match != null) {
                    EDRLogger.w(TAG, "observeTelemetry: Threat match detected for package '${pkgEvent.packageName}' -> Rule ${match.rule.id}")
                    containmentManager.handleRuleMatch(match)
                }
            }
        }
    }

    private fun observeThreatLogs() {
        viewModelScope.launch {
            containmentManager.threatLogs.collect { log ->
                EDRLogger.i(TAG, "observeThreatLogs: New threat log entry '${log.ruleId}'")
                _uiState.update { current ->
                    current.copy(
                        threatLogs = listOf(log) + current.threatLogs,
                        activeThreatPopup = log
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(application) as T
        }
    }
}
