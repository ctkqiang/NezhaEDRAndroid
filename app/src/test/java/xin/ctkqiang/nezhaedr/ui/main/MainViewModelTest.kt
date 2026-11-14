package xin.ctkqiang.nezhaedr.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xin.ctkqiang.nezhaedr.data.model.ContainmentAction
import xin.ctkqiang.nezhaedr.data.model.DetectionRule
import xin.ctkqiang.nezhaedr.data.model.RuleCondition
import xin.ctkqiang.nezhaedr.ui.theme.AppThemeMode

class MainViewModelTest {

    @Test
    fun testMainUiStateTabSelection() {
        val initialState = MainUiState()
        assertEquals(0, initialState.selectedTab)

        val updatedState = initialState.copy(selectedTab = 2)
        assertEquals(2, updatedState.selectedTab)
    }

    @Test
    fun testMainUiStateRuleSelectionAndDismissal() {
        val testRule = DetectionRule(
            id = "c2_threat_block",
            schemaVersion = "2.0",
            ruleId = "RULE-C2-001",
            name = "Known C2 Threat IP Block",
            description = "Detects egress to known C2 server",
            severity = "HIGH",
            category = "NETWORK",
            enabled = true,
            engineType = "signature",
            targetSource = "dns_query",
            condition = RuleCondition(),
            action = ContainmentAction.BLOCK
        )

        val stateWithRule = MainUiState(selectedRule = testRule)
        assertNotNull(stateWithRule.selectedRule)
        assertEquals("RULE-C2-001", stateWithRule.selectedRule?.ruleId)

        val stateDismissed = stateWithRule.copy(selectedRule = null)
        assertNull(stateDismissed.selectedRule)
    }

    @Test
    fun testMainUiStateThemeAndDialogToggle() {
        val defaultState = MainUiState()
        assertEquals(AppThemeMode.RED, defaultState.appTheme)
        assertFalse(defaultState.showSyncPickerDialog)

        val dialogState = defaultState.copy(showSyncPickerDialog = true, appTheme = AppThemeMode.BLUE)
        assertTrue(dialogState.showSyncPickerDialog)
        assertEquals(AppThemeMode.BLUE, dialogState.appTheme)
    }
}
