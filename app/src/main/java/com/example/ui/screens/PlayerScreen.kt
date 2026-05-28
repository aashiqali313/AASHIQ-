package com.example.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.content.pm.ActivityInfo
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
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.database.LessonEntity
import coil.compose.AsyncImage
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

    // Fullscreen state
    var isFullscreen by remember { mutableStateOf(false) }

    // Screen auto-rotation helper for fullscreen landscape video playback
    val activity = remember(context) {
        var c = context
        while (c is ContextWrapper) {
            if (c is Activity) break
            c = c.baseContext
        }
        c as? Activity
    }

    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            val format = viewModel.exoPlayer?.videoFormat
            val rotateToLandscape = if (format != null && format.width > 0 && format.height > 0) {
                format.width > format.height
            } else {
                true // Default true if format not parsed or empty
            }

            if (rotateToLandscape) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

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
        if (isFullscreen) {
            FullscreenPlayerView(
                viewModel = viewModel,
                activeLesson = activeLesson,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                totalDuration = totalDuration,
                playbackSpeed = playbackSpeed,
                subtitlesActive = subtitlesActive,
                onPlayPauseToggle = {
                    val player = viewModel.exoPlayer ?: return@FullscreenPlayerView
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        player.play()
                        isPlaying = true
                    }
                },
                onSeek = { ratio ->
                    val player = viewModel.exoPlayer ?: return@FullscreenPlayerView
                    val target = (ratio * totalDuration).toLong()
                    player.seekTo(target)
                    currentPosition = target
                },
                onSpeedChanged = {
                    playbackSpeed = it
                    viewModel.exoPlayer?.setPlaybackSpeed(it)
                },
                onSubtitleToggle = { subtitlesActive = !subtitlesActive },
                onExitFullscreen = { isFullscreen = false }
            )
        } else {
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
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = activeLesson.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
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

            if (activeLesson.videoUri.isNotEmpty()) {
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
                                        try {
                                            val player = viewModel.exoPlayer ?: return@detectTapGestures
                                            val isLeft = offset.x < size.width * 0.4f
                                            val seekAmount = if (isLeft) -10000 else 10000
                                            val duration = if (player.duration > 0) player.duration else 0L
                                            val targetPosition = (player.currentPosition + seekAmount).coerceIn(0, duration)
                                            player.seekTo(targetPosition)
                                            currentPosition = player.currentPosition
                                            
                                            gestureIndicatorText = if (isLeft) "⏪ REWIND 10S" else "FORWARD 10S ⏩"
                                            coroutineScope.launch {
                                                showGestureIndicator = true
                                                delay(800)
                                                showGestureIndicator = false
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
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
                    onFullscreenToggle = { isFullscreen = true }
                )
            } else {
                // 1. PREMIUM VISUAL HERO HEADERS FOR NON-VIDEO LEARNING ELEMENTS
                val isLightTheme = AashiqTheme.colors.isLight
                val headerBrush = if (isLightTheme) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFDFBF7), Color(0xFFF3EFE7))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1708), Color(0xFF141414))
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(brush = headerBrush)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val bigIcon = when (activeLesson.type) {
                            "pdf" -> Icons.Default.Description
                            "article" -> Icons.Default.Article
                            "quick_note" -> Icons.Default.FlashOn
                            "gallery" -> Icons.Default.PhotoLibrary
                            else -> Icons.Default.MenuBook
                        }
                        
                        val typeTitle = when (activeLesson.type) {
                            "pdf" -> "PDF DIGITAL MANUAL"
                            "article" -> "EDUCATIONAL READING LAB"
                            "quick_note" -> "CORE HABITS & PROTOCOL"
                            "gallery" -> "INSPIRATIONAL GALLERY"
                            else -> "OFFLINE INTELLECT HANDBOOK"
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(PremiumGold.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, PremiumGold.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = bigIcon,
                                contentDescription = null,
                                tint = PremiumGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = typeTitle,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PremiumGold,
                            letterSpacing = 2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = activeLesson.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Action buttons
                        if (activeLesson.type == "pdf") {
                            Button(
                                onClick = { activeTab = 2 },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OPEN HIGH-RES HANDBOOK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { activeTab = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("READ LESSON WORKBOOK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. EXPANDABLE tabs section (Learning notes | Course Index | PDF Reader)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (activeTab) {
                    0 -> {
                        if (activeLesson.type.lowercase() == "gallery") {
                            InspirationalGalleryContent(
                                activeLesson = activeLesson,
                                viewModel = viewModel
                            )
                        } else {
                            NotesTabContent(
                                activeLesson = activeLesson,
                                viewModel = viewModel,
                                onOpenPdf = { activeTab = 2 },
                                onOpenLesson = { lessonId -> viewModel.selectedLessonId.value = lessonId }
                            )
                        }
                    }
                    1 -> LessonsTabContent(
                        lessons = lessons,
                        activeLessonId = activeLesson.id,
                        onLessonClick = { id ->
                            viewModel.selectedLessonId.value = id
                        }
                    )
                    2 -> PdfViewerMockContent(activeLesson = activeLesson, viewModel = viewModel)
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
    onFullscreenToggle: () -> Unit
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

                    // Fullscreen button
                    IconButton(
                        onClick = onFullscreenToggle,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFF1C1C1C), RoundedCornerShape(4.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Enter Fullscreen",
                            tint = Color.White,
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
                
                // Simple empty spacer on the right so the layout balanced exactly as before
                Spacer(modifier = Modifier.size(32.dp))
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
    activeLesson: LessonEntity,
    viewModel: AppViewModel,
    onOpenPdf: (String) -> Unit = {},
    onOpenLesson: (String) -> Unit = {}
) {
    val localNote = activeLesson.notePath ?: "## General Information\nEnjoy this high performance lecture."
    val type = activeLesson.type.lowercase()

    if (type == "article") {
        val scrollState = rememberScrollState()
        
        // Listen to scroll state changes reactively to sync article reading progress offline!
        LaunchedEffect(scrollState.value, scrollState.maxValue) {
            val max = scrollState.maxValue
            if (max > 0) {
                val progress = ((scrollState.value.toFloat() / max.toFloat()) * 100).toInt().coerceIn(0, 100)
                if (progress > activeLesson.articleProgress) {
                    viewModel.updateLessonProgressState(activeLesson.id, progress)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Read percentage indicator header block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PremiumGold.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(0.5.dp, PremiumGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📖", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "READING PROGRESS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumGold,
                        letterSpacing = 1.sp
                    )
                }
                
                Text(
                    text = "${activeLesson.articleProgress}% READ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeLesson.isCompleted) Color(0xFF81C784) else PremiumGold,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            if (activeLesson.isCompleted) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E7D32).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🛡️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lesson Complete! Earned +10 XP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            RichNotesRenderer(
                noteContent = localNote,
                onOpenPdf = onOpenPdf,
                onOpenLesson = onOpenLesson,
                isNested = true
            )
        }
    } else {
        // Standard view
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            RichNotesRenderer(
                noteContent = localNote,
                onOpenPdf = onOpenPdf,
                onOpenLesson = onOpenLesson,
                isNested = false
            )
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
                color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (isActive) PremiumGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
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
                            .background(if (isActive) PremiumGold else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
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
                            color = if (isActive) PremiumGold else MaterialTheme.colorScheme.onSurface,
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
fun InspirationalGalleryContent(
    activeLesson: LessonEntity,
    viewModel: AppViewModel
) {
    val images = listOf(
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=500&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1513829096999-4978602294fc?w=500&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=500&auto=format&fit=crop"
    )

    var currentIdx by remember(activeLesson.id) { mutableStateOf(0) }
    val viewedSet = remember(activeLesson.id) { mutableStateOf(setOf(0)) }

    LaunchedEffect(currentIdx) {
        val nextSet = viewedSet.value + currentIdx
        viewedSet.value = nextSet
        val percent = ((nextSet.size.toFloat() / images.size.toFloat()) * 100).toInt().coerceIn(0, 100)
        if (percent > activeLesson.imageProgress) {
            viewModel.updateLessonProgressState(activeLesson.id, percent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "INSPIRATIONAL GALLERY DECK",
            color = PremiumGold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black, RoundedCornerShape(12.dp))
                .border(1.dp, PremiumGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = images[currentIdx],
                contentDescription = "Visual Asset $currentIdx",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Curated Aesthetic Element ${currentIdx + 1} of ${images.size}",
                    color = WarmWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (currentIdx > 0) currentIdx-- },
                enabled = currentIdx > 0,
                modifier = Modifier.background(Graphite, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Prev", tint = PremiumGold)
            }

            Text(
                text = "Viewed: ${viewedSet.value.size} / ${images.size} (${activeLesson.imageProgress}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WarmWhite
            )

            IconButton(
                onClick = { if (currentIdx < images.size - 1) currentIdx++ },
                enabled = currentIdx < images.size - 1,
                modifier = Modifier.background(Graphite, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next", tint = PremiumGold)
            }
        }

        if (activeLesson.isCompleted) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛡️", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gallery Completed (100% Viewed)! Earned +10 XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784)
                )
            }
        }
    }
}

@Composable
fun PdfViewerMockContent(
    activeLesson: LessonEntity,
    viewModel: AppViewModel
) {
    val pdfUri = activeLesson.pdfUri
    val context = LocalContext.current
    val visitedPages = remember(activeLesson.id) { mutableStateOf(setOf<Int>()) }

    if (!pdfUri.isNullOrEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            InAppPdfViewer(
                pdfPath = null,
                pdfUriStr = pdfUri,
                onClose = { /* Local embed inside player screen tabs - no closure needed */ },
                title = activeLesson.title,
                onPageChanged = { currentPage, totalPages ->
                    if (totalPages > 0) {
                        val nextSet = visitedPages.value + currentPage
                        visitedPages.value = nextSet
                        val percent = ((nextSet.size.toFloat() / totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
                        if (percent > activeLesson.pdfProgress) {
                            viewModel.updateLessonProgressState(activeLesson.id, percent)
                        }
                    }
                }
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "NO COMPENDIUM HANDBOOK ATTACHED",
                    color = PremiumGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This specific Masterclass segment does not include an offline PDF compendium.",
                    color = SubduedGray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FullscreenPlayerView(
    viewModel: AppViewModel,
    activeLesson: LessonEntity,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    playbackSpeed: Float,
    subtitlesActive: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSubtitleToggle: () -> Unit,
    onExitFullscreen: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var gestureIndicatorText by remember { mutableStateOf("") }
    var showGestureIndicator by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Auto-hide controls after 3.5 seconds
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { controlsVisible = !controlsVisible }
    ) {
        // Core Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Tapping Gestures Overlay (Invisible double tap / skip controls)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            try {
                                val player = viewModel.exoPlayer ?: return@detectTapGestures
                                val isLeft = offset.x < size.width * 0.4f
                                val seekAmount = if (isLeft) -10000 else 10000
                                val duration = if (player.duration > 0) player.duration else 0L
                                val targetPosition = (player.currentPosition + seekAmount).coerceIn(0, duration)
                                player.seekTo(targetPosition)
                                
                                gestureIndicatorText = if (isLeft) "⏪ REWIND 10S" else "FORWARD 10S ⏩"
                                coroutineScope.launch {
                                    showGestureIndicator = true
                                    delay(800)
                                    showGestureIndicator = false
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        onTap = {
                            controlsVisible = !controlsVisible
                        }
                    )
                }
        )

        // Subtitles Overlay (Native looking customized overlay)
        if (subtitlesActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (controlsVisible) 90.dp else 40.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "[Cinematic audio - " + formatTime(currentPosition) + "]",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Gesture Action Feedback
        AnimatedVisibility(
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Animated HUD Controls Layer
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            ) {
                // Top controls bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onExitFullscreen,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Fullscreen",
                                tint = PremiumGold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = activeLesson.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "FULLSCREEN DISCIPLINE HUD",
                                color = PremiumGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Middle Quick Play Pause Circle
                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(PremiumGold, CircleShape)
                        .size(60.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Play Progress",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Bottom Timeline and Sub controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = SubduedGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Slider(
                            value = if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f,
                            onValueChange = onSeek,
                            colors = SliderDefaults.colors(
                                activeTrackColor = PremiumGold,
                                inactiveTrackColor = Color(0xFF333333),
                                thumbColor = PremiumGold
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )
                        Text(
                            text = formatTime(totalDuration),
                            color = SubduedGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // play speed configuration cycle
                            TextButton(
                                onClick = {
                                    val nextSpeed = when (playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 1.0f
                                    }
                                    onSpeedChanged(nextSpeed)
                                },
                                modifier = Modifier
                                    .background(Color(0xFF222222), RoundedCornerShape(6.dp))
                                    .height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x Speed",
                                    color = PremiumGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // CC subtitling toggle
                            IconButton(
                                onClick = onSubtitleToggle,
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(if (subtitlesActive) Color(0xFF2F240E) else Color(0xFF222222), RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = "Toggle Subtitles",
                                    tint = if (subtitlesActive) PremiumGold else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onExitFullscreen,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF222222), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
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
