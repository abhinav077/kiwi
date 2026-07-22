package com.abhinavsirohi.kiwi.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.abhinavsirohi.kiwi.feature.review.ReviewRoute
import com.abhinavsirohi.kiwi.feature.diary.DiaryRoute
import com.abhinavsirohi.kiwi.feature.selfcare.SelfCareRoute
import com.abhinavsirohi.kiwi.feature.settings.SettingsRoute
import com.abhinavsirohi.kiwi.feature.downloads.PinterestDownloadsRoute
import com.abhinavsirohi.kiwi.ui.theme.KiwiCharcoal
import com.abhinavsirohi.kiwi.ui.theme.KiwiDimensions
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray

private val bottomDockDestinations = listOf(
    KiwiDestination.Today,
    KiwiDestination.Calendar,
    KiwiDestination.Diary,
    KiwiDestination.Wellness,
    KiwiDestination.More,
)

@Composable
fun KiwiApp(
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    var moreSheetOpen by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val authenticatedRoutes = KiwiDestination.entries
        .filterNot { it in setOf(
            KiwiDestination.SessionRestore,
            KiwiDestination.Welcome,
            KiwiDestination.SignIn,
            KiwiDestination.AccessGate,
            KiwiDestination.ProfileSetup,
        ) }
        .map(KiwiDestination::route)

    LaunchedEffect(sharedText, currentDestination?.route) {
        if (
            !sharedText.isNullOrBlank() &&
            currentDestination?.route in authenticatedRoutes &&
            currentDestination?.route != KiwiDestination.Downloads.route
        ) {
            navController.navigate(KiwiDestination.Downloads.route) { launchSingleTop = true }
        }
    }

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
                    moreSheetOpen = moreSheetOpen,
                    onDestinationSelected = { destination ->
                        if (destination == KiwiDestination.More) {
                            moreSheetOpen = true
                        } else {
                            navController.navigateTo(destination)
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            KiwiNavHost(
                navController = navController,
                sharedText = sharedText,
                onSharedTextConsumed = onSharedTextConsumed,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .align(Alignment.Center)
            )
        }
    }
    if (moreSheetOpen) {
        KiwiMoreSheet(
            onDismiss = { moreSheetOpen = false },
            onDestinationSelected = { destination ->
                moreSheetOpen = false
                navController.navigateTo(destination)
            },
        )
    }
}

@Composable
private fun KiwiNavHost(
    navController: NavHostController,
    sharedText: String?,
    onSharedTextConsumed: () -> Unit,
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
        composable(KiwiDestination.Wellness.route) { WellnessRoute() }
        composable(KiwiDestination.Review.route) { ReviewRoute() }
        composable(KiwiDestination.Diary.route) { DiaryRoute() }
        composable(KiwiDestination.SelfCare.route) { SelfCareRoute() }
        composable(KiwiDestination.Downloads.route) {
            PinterestDownloadsRoute(
                initialSharedText = sharedText,
                onSharedTextConsumed = onSharedTextConsumed,
                onBack = navController::popBackStack,
            )
        }
        composable(KiwiDestination.More.route) {
            SettingsRoute(
                onOpenWellness = { navController.navigateTo(KiwiDestination.Wellness) },
                onOpenReview = { navController.navigateTo(KiwiDestination.Review) },
                onOpenSelfCare = { navController.navigateTo(KiwiDestination.SelfCare) },
                onOpenDownloads = { navController.navigateTo(KiwiDestination.Downloads) },
                onSignedOut = {
                    navController.navigate(KiwiDestination.SignIn.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        KiwiDestination.entries
            .filter { it.appearsInBottomDock && it !in setOf(KiwiDestination.Diary, KiwiDestination.More, KiwiDestination.Wellness) }
            .forEach { destination ->
                composable(destination.route) {
                    when (destination) {
                        KiwiDestination.Today -> TodayRoute()
                        KiwiDestination.Calendar -> CalendarRoute()
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
    moreSheetOpen: Boolean,
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
            bottomDockDestinations
                .forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true || (destination == KiwiDestination.More && moreSheetOpen)
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
@OptIn(ExperimentalMaterial3Api::class)
private fun KiwiMoreSheet(
    onDismiss: () -> Unit,
    onDestinationSelected: (KiwiDestination) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KiwiSpacing.xl, vertical = KiwiSpacing.sm)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(KiwiSpacing.xs),
        ) {
            Text(
                text = "More of your space",
                style = MaterialTheme.typography.headlineLarge,
                color = KiwiCharcoal,
                modifier = Modifier.padding(bottom = KiwiSpacing.sm),
            )
            KiwiMoreRow("◌", "Review & Reflections", "Look back at your recorded rhythm") {
                onDestinationSelected(KiwiDestination.Review)
            }
            KiwiMoreRow("♡", "Self-care Routines", "Keep small rituals close") {
                onDestinationSelected(KiwiDestination.SelfCare)
            }
            KiwiMoreRow("↓", "Pinterest Downloader", "Save a public Pin to your device") {
                onDestinationSelected(KiwiDestination.Downloads)
            }
            KiwiMoreRow("⚙", "Settings", "Themes, privacy, and protection") {
                onDestinationSelected(KiwiDestination.More)
            }
            Spacer(Modifier.height(KiwiSpacing.sm))
        }
    }
}

@Composable
private fun KiwiMoreRow(
    symbol: String,
    label: String,
    supporting: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .padding(horizontal = KiwiSpacing.sm, vertical = KiwiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = KiwiSpacing.md),
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = KiwiWarmGray)
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
