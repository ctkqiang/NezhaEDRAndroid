package xin.ctkqiang.nezhaedr.ui.main

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
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
import xin.ctkqiang.nezhaedr.ui.theme.AppThemeMode
import xin.ctkqiang.nezhaedr.ui.theme.ThemeDarkLightMode
import xin.ctkqiang.nezhaedr.util.EDRLogger
import xin.ctkqiang.nezhaedr.util.LocaleHelper

class MainViewModel(
    application: Application,
    private val ruleRepository: RuleRepository = RuleRepositoryImpl(application),
    private val evaluationEngine: EDREvaluationEngine = EDREvaluationEngine(),
    private val containmentManager: ContainmentManager = ContainmentManager(application),
    private val dbHelper: TelemetryDatabaseHelper = TelemetryDatabaseHelper(application)
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("nezha_edr_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        MainUiState(
            isLoading = true,
            appTheme = loadSavedTheme(),
            darkLightMode = loadSavedDarkLightMode(),
            appLanguageCode = loadSavedLanguage()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        EDRLogger.i(TAG, "MainViewModel: Initializing state observers and loading rules")
        observeVpnState()
        observeTelemetry()
        observeThreatLogs()
        loadRules()
        loadVisitedSitesFromDb()
    }

    fun setAppTheme(theme: AppThemeMode) {
        EDRLogger.i(TAG, "setAppTheme: Changing app theme mode to $theme")
        prefs.edit().putString("app_theme_mode", theme.name).apply()
        _uiState.update { it.copy(appTheme = theme) }
    }

    fun setDarkLightMode(mode: ThemeDarkLightMode) {
        EDRLogger.i(TAG, "setDarkLightMode: Changing dark/light mode to $mode")
        prefs.edit().putString("dark_light_mode", mode.name).apply()
        _uiState.update { it.copy(darkLightMode = mode) }
    }

    fun toggleVpnState() {
        _uiState.update { current ->
            val nextState = !current.isVpnActive
            EDRLogger.i(TAG, "toggleVpnState: User toggled protection active state to $nextState")
            current.copy(isVpnActive = nextState)
        }
    }

    private fun loadSavedTheme(): AppThemeMode {
        val savedName = prefs.getString("app_theme_mode", AppThemeMode.RED.name)
        return try {
            AppThemeMode.valueOf(savedName ?: AppThemeMode.RED.name)
        } catch (e: Exception) {
            AppThemeMode.RED
        }
    }

    private fun loadSavedDarkLightMode(): ThemeDarkLightMode {
        val savedName = prefs.getString("dark_light_mode", ThemeDarkLightMode.AUTO.name)
        return try {
            ThemeDarkLightMode.valueOf(savedName ?: ThemeDarkLightMode.AUTO.name)
        } catch (e: Exception) {
            ThemeDarkLightMode.AUTO
        }
    }

    fun setAppLanguage(context: Context, languageCode: String) {
        EDRLogger.i(TAG, "setAppLanguage: Changing app language code to $languageCode")
        LocaleHelper.applyLanguage(context, languageCode)
        _uiState.update { current ->
            current.copy(
                appLanguageCode = languageCode,
                languageRecomposeKey = current.languageRecomposeKey + 1
            )
        }
        (context as? Activity)?.recreate()
    }

    private fun loadSavedLanguage(): String {
        return LocaleHelper.getSavedLanguage(getApplication())
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

    fun openSyncPickerDialog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingRules = true) }
            val catalog = ruleRepository.fetchRemoteGithubCatalog()
            _uiState.update {
                it.copy(
                    showSyncPickerDialog = true,
                    isSyncingRules = false,
                    availableRemoteCatalog = catalog,
                    selectedRemoteFilesToSync = catalog.map { item -> item.fileName }.toSet()
                )
            }
        }
    }

    fun dismissSyncPickerDialog() {
        _uiState.update { it.copy(showSyncPickerDialog = false) }
    }

    fun toggleRemoteRuleSelection(fileName: String) {
        _uiState.update { current ->
            val set = current.selectedRemoteFilesToSync.toMutableSet()
            if (set.contains(fileName)) set.remove(fileName) else set.add(fileName)
            current.copy(selectedRemoteFilesToSync = set)
        }
    }

    fun selectAllRemoteRules() {
        _uiState.update { current ->
            current.copy(selectedRemoteFilesToSync = current.availableRemoteCatalog.map { it.fileName }.toSet())
        }
    }

    fun deselectAllRemoteRules() {
        _uiState.update { current ->
            current.copy(selectedRemoteFilesToSync = emptySet())
        }
    }

    fun syncSelectedRulesFromGithub(onComplete: (Boolean) -> Unit = {}) {
        val selectedFiles = _uiState.value.selectedRemoteFilesToSync.toList()
        if (selectedFiles.isEmpty()) {
            _uiState.update { it.copy(showSyncPickerDialog = false) }
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(showSyncPickerDialog = false, isSyncingRules = true) }
            val result = ruleRepository.syncSelectedRulesFromGithub(selectedFiles)
            result.getOrNull()?.let { rules ->
                evaluationEngine.updateRules(rules)
                _uiState.update { it.copy(rulePackage = rules, isSyncingRules = false) }
                EDRLogger.i(TAG, "syncSelectedRulesFromGithub: Successfully synced ${rules.rules.size} rules")
                onComplete(true)
            } ?: run {
                _uiState.update { it.copy(isSyncingRules = false) }
                onComplete(false)
            }
        }
    }

    fun confirmDeleteRule(rule: DetectionRule?) {
        _uiState.update { it.copy(deleteRuleConfirm = rule) }
    }

    fun executeDeleteRule(rule: DetectionRule, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(deleteRuleConfirm = null, isLoading = true) }
            val result = ruleRepository.deleteRule(rule.id)
            result.getOrNull()?.let { rules ->
                evaluationEngine.updateRules(rules)
                _uiState.update { it.copy(rulePackage = rules, isLoading = false) }
                EDRLogger.i(TAG, "executeDeleteRule: Deleted rule ${rule.id}, remaining: ${rules.rules.size}")
                onComplete(true)
            } ?: run {
                _uiState.update { it.copy(isLoading = false) }
                onComplete(false)
            }
        }
    }

    fun importCustomYamlRule(uri: Uri, onComplete: (Boolean) -> Unit = {}) {
        EDRLogger.i(TAG, "importCustomYamlRule: Importing YAML rule from Uri $uri")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                }
                if (content.isNullOrEmpty()) {
                    EDRLogger.e(TAG, "importCustomYamlRule: Content stream empty")
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@launch
                }

                val fileName = uri.lastPathSegment ?: "custom_rule.yaml"
                val result = ruleRepository.importCustomYamlRule(content, fileName)
                withContext(Dispatchers.Main) {
                    result.getOrNull()?.let { rules ->
                        evaluationEngine.updateRules(rules)
                        _uiState.update { it.copy(rulePackage = rules) }
                        EDRLogger.i(TAG, "importCustomYamlRule: Imported rule, total rules: ${rules.rules.size}")
                        onComplete(true)
                    } ?: onComplete(false)
                }
            } catch (e: Exception) {
                EDRLogger.e(TAG, "Exception reading custom YAML file Uri", e)
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun clearLocalTelemetryData() {
        EDRLogger.w(TAG, "clearLocalTelemetryData: Clearing local SQLite telemetry history")
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.clearAllVisitedSites()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(visitedSites = emptyList(), blockedAttemptsCount = 0, blockedSitesTrail = emptyList()) }
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
            val blockedCount = dbHelper.getBlockedThreatAttemptsCount()
            val blockedTrail = dbHelper.getBlockedThreatAttempts()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        visitedSites = sites,
                        blockedAttemptsCount = blockedCount,
                        blockedSitesTrail = blockedTrail
                    )
                }
                EDRLogger.i(TAG, "loadVisitedSitesFromDb: Loaded ${sites.size} records ($blockedCount blocked attempts) into state")
            }
        }
    }

    private fun recordVisitedSite(record: VisitedSiteRecord) {
        EDRLogger.v(TAG, "recordVisitedSite: Recording IP '${record.destinationIp}' into SQLite")
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.insertVisitedSite(record)
            val updatedList = dbHelper.getAllVisitedSites()
            val blockedCount = dbHelper.getBlockedThreatAttemptsCount()
            val blockedTrail = dbHelper.getBlockedThreatAttempts()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        visitedSites = updatedList,
                        blockedAttemptsCount = blockedCount,
                        blockedSitesTrail = blockedTrail
                    )
                }
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
                EDRLogger.v(TAG, "observeTelemetry: Captured network event for IP '${netEvent.destinationIp}' (domain: '${netEvent.domain}')")
                val match = evaluationEngine.evaluateNetworkEvent(netEvent)
                val isThreat = match != null
                if (match != null) {
                    EDRLogger.w(TAG, "observeTelemetry: Threat match detected for IP '${netEvent.destinationIp}' -> Rule ${match.rule.id}")
                    containmentManager.handleRuleMatch(match)
                }

                recordVisitedSite(
                    VisitedSiteRecord(
                        destinationIp = netEvent.destinationIp,
                        domain = netEvent.domain,
                        protocol = netEvent.protocol,
                        isThreat = isThreat,
                        appName = "Network Protection Engine"
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
