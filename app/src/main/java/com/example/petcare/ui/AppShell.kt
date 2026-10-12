package com.example.petcare.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
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
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(model: PetCareViewModel = viewModel()) {
    val title by model.title.collectAsStateWithLifecycle()
    val welcomeImage by model.welcomeImage.collectAsStateWithLifecycle()
    LaunchedEffect(welcomeImage) { model.refreshSettingsImage(welcomeImage) }
    val petSnapshot by model.pets.collectAsStateWithLifecycle()
    val recordSnapshot by model.records.collectAsStateWithLifecycle()
    val pets = petSnapshot?.takeIf { it.first === model.activeRepository }?.second
    val records = recordSnapshot.takeIf { it.first === model.activeRepository }?.second.orEmpty()
    val selected = pets?.firstOrNull { it.pet.id == model.selectedPetId }
    val context = LocalContext.current
    var permissionExplanation by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        model.refreshNotificationsAndAlarms()
    }
    fun systemNotificationSettings() {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(model.notice) {
        model.notice?.let { snackbar.showSnackbar(it); model.notice = null }
    }
    val notificationNotice: @Composable () -> Unit = {
        if (!model.notificationsEnabled) NotificationNotice(onEnable = { permissionExplanation = true }, onSettings = ::systemNotificationSettings)
    }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    fun goTo(screen: Screen) {
        model.navigate(screen)
        scope.launch { drawer.close() }
    }
    BackHandler(enabled = drawer.isOpen || model.screen != Screen.Welcome) {
        if (drawer.isOpen) scope.launch { drawer.close() }
        else model.back()
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
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(if (model.demoMode) "Demo · $title" else title, maxLines = 1, overflow = TextOverflow.Ellipsis,
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
                        IconButton(onClick = model::showSettings, enabled = !model.careSaving) {
                            Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets)) {
                if (model.demoMode) {
                    val simulated = model.demoNow?.atZone(model.nowZone())?.format(
                        DateTimeFormatter.ofPattern("MMM d, h:mm a")) ?: "Starting"
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("DEMO · $simulated · 1 sample day per minute while visible",
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Box(Modifier.weight(1f)) { when (model.screen) {
                Screen.Welcome -> WelcomeScreen(Modifier, imagePath = welcomeImage,
                    notificationNotice = notificationNotice, onEnter = model::enter)
                Screen.Home -> HomeScreen(Modifier, pets, model.dataError, model::startPet, model::openPet,
                    today = model.nowDate())
                Screen.Pets -> HomeScreen(Modifier, pets, model.dataError, model::startPet, model::openPet,
                    medicationArea = true, today = model.nowDate())
                Screen.AddPet, Screen.EditPet -> PetForm(model, Modifier)
                Screen.FirstTreatment, Screen.ConfirmTreatment -> TreatmentForm(model, selected, Modifier, notificationNotice)
                Screen.NextDate -> NextDateForm(model, selected, Modifier, notificationNotice)
                Screen.Detail -> TreatmentDetail(model, selected, records.filter { it.petId == model.selectedPetId }, Modifier)
                } }
            }
        }
    }
    if (model.settingsOpen) SettingsSheet(model)
    if (model.nextTreatmentPrompt && !model.schedulingFailed) AlertDialog(
        onDismissRequest = { model.nextTreatmentPrompt = false },
        title = { Text("Treatment recorded") },
        text = { Text("Would you like to schedule another treatment?") },
        confirmButton = { TextButton(onClick = model::scheduleAfterConfirmation) { Text("Yes") } },
        dismissButton = { TextButton(onClick = { model.nextTreatmentPrompt = false }) { Text("Not now") } })
    if (model.newPetPrompt) AlertDialog(onDismissRequest = { model.newPetPrompt = false },
        title = { Text("Pet saved") },
        text = { Text("Would you like to add the first flea treatment now? Use the menu at the top left to return to Medication or Home anytime.") },
        confirmButton = { TextButton(onClick = model::startTreatment) { Text("Add treatment") } },
        dismissButton = { TextButton(onClick = { model.newPetPrompt = false }) { Text("Not now") } })
    if (permissionExplanation) AlertDialog(onDismissRequest = { permissionExplanation = false },
        title = { Text("Phone reminders") },
        text = { Text("Allow notifications to receive purchase, treatment and overdue reminders. You can still save and view records if you decline. Android may delay reminders to save battery.") },
        confirmButton = { TextButton(onClick = {
            permissionExplanation = false
            if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else systemNotificationSettings()
        }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { permissionExplanation = false }) { Text("Not now") } })
    if (model.schedulingFailed) AlertDialog(onDismissRequest = { },
        title = { Text("Reminder scheduling needs attention") },
        text = { Text("Reminder scheduling could not finish. Retry now or reopen the app later; phone alerts may be missing until scheduling succeeds.") },
        confirmButton = { TextButton(onClick = model::refreshNotificationsAndAlarms) { Text("Retry") } },
        dismissButton = { TextButton(onClick = { model.dismissScheduleWarning() }) { Text("Keep using records") } })
}
