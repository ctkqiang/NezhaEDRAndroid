package xin.ctkqiang.nezhaedr.ui.main

import xin.ctkqiang.nezhaedr.containment.ThreatLogEntry
import xin.ctkqiang.nezhaedr.data.local.VisitedSiteRecord
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.DetectionRulePackage

data class MainUiState(
    val isVpnActive: Boolean = false,
    val rulePackage: DetectionRulePackage? = null,
    val threatLogs: List<ThreatLogEntry> = emptyList(),
    val visitedSites: List<VisitedSiteRecord> = emptyList(),
    val selectedRule: DetectionRule? = null,
    val activeThreatPopup: ThreatLogEntry? = null,
    val selectedTab: Int = 0,
    val isLoading: Boolean = false
)
