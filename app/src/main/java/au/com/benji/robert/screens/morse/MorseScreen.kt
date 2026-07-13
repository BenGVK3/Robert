package au.com.benji.robert.screens.morse

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                    MorseSection.Menu -> MorseMenu(onSectionSelect = { viewModel.setSection(it) })
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
fun MorseMenu(onSectionSelect: (MorseSection) -> Unit) {
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
    val stats by viewModel.receiveStats.collectAsStateWithLifecycle()
    val feedback by viewModel.receiveFeedback.collectAsStateWithLifecycle()
    val currentType by viewModel.currentExerciseType.collectAsStateWithLifecycle()
    
    var userAnswer by remember { mutableStateOf("") }
    val hasStarted by remember { derivedStateOf { currentText.isNotEmpty() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        // 1. Horizontal Stats Strip
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Medium, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem("ACCURACY", "${if (stats.totalCount > 0) (stats.correctCount * 100 / stats.totalCount) else 0}%", "")
                VerticalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = RobertColors.TextSecondary.copy(alpha = 0.2f))
                StatItem("STREAK", "${stats.currentStreak}", "")
                VerticalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = RobertColors.TextSecondary.copy(alpha = 0.2f))
                StatItem("BEST", "${stats.longestStreak}", "")
            }
        }

        // 2. Difficulty Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(ExerciseType.Beginner, ExerciseType.Intermediate, ExerciseType.Advanced, ExerciseType.Expert).forEach { type ->
                FilterChip(
                    selected = currentType == type,
                    onClick = { 
                        userAnswer = ""
                        viewModel.generateNewExercise(type) 
                    },
                    label = { 
                        Text(
                            text = type.name, 
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        ) 
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Main Feedback Area (Flexible Weight)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
            border = BorderStroke(1.dp, RobertColors.Primary.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isPlaying) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PlaybackAnimation()
                        Text("LISTENING...", style = MaterialTheme.typography.titleMedium, color = RobertColors.Primary, fontWeight = FontWeight.Bold)
                    }
                } else if (!hasStarted) {
                    Text("READY", style = MaterialTheme.typography.headlineSmall, color = RobertColors.TextSecondary.copy(alpha = 0.3f), fontWeight = FontWeight.Black)
                } else if (feedback == null) {
                    Text("TYPE YOUR COPY", style = MaterialTheme.typography.titleMedium, color = RobertColors.TextSecondary.copy(alpha = 0.5f))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (feedback!!.isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                            null,
                            tint = if (feedback!!.isCorrect) RobertColors.StatusGreen else RobertColors.StatusRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            if (feedback!!.isCorrect) "CORRECT" else "INCORRECT",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = if (feedback!!.isCorrect) RobertColors.StatusGreen else RobertColors.StatusRed
                        )
                        if (!feedback!!.isCorrect) {
                            Text(
                                "Expected: ${feedback!!.expected}",
                                style = MaterialTheme.typography.titleMedium,
                                color = RobertColors.TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 4. Your Copy (Compact 1-line)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Medium),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (feedback == null) {
                    Text(
                        text = userAnswer.ifEmpty { "•••••" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (userAnswer.isEmpty()) RobertColors.TextSecondary.copy(alpha = 0.2f) else RobertColors.Primary,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    feedback!!.comparison.forEachIndexed { index, (expectedChar, isMatch) ->
                        val displayChar = if (isMatch) expectedChar else feedback!!.received.getOrNull(index) ?: expectedChar
                        Text(
                            text = displayChar.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isMatch) RobertColors.StatusGreen else RobertColors.StatusRed,
                            modifier = Modifier.padding(horizontal = 2.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 5. Custom QWERTY Morse Keyboard
        MorseKeyboard(
            onKey = { char -> if (!isPlaying && feedback == null) userAnswer += char },
            onBackspace = { if (userAnswer.isNotEmpty() && feedback == null) userAnswer = userAnswer.dropLast(1) },
            enabled = !isPlaying && feedback == null
        )

        // 6. Primary Action Button
        val (btnText, btnColor, btnIcon) = when {
            isPlaying -> Triple("STOP", RobertColors.StatusRed, Icons.Default.Stop)
            !hasStarted -> Triple("START PRACTICE", RobertColors.Primary, Icons.Default.PlayArrow)
            feedback != null -> Triple("NEXT EXERCISE", RobertColors.StatusGreen, Icons.AutoMirrored.Filled.ArrowForward)
            userAnswer.isEmpty() -> Triple("PLAY MORSE", RobertColors.Primary, Icons.Default.PlayArrow)
            else -> Triple("CHECK ANSWER", RobertColors.Primary, Icons.Default.Check)
        }

        Button(
            onClick = {
                when {
                    isPlaying -> viewModel.stopPlayback()
                    feedback != null -> {
                        userAnswer = ""
                        viewModel.generateNewExercise(currentType)
                    }
                    userAnswer.isEmpty() -> viewModel.playText(currentText.ifEmpty { viewModel.generateNewExercise(currentType); "" })
                    else -> viewModel.checkReceiveAnswer(userAnswer)
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = btnColor)
        ) {
            Icon(btnIcon, null)
            Spacer(Modifier.width(8.dp))
            Text(btnText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        // 7. Secondary Controls
        if (hasStarted && !isPlaying) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { viewModel.playText(currentText) }) {
                    Icon(Icons.Default.Replay, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("REPLAY")
                }
                if (feedback == null && userAnswer.isNotEmpty()) {
                    TextButton(onClick = { userAnswer = "" }) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CLEAR")
                    }
                }
            }
        } else if (isPlaying) {
            Spacer(Modifier.height(48.dp)) // Maintain layout height
        }
    }
}

@Composable
fun MorseKeyboard(
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    enabled: Boolean = true
) {
    val rows = listOf(
        "1234567890".toList(),
        "QWERTYUIOP".toList(),
        "ASDFGHJKL".toList(),
        "ZXCVBNM".toList()
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth(if (index == 2) 0.95f else 1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { char ->
                    KeyButton(
                        text = char.toString(),
                        onClick = { onKey(char) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled
                    )
                }
                if (index == 3) {
                    KeyButton(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        onClick = onBackspace,
                        modifier = Modifier.weight(1.5f),
                        containerColor = RobertColors.Surface,
                        contentColor = RobertColors.TextSecondary,
                        enabled = enabled
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    containerColor: Color = RobertColors.Surface,
    contentColor: Color = RobertColors.TextPrimary,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        enabled = enabled
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
        } else if (text != null) {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
fun StraightKeyArea(onDown: () -> Unit, onUp: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .size(240.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                isPressed = true
                                onDown()
                            }
                            PointerEventType.Release -> {
                                isPressed = false
                                onUp()
                            }
                        }
                    }
                }
            },
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
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                isPressed = true
                                onDown()
                            }
                            PointerEventType.Release -> {
                                isPressed = false
                                onUp()
                            }
                        }
                    }
                }
            },
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
    val trainerMode by viewModel.trainerMode.collectAsStateWithLifecycle()
    
    AnimatedContent(targetState = trainerMode, label = "TrainerModeTransition") { mode ->
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
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        item {
            Text(
                "LEARN MORSE",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = RobertColors.Primary
            )
        }
        
        // Progress Overview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Mastery", style = MaterialTheme.typography.labelMedium, color = RobertColors.TextSecondary)
                            Text("${progress.charactersMastered.size} / 40 Characters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        CircularProgressIndicator(
                            progress = { progress.charactersMastered.size / 40f },
                            modifier = Modifier.size(48.dp),
                            color = RobertColors.Primary,
                            trackColor = RobertColors.Surface
                        )
                    }
                    
                    Spacer(Modifier.height(Spacing.Medium))
                    
                    Button(
                        onClick = { viewModel.setTrainerMode(TrainerMode.Koch) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("RESUME KOCH COURSE")
                    }
                }
            }
        }
        
        // Practice Modes Grid
        item {
            Text("Training Modes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        val modes = listOf(
            TrainerMenuItem("Koch Course", "The standard amateur radio path", Icons.Default.School, TrainerMode.Koch, RobertColors.Primary),
            TrainerMenuItem("Callsigns", "Copy realistic world callsigns", Icons.Default.Public, TrainerMode.Callsigns, Color(0xFF00BCD4)),
            TrainerMenuItem("Words", "Common English words", Icons.Default.TextFields, TrainerMode.Words, RobertColors.StatusGreen),
            TrainerMenuItem("Amateur Radio", "CQ, signal reports, and shorthand", Icons.Default.Radio, TrainerMode.AmateurRadio, RobertColors.StatusOrange),
            TrainerMenuItem("Weak Review", "Focus on your difficult letters", Icons.Default.Psychology, TrainerMode.WeakReview, Color(0xFF9C27B0))
        )
        
        items(modes) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setTrainerMode(item.mode) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(item.icon, null, tint = item.color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(Spacing.Medium))
                    Column {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = RobertColors.TextSecondary)
                    }
                }
            }
        }
    }
}

data class TrainerMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val mode: TrainerMode,
    val color: Color
)

@Composable
fun TrainerExercise(viewModel: MorseViewModel, mode: TrainerMode) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val trainerTarget by viewModel.trainerTarget.collectAsStateWithLifecycle()
    val decodedText by viewModel.decodedText.collectAsStateWithLifecycle()
    val feedback by viewModel.trainerFeedback.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isKeyBusy by viewModel.isKeyBusy.collectAsStateWithLifecycle()
    val lastElementSent by viewModel.lastElementSent.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(Spacing.Medium)) {
        // Exercise Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.setSection(MorseSection.Menu) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(mode.name.uppercase(), style = MaterialTheme.typography.labelLarge, color = RobertColors.Primary, fontWeight = FontWeight.Bold)
                if (mode == TrainerMode.Koch) {
                    Text("Lesson ${progress.currentLessonIndex + 1}", style = MaterialTheme.typography.bodySmall, color = RobertColors.TextSecondary)
                }
            }
            IconButton(onClick = { viewModel.resetTrainerExercise() }) {
                Icon(Icons.Default.Refresh, null, tint = RobertColors.Primary)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        // Target Card (Hidden initially for some modes, but here as per requirement for "Send this")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(Spacing.Large), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SEND THIS:", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
                
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
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
