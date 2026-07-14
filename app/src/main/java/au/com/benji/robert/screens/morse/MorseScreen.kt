package au.com.benji.robert.screens.morse

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
            onSave = { viewModel.updateSettings(it) }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (currentSection == MorseSection.Menu) "MORSE SUITE" else currentSection.name.uppercase(),
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
        containerColor = RobertColors.Background,
        modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "MorseSectionTransition"
            ) { section ->
                when (section) {
                    MorseSection.Menu -> MorseMenu(viewModel)
                    MorseSection.Receive -> ReceiveScreen(viewModel)
                    MorseSection.Send -> SendScreen(viewModel)
                    MorseSection.Decoder -> DecoderScreen(viewModel)
                    MorseSection.Trainer -> TrainerScreen(viewModel)
                    MorseSection.Simulator -> SimulatorScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MorseSettingsDialog(
    settings: MorseSettings,
    onDismiss: () -> Unit,
    onSave: (MorseSettings) -> Unit
) {
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
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("SAVE CONFIGURATION") }
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
        MorseMenuItem("Trainer", "Structured Koch Course", Icons.Default.School, MorseSection.Trainer, RobertColors.Primary),
        MorseMenuItem("Receive", "Improve your copying skills", Icons.Default.Hearing, MorseSection.Receive, Color(0xFF00BCD4)),
        MorseMenuItem("Send", "Master your keying technique", Icons.Default.Keyboard, MorseSection.Send, RobertColors.StatusGreen),
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
                    Text("Overall Mastery", style = MaterialTheme.typography.labelMedium, color = RobertColors.TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${progress.currentLessonIndex + 1} / 40", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                        Spacer(Modifier.weight(1f))
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (progress.currentLessonIndex + 1) / 40f },
                                modifier = Modifier.size(64.dp),
                                strokeWidth = 8.dp,
                                color = RobertColors.Primary,
                                trackColor = RobertColors.Surface
                            )
                            Text("${((progress.currentLessonIndex + 1) * 100 / 40)}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(Modifier.height(Spacing.Medium))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                        StatItemSmall("Accuracy", "${progress.totalAccuracy.toInt()}%")
                        StatItemSmall("Streak", "${progress.practiceStreak}")
                        StatItemSmall("Best", "${progress.longestStreak}")
                        StatItemSmall("Total Chars", "${progress.totalCharactersCopied}")
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
fun ReceiveScreen(viewModel: MorseViewModel) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentText by viewModel.currentText.collectAsStateWithLifecycle()
    val feedback by viewModel.receiveFeedback.collectAsStateWithLifecycle()
    val currentType by viewModel.currentExerciseType.collectAsStateWithLifecycle()
    var userAnswer by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseType.entries.take(4).forEach { type ->
                FilterChip(
                    selected = currentType == type,
                    onClick = { viewModel.generateNewExercise(type); userAnswer = "" },
                    label = { Text(type.name, fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
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
            Button(
                onClick = { viewModel.playCurrentText() },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RobertColors.Primary)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("PLAY")
            }

            if (feedback == null) {
                Button(
                    onClick = { viewModel.checkReceiveAnswer(userAnswer) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = userAnswer.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("CHECK")
                }
            } else {
                Button(
                    onClick = { viewModel.generateNewExercise(currentType); userAnswer = "" },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RobertColors.StatusGreen)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    Spacer(Modifier.width(8.dp))
                    Text("NEXT")
                }
            }
        }
        
        MorseKeyboard(
            onKey = { if (feedback == null) userAnswer += it },
            onBackspace = { if (userAnswer.isNotEmpty()) userAnswer = userAnswer.dropLast(1) },
            enabled = feedback == null
        )
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
fun TrainerScreen(viewModel: MorseViewModel) {
    val trainerMode by viewModel.trainerMode.collectAsStateWithLifecycle()
    
    AnimatedContent(targetState = trainerMode, label = "TrainerMode") { mode ->
        if (mode == null) {
            TrainerHome(viewModel)
        } else {
            TrainerExercise(viewModel, mode)
        }
    }
}

@Composable
fun TrainerHome(viewModel: MorseViewModel) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Text("LEARN MORSE", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = RobertColors.Primary)
        
        Card(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.setTrainerMode(TrainerMode.Koch) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
        ) {
            Row(modifier = Modifier.padding(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Koch Course", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Lesson ${progress.currentLessonIndex + 1} / 40", color = RobertColors.TextSecondary)
                    LinearProgressIndicator(
                        progress = { (progress.currentLessonIndex + 1) / 40f },
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = RobertColors.Primary
                    )
                }
                Spacer(Modifier.width(Spacing.Medium))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = RobertColors.Primary)
            }
        }
        
        Text("Other Modes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        val otherModes = listOf(
            TrainerMenuItemDef("Callsigns", "Copy realistic world callsigns", Icons.Default.Public, TrainerMode.Callsigns, Color(0xFF00BCD4)),
            TrainerMenuItemDef("Words", "Common English words", Icons.Default.TextFields, TrainerMode.Words, RobertColors.StatusGreen),
            TrainerMenuItemDef("Numbers", "Focus on digits", Icons.Default.Numbers, TrainerMode.Numbers, RobertColors.StatusOrange)
        )
        
        otherModes.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.setTrainerMode(item.mode) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
            ) {
                Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                    Icon(item.icon, null, tint = item.color)
                    Spacer(Modifier.width(Spacing.Medium))
                    Text(item.title, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class TrainerMenuItemDef(val title: String, val subtitle: String, val icon: ImageVector, val mode: TrainerMode, val color: Color)

@Composable
fun TrainerExercise(viewModel: MorseViewModel, mode: TrainerMode) {
    val session by viewModel.trainerSession.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val target by viewModel.trainerTarget.collectAsStateWithLifecycle()
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val feedback by viewModel.trainerFeedback.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.setSection(MorseSection.Trainer) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(if (mode == TrainerMode.Koch) "Lesson ${session.lessonNumber}" else mode.name, fontWeight = FontWeight.Black)
                LinearProgressIndicator(
                    progress = { session.currentCorrect.toFloat() / session.targetCorrect },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = RobertColors.StatusGreen
                )
            }
            Text("${session.currentCorrect}/${session.targetCorrect}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItemSmall("Accuracy", "${if (session.recentResults.isEmpty()) 0 else (session.recentResults.count { it } * 100 / session.recentResults.size)}%")
            StatItemSmall("Global Best", "${progress.longestStreak}")
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
                    TrainerFeedbackDisplay(feedback!!)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TARGET", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
                            Text(
                                target,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = RobertColors.Primary,
                                letterSpacing = 8.sp
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.width(100.dp).padding(vertical = 8.dp), color = RobertColors.TextSecondary.copy(alpha = 0.2f))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YOU SENT", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
                            Text(
                                decodedText.ifEmpty { "...." },
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (decodedText.isEmpty()) RobertColors.TextSecondary.copy(alpha = 0.1f) else RobertColors.TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Button(
                onClick = { viewModel.playCurrentText() },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Hearing, null)
                Spacer(Modifier.width(8.dp))
                Text("REPLAY")
            }
            
            if (feedback == null) {
                Button(
                    onClick = { viewModel.submitTrainerAnswer() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RobertColors.Primary),
                    enabled = decodedText.isNotEmpty()
                ) {
                    Text("SUBMIT")
                }
            } else {
                Button(
                    onClick = { viewModel.nextTrainerExercise() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RobertColors.StatusGreen)
                ) {
                    Text("NEXT")
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
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
                Text(currentSymbolBuffer, modifier = Modifier.align(Alignment.BottomStart), style = MaterialTheme.typography.labelMedium, color = RobertColors.TextSecondary)
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

@Composable
fun DecoderScreen(viewModel: MorseViewModel) {
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val isDecoding by viewModel.isDecoding.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium)) {
        Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface), shape = RoundedCornerShape(24.dp)) {
            Box(modifier = Modifier.padding(Spacing.Medium).fillMaxSize()) {
                Text(decodedText.ifEmpty { if (isDecoding) "LISTENING..." else "START LISTENING..." }, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Button(onClick = { viewModel.toggleDecoding() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isDecoding) RobertColors.StatusRed else RobertColors.Primary)) {
            Icon(if (isDecoding) Icons.Default.MicOff else Icons.Default.Mic, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isDecoding) "STOP" else "START")
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
    var isPressed by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.size(180.dp).clip(CircleShape).pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown()
                isPressed = true
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
            Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(48.dp), tint = if (isPressed) RobertColors.Primary else RobertColors.TextSecondary)
        }
    }
}

@Composable
fun IambicPaddleArea(settings: MorseSettings, isBusy: Boolean, lastElement: String, onLeftDown: () -> Unit, onLeftUp: () -> Unit, onRightDown: () -> Unit, onRightUp: () -> Unit) {
    val leftLabel = if (settings.paddleOrientation == PaddleOrientation.LeftDitRightDah) "DIT" else "DAH"
    val rightLabel = if (settings.paddleOrientation == PaddleOrientation.LeftDitRightDah) "DAH" else "DIT"
    Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Paddle(Modifier.weight(1f), leftLabel, isBusy && ((leftLabel == "DIT" && lastElement == ".") || (leftLabel == "DAH" && lastElement == "-")), onLeftDown, onLeftUp)
        Paddle(Modifier.weight(1f), rightLabel, isBusy && ((rightLabel == "DIT" && lastElement == ".") || (rightLabel == "DAH" && lastElement == "-")), onRightDown, onRightUp)
    }
}

@Composable
fun Paddle(modifier: Modifier, label: String, isActive: Boolean, onDown: () -> Unit, onUp: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp)).pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown()
                isPressed = true
                onDown()
                waitForUpOrCancellation()
                isPressed = false
                onUp()
            }
        },
        color = if (isPressed || isActive) RobertColors.Primary.copy(alpha = 0.3f) else RobertColors.Surface,
        border = BorderStroke(2.dp, if (isPressed || isActive) RobertColors.Primary else RobertColors.Surface)
    ) {
        Box(contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.Black, color = if (isPressed || isActive) RobertColors.Primary else RobertColors.TextSecondary) }
    }
}
