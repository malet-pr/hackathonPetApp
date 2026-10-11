package com.example.petcare.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(model: PetCareViewModel = viewModel()) {
    val title by model.title.collectAsStateWithLifecycle()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    fun goTo(screen: Screen) {
        model.navigate(screen)
        scope.launch { drawer.close() }
    }
    BackHandler(enabled = drawer.isOpen || model.screen != Screen.Welcome) {
        if (drawer.isOpen) scope.launch { drawer.close() }
        else model.navigate(if (model.screen == Screen.Pets) Screen.Home else Screen.Welcome)
    }
    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = model.screen != Screen.Welcome && !model.settingsOpen,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Icon(painterResource(R.drawable.ic_paw), null,
                    Modifier.padding(horizontal = 28.dp).size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, Modifier.padding(28.dp, 16.dp), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                HorizontalDivider(Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.home)) },
                    selected = model.screen == Screen.Home,
                    onClick = { goTo(Screen.Home) }, modifier = Modifier.padding(horizontal = 12.dp))
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.medication)) },
                    selected = model.screen == Screen.Pets,
                    onClick = { goTo(Screen.Pets) }, modifier = Modifier.padding(horizontal = 12.dp))
                listOf(R.string.vaccinations, R.string.appointments, R.string.providers).forEach { name ->
                    ListItem(
                        headlineContent = { Text(stringResource(name), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = { Text(stringResource(R.string.later), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface))
                }
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        if (model.screen == Screen.Welcome) {
                            Icon(painterResource(R.drawable.ic_paw), null,
                                Modifier.padding(start = 20.dp, end = 12.dp).size(28.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        } else {
                            IconButton(onClick = { scope.launch { drawer.open() } }) {
                                Icon(painterResource(R.drawable.ic_menu), stringResource(R.string.open_menu))
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = model::showSettings) {
                            Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
        ) { insets ->
            when (model.screen) {
                Screen.Welcome -> WelcomeScreen(Modifier.padding(insets)) { model.navigate(Screen.Home) }
                Screen.Home -> HomeScreen(Modifier.padding(insets)) { model.navigate(Screen.Pets) }
                Screen.Pets -> PetsScreen(Modifier.padding(insets))
            }
        }
    }
    if (model.settingsOpen) SettingsSheet(model)
}
