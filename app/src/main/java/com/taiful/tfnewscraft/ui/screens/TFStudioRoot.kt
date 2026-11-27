package com.taiful.tfnewscraft.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.taiful.tfnewscraft.R
import com.taiful.tfnewscraft.data.model.HomeMode
import com.taiful.tfnewscraft.ui.components.ArticleReaderSheet
import com.taiful.tfnewscraft.ui.components.NewspaperBrowserOverlay
import com.taiful.tfnewscraft.ui.theme.TFReelsPostMakerTheme
import com.taiful.tfnewscraft.viewmodel.StudioViewModel
import kotlinx.serialization.Serializable

@Serializable
data object RouteNews

@Serializable
data object RouteAiStudio

@Serializable
data object RoutePhotoCard

@Serializable
data object RoutePostReels

private data class NavHistoryEntry(
    val routeKey: String,
    val homeSection: HomeMode?,
    val postReelsTab: Int
)

private data class NavDestinationItem(
    val route: Any,
    val routeQualifiedName: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TFStudioRoot(
    viewModel: StudioViewModel,
    onRequestNotificationPermission: () -> Unit
) {
    val brand by viewModel.brandSettings.collectAsStateWithLifecycle()
    val logoBitmap by viewModel.logoBitmap.collectAsStateWithLifecycle()
    val reelSlides by viewModel.reelSlides.collectAsStateWithLifecycle()
    val activeBrowserPaper by viewModel.activeBrowserPaper.collectAsStateWithLifecycle()
    val adFilterEnabled by viewModel.adFilterEnabled.collectAsStateWithLifecycle()
    val showArticleReader by viewModel.showArticleReader.collectAsStateWithLifecycle()
    val activeArticle by viewModel.activeArticle.collectAsStateWithLifecycle()
    val activeHomeSection by viewModel.activeHomeSection.collectAsStateWithLifecycle()
    val postReelsTab by viewModel.postReelsTabIndex.collectAsStateWithLifecycle()
    val isPreparingFacebook by viewModel.isPreparingFacebook.collectAsStateWithLifecycle()
    val isFetchingUrl by viewModel.isFetchingUrl.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBrandSheet by remember { mutableStateOf(false) }
    var showCleanConfirmDialog by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: RouteNews::class.qualifiedName.orEmpty()

    // Global Back / Forward / Home history stack across all screens and inner sections
    val newsRouteKey = RouteNews::class.qualifiedName.orEmpty()
    val aiRouteKey = RouteAiStudio::class.qualifiedName.orEmpty()
    val cardRouteKey = RoutePhotoCard::class.qualifiedName.orEmpty()
    val postReelsRouteKey = RoutePostReels::class.qualifiedName.orEmpty()

    var navHistory by remember {
        mutableStateOf(listOf(NavHistoryEntry(newsRouteKey, null, 0)))
    }
    var navHistoryIndex by remember { mutableIntStateOf(0) }
    var isRestoringHistory by remember { mutableStateOf(false) }

    val destinations = remember {
        listOf(
            NavDestinationItem(
                route = RouteNews,
                routeQualifiedName = RouteNews::class.qualifiedName.orEmpty(),
                labelRes = R.string.nav_news,
                selectedIcon = Icons.Filled.Newspaper,
                unselectedIcon = Icons.Outlined.Newspaper,
                testTag = "nav_tab_news"
            ),
            NavDestinationItem(
                route = RouteAiStudio,
                routeQualifiedName = RouteAiStudio::class.qualifiedName.orEmpty(),
                labelRes = R.string.nav_ai_studio,
                selectedIcon = Icons.Filled.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome,
                testTag = "nav_tab_ai_studio"
            ),
            NavDestinationItem(
                route = RoutePhotoCard,
                routeQualifiedName = RoutePhotoCard::class.qualifiedName.orEmpty(),
                labelRes = R.string.nav_photo_card,
                selectedIcon = Icons.Filled.Image,
                unselectedIcon = Icons.Outlined.Image,
                testTag = "nav_tab_photo_card"
            ),
            NavDestinationItem(
                route = RoutePostReels,
                routeQualifiedName = RoutePostReels::class.qualifiedName.orEmpty(),
                labelRes = R.string.nav_post_reels,
                selectedIcon = Icons.Filled.MovieCreation,
                unselectedIcon = Icons.Outlined.MovieCreation,
                testTag = "nav_tab_post_reels"
            )
        )
    }

    fun navigateToRouteOnly(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToTab(route: Any) {
        if (route == RouteNews) {
            viewModel.returnToHomeHub()
        }
        navigateToRouteOnly(route)
    }

    fun applyHistoryEntry(entry: NavHistoryEntry) {
        isRestoringHistory = true
        if (entry.routeKey == newsRouteKey) {
            if (entry.homeSection == null) {
                viewModel.returnToHomeHub()
            } else {
                viewModel.openHomeSection(entry.homeSection)
            }
        }
        if (entry.routeKey == postReelsRouteKey) {
            viewModel.setPostReelsTab(entry.postReelsTab)
        }
        val targetRoute: Any = when (entry.routeKey) {
            aiRouteKey -> RouteAiStudio
            cardRouteKey -> RoutePhotoCard
            postReelsRouteKey -> RoutePostReels
            else -> RouteNews
        }
        if (currentRoute != entry.routeKey) {
            navigateToRouteOnly(targetRoute)
        }
    }

    LaunchedEffect(currentRoute, activeHomeSection, postReelsTab) {
        val currentEntry = NavHistoryEntry(
            routeKey = currentRoute,
            homeSection = if (currentRoute == newsRouteKey) activeHomeSection else null,
            postReelsTab = if (currentRoute == postReelsRouteKey) postReelsTab else 0
        )
        if (isRestoringHistory) {
            isRestoringHistory = false
            return@LaunchedEffect
        }
        val cur = navHistory.getOrNull(navHistoryIndex)
        if (cur != currentEntry) {
            val trimmed = navHistory.take(navHistoryIndex + 1) + currentEntry
            val bounded = trimmed.takeLast(30)
            navHistory = bounded
            navHistoryIndex = bounded.lastIndex
        }
    }

    val canGoBackGlobal = navHistoryIndex > 0
    val canGoForwardGlobal = navHistoryIndex < navHistory.lastIndex
    val currentPageLabel = when (currentRoute) {
        aiRouteKey -> "AI স্টুডিও"
        cardRouteKey -> "ফটো কার্ড"
        postReelsRouteKey -> if (postReelsTab == 1) "রিলস মেকার" else "ফেসবুক পোস্ট"
        else -> when (activeHomeSection) {
            HomeMode.LIVE_NEWS -> "লাইভ নিউজ"
            HomeMode.MULTI_REELS -> "মাল্টি নিউজ রিলস"
            HomeMode.PAPER_AND_VIDEO -> "পত্রিকা থেকে নিউজ"
            null -> "হোম"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    TFReelsPostMakerTheme(darkTheme = brand.darkMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 680.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (logoBitmap != null) {
                                            Image(
                                                bitmap = logoBitmap!!.asImageBitmap(),
                                                contentDescription = stringResource(R.string.cd_page_logo),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_tf_news_desk_logo),
                                                contentDescription = stringResource(R.string.cd_page_logo),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = brand.pageName.ifBlank { stringResource(R.string.app_name) },
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "TF FB Reels & Post Studio",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showCleanConfirmDialog = true },
                                    modifier = Modifier.testTag("clean_workspace_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CleaningServices,
                                        contentDescription = "হোম পেজ ও কাজ পরিষ্কার করুন"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.updateBrandSettings { it.copy(darkMode = !it.darkMode) }
                                    },
                                    modifier = Modifier.testTag("toggle_theme_button")
                                ) {
                                    Icon(
                                        imageVector = if (brand.darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = "থিম পরিবর্তন"
                                    )
                                }
                                IconButton(
                                    onClick = { showBrandSheet = true },
                                    modifier = Modifier.testTag("open_brand_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = stringResource(R.string.action_brand_settings)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Global Compact Back / Forward / Home Bar visible on EVERY page
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (navHistoryIndex > 0) {
                                            navHistoryIndex -= 1
                                            applyHistoryEntry(navHistory[navHistoryIndex])
                                        }
                                    },
                                    enabled = canGoBackGlobal,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("global_nav_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "পেছনে",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (navHistoryIndex < navHistory.lastIndex) {
                                            navHistoryIndex += 1
                                            applyHistoryEntry(navHistory[navHistoryIndex])
                                        }
                                    },
                                    enabled = canGoForwardGlobal,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("global_nav_forward_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "সামনে",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Surface(
                                    onClick = { navigateToTab(RouteNews) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (currentRoute == newsRouteKey && activeHomeSection == null) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.testTag("global_nav_home_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Home,
                                            contentDescription = "হোম",
                                            tint = if (currentRoute == newsRouteKey && activeHomeSection == null) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "হোম",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (currentRoute == newsRouteKey && activeHomeSection == null) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = currentPageLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.routeQualifiedName
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    modifier = Modifier.testTag(dest.testTag),
                                    icon = {
                                        if (dest.route == RoutePostReels && reelSlides.isNotEmpty()) {
                                            BadgedBox(badge = { Badge { Text("${reelSlides.size}") } }) {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = stringResource(dest.labelRes)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelRes)
                                            )
                                        }
                                    },
                                    label = { Text(stringResource(dest.labelRes)) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.routeQualifiedName
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    modifier = Modifier.testTag(dest.testTag),
                                    icon = {
                                        if (dest.route == RoutePostReels && reelSlides.isNotEmpty()) {
                                            BadgedBox(badge = { Badge { Text("${reelSlides.size}") } }) {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = stringResource(dest.labelRes)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelRes)
                                            )
                                        }
                                    },
                                    label = { Text(stringResource(dest.labelRes)) }
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = RouteNews,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable<RouteNews> {
                            NewsScreen(
                                viewModel = viewModel,
                                onOpenAiStudio = { navigateToTab(RouteAiStudio) },
                                onOpenPhotoCard = { navigateToTab(RoutePhotoCard) },
                                onOpenReels = {
                                    viewModel.setPostReelsTab(1)
                                    navigateToTab(RoutePostReels)
                                },
                                onRequestCleanWorkspace = { showCleanConfirmDialog = true }
                            )
                        }
                        composable<RouteAiStudio> {
                            AiStudioScreen(
                                viewModel = viewModel,
                                onGoToNews = { navigateToTab(RouteNews) },
                                onGoToPhotoCard = { navigateToTab(RoutePhotoCard) },
                                onGoToFacebook = {
                                    viewModel.setPostReelsTab(0)
                                    navigateToTab(RoutePostReels)
                                }
                            )
                        }
                        composable<RoutePhotoCard> {
                            PhotoCardScreen(
                                viewModel = viewModel,
                                onGoToReels = {
                                    viewModel.setPostReelsTab(1)
                                    navigateToTab(RoutePostReels)
                                }
                            )
                        }
                        composable<RoutePostReels> {
                            FacebookReelsScreen(
                                viewModel = viewModel,
                                onRequestNotificationPermission = onRequestNotificationPermission
                            )
                        }
                    }
                }
            }

            // Full-screen in-app Newspaper Browser Overlay
            activeBrowserPaper?.let { paper ->
                NewspaperBrowserOverlay(
                    newspaper = paper,
                    reelCount = reelSlides.size,
                    isExtracting = isFetchingUrl,
                    adFilterEnabled = adFilterEnabled,
                    onPickForCard = { url, title ->
                        viewModel.importBrowserPageToCard(url, title) {
                            navigateToTab(RoutePhotoCard)
                        }
                    },
                    onAddToReels = { url, title ->
                        viewModel.addBrowserPageToReels(url, title)
                    },
                    onOpenReels = {
                        viewModel.closeNewspaperBrowser()
                        viewModel.setPostReelsTab(1)
                        navigateToTab(RoutePostReels)
                    },
                    onClose = { viewModel.closeNewspaperBrowser() }
                )
            }

            // Original Article Reader Sheet ("মূল খবর")
            if (showArticleReader && activeArticle != null) {
                ArticleReaderSheet(
                    article = activeArticle!!,
                    isPreparingFacebook = isPreparingFacebook,
                    onSaveEdit = { newTitle, newBody ->
                        viewModel.updateActiveArticleText(newTitle, newBody)
                    },
                    onPrepareForFacebook = {
                        viewModel.prepareForFacebookOneTap {
                            viewModel.closeArticleReader()
                            viewModel.setPostReelsTab(0)
                            navigateToTab(RoutePostReels)
                        }
                    },
                    onDismiss = { viewModel.closeArticleReader() }
                )
            }

            if (showBrandSheet) {
                BrandSettingsSheet(
                    viewModel = viewModel,
                    onDismiss = { showBrandSheet = false }
                )
            }

            if (showCleanConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showCleanConfirmDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = { Text("হোম পেজ পরিষ্কার করবেন?") },
                    text = {
                        Text(
                            "নির্বাচিত খবর, রিলস বাস্কেট, সার্চ ও সব ড্রাফট মুছে নতুন করে শুরু হবে। আপনার পেজের নাম, লোগো ও থিম অক্ষুণ্ণ থাকবে।"
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCleanConfirmDialog = false
                                viewModel.clearWorkspace()
                                navigateToTab(RouteNews)
                            },
                            modifier = Modifier.testTag("confirm_clean_workspace_button")
                        ) {
                            Text("পরিষ্কার করুন")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCleanConfirmDialog = false }) {
                            Text("বাতিল")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrandSettingsSheet(
    viewModel: StudioViewModel,
    onDismiss: () -> Unit
) {
    val brand by viewModel.brandSettings.collectAsStateWithLifecycle()
    val logoBitmap by viewModel.logoBitmap.collectAsStateWithLifecycle()

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.savePickedLogo(uri)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.action_brand_settings), style = MaterialTheme.typography.titleLarge)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoBitmap != null) {
                        Image(
                            bitmap = logoBitmap!!.asImageBitmap(),
                            contentDescription = stringResource(R.string.cd_page_logo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_tf_news_desk_logo),
                            contentDescription = stringResource(R.string.cd_page_logo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        logoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("brand_pick_logo_button")
                ) {
                    Text(stringResource(R.string.action_pick_logo))
                }
                if (brand.logoFilePath.isNotBlank()) {
                    TextButton(
                        onClick = { viewModel.removeLogo() },
                        modifier = Modifier.testTag("brand_remove_logo_button")
                    ) {
                        Text("ডিফল্ট লোগো", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            OutlinedTextField(
                value = brand.pageName,
                onValueChange = { v -> viewModel.updateBrandSettings { it.copy(pageName = v) } },
                label = { Text("ফেসবুক পেজের নাম") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("brand_page_name_input")
            )

            OutlinedTextField(
                value = brand.pageHandle,
                onValueChange = { v -> viewModel.updateBrandSettings { it.copy(pageHandle = v) } },
                label = { Text("পেজ হ্যান্ডেল / ওয়েবসাইট") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ডার্ক ব্রডকাস্ট থিম", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = brand.darkMode,
                    onCheckedChange = { dark -> viewModel.updateBrandSettings { it.copy(darkMode = dark) } }
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("brand_settings_done_button")
            ) {
                Text("সম্পন্ন")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
