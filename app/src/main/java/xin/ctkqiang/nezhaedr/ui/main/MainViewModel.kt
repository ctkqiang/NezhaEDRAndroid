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
        observeVpnState()
        observeTelemetry()
        observeSiemLogs()
        loadRules()
        loadVisitedSitesFromDb()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectRule(rule: DetectionRule?) {
        _uiState.update { it.copy(selectedRule = rule) }
    }

    fun clearLocalTelemetryData() {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.clearAllVisitedSites()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(visitedSites = emptyList()) }
            }
        }
    }

    fun exportTelemetryDataJson(): String {
        return dbHelper.exportVisitedSitesToJson()
    }

    private fun loadVisitedSitesFromDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val sites = dbHelper.getAllVisitedSites()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(visitedSites = sites) }
            }
        }
    }

    private fun recordVisitedSite(record: VisitedSiteRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.insertVisitedSite(record)
            val updatedList = dbHelper.getAllVisitedSites()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(visitedSites = updatedList) }
            }
        }
    }

    private fun loadRules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = ruleRepository.fetchRules()
            result.getOrNull()?.let { rules ->
                evaluationEngine.updateRules(rules)
                _uiState.update { it.copy(rulePackage = rules, isLoading = false) }
            } ?: run {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun observeVpnState() {
        viewModelScope.launch {
            NezhaVpnService.vpnState.collect { isActive ->
                _uiState.update { it.copy(isVpnActive = isActive) }
            }
        }
    }

    private fun observeTelemetry() {
        viewModelScope.launch {
            NezhaVpnService.telemetryFlow.collect { netEvent ->
                val match = evaluationEngine.evaluateNetworkEvent(netEvent)
                val isThreat = match != null
                if (match != null) {
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
                val match = evaluationEngine.evaluatePackageEvent(pkgEvent)
                if (match != null) {
                    containmentManager.handleRuleMatch(match)
                }
            }
        }
    }

    private fun observeSiemLogs() {
        viewModelScope.launch {
            containmentManager.siemLogs.collect { log ->
                _uiState.update { current ->
                    current.copy(threatLogs = listOf(log) + current.threatLogs)
                }
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(application) as T
        }
    }
}
