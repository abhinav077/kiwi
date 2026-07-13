package com.abhinavsirohi.kiwi.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.feature.onboarding.WelcomeScreen
import com.abhinavsirohi.kiwi.feature.onboarding.GoogleSignInRoute
import com.abhinavsirohi.kiwi.feature.onboarding.ApprovedUserRoute
import com.abhinavsirohi.kiwi.feature.onboarding.MinimalSetupRoute
import com.abhinavsirohi.kiwi.feature.onboarding.SessionRestorationRoute
import com.abhinavsirohi.kiwi.feature.today.TodayRoute
import com.abhinavsirohi.kiwi.feature.calendar.CalendarRoute
import com.abhinavsirohi.kiwi.feature.wellness.WellnessRoute
import com.abhinavsirohi.kiwi.feature.diary.DiaryRoute
import com.abhinavsirohi.kiwi.feature.selfcare.SelfCareRoute
import com.abhinavsirohi.kiwi.ui.theme.KiwiCharcoal
import com.abhinavsirohi.kiwi.ui.theme.KiwiDimensions
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray

@Composable
fun KiwiApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            if (currentDestination?.route in KiwiDestination.entries
                    .filter(KiwiDestination::appearsInBottomDock)
                    .map(KiwiDestination::route)
            ) {
                KiwiBottomDock(
                    currentDestination = currentDestination,
                    onDestinationSelected = { destination ->
                        navController.navigateTo(destination)
                    }
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            KiwiNavHost(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun KiwiNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = KiwiDestination.SessionRestore.route,
        modifier = modifier
    ) {
        composable(KiwiDestination.SessionRestore.route) {
            SessionRestorationRoute(
                onSignedOut = { returningUser ->
                    val destination = if (returningUser) {
                        KiwiDestination.SignIn
                    } else {
                        KiwiDestination.Welcome
                    }
                    navController.navigate(destination.route) {
                        popUpTo(KiwiDestination.SessionRestore.route) { inclusive = true }
                    }
                },
                onAccessDenied = {
                    navController.navigate(KiwiDestination.AccessGate.route) {
                        popUpTo(KiwiDestination.SessionRestore.route) { inclusive = true }
                    }
                },
                onNeedsProfileSetup = {
                    navController.navigate(KiwiDestination.ProfileSetup.route) {
                        popUpTo(KiwiDestination.SessionRestore.route) { inclusive = true }
                    }
                },
                onOpenToday = {
                    navController.navigate(KiwiDestination.Today.route) {
                        popUpTo(KiwiDestination.SessionRestore.route) { inclusive = true }
                    }
                },
            )
        }
        composable(KiwiDestination.Welcome.route) {
            WelcomeScreen(onContinue = { navController.navigate(KiwiDestination.SignIn.route) })
        }
        composable(KiwiDestination.SignIn.route) {
            GoogleSignInRoute(
                onAuthenticated = {
                    navController.navigate(KiwiDestination.SessionRestore.route) {
                        popUpTo(KiwiDestination.SignIn.route) { inclusive = true }
                    }
                },
            )
        }
        composable(KiwiDestination.AccessGate.route) {
            ApprovedUserRoute(
                onApproved = {
                    navController.navigate(KiwiDestination.ProfileSetup.route) {
                        popUpTo(KiwiDestination.AccessGate.route) { inclusive = true }
                    }
                },
                onSignedOut = {
                    navController.navigate(KiwiDestination.SignIn.route) {
                        popUpTo(KiwiDestination.AccessGate.route) { inclusive = true }
                    }
                },
            )
        }
        composable(KiwiDestination.ProfileSetup.route) {
            MinimalSetupRoute(
                onComplete = {
                    navController.navigate(KiwiDestination.Today.route) {
                        popUpTo(KiwiDestination.ProfileSetup.route) { inclusive = true }
                    }
                },
            )
        }
        composable(KiwiDestination.Assistant.route) {
            KiwiPlaceholderScreen(KiwiDestination.Assistant, message = "Ask Kiwi will be ready soon.")
        }
        KiwiDestination.entries
            .filter(KiwiDestination::appearsInBottomDock)
            .forEach { destination ->
                composable(destination.route) {
                    when (destination) {
                        KiwiDestination.Today -> TodayRoute(onAskKiwi = {
                            navController.navigate(KiwiDestination.Assistant.route)
                        })
                        KiwiDestination.Calendar -> CalendarRoute()
                        KiwiDestination.Wellness -> WellnessRoute()
                        KiwiDestination.Diary -> DiaryRoute()
                        KiwiDestination.SelfCare -> SelfCareRoute()
                        else -> KiwiPlaceholderScreen(destination = destination)
                    }
                }
            }
    }
}

@Composable
private fun KiwiPlaceholderScreen(
    destination: KiwiDestination,
    message: String = "Placeholder destination"
) {
    KiwiBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = KiwiSpacing.lg, vertical = KiwiSpacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = destination.symbol,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = destination.label,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = KiwiWarmGray
            )
        }
    }
}

@Composable
private fun KiwiBottomDock(
    currentDestination: androidx.navigation.NavDestination?,
    onDestinationSelected: (KiwiDestination) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KiwiSpacing.lg)
            .navigationBarsPadding()
            .height(KiwiDimensions.bottomDockHeight),
        shape = RoundedCornerShape(KiwiDimensions.heroCardRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KiwiSpacing.xs, vertical = KiwiSpacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KiwiDestination.entries
                .filter(KiwiDestination::appearsInBottomDock)
                .forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    KiwiDockItem(
                        destination = destination,
                        selected = selected,
                        onClick = { onDestinationSelected(destination) }
                    )
                }
        }
    }
}

@Composable
private fun RowScope.KiwiDockItem(
    destination: KiwiDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val itemModifier = Modifier
        .weight(1f)
        .semantics {
            contentDescription = destination.label
            role = Role.Tab
            this.selected = selected
        }
        .clickable(onClick = onClick)
        .background(
            color = if (selected) KiwiCharcoal else androidx.compose.ui.graphics.Color.Transparent,
            shape = RoundedCornerShape(KiwiDimensions.chipRadius)
        )
        .padding(horizontal = KiwiSpacing.xs, vertical = KiwiSpacing.xs)

    Column(
        modifier = itemModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = destination.symbol,
            style = MaterialTheme.typography.titleLarge,
            color = if (selected) MaterialTheme.colorScheme.surface else KiwiWarmGray
        )
        if (selected) {
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.surface
            )
        }
    }
}

private fun NavHostController.navigateTo(destination: KiwiDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
