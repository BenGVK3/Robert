package au.com.benji.robert.screens.morse

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val latencyLog by viewModel.latencyLog.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        MorseSettingsDialog(
            settings = settings,
            diagnostics = diagnostics,
            latencyLog = latencyLog,
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
                    MorseSection.Menu -> MorseMenu(onSectionSelect = { viewModel.setSection(it) }, settings = settings)
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
    diagnostics: List<KeyerDiagnosticEntry>,
    latencyLog: List<String>,
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
                // Keyer Section
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
                        
                        if (settings.keyerMode != KeyerMode.Straight) {
                            Text("Paddle Orientation", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PaddleOrientation.entries.forEach { orientation ->
                                    FilterChip(
                                        selected = settings.paddleOrientation == orientation,
                                        onClick = { onSave(settings.copy(paddleOrientation = orientation)) },
                                        label = { Text(if (orientation == PaddleOrientation.LeftDitRightDah) "L-Dit / R-Dah" else "L-Dah / R-Dit") }
                                    )
                                }
                            }
                        }
                    }
                }

                // Audio Section
                PreferenceCard(title = "Audio", icon = Icons.AutoMirrored.Filled.VolumeUp) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        SettingSlider(
                            label = "Sidetone Volume",
                            value = settings.sidetoneVolume,
                            onValueChange = { onSave(settings.copy(sidetoneVolume = it)) },
                            valueRange = 0f..1f
                        )
                        SettingSlider(
                            label = "Playback Volume",
                            value = settings.volume,
                            onValueChange = { onSave(settings.copy(volume = it)) },
                            valueRange = 0f..1f
                        )
                        SettingSlider(
                            label = "Tone Frequency",
                            value = settings.frequency.toFloat(),
                            onValueChange = { onSave(settings.copy(frequency = it.toInt())) },
                            valueRange = 300f..1000f,
                            suffix = " Hz"
                        )
                    }
                }

                // Timing Section
                PreferenceCard(title = "Timing", icon = Icons.Default.Timer) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        SettingSlider(
                            label = "Overall Speed",
                            value = settings.wpm.toFloat(),
                            onValueChange = { onSave(settings.copy(wpm = it.toInt())) },
                            valueRange = 5f..45f,
                            suffix = " WPM"
                        )
                        SettingSlider(
                            label = "Farnsworth Speed",
                            value = settings.farnsworthWpm.toFloat(),
                            onValueChange = { onSave(settings.copy(farnsworthWpm = it.toInt())) },
                            valueRange = 5f..45f,
                            suffix = " WPM"
                        )
                        SettingSlider(
                            label = "Key Weighting",
                            value = settings.weighting,
                            onValueChange = { onSave(settings.copy(weighting = it)) },
                            valueRange = 0.5f..2.0f
                        )
                    }
                }

                // Diagnostics Section
                PreferenceCard(title = "Keyer Diagnostics", icon = Icons.Default.BarChart) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Live Timing (ms)", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
                        diagnostics.forEach { diag ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${diag.type}: ${diag.durationMs}ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (diag.durationMs in (diag.expectedMs - 30)..(diag.expectedMs + 30)) RobertColors.StatusGreen else RobertColors.TextPrimary
                                )
                                Text(
                                    "(Exp: ${diag.expectedMs}ms)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RobertColors.TextSecondary
                                )
                            }
                        }

                        if (latencyLog.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = RobertColors.TextSecondary.copy(alpha = 0.1f))
                            Text("Latency Monitor (Touch -> Sidetone)", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
                            latencyLog.forEach { log ->
                                Text(log, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            }
                        }
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Spacing.Small)) {
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
            Text(
                if (suffix.isNotEmpty()) "${value.toInt()}$suffix" else String.format(Locale.getDefault(), "%.2f", value),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
fun MorseMenu(onSectionSelect: (MorseSection) -> Unit, settings: MorseSettings) {
    val menuItems = listOf(
        MorseMenuItem("Receive", "Improve your copying skills", Icons.Default.Hearing, MorseSection.Receive, RobertColors.Primary),
        MorseMenuItem("Send", "Master your keying technique", Icons.Default.Keyboard, MorseSection.Send, Color(0xFF00BCD4)),
        MorseMenuItem("Trainer", "Interactive Morse Tutor", Icons.Default.School, MorseSection.Trainer, RobertColors.StatusGreen),
        MorseMenuItem("Decoder", "Live audio transcription", Icons.Default.GraphicEq, MorseSection.Decoder, RobertColors.StatusOrange),
        MorseMenuItem("Simulator", "Virtual Radio Operator", Icons.Default.RecordVoiceOver, MorseSection.Simulator, Color(0xFF9C27B0))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        items(menuItems) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSectionSelect(item.section) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(item.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, null, tint = item.color, modifier = Modifier.size(32.dp))
                    }
                    
                    Spacer(Modifier.width(Spacing.Medium))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = RobertColors.TextSecondary)
                    }
                    
                    Icon(Icons.Default.ChevronRight, null, tint = RobertColors.TextSecondary.copy(alpha = 0.5f))
                }
            }
        }
    }
}

data class MorseMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val section: MorseSection,
    val color: Color
)

@Composable
fun ReceiveScreen(viewModel: MorseViewModel) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentText by viewModel.currentText.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var revealed by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium)) {
        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(Spacing.Medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem("SPEED", "${settings.wpm}", "WPM")
                VerticalDivider(modifier = Modifier.height(32.dp).width(1.dp), color = RobertColors.TextSecondary.copy(alpha = 0.2f))
                StatItem("PITCH", "${settings.frequency}", "Hz")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        // Main Display
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
            border = BorderStroke(1.dp, RobertColors.Primary.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isPlaying) {
                    PlaybackAnimation()
                }
                
                Text(
                    text = if (revealed || !settings.hideTextDuringPlayback) currentText else "•••••",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = if (revealed) RobertColors.Primary else RobertColors.TextSecondary.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(Spacing.Medium)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Button(
                onClick = { viewModel.playText(currentText) },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isPlaying && currentText.isNotEmpty()
            ) {
                Icon(Icons.Default.Replay, null)
                Spacer(Modifier.width(8.dp))
                Text("REPLAY")
            }

            Button(
                onClick = { 
                    revealed = false
                    viewModel.generateNewExercise(ExerciseType.Mixed) 
                },
                modifier = Modifier.weight(1.5f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isPlaying,
                colors = ButtonDefaults.buttonColors(containerColor = RobertColors.Primary)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("NEXT")
            }

            IconButton(
                onClick = { revealed = !revealed },
                modifier = Modifier.size(56.dp).background(RobertColors.Surface, RoundedCornerShape(16.dp))
            ) {
                Icon(if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = RobertColors.Primary)
            }
        }
        
        if (isPlaying) {
            TextButton(
                onClick = { viewModel.stopPlayback() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.textButtonColors(contentColor = RobertColors.StatusRed)
            ) {
                Text("STOP PLAYBACK")
            }
        }
    }
}

@Composable
fun PlaybackAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(RobertColors.Primary.copy(alpha = 0.1f * scale), Color.Transparent)
                )
            )
    )
}

@Composable
fun StatItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(2.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
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
        // Output Window
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
        ) {
            Box(modifier = Modifier.padding(Spacing.Medium).fillMaxSize()) {
                Text(
                    text = decodedText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = RobertColors.Primary,
                    fontFamily = FontFamily.Monospace
                )
                
                Row(modifier = Modifier.align(Alignment.BottomStart), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentSymbolBuffer,
                        style = MaterialTheme.typography.labelMedium,
                        color = RobertColors.TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    if (lastElementSent.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(if (lastElementSent == ".") 12.dp else 24.dp, 12.dp)
                                .clip(CircleShape)
                                .background(RobertColors.Primary)
                        )
                    }
                }
                
                if (decodedText.isEmpty() && currentSymbolBuffer.isEmpty()) {
                    Text(
                        "START KEYING...",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = RobertColors.TextSecondary.copy(alpha = 0.3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Large))

        // Keying Area
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (settings.keyerMode == KeyerMode.Straight) {
                StraightKeyArea(
                    isBusy = isKeyBusy,
                    onDown = { viewModel.onKeyDown(false) }, 
                    onUp = { viewModel.onKeyUp(false) }
                )
            } else {
                IambicPaddleArea(
                    settings = settings,
                    isBusy = isKeyBusy,
                    lastElement = lastElementSent,
                    onLeftDown = { viewModel.onKeyDown(false) },
                    onLeftUp = { viewModel.onKeyUp(false) },
                    onRightDown = { viewModel.onKeyDown(true) },
                    onRightUp = { viewModel.onKeyUp(true) }
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            OutlinedButton(onClick = { viewModel.clearSentText() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("CLEAR")
            }
            Button(onClick = { viewModel.submitSentText() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("SUBMIT")
            }
        }
    }
}

@Composable
fun StraightKeyArea(isBusy: Boolean, onDown: () -> Unit, onUp: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) onDown() else onUp()
    }

    Surface(
        modifier = Modifier
            .size(240.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
        color = if (isPressed) RobertColors.Primary.copy(alpha = 0.3f) else RobertColors.Surface,
        border = BorderStroke(4.dp, if (isPressed) RobertColors.Primary else RobertColors.Surface)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.TouchApp, 
                    null, 
                    modifier = Modifier.size(48.dp), 
                    tint = if (isPressed) RobertColors.Primary else RobertColors.TextSecondary
                )
                Text(
                    if (isPressed) "DOWN" else "TAP", 
                    fontWeight = FontWeight.Black, 
                    color = if (isPressed) RobertColors.Primary else RobertColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun IambicPaddleArea(
    settings: MorseSettings,
    isBusy: Boolean,
    lastElement: String,
    onLeftDown: () -> Unit,
    onLeftUp: () -> Unit,
    onRightDown: () -> Unit,
    onRightUp: () -> Unit
) {
    val leftLabel = if (settings.paddleOrientation == PaddleOrientation.LeftDitRightDah) "DIT" else "DAH"
    val rightLabel = if (settings.paddleOrientation == PaddleOrientation.LeftDitRightDah) "DAH" else "DIT"

    Row(modifier = Modifier.fillMaxWidth().height(280.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Paddle(
            modifier = Modifier.weight(1f), 
            label = leftLabel, 
            isActive = isBusy && ((leftLabel == "DIT" && lastElement == ".") || (leftLabel == "DAH" && lastElement == "-")),
            onDown = onLeftDown, 
            onUp = onLeftUp
        )
        Paddle(
            modifier = Modifier.weight(1f), 
            label = rightLabel, 
            isActive = isBusy && ((rightLabel == "DIT" && lastElement == ".") || (rightLabel == "DAH" && lastElement == "-")),
            onDown = onRightDown, 
            onUp = onRightUp
        )
    }
}

@Composable
fun Paddle(modifier: Modifier, label: String, isActive: Boolean, onDown: () -> Unit, onUp: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) onDown() else onUp()
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
        color = if (isPressed || isActive) RobertColors.Primary.copy(alpha = 0.3f) else RobertColors.Surface,
        border = BorderStroke(2.dp, if (isPressed || isActive) RobertColors.Primary else RobertColors.Surface)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.Black, color = if (isPressed || isActive) RobertColors.Primary else RobertColors.TextSecondary)
        }
    }
}

@Composable
fun TrainerScreen(viewModel: MorseViewModel) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val trainerTarget by viewModel.trainerTarget.collectAsStateWithLifecycle()
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val feedback by viewModel.trainerFeedback.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isKeyBusy by viewModel.isKeyBusy.collectAsStateWithLifecycle()
    val lastElementSent by viewModel.lastElementSent.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium)) {
        // Progress Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("LESSON ${progress.currentLessonIndex + 1}", style = MaterialTheme.typography.labelLarge, color = RobertColors.Primary, fontWeight = FontWeight.Bold)
                Text("Interactive Training", style = MaterialTheme.typography.bodySmall, color = RobertColors.TextSecondary)
            }
            IconButton(onClick = { viewModel.resetTrainerExercise() }) {
                Icon(Icons.Default.Refresh, "Reset Exercise", tint = RobertColors.Primary)
            }
            Text("${(progress.totalAccuracy).toInt()}% ACCURACY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
        }
        
        LinearProgressIndicator(
            progress = { (progress.currentLessonIndex + 1) / 10f },
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small).height(6.dp).clip(CircleShape),
            color = RobertColors.Primary,
            trackColor = RobertColors.Surface
        )

        Spacer(modifier = Modifier.height(Spacing.Medium))

        // Target Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(Spacing.Large), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SEND THIS:", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
                
                Row(horizontalArrangement = Arrangement.Center) {
                    trainerTarget.forEachIndexed { index, char ->
                        val isDecoded = decodedText.trim().length > index
                        val isCurrent = decodedText.trim().length == index
                        val color = when {
                            isDecoded -> RobertColors.StatusGreen
                            isCurrent -> RobertColors.Primary
                            else -> RobertColors.TextSecondary.copy(alpha = 0.3f)
                        }
                        
                        Text(
                            char.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = color,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                
                val targetCode = trainerTarget.map { MorseCodeMap[it] ?: "" }.joinToString("   ")
                Text(targetCode, style = MaterialTheme.typography.titleMedium, color = RobertColors.TextSecondary.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        // Feedback Section
        AnimatedVisibility(visible = feedback != null) {
            feedback?.let { fb ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (fb.isCorrect) RobertColors.StatusGreen.copy(alpha = 0.1f) else RobertColors.StatusRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, if (fb.isCorrect) RobertColors.StatusGreen else RobertColors.StatusRed)
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (fb.isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                                null,
                                tint = if (fb.isCorrect) RobertColors.StatusGreen else RobertColors.StatusRed
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(fb.message, fontWeight = FontWeight.Bold, color = if (fb.isCorrect) RobertColors.StatusGreen else RobertColors.StatusRed)
                        }
                        
                        if (!fb.isCorrect) {
                            Spacer(modifier = Modifier.height(Spacing.Small))
                            Text("Received: ${fb.received}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Code: ${fb.receivedCode}", style = MaterialTheme.typography.bodySmall, color = RobertColors.TextSecondary, fontFamily = FontFamily.Monospace)
                            
                            Spacer(modifier = Modifier.height(Spacing.Small))
                            Text("Mistake highlighted above. Focus on element timing.", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Input Display Overlay
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text(
                decodedText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = RobertColors.Primary,
                letterSpacing = 4.sp
            )
        }

        // Keying Area
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            if (settings.keyerMode == KeyerMode.Straight) {
                StraightKeyArea(
                    isBusy = isKeyBusy,
                    onDown = { viewModel.onKeyDown(false) }, 
                    onUp = { viewModel.onKeyUp(false) }
                )
            } else {
                IambicPaddleArea(
                    settings = settings,
                    isBusy = isKeyBusy,
                    lastElement = lastElementSent,
                    onLeftDown = { viewModel.onKeyDown(false) },
                    onLeftUp = { viewModel.onKeyUp(false) },
                    onRightDown = { viewModel.onKeyDown(true) },
                    onRightUp = { viewModel.onKeyUp(true) }
                )
            }
        }
    }
}

@Composable
fun DecoderScreen(viewModel: MorseViewModel) {
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val isDecoding by viewModel.isDecoding.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium)) {
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(modifier = Modifier.padding(Spacing.Medium).fillMaxSize()) {
                Text(
                    text = decodedText.ifEmpty { if (isDecoding) "LISTENING..." else "START LISTENING..." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (decodedText.isEmpty()) RobertColors.TextSecondary.copy(alpha = 0.3f) else RobertColors.TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.Medium))
        
        Button(
            onClick = { viewModel.toggleDecoding() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDecoding) RobertColors.StatusRed else RobertColors.Primary
            )
        ) {
            Icon(if (isDecoding) Icons.Default.MicOff else Icons.Default.Mic, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isDecoding) "STOP LISTENING" else "START LISTENING")
        }
    }
}

@Composable
fun SimulatorScreen(viewModel: MorseViewModel) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            contentPadding = PaddingValues(bottom = Spacing.Medium)
        ) {
            items(messages) { (author, text) ->
                ChatBubble(author = author, text = text)
            }
        }
        
        // Live Decoded Text for Simulator
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface.copy(alpha = 0.5f))
        ) {
            Text(
                decodedText.ifEmpty { "KEY YOUR REPLY..." },
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (decodedText.isEmpty()) RobertColors.TextSecondary.copy(alpha = 0.3f) else RobertColors.Primary,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Button(
                onClick = { viewModel.submitSentText() },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = decodedText.isNotEmpty()
            ) {
                Text("SEND")
            }
            OutlinedButton(
                onClick = { viewModel.clearSentText() },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Clear, null)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))
        
        // Keying Area for Simulator
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val isKeyBusy by viewModel.isKeyBusy.collectAsStateWithLifecycle()
            val lastElementSent by viewModel.lastElementSent.collectAsStateWithLifecycle()
            
            if (settings.keyerMode == KeyerMode.Straight) {
                StraightKeyArea(
                    isBusy = isKeyBusy,
                    onDown = { viewModel.onKeyDown(false) }, 
                    onUp = { viewModel.onKeyUp(false) }
                )
            } else {
                IambicPaddleArea(
                    settings = settings,
                    isBusy = isKeyBusy,
                    lastElement = lastElementSent,
                    onLeftDown = { viewModel.onKeyDown(false) },
                    onLeftUp = { viewModel.onKeyUp(false) },
                    onRightDown = { viewModel.onKeyDown(true) },
                    onRightUp = { viewModel.onKeyUp(true) }
                )
            }
        }
    }
}

@Composable
fun ChatBubble(author: String, text: String) {
    val isMe = author == "You"
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Surface(
            color = if (isMe) RobertColors.Primary else RobertColors.Surface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp
            )
        ) {
            Text(text, modifier = Modifier.padding(Spacing.Medium), color = if (isMe) Color.White else RobertColors.TextPrimary, fontFamily = FontFamily.Monospace)
        }
        Text(author, style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary, modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp))
    }
}
