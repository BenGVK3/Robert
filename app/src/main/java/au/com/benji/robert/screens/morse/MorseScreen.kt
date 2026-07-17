package au.com.benji.robert.screens.morse

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MorseScreen(
    paddingValues: PaddingValues,
    viewModel: MorseViewModel = viewModel(),
    onBack: () -> Unit
) {
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        MorseSettingsDialog(
            settings = settings,
            onDismiss = { showSettings = false },
            onSave = { viewModel.updateSettings(it) },
            onResetKoch = { viewModel.resetKochCourse() },
            onResetReceive = { viewModel.resetReceiveCourse() }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (currentSection == MorseSection.Menu) "MORSE SUITE" 
                        else if (currentSection == MorseSection.Trainer) "TRAINING"
                        else currentSection.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = RobertColors.Primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSection == MorseSection.Menu) onBack()
                        else viewModel.setSection(MorseSection.Menu)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, null, tint = RobertColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = RobertColors.Background
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(top = innerPadding.calculateTopPadding(), bottom = paddingValues.calculateBottomPadding())
            .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "MorseSectionTransition"
            ) { section ->
                when (section) {
                    MorseSection.Menu -> MorseMenu(viewModel)
                    MorseSection.Send -> SendScreen(viewModel)
                    MorseSection.Decoder -> DecoderScreen(viewModel)
                    MorseSection.Trainer -> TrainerScreen(viewModel)
                    MorseSection.Simulator -> SimulatorScreen(viewModel)
                    MorseSection.Practice -> PracticeScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MorseSettingsDialog(
    settings: MorseSettings,
    onDismiss: () -> Unit,
    onSave: (MorseSettings) -> Unit,
    onResetKoch: () -> Unit,
    onResetReceive: () -> Unit
) {
    var showKochConfirm by remember { mutableStateOf(false) }
    var showReceiveConfirm by remember { mutableStateOf(false) }

    if (showKochConfirm) {
        AlertDialog(
            onDismissRequest = { showKochConfirm = false },
            title = { Text("Reset Sending Progress?") },
            text = { Text("This will reset your progress in the Sending course back to Lesson 1. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetKoch()
                        showKochConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RobertColors.StatusRed)
                ) { Text("RESET") }
            },
            dismissButton = {
                TextButton(onClick = { showKochConfirm = false }) { Text("CANCEL") }
            }
        )
    }

    if (showReceiveConfirm) {
        AlertDialog(
            onDismissRequest = { showReceiveConfirm = false },
            title = { Text("Reset Receive Progress?") },
            text = { Text("This will reset your progress in the Receive Practice (Listening) course back to Lesson 1. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetReceive()
                        showReceiveConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RobertColors.StatusRed)
                ) { Text("RESET") }
            },
            dismissButton = {
                TextButton(onClick = { showReceiveConfirm = false }) { Text("CANCEL") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                PreferenceCard(title = "Keyer Configuration", icon = Icons.Default.Keyboard) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Text("Keyer Type", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KeyerMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.keyerMode == mode,
                                    onClick = { onSave(settings.copy(keyerMode = mode)) },
                                    label = { Text(mode.name) }
                                )
                            }
                        }
                    }
                }

                PreferenceCard(title = "Audio", icon = Icons.AutoMirrored.Filled.VolumeUp) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        SettingSlider("Sidetone Volume", settings.sidetoneVolume, { onSave(settings.copy(sidetoneVolume = it)) }, 0f..1f)
                        SettingSlider("Tone Frequency", settings.frequency.toFloat(), { onSave(settings.copy(frequency = it.toInt())) }, 300f..1000f, " Hz")
                    }
                }

                PreferenceCard(title = "Timing", icon = Icons.Default.Timer) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        SettingSlider("Overall Speed", settings.wpm.toFloat(), { onSave(settings.copy(wpm = it.toInt())) }, 5f..45f, " WPM")
                        SettingSlider("Farnsworth Speed", settings.farnsworthWpm.toFloat(), { onSave(settings.copy(farnsworthWpm = it.toInt())) }, 5f..45f, " WPM")
                    }
                }

                PreferenceCard(title = "Danger Zone", icon = Icons.Default.Warning) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Button(
                            onClick = { showKochConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RobertColors.StatusRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RESET SENDING COURSE PROGRESS")
                        }
                        
                        Button(
                            onClick = { showReceiveConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RobertColors.StatusRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RESET RECEIVE COURSE PROGRESS")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE") }
        }
    )
}

@Composable
fun PreferenceCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = RobertColors.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = RobertColors.TextSecondary.copy(alpha = 0.1f))
            content()
        }
    }
}

@Composable
fun SettingSlider(label: String, value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>, suffix: String = "") {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = RobertColors.TextSecondary)
            Text(if (suffix.isNotEmpty()) "${value.toInt()}$suffix" else String.format(Locale.getDefault(), "%.2f", value), fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
fun MorseMenu(viewModel: MorseViewModel) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    
    val menuItems = listOf(
        MorseMenuItem("Training", "Course & Practice Modes • 20+ WPM Recommended", Icons.Default.School, MorseSection.Trainer, RobertColors.Primary),
        MorseMenuItem("Practice", "Callsigns, Words, Numbers & Sending", Icons.Default.Keyboard, MorseSection.Practice, RobertColors.StatusGreen),
        MorseMenuItem("Decoder", "Live audio transcription", Icons.Default.GraphicEq, MorseSection.Decoder, RobertColors.StatusOrange),
        MorseMenuItem("Simulator", "Virtual Radio Operator", Icons.Default.RecordVoiceOver, MorseSection.Simulator, Color(0xFF9C27B0))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Text("Training Progress", style = MaterialTheme.typography.labelMedium, color = RobertColors.TextSecondary)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Keyboard, null, tint = RobertColors.Primary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sending: ${progress.kochLessonIndex + 1}/53", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { (progress.kochLessonIndex + 1) / 53f },
                                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(0.9f).height(4.dp).clip(CircleShape),
                                color = RobertColors.Primary
                            )
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color(0xFF00BCD4), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Receive: ${progress.receiveLessonIndex + 1}/53", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { (progress.receiveLessonIndex + 1) / 53f },
                                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(0.9f).height(4.dp).clip(CircleShape),
                                color = Color(0xFF00BCD4)
                            )
                        }

                        Box(contentAlignment = Alignment.Center) {
                            val overallProgress = ((progress.kochLessonIndex + 1 + progress.receiveLessonIndex + 1) / 106f)
                            CircularProgressIndicator(
                                progress = { overallProgress },
                                modifier = Modifier.size(56.dp),
                                strokeWidth = 6.dp,
                                color = RobertColors.Primary,
                                trackColor = RobertColors.Surface
                            )
                            Text("${(overallProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(Modifier.height(Spacing.Small))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatItemSmall("Accuracy", "${progress.totalAccuracy.toInt()}%")
                        StatItemSmall("Streak", "${progress.practiceStreak}")
                        StatItemSmall("Best", "${progress.longestStreak}")
                        StatItemSmall("Chars", "${progress.totalCharactersCopied}")
                    }
                }
            }
        }

        items(menuItems) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.setSection(item.section) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
            ) {
                Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(item.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(item.icon, null, tint = item.color, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.width(Spacing.Medium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = RobertColors.TextSecondary)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = RobertColors.TextSecondary.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun StatItemSmall(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black)
    }
}

data class MorseMenuItem(val title: String, val subtitle: String, val icon: ImageVector, val section: MorseSection, val color: Color)

@Composable
fun TrainerScreen(viewModel: MorseViewModel) {
    val trainerMode by viewModel.trainerMode.collectAsStateWithLifecycle()
    
    AnimatedContent(targetState = trainerMode, label = "TrainerMode") { mode ->
        if (mode == null) {
            TrainerHome(viewModel)
        } else if (mode == TrainerMode.Character) {
             ReceivePracticeInternal(viewModel)
        } else {
            TrainerExercise(viewModel, mode)
        }
    }
}

@Composable
fun TrainerHome(viewModel: MorseViewModel) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    var showCurriculum by remember { mutableStateOf(false) }

    if (showCurriculum) {
        CurriculumDialog(viewModel.kochSequence, progress.kochLessonIndex + 1) { showCurriculum = false }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Text("STRUCTURED LEARNING", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = RobertColors.Primary)
        
        // Sending Card (formerly Koch Course)
        Card(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.setTrainerMode(TrainerMode.Koch) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
        ) {
            Row(modifier = Modifier.padding(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(RobertColors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Keyboard, null, tint = RobertColors.Primary, modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.width(Spacing.Medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sending", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Lesson ${progress.kochLessonIndex + 1} / 53", color = RobertColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                    LinearProgressIndicator(
                        progress = { (progress.kochLessonIndex + 1) / 53f },
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = RobertColors.Primary
                    )
                }
                Spacer(Modifier.width(Spacing.Medium))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = RobertColors.Primary)
            }
        }

        // Receive Card (formerly Receive Practice)
        Card(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.setTrainerMode(TrainerMode.Character) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
        ) {
            Row(modifier = Modifier.padding(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF00BCD4).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color(0xFF00BCD4), modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.width(Spacing.Medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Receive", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Lesson ${progress.receiveLessonIndex + 1} / 53", color = RobertColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                    LinearProgressIndicator(
                        progress = { (progress.receiveLessonIndex + 1) / 53f },
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color(0xFF00BCD4)
                    )
                }
                Spacer(Modifier.width(Spacing.Medium))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF00BCD4))
            }
        }

        // Curriculum Card
        Card(
            modifier = Modifier.fillMaxWidth().clickable { showCurriculum = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
        ) {
            Row(modifier = Modifier.padding(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(RobertColors.StatusOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.List, null, tint = RobertColors.StatusOrange, modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.width(Spacing.Medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Curriculum", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Full 53-Lesson Sequence", color = RobertColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(Spacing.Medium))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = RobertColors.StatusOrange)
            }
        }
    }
}

data class TrainerMenuItemDef(val title: String, val subtitle: String, val icon: ImageVector, val mode: TrainerMode, val color: Color)

@Composable
fun PracticeScreen(viewModel: MorseViewModel) {
    val trainerMode by viewModel.trainerMode.collectAsStateWithLifecycle()
    
    AnimatedContent(targetState = trainerMode, label = "PracticeMode") { mode ->
        if (mode == null) {
            PracticeHome(viewModel)
        } else {
            TrainerExercise(viewModel, mode)
        }
    }
}

@Composable
fun PracticeHome(viewModel: MorseViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Text("FREE PRACTICE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = RobertColors.Primary)
        
        // Free-form Send Card
        Card(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.setSection(MorseSection.Send) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
        ) {
            Row(modifier = Modifier.padding(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(RobertColors.StatusGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Keyboard, null, tint = RobertColors.StatusGreen, modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.width(Spacing.Medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Send", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Free-form sending practice", color = RobertColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = RobertColors.TextSecondary.copy(alpha = 0.5f))
            }
        }

        val practiceModes = listOf(
            TrainerMenuItemDef("Callsigns", "Copy realistic world callsigns", Icons.Default.Public, TrainerMode.Callsigns, Color(0xFF00BCD4)),
            TrainerMenuItemDef("Words", "Common English words", Icons.Default.TextFields, TrainerMode.Words, RobertColors.Primary),
            TrainerMenuItemDef("Numbers", "Focus on digits", Icons.Default.Numbers, TrainerMode.Numbers, RobertColors.StatusOrange),
            TrainerMenuItemDef("Q Codes Etc.", "Glossary & Radio Jargon", Icons.AutoMirrored.Filled.MenuBook, TrainerMode.Prosigns, Color(0xFF9C27B0))
        )

        practiceModes.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.setTrainerMode(item.mode) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
            ) {
                Row(modifier = Modifier.padding(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(item.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(item.icon, null, tint = item.color, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.width(Spacing.Medium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, color = RobertColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = RobertColors.TextSecondary.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun CurriculumDialog(sequence: List<Char>, currentLesson: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Morse Curriculum", fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "The curriculum introduces characters one by one. Master each lesson with 90% accuracy before moving on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RobertColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                items(sequence.size) { index ->
                    val lessonNum = index + 1
                    val char = sequence[index]
                    val isUnlocked = lessonNum <= currentLesson + 1 // Lesson 1 has 2 chars
                    
                    val actualChar = if (index == 0) "K & M" else char.toString()
                    val actualLesson = if (index == 0) 1 else index + 1
                    
                    if (index == 1) return@items // Skip index 1 since it's merged with 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnlocked) RobertColors.Surface else RobertColors.Surface.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (actualLesson == currentLesson) BorderStroke(1.dp, RobertColors.Primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Lesson $actualLesson",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUnlocked) RobertColors.Primary else RobertColors.TextSecondary
                                )
                                Text(
                                    actualChar,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) RobertColors.TextPrimary else RobertColors.TextSecondary
                                )
                            }
                            
                            if (isUnlocked) {
                                Text(
                                    if (index == 0) "-.-  --" else MorseCodeMap[char] ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = RobertColors.Primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.CheckCircle, null, tint = RobertColors.StatusGreen, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.Lock, null, tint = RobertColors.TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("GOT IT") }
        }
    )
}

@Composable
fun TrainerExercise(viewModel: MorseViewModel, mode: TrainerMode) {
    val session by viewModel.trainerSession.collectAsStateWithLifecycle()
    val target by viewModel.trainerTarget.collectAsStateWithLifecycle()
    val targetMeaning by viewModel.trainerTargetMeaning.collectAsStateWithLifecycle()
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val feedback by viewModel.trainerFeedback.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val revealed by viewModel.revealed.collectAsStateWithLifecycle()
    val currentSymbolBuffer by viewModel.currentSymbolBuffer.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        // Lesson Progress Header - Only for structured learning
        if (mode == TrainerMode.Koch) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.setSection(MorseSection.Menu) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Lesson ${session.lessonNumber}",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                            LinearProgressIndicator(
                                progress = { session.currentCorrect.toFloat() / session.targetCorrect },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = RobertColors.StatusGreen,
                                trackColor = RobertColors.TextSecondary.copy(alpha = 0.1f)
                            )
                        }
                        Text(
                            "${session.currentCorrect}/${session.targetCorrect}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.Small))
        } else {
            // Simple header for endless modes
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { viewModel.setSection(MorseSection.Menu) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Text(mode.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Target Character Area - Smaller card
        Card(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("TARGET", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
                    Text(
                        target,
                        style = if (target.length > 5) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = RobertColors.Primary
                    )
                    
                    if (targetMeaning != null) {
                        Text(
                            targetMeaning!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = RobertColors.TextSecondary,
                            maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    
                    // Visual Hint Logic: Show hint for first 3 attempts of a new character
                    val targetChar = target.firstOrNull() ?: ' '
                    val isIntroduced = session.introducedCharacters.contains(targetChar)
                    val appearances = session.newCharAppearances[targetChar] ?: 0
                    val shouldShowAutoHint = mode == TrainerMode.Koch && isIntroduced && appearances <= 3 && targetChar != ' '
                    
                    if (revealed || shouldShowAutoHint) {
                        Text(
                            target.map { MorseCodeMap[it] ?: "" }.joinToString("  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = RobertColors.Primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Your Morse Area - Prominent
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isPlaying) {
                    PlaybackAnimation()
                } else if (feedback != null) {
                    TrainerFeedbackDisplay(feedback!!)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("YOUR MORSE", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
                        Text(
                            currentSymbolBuffer,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = RobertColors.Primary.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp
                        )

                        Spacer(Modifier.height(Spacing.Medium))

                        Text("DECODED", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
                        val statusColor = when (session.lastDecodedStatus) {
                            DecodeStatus.Correct -> RobertColors.StatusGreen
                            DecodeStatus.Wrong -> RobertColors.StatusOrange
                            DecodeStatus.Invalid -> RobertColors.StatusRed
                            else -> RobertColors.TextSecondary.copy(alpha = 0.2f)
                        }
                        Text(
                            decodedText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                    }
                }
            }
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            ControlIconButton(Icons.Default.Hearing, "PLAY", onClick = { viewModel.playCurrentText() }, tint = RobertColors.Primary)
            ControlIconButton(Icons.Default.Visibility, "REVEAL", onClick = { viewModel.revealCurrentTarget() }, enabled = feedback == null, tint = RobertColors.StatusOrange)
            ControlIconButton(Icons.Default.Refresh, "RESET", onClick = { viewModel.resetTrainerExercise() }, tint = Color(0xFFF57C00)) // Orange
            ControlIconButton(Icons.Default.SkipNext, "SKIP", onClick = { viewModel.nextTrainerExercise() }, tint = RobertColors.TextSecondary)
        }

        // Keyer Area
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val isKeyBusy by viewModel.isKeyBusy.collectAsStateWithLifecycle()
            val lastElementSent by viewModel.lastElementSent.collectAsStateWithLifecycle()
            if (settings.keyerMode == KeyerMode.Straight) {
                StraightKeyArea({ viewModel.onKeyDown(false) }, { viewModel.onKeyUp(false) })
            } else {
                IambicPaddleArea(settings, isKeyBusy, lastElementSent, 
                    { viewModel.onKeyDown(false) }, { viewModel.onKeyUp(false) },
                    { viewModel.onKeyDown(true) }, { viewModel.onKeyUp(true) }
                )
            }
        }
    }
}

@Composable
fun ReceivePracticeInternal(viewModel: MorseViewModel) {
    val session by viewModel.trainerSession.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentText by viewModel.currentText.collectAsStateWithLifecycle()
    val feedback by viewModel.receiveFeedback.collectAsStateWithLifecycle()
    val revealed by viewModel.revealed.collectAsStateWithLifecycle()
    var userAnswer by remember { mutableStateOf("") }
    
    // Clear answer when target changes or is revealed
    LaunchedEffect(currentText, revealed, feedback) {
        if (currentText.isEmpty() || revealed || feedback == null) {
            userAnswer = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        // Lesson Progress Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.setSection(MorseSection.Menu) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Lesson ${session.lessonNumber}",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium
                        )
                        LinearProgressIndicator(
                            progress = { session.currentCorrect.toFloat() / session.targetCorrect },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = RobertColors.StatusGreen,
                            trackColor = RobertColors.TextSecondary.copy(alpha = 0.1f)
                        )
                    }
                    Text(
                        "${session.currentCorrect}/${session.targetCorrect}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isPlaying) {
                    PlaybackAnimation()
                } else if (feedback != null) {
                    FeedbackDisplay(feedback!!)
                } else if (revealed) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            currentText,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = RobertColors.StatusOrange
                        )
                        Text(
                            currentText.map { MorseCodeMap[it] ?: "" }.joinToString(" "),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Monospace,
                            color = RobertColors.StatusOrange
                        )
                    }
                } else {
                    Text(
                        userAnswer.ifEmpty { "READY" },
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (userAnswer.isEmpty()) RobertColors.TextSecondary.copy(alpha = 0.2f) else RobertColors.Primary,
                        letterSpacing = 4.sp
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            if (feedback == null && !revealed) {
                Button(
                    onClick = { viewModel.playCurrentText() },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RobertColors.Primary),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PLAY", fontSize = 10.sp, maxLines = 1)
                }

                Button(
                    onClick = { viewModel.revealCurrentTarget() },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RobertColors.StatusOrange),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("REVEAL", fontSize = 10.sp, maxLines = 1)
                }

                Button(
                    onClick = { viewModel.checkReceiveAnswer(userAnswer) },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = userAnswer.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CHECK", fontSize = 10.sp, maxLines = 1)
                }
            } else if (feedback?.isCorrect != true) {
                Button(
                    onClick = { viewModel.nextTrainerExercise() },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RobertColors.StatusGreen),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("NEXT", fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        
        MorseKeyboard(
            onKey = { if (feedback == null && !revealed) userAnswer += it },
            onBackspace = { if (userAnswer.isNotEmpty() && feedback == null && !revealed) userAnswer = userAnswer.dropLast(1) },
            enabled = feedback == null && !revealed
        )
    }
}

@Composable
fun ControlIconButton(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true, tint: Color = RobertColors.Primary) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.5f))
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = tint)
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

@Composable
fun TrainerFeedbackDisplay(feedback: TrainerFeedback) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            feedback.message,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = if (feedback.isCorrect) RobertColors.StatusGreen else RobertColors.StatusRed
        )
        Text("You copied: \"${feedback.received}\"", color = RobertColors.TextPrimary)
        if (!feedback.isCorrect) {
            Text("Correct answer: \"${feedback.expected}\"", fontWeight = FontWeight.Bold, color = RobertColors.Primary)
        }
    }
}

@Composable
fun FeedbackDisplay(feedback: ReceiveFeedback) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            if (feedback.isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
            null,
            tint = if (feedback.isCorrect) RobertColors.StatusGreen else RobertColors.StatusRed,
            modifier = Modifier.size(64.dp)
        )
        Text(
            if (feedback.isCorrect) "✓ Correct!" else "✗ Incorrect",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = if (feedback.isCorrect) RobertColors.StatusGreen else RobertColors.StatusRed
        )
        if (!feedback.isCorrect) {
            Text("Correct: ${feedback.expected}", style = MaterialTheme.typography.titleMedium, color = RobertColors.TextSecondary)
        }
    }
}

@Composable
fun SendScreen(viewModel: MorseViewModel) {
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentSymbolBuffer by viewModel.currentSymbolBuffer.collectAsStateWithLifecycle()
    val lastElementSent by viewModel.lastElementSent.collectAsStateWithLifecycle()
    val isKeyBusy by viewModel.isKeyBusy.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
        ) {
            Box(modifier = Modifier.padding(Spacing.Medium).fillMaxSize()) {
                Text(decodedText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = RobertColors.Primary, fontFamily = FontFamily.Monospace)
                Text(currentSymbolBuffer, modifier = Modifier.align(Alignment.BottomStart), style = MaterialTheme.typography.labelMedium, color = RobertColors.TextSecondary.copy(alpha = 0.5f))
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Large))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (settings.keyerMode == KeyerMode.Straight) {
                StraightKeyArea({ viewModel.onKeyDown(false) }, { viewModel.onKeyUp(false) })
            } else {
                IambicPaddleArea(settings, isKeyBusy, lastElementSent, 
                    { viewModel.onKeyDown(false) }, { viewModel.onKeyUp(false) },
                    { viewModel.onKeyDown(true) }, { viewModel.onKeyUp(true) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            OutlinedButton(onClick = { viewModel.clearSentText() }, modifier = Modifier.weight(1f)) { Text("CLEAR") }
            Button(onClick = { viewModel.submitSentText() }, modifier = Modifier.weight(1f)) { Text("SUBMIT") }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DecoderScreen(viewModel: MorseViewModel) {
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val isDecoding by viewModel.isDecoding.collectAsStateWithLifecycle()
    val visualizerData by viewModel.visualizerData.collectAsStateWithLifecycle()
    
    val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium)) {
        Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface), shape = RoundedCornerShape(24.dp)) {
            Box(modifier = Modifier.padding(Spacing.Medium).fillMaxSize()) {
                Text(
                    decodedText.ifEmpty { if (isDecoding) "LISTENING..." else "START LISTENING..." }, 
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.Medium))

        // Spectrum Visualizer
        Card(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                if (isDecoding) {
                    SpectrumVisualizer(visualizerData)
                } else {
                    Text("AUDIO VISUALIZER", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary.copy(alpha = 0.3f))
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Button(
            onClick = { 
                if (micPermissionState.status.isGranted) {
                    viewModel.toggleDecoding() 
                } else {
                    micPermissionState.launchPermissionRequest()
                }
            }, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = if (isDecoding) RobertColors.StatusRed else RobertColors.Primary)
        ) {
            Icon(if (isDecoding) Icons.Default.MicOff else Icons.Default.Mic, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isDecoding) "STOP" else "START")
        }
    }
}

@Composable
fun SpectrumVisualizer(data: FloatArray) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        data.forEach { magnitude ->
            val heightFactor by animateFloatAsState(
                targetValue = magnitude.coerceIn(0.02f, 1f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "barHeight"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFactor)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                RobertColors.Secondary,
                                RobertColors.Primary.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun SimulatorScreen(viewModel: MorseViewModel) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium)) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            items(messages) { (author, text) -> ChatBubble(author, text) }
        }
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface.copy(alpha = 0.5f))) {
            Text(decodedText.ifEmpty { "KEY YOUR REPLY..." }, modifier = Modifier.padding(8.dp), color = RobertColors.Primary, fontFamily = FontFamily.Monospace)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Button(onClick = { viewModel.submitSentText() }, modifier = Modifier.weight(1f), enabled = decodedText.isNotEmpty()) { Text("SEND") }
            OutlinedButton(onClick = { viewModel.clearSentText() }) { Icon(Icons.Default.Clear, null) }
        }
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val isKeyBusy by viewModel.isKeyBusy.collectAsStateWithLifecycle()
            val lastElementSent by viewModel.lastElementSent.collectAsStateWithLifecycle()
            if (settings.keyerMode == KeyerMode.Straight) {
                StraightKeyArea({ viewModel.onKeyDown(false) }, { viewModel.onKeyUp(false) })
            } else {
                IambicPaddleArea(settings, isKeyBusy, lastElementSent, 
                    { viewModel.onKeyDown(false) }, { viewModel.onKeyUp(false) },
                    { viewModel.onKeyDown(true) }, { viewModel.onKeyUp(true) }
                )
            }
        }
    }
}

@Composable
fun ChatBubble(author: String, text: String) {
    val isMe = author == "You"
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Surface(color = if (isMe) RobertColors.Primary else RobertColors.Surface, shape = RoundedCornerShape(12.dp)) {
            Text(text, modifier = Modifier.padding(8.dp), color = if (isMe) Color.White else RobertColors.TextPrimary, fontFamily = FontFamily.Monospace)
        }
        Text(author, style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
    }
}

@Composable
fun MorseKeyboard(onKey: (Char) -> Unit, onBackspace: () -> Unit, enabled: Boolean = true) {
    val rows = listOf("1234567890".toList(), "QWERTYUIOP".toList(), "ASDFGHJKL".toList(), "ZXCVBNM".toList())
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEachIndexed { index, row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                row.forEach { char ->
                    KeyButton(text = char.toString(), onClick = { onKey(char) }, modifier = Modifier.weight(1f), enabled = enabled)
                }
                if (index == 3) {
                    KeyButton(icon = Icons.AutoMirrored.Filled.Backspace, onClick = onBackspace, modifier = Modifier.weight(1.5f), enabled = enabled)
                }
            }
        }
    }
}

@Composable
fun KeyButton(modifier: Modifier = Modifier, text: String? = null, icon: ImageVector? = null, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier.height(42.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = RobertColors.Surface, contentColor = RobertColors.TextPrimary), enabled = enabled) {
        if (icon != null) Icon(icon, null, modifier = Modifier.size(18.dp))
        else if (text != null) Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlaybackAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(0.8f, 1.2f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "scale")
    Box(modifier = Modifier.size(160.dp).clip(CircleShape).background(Brush.radialGradient(listOf(RobertColors.Primary.copy(alpha = 0.2f * scale), Color.Transparent))))
}

@Composable
fun StraightKeyArea(onDown: () -> Unit, onUp: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 1.05f else 1f, label = "scale")

    Surface(
        modifier = Modifier
            .size(180.dp)
            .scale(scale)
            .clip(CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDown()
                    waitForUpOrCancellation()
                    isPressed = false
                    onUp()
                }
            },
        color = if (isPressed) RobertColors.Primary.copy(alpha = 0.3f) else RobertColors.Surface,
        border = BorderStroke(4.dp, if (isPressed) RobertColors.Primary else RobertColors.Surface)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.TouchApp, 
                null, 
                modifier = Modifier.size(48.dp), 
                tint = if (isPressed) RobertColors.Primary else RobertColors.TextSecondary
            )
        }
    }
}

@Composable
fun IambicPaddleArea(settings: MorseSettings, isBusy: Boolean, lastElement: String, onLeftDown: () -> Unit, onLeftUp: () -> Unit, onRightDown: () -> Unit, onRightUp: () -> Unit) {
    val leftLabel = if (settings.paddleOrientation == PaddleOrientation.LeftDitRightDah) "DIT" else "DAH"
    val rightLabel = if (settings.paddleOrientation == PaddleOrientation.LeftDitRightDah) "DAH" else "DIT"
    Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Paddle(Modifier.weight(1f), leftLabel, isBusy && ((leftLabel == "DIT" && lastElement == ".") || (leftLabel == "DAH" && lastElement == "-")), onLeftDown, onLeftUp)
        Paddle(Modifier.weight(1f), rightLabel, isBusy && ((rightLabel == "DIT" && lastElement == ".") || (rightLabel == "DAH" && lastElement == "-")), onRightDown, onRightUp)
    }
}

@Composable
fun Paddle(modifier: Modifier, label: String, isActive: Boolean, onDown: () -> Unit, onUp: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed || isActive) 1.05f else 1f, label = "scale")

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDown()
                    waitForUpOrCancellation()
                    isPressed = false
                    onUp()
                }
            },
        color = if (isPressed || isActive) RobertColors.Primary.copy(alpha = 0.3f) else RobertColors.Surface,
        border = BorderStroke(2.dp, if (isPressed || isActive) RobertColors.Primary else RobertColors.Surface)
    ) {
        Box(contentAlignment = Alignment.Center) { 
            Text(
                label, 
                fontWeight = FontWeight.Black, 
                color = if (isPressed || isActive) RobertColors.Primary else RobertColors.TextSecondary
            ) 
        }
    }
}
