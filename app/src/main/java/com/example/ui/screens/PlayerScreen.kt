package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.database.LessonEntity
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    viewModel: AppViewModel,
    lessonId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lesson by viewModel.activeLesson.collectAsState()
    val lessons by viewModel.activeLessons.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    
    // Swipe & Overlay HUD states
    var gestureIndicatorText by remember { mutableStateOf("") }
    var showGestureIndicator by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Notes, 1: Lessons, 2: PDF attachments
    
    // Custom note writing
    var customNoteText by remember { mutableStateOf("") }
    var noteSaveAlertVisible by remember { mutableStateOf(false) }

    // Subtitle toggle
    var subtitlesActive by remember { mutableStateOf(settings.showSubtitles) }

    // Speed setting
    var playbackSpeed by remember { mutableFloatStateOf(settings.defaultSpeed) }

    // Preload lesson media
    LaunchedEffect(lessonId) {
        viewModel.selectedLessonId.value = lessonId
    }

    val activeLesson = lesson ?: return

    // Auto-advance checking and track loading in ExoPlayer view
    LaunchedEffect(activeLesson) {
        viewModel.playLessonVideo(activeLesson)
        playbackSpeed = settings.defaultSpeed
        viewModel.exoPlayer?.setPlaybackSpeed(playbackSpeed)
        customNoteText = ""
    }

    // Ensure we pause playback when navigating away from the player screen
    DisposableEffect(viewModel.exoPlayer) {
        onDispose {
            viewModel.exoPlayer?.pause()
        }
    }

    // Reactively listen to ExoPlayer events to update isPlaying, totalDuration, and keep states accurate
    DisposableEffect(viewModel.exoPlayer, activeLesson) {
        val player = viewModel.exoPlayer
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    totalDuration = player?.duration?.coerceAtLeast(1L) ?: 1L
                }
            }
        }
        player?.addListener(listener)
        
        // Initial state sync
        isPlaying = player?.isPlaying == true
        currentPosition = player?.currentPosition ?: 0L
        totalDuration = player?.duration?.coerceAtLeast(1L) ?: 1L

        onDispose {
            player?.removeListener(listener)
        }
    }

    // Monitor current position and save watched progress to Room DB while active
    LaunchedEffect(isPlaying, activeLesson) {
        if (isPlaying) {
            val player = viewModel.exoPlayer
            while (true) {
                currentPosition = player?.currentPosition ?: 0L
                totalDuration = (player?.duration ?: 1L).coerceAtLeast(1L)
                viewModel.savePlayingProgress(activeLesson.id, currentPosition, totalDuration)
                delay(1000)
            }
        }
    }

    // Audio volume system binding
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // BACK BUTTON ROW (PORTRAIT FLOATING INTERFACE)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = activeLesson.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "AASHIQ+ CINEMATIC INTERFACE",
                        fontSize = 10.sp,
                        color = PremiumGold,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 1. CINEMATIC VIDEO PLAYER WINDOW WITH GESTURE HUD OVERLAY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(Color.Black)
            ) {
                // ExoPlayer core binding
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = viewModel.exoPlayer
                            useController = false // Custom gold HUD implemented in Compose
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Immersive Gesture overlay (Double tap seek, slide volume)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val player = viewModel.exoPlayer ?: return@detectTapGestures
                                    val isLeft = offset.x < size.width * 0.4f
                                    val seekAmount = if (isLeft) -10000 else 10000
                                    player.seekTo((player.currentPosition + seekAmount).coerceIn(0, player.duration))
                                    currentPosition = player.currentPosition
                                    
                                    gestureIndicatorText = if (isLeft) "⏪ REWIND 10S" else "FORWARD 10S ⏩"
                                    coroutineScope.launch {
                                        showGestureIndicator = true
                                        delay(800)
                                        showGestureIndicator = false
                                    }
                                },
                                onTap = {
                                    val player = viewModel.exoPlayer ?: return@detectTapGestures
                                    if (player.isPlaying) {
                                        player.pause()
                                        isPlaying = false
                                    } else {
                                        player.play()
                                        isPlaying = true
                                    }
                                }
                            )
                        }
                )

                // SUBTITLE OVERLAY PANEL (srt simulation matching settings)
                if (subtitlesActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "[Cinematic audio playing - " + formatTime(currentPosition) + "]",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Temporary gesture feedback indicator (volume/seeking bounds)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showGestureIndicator,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TranslucentBlack),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = gestureIndicatorText,
                            color = PremiumGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // 2. GOLD MINIMAL PLAYER PROGRESS AND CONTROLS
            PlayerToolbarOverlay(
                isPlaying = isPlaying,
                currentPos = currentPosition,
                totalDur = totalDuration,
                speed = playbackSpeed,
                subtitlesActive = subtitlesActive,
                onPlayPauseToggle = {
                    val player = viewModel.exoPlayer ?: return@PlayerToolbarOverlay
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        player.play()
                        isPlaying = true
                    }
                },
                onSeek = { ratio ->
                    val player = viewModel.exoPlayer ?: return@PlayerToolbarOverlay
                    val target = (ratio * totalDuration).toLong()
                    player.seekTo(target)
                    currentPosition = target
                },
                onSpeedChanged = {
                    playbackSpeed = it
                    viewModel.exoPlayer?.setPlaybackSpeed(it)
                },
                onSubtitleToggle = { subtitlesActive = !subtitlesActive },
                onBookmarkToggle = { viewModel.toggleBookmark(activeLesson.id) },
                isBookmarked = activeLesson.isBookmarked
            )

            // 3. EXPANDABLE tabs section (Learning notes | Course Index | PDF Reader)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabItem(title = "LECTURE NOTES", isActive = activeTab == 0, onClick = { activeTab = 0 })
                TabItem(title = "SUB-CHAPTERS", isActive = activeTab == 1, onClick = { activeTab = 1 })
                TabItem(title = "PDF MATERIAL", isActive = activeTab == 2, onClick = { activeTab = 2 })
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MatteBlack)
            ) {
                when (activeTab) {
                    0 -> NotesTabContent(
                        localNote = activeLesson.notePath ?: "## General Information\nEnjoy this high performance lecture.",
                        currentTimeString = formatTime(currentPosition),
                        onSaveTimestampNote = { note ->
                            val timestampedLine = "\n\n* **Timestamp Saved [${formatTime(currentPosition)}]**: $note"
                            customNoteText = ""
                            noteSaveAlertVisible = true
                            val updatedNote = (activeLesson.notePath ?: "") + timestampedLine
                            viewModel.saveTimestampNoteForLesson(activeLesson.id, updatedNote)
                        }
                    )
                    1 -> LessonsTabContent(
                        lessons = lessons,
                        activeLessonId = activeLesson.id,
                        onLessonClick = { id ->
                            viewModel.selectedLessonId.value = id
                        }
                    )
                    2 -> PdfViewerMockContent()
                }

                if (noteSaveAlertVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .background(Color(0xFF1E2818), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color(0xFF81C784), RoundedCornerShape(8.dp))
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "SUCCESS: Live timestamp note saved to local text cache!",
                                color = Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "DISMISS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.clickable { noteSaveAlertVisible = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerToolbarOverlay(
    isPlaying: Boolean,
    currentPos: Long,
    totalDur: Long,
    speed: Float,
    subtitlesActive: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSubtitleToggle: () -> Unit,
    onBookmarkToggle: () -> Unit,
    isBookmarked: Boolean
) {
    Surface(
        color = Color(0xFF141414),
        border = BorderStroke(0.5.dp, Color(0xFF222222)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Timeline track progressbar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatTime(currentPos),
                    color = SubduedGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                // Gold interactive slider bar
                Slider(
                    value = if (totalDur > 0) currentPos.toFloat() / totalDur.toFloat() else 0f,
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(
                        activeTrackColor = PremiumGold,
                        inactiveTrackColor = Color(0xFF333333),
                        thumbColor = PremiumGold
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .height(28.dp)
                )

                Text(
                    text = formatTime(totalDur),
                    color = SubduedGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Control Actions Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary Controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Velocities control cycle
                    TextButton(
                        onClick = {
                            val nextSpeed = when (speed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            onSpeedChanged(nextSpeed)
                        },
                        modifier = Modifier
                            .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
                            .height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "${speed}x",
                            color = PremiumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // CC button
                    IconButton(
                        onClick = onSubtitleToggle,
                        modifier = Modifier
                            .size(30.dp)
                            .background(if (subtitlesActive) Color(0xFF2B220C) else Color(0xFF1C1C1C), RoundedCornerShape(4.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClosedCaption,
                            contentDescription = "Toggle Subtitles",
                            tint = if (subtitlesActive) PremiumGold else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Centered play pause keys
                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier
                        .background(PremiumGold, CircleShape)
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Play Progress",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Right action items (bookmark toggle)
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (isBookmarked) Color(0xFF2F240E) else Color(0xFF1E1E1E), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Progress moment",
                        tint = if (isBookmarked) PremiumGold else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.TabItem(title: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) PremiumGold else SubduedGray,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .background(if (isActive) PremiumGold else Color.Transparent)
            )
        }
    }
}

@Composable
fun NotesTabContent(
    localNote: String,
    currentTimeString: String,
    onSaveTimestampNote: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick insert note card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                .border(0.5.dp, Color(0xFF292929), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = rawText,
                onValueChange = { rawText = it },
                placeholder = { Text("Write custom timestamped learning note...", fontSize = 11.sp, color = SubduedGray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            TextButton(
                onClick = {
                    if (rawText.isNotBlank()) {
                        onSaveTimestampNote(rawText)
                        rawText = ""
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = PremiumGold)
            ) {
                Text("SAVE AT $currentTimeString", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Custom stylized simple Markdown viewer
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            val lines = localNote.split("\n")
            items(lines) { line ->
                val lineTrimmed = line.trim()
                when {
                    lineTrimmed.startsWith("# ") -> {
                        Text(
                            text = lineTrimmed.removePrefix("# "),
                            style = MaterialTheme.typography.titleLarge,
                            color = PremiumGold,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                        )
                    }
                    lineTrimmed.startsWith("## ") -> {
                        Text(
                            text = lineTrimmed.removePrefix("## "),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    lineTrimmed.startsWith("* ") || lineTrimmed.startsWith("- ") -> {
                        val text = if (lineTrimmed.startsWith("* ")) lineTrimmed.removePrefix("* ") else lineTrimmed.removePrefix("- ")
                        Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)) {
                            Text(text = "•", color = PremiumGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                            Text(text = text, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        }
                    }
                    lineTrimmed.startsWith("1. ") -> {
                        Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)) {
                            Text(text = lineTrimmed.take(3), color = PremiumGold, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(end = 4.dp))
                            Text(text = lineTrimmed.drop(3), style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        }
                    }
                    lineTrimmed.isNotBlank() -> {
                        Text(
                            text = lineTrimmed,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SubduedGray,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    else -> {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LessonsTabContent(
    lessons: List<LessonEntity>,
    activeLessonId: String,
    onLessonClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(lessons, key = { it.id }) { lesson ->
            val isActive = lesson.id == activeLessonId
            Surface(
                color = if (isActive) Color(0xFF1E1C12) else Color(0xFF121212),
                border = BorderStroke(1.dp, if (isActive) PremiumGold else Color(0xFF222222)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLessonClick(lesson.id) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (isActive) PremiumGold else Color(0xFF222222), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.Menu,
                            contentDescription = null,
                            tint = if (isActive) Color.Black else SubduedGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lesson.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) PremiumGold else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Lesson ${lesson.orderIndex}",
                            fontSize = 10.sp,
                            color = SubduedGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfViewerMockContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF141414), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "LOCAL ATTACHED PDF MEMORY PREVIEW",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AASHI_PDF_CORE017.pdf • 16 pages cached",
                    color = SubduedGray,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                // Mock Zoom/Page select
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}, modifier = Modifier.size(24.dp).background(Color(0xFF222222), CircleShape)) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                    Text(text = "100%", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    IconButton(onClick = {}, modifier = Modifier.size(24.dp).background(Color(0xFF222222), CircleShape)) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "✓ PDF document read progress automatically synced offline to current lesson metadata.",
            color = Color(0xFF81C784),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// Helper: Formatter of long millis to minutes/seconds
private fun formatTime(millis: Long): String {
    val sec = (millis / 1000) % 60
    val min = (millis / (1000 * 60)) % 60
    val hr = (millis / (1000 * 60 * 60))
    return if (hr > 0) {
        String.format("%d:%02d:%02d", hr, min, sec)
    } else {
        String.format("%02d:%02d", min, sec)
    }
}
