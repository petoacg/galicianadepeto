package gal.galiciana.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gal.galiciana.app.ui.colecciones.ColeccionesScreen
import gal.galiciana.app.ui.datos.DatosScreen
import gal.galiciana.app.ui.hemeroteca.HemerotecaScreen
import gal.galiciana.app.ui.home.HomeScreen
import gal.galiciana.app.ui.home.FeaturedScreen
import gal.galiciana.app.ui.search.SearchScreen
import gal.galiciana.app.ui.fav.FavScreen
import gal.galiciana.app.ui.fav.FavoritesBrowseState
import gal.galiciana.app.ui.viewer.ViewerScreen

@Composable
fun GalicianaApp() {
    var revision by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.key(revision) { GalicianaContent(onDataRestored = { revision++ }) }
}

@Composable
private fun GalicianaContent(onDataRestored: () -> Unit) {
    var dataBusy by remember { mutableStateOf(false) }
    val nav = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val favoritesBrowse = remember { FavoritesBrowseState(gal.galiciana.app.data.LocalStore(context).favoritesBrowse()) }
    val favoritesList = rememberLazyListState(
        favoritesBrowse.saved.optInt("index").coerceAtLeast(0),
        favoritesBrowse.saved.optInt("offset").coerceAtLeast(0))
    val favoritesFolders = rememberLazyListState(
        favoritesBrowse.saved.optInt("folderIndex").coerceAtLeast(0),
        favoritesBrowse.saved.optInt("folderOffset").coerceAtLeast(0))
    val seededSearchList = rememberLazyListState()
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route.orEmpty()
    val reader = route.startsWith("viewer/")
    val activeTab = when {
        route == "favoritos" -> "favoritos"
        route == "explore" || route == "hemeroteca" || route == "colecciones" || route == "datos" || route == "about" || route == "legal" || route.startsWith("featured/") -> "explore"
        else -> "home"
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        if (!reader) NavigationBar(containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp) {
            listOf(Triple("home", "Buscar", Icons.Outlined.Search),
                Triple("explore", "Explorar", Icons.Outlined.Explore),
                Triple("favoritos", "Guardados", Icons.Outlined.Bookmarks)).forEach { (target, label, icon) ->
                NavigationBarItem(selected = activeTab == target, enabled = !dataBusy, onClick = {
                    // Tabs always open their named destination. Restoring a saved
                    // nested stack here could send Buscar back to Acerca de.
                    if (route != target) nav.navigate(target) {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                }, icon = { Icon(icon, null) }, label = { Text(label) })
            }
        }
    }) { appPadding ->
    NavHost(
        navController = nav,
        modifier = Modifier.padding(appPadding),
        startDestination = "home",
        enterTransition = {
            fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 18 }
        },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = {
            fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { it / 18 }
        },
    ) {
        composable("backup") { BackupScreen(onBack = { nav.popBackStack() }, onRestored = onDataRestored,
            onBusyChanged = { dataBusy = it }) }
        composable("searches") { gal.galiciana.app.ui.search.SearchHistoryScreen(onBack = { nav.popBackStack() }, onOpen = { preset ->
            nav.navigate("search?preset=${android.net.Uri.encode(preset.json().toString())}")
        }) }
        composable("about") { AboutScreen(onBack = { nav.popBackStack() }, onLegal = { nav.navigate("legal") },
            onBackup = { nav.navigate("backup") }) }
        composable("legal") { LegalScreen(onBack = { nav.popBackStack() }) }
        composable("explore") {
            ExploreScreen(onHemeroteca = { nav.navigate("hemeroteca") },
                onCollections = { nav.navigate("colecciones") }, onData = { nav.navigate("datos") },
                onAbout = { nav.navigate("about") })
        }
        composable("home") {
            HomeScreen(
                onHistory = { nav.navigate("searches") },
                onOpenSearch = { nav.navigate("search?q=&t=&f=true") },
                onSearchQuery = { query ->
                    nav.navigate("search?q=${android.net.Uri.encode(query)}&t=")
                },
                onAbout = { nav.navigate("about") },
                onFavoritos = { nav.navigate("favoritos") },
                onOpenSaved = { path, pos, img, query ->
                    val im = img?.ifBlank { null } ?: "-"
                    val encodedQuery = android.net.Uri.encode(query.ifBlank { "-" })
                    nav.navigate("viewer/$path/$pos/$encodedQuery/$im")
                },
                onOpenFeatured = { id, title -> nav.navigate("featured/$id?title=${android.net.Uri.encode(title)}") },
            )
        }
        composable("featured/{id}?title={title}", arguments = listOf(navArgument("title") { type = NavType.StringType; defaultValue = "" })) { entry ->
            FeaturedScreen(
                recordId = entry.arguments?.getString("id").orEmpty(),
                seedTitle = entry.arguments?.getString("title").orEmpty(),
                onBack = { nav.popBackStack() },
                onHome = { nav.popBackStack("home", inclusive = false) },
                onOpenPath = { path -> nav.navigate("viewer/$path/1/-/-") },
            )
        }
        composable(
            route = "search?q={q}&t={t}&f={f}&preset={preset}",
            arguments = listOf(
                navArgument("q") { type = NavType.StringType; defaultValue = "" },
                navArgument("t") { type = NavType.StringType; defaultValue = "" },
                navArgument("f") { type = NavType.BoolType; defaultValue = false },
                navArgument("preset") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            SearchScreen(
                listState = seededSearchList,
                onHistory = { nav.navigate("searches") },
                seedPreset = entry.arguments?.getString("preset")?.takeIf { it.isNotBlank() }?.let {
                    runCatching { gal.galiciana.app.data.SearchPreset.fromJson(org.json.JSONObject(it)) }.getOrNull()
                },
                seedFilters = entry.arguments?.getBoolean("f") ?: false,
                seedQuery = android.net.Uri.decode(entry.arguments?.getString("q").orEmpty()),
                seedTitle = android.net.Uri.decode(entry.arguments?.getString("t").orEmpty()),
                onOpenHit = { path, pos, q, img ->
                    val enc = android.net.Uri.encode(q.ifBlank { "-" })
                    val im = img?.ifBlank { null } ?: "-"
                    nav.navigate("viewer/$path/$pos/$enc/$im")
                },
                onBack = { nav.popBackStack() },
                onHome = { nav.popBackStack("home", inclusive = false) },
            )
        }
        composable("hemeroteca") {
            HemerotecaScreen(
                onHome = { nav.popBackStack() },
                onConsulta = { nav.navigate("search") },
                onDatos = { nav.navigate("datos") },
                onOpenPath = { path: String -> nav.navigate("viewer/$path/1/-/-") },
                onFavoritos = { nav.navigate("favoritos") },
            )
        }
        composable("colecciones") {
            ColeccionesScreen(
                onHome = { nav.popBackStack() },
                onConsulta = { nav.navigate("search") },
                onHemeroteca = { nav.navigate("hemeroteca") },
                onDatos = { nav.navigate("datos") },
                onFavoritos = { nav.navigate("favoritos") },
                onBuscar = { title ->
                    val t = android.net.Uri.encode(title)
                    nav.navigate("search?q=&t=$t")
                },
                onOpenPath = { path, pos, q, img ->
                    val enc = android.net.Uri.encode(q.ifBlank { "-" })
                    val im = img?.ifBlank { null } ?: "-"
                    nav.navigate("viewer/$path/$pos/$enc/$im")
                },
            )
        }
        composable("datos") {
            DatosScreen(
                onHome = { nav.popBackStack() },
                onConsulta = { nav.navigate("search") },
                onHemeroteca = { nav.navigate("hemeroteca") },
                onFavoritos = { nav.navigate("favoritos") },
                onOpenPath = { path: String -> nav.navigate("viewer/$path/1/-/-") },
                onBuscarTitle = { title: String ->
                    nav.navigate("search?q=&t=${android.net.Uri.encode(title)}")
                },
            )
        }
        composable("favoritos") {
            FavScreen(
                onBackup = { nav.navigate("backup") },
                browseState = favoritesBrowse,
                listState = favoritesList,
                folderListState = favoritesFolders,
                onHome = { nav.popBackStack("home", inclusive = false) },
                onConsulta = { nav.navigate("search") },
                onHemeroteca = { nav.navigate("hemeroteca") },
                onDatos = { nav.navigate("datos") },
                onOpen = { path, pos, q ->
                    val enc = android.net.Uri.encode(q.ifBlank { "-" })
                    val image = gal.galiciana.app.data.LocalStore(context).favHits()
                        .firstOrNull { it.path == path && it.position == pos }?.imageId?.ifBlank { null } ?: "-"
                    nav.navigate("viewer/$path/$pos/$enc/$image")
                },
            )
        }
        composable(
            route = "viewer/{path}/{pos}/{q}/{img}",
            arguments = listOf(
                navArgument("path") { type = NavType.StringType },
                navArgument("pos") { type = NavType.IntType },
                navArgument("q") { type = NavType.StringType },
                navArgument("img") { type = NavType.StringType },
            )
        ) { entry ->
            val path = entry.arguments?.getString("path").orEmpty()
            val pos = entry.arguments?.getInt("pos") ?: 1
            val q = android.net.Uri.decode(entry.arguments?.getString("q").orEmpty()).let {
                if (it == "-") "" else it
            }
            val img = entry.arguments?.getString("img")?.let { if (it == "-") null else it }
            ViewerScreen(
                path = path,
                position = pos,
                initialQuery = q,
                seedImageId = img,
                onBack = { nav.popBackStack() },
                onHome = { nav.popBackStack("home", inclusive = false) },
                onGo = { },
            )
        }
    }
    }
}
