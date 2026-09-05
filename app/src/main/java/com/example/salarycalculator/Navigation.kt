package com.example.salarycalculator

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.salarycalculator.domain.SalaryRepository
import com.example.salarycalculator.ui.calculator.CalculatorScreen
import com.example.salarycalculator.ui.history.HistoryScreen
import com.example.salarycalculator.ui.settings.SettingsScreen

enum class BookStyleRightTab {
    HISTORY,
    SETTINGS
}

@Composable
fun MainNavigation(salaryRepository: SalaryRepository) {
    val backStack = rememberNavBackStack(Calculator)
    val currentTab = backStack.lastOrNull() ?: Calculator

    var rightPaneTab by remember { mutableStateOf(BookStyleRightTab.HISTORY) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isBookStyleFoldable = maxWidth >= 720.dp

        if (isBookStyleFoldable) {
            // Book-Style Dual-Screen Foldable Architecture
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Screen: Fullscreen Main Calculator
                    Box(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                    ) {
                        CalculatorScreen(
                            salaryRepository = salaryRepository,
                            modifier = Modifier.fillMaxSize(),
                            onNavigateToSettings = { rightPaneTab = BookStyleRightTab.SETTINGS }
                        )
                    }

                    // Physical Fold Hinge Divider
                    VerticalDivider(
                        thickness = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )

                    // Right Screen: Fullscreen Companion Workspace (History & Settings)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    ) {
                        // Right Screen Top Navigation Switcher
                        Surface(
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SegmentedButton(
                                        selected = rightPaneTab == BookStyleRightTab.HISTORY,
                                        onClick = { rightPaneTab = BookStyleRightTab.HISTORY },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                        icon = {
                                            Icon(
                                                imageVector = if (rightPaneTab == BookStyleRightTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                                contentDescription = "History",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    ) {
                                        Text("Salary History", style = MaterialTheme.typography.labelMedium, fontSize = 12.sp, maxLines = 1)
                                    }

                                    SegmentedButton(
                                        selected = rightPaneTab == BookStyleRightTab.SETTINGS,
                                        onClick = { rightPaneTab = BookStyleRightTab.SETTINGS },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                        icon = {
                                            Icon(
                                                imageVector = if (rightPaneTab == BookStyleRightTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                                contentDescription = "Settings",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    ) {
                                        Text("Preferences", style = MaterialTheme.typography.labelMedium, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // Right Screen Active Destination
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            AnimatedContent(
                                targetState = rightPaneTab,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "RightPaneTransition"
                            ) { activeTab ->
                                when (activeTab) {
                                    BookStyleRightTab.HISTORY -> HistoryScreen(
                                        salaryRepository = salaryRepository,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    BookStyleRightTab.SETTINGS -> SettingsScreen(
                                        salaryRepository = salaryRepository,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Standard / Compact Mobile Layout with Bottom Navigation Bar
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        tonalElevation = 8.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        val isCalculator = currentTab == Calculator
                        val isHistory = currentTab == History
                        val isSettings = currentTab == Settings

                        NavigationBarItem(
                            selected = isCalculator,
                            onClick = {
                                if (!isCalculator) {
                                    backStack.clear()
                                    backStack.add(Calculator)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isCalculator) Icons.Filled.Calculate else Icons.Outlined.Calculate,
                                    contentDescription = "Calculator"
                                )
                            },
                            label = { Text("Calculator", style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        NavigationBarItem(
                            selected = isHistory,
                            onClick = {
                                if (!isHistory) {
                                    backStack.clear()
                                    backStack.add(History)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isHistory) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = "History"
                                )
                            },
                            label = { Text("History", style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        NavigationBarItem(
                            selected = isSettings,
                            onClick = {
                                if (!isSettings) {
                                    backStack.clear()
                                    backStack.add(Settings)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSettings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings", style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            ) { padding ->
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<Calculator> {
                            CalculatorScreen(
                                salaryRepository = salaryRepository,
                                modifier = Modifier.padding(padding),
                                onNavigateToSettings = {
                                    if (currentTab != Settings) {
                                        backStack.clear()
                                        backStack.add(Settings)
                                    }
                                }
                            )
                        }
                        entry<History> { HistoryScreen(salaryRepository, Modifier.padding(padding)) }
                        entry<Settings> { SettingsScreen(salaryRepository, Modifier.padding(padding)) }
                    }
                )
            }
        }
    }
}
