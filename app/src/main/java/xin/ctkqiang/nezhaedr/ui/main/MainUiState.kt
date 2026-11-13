package xin.ctkqiang.nezhaedr.ui.main

import xin.ctkqiang.nezhaedr.containment.ThreatLogEntry
import xin.ctkqiang.nezhaedr.data.local.VisitedSiteRecord
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.DetectionRulePackage
import xin.ctkqiang.nezhaedr.data.repository.RemoteRuleCatalogItem
import xin.ctkqiang.nezhaedr.ui.theme.AppThemeMode

data class MainUiState(
    val isVpnActive: Boolean = false,
    val rulePackage: DetectionRulePackage? = null,
    val threatLogs: List<ThreatLogEntry> = emptyList(),
    val visitedSites: List<VisitedSiteRecord> = emptyList(),
    val blockedAttemptsCount: Int = 0,
    val blockedSitesTrail: List<VisitedSiteRecord> = emptyList(),
    val selectedRule: DetectionRule? = null,
    val activeThreatPopup: ThreatLogEntry? = null,
    val deleteRuleConfirm: DetectionRule? = null,
    val showSyncPickerDialog: Boolean = false,
    val availableRemoteCatalog: List<RemoteRuleCatalogItem> = emptyList(),
    val selectedRemoteFilesToSync: Set<String> = emptySet(),
    val appTheme: AppThemeMode = AppThemeMode.RED,
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val isSyncingRules: Boolean = false
)
