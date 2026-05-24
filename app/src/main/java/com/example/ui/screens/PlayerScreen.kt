package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.domain.Bookmark
import com.example.domain.Course
import com.example.domain.Lesson
import com.example.domain.PlaybackProgress
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    courseId: String,
    lessonId: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val activeCourse by viewModel.activeCourse.collectAsState()
    val progressFlow by viewModel.getProgressForLesson(lessonId).collectAsState(initial = null)
    val bookmarks by viewModel.allBookmarks.collectAsState()
    val noteContent by viewModel.activeNoteContent.collectAsState()
    val isNoteLoading by viewModel.isNoteLoading.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // 1. Resolve structures
    val course = activeCourse
    val lessons = remember(course) { course?.modules?.flatMap { it.lessons } ?: emptyList() }
    val currentLesson = remember(lessons, lessonId) { lessons.find { it.id == lessonId } }

    // Navigation queue logic
    val currentLessonIndex = remember(lessons, lessonId) { lessons.indexOfFirst { it.id == lessonId } }
    val nextLesson = remember(lessons, currentLessonIndex) {
        if (currentLessonIndex != -1 && currentLessonIndex < lessons.lastIndex) {
            lessons[currentLessonIndex + 1]
        } else null
    }

    // 2. Local audio and brightness managers
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    
    var localVolume by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var localBrightness by remember {
        val act = context as? Activity
        val lp = act?.window?.attributes
        val startBright = if (lp != null && lp.screenBrightness >= 0f) lp.screenBrightness else 0.5f
        mutableStateOf(startBright)
    }

    // Fade-out gesture indicators
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }

    // Player states
    var isPlaying by remember { mutableStateOf(false) }
    var videoPosition by remember { mutableStateOf(0L) }
    var videoDuration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var currentSpeed by remember { mutableStateOf(settings.defaultSpeed) }

    // Tab control system
    var activeTab by remember { mutableStateOf(0) } // 0: Player queue, 1: Markdown Notes, 2: Bookmarks

    // 3. Setup ExoPlayer instance safely
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Load lesson Note content when screen wakes
    LaunchedEffect(course, currentLesson) {
        if (course != null && currentLesson != null) {
            viewModel.loadLessonNote(course.courseUri, currentLesson.note)
        }
    }

    // Bind ExoPlayer life-cycles
    DisposableEffect(exoPlayer) {
        onDispose {
            // Auto-save progress on exit
            if (currentLesson != null) {
                viewModel.saveProgress(
                    lessonId = currentLesson.id,
                    courseId = courseId,
                    positionMs = exoPlayer.currentPosition,
                    completed = exoPlayer.currentPosition >= exoPlayer.duration * 0.95f || exoPlayer.currentPosition >= (exoPlayer.duration - 10000), // marked complete above 95%
                    speed = currentSpeed
                )
            }
            exoPlayer.release()
        }
    }

    // Handle Media Item loads & playback speeds
    LaunchedEffect(currentLesson, progressFlow) {
        if (currentLesson != null) {
            val videoUri = Uri.parse(currentLesson.video)
            val mediaItem = MediaItem.fromUri(videoUri)
            
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            
            // Set playback speed
            exoPlayer.setPlaybackSpeed(currentSpeed)

            // Seek back to previous progress if available
            val previousProgress = progressFlow
            if (previousProgress != null) {
                exoPlayer.seekTo(previousProgress.currentPosition)
            } else {
                exoPlayer.seekTo(0L)
            }
            exoPlayer.playWhenReady = true
            isPlaying = true
        }
    }

    // Periodically sync progress
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoPosition = exoPlayer.currentPosition
            videoDuration = exoPlayer.duration
            delay(1000L)
        }
    }

    // Handle play state changes & autoplay next on finish
    var lastState by remember { mutableStateOf(Player.STATE_IDLE) }
    val playerListener = remember {
        object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                lastState = playbackState
                if (playbackState == Player.STATE_ENDED) {
                    // Save final 100% completed progress
                    if (currentLesson != null) {
                        viewModel.saveProgress(
                            lessonId = currentLesson.id,
                            courseId = courseId,
                            positionMs = exoPlayer.duration,
                            completed = true,
                            speed = currentSpeed
                        )
                    }
                    // Trigger next lesson autoplay if specified
                    if (settings.autoplay && nextLesson != null) {
                        viewModel.selectLesson(nextLesson.id)
                    }
                }
            }
        }
    }

    DisposableEffect(exoPlayer) {
        exoPlayer.addListener(playerListener)
        onDispose {
            exoPlayer.removeListener(playerListener)
        }
    }

    // Hide controls overlay after 4 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000L)
            showControls = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Header layout (bypasses TopAppBar and eliminates Experimental annotations)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("player_back_btn")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Go back", tint = MaterialTheme.colorScheme.onBackground)
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = currentLesson?.title ?: "Offline Premium Player",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = course?.title ?: "Learning Series",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    val curPos = exoPlayer.currentPosition
                    if (currentLesson != null) {
                        viewModel.addBookmark(
                            courseId = courseId,
                            lessonId = currentLesson.id,
                            lessonTitle = currentLesson.title,
                            timestampMs = curPos
                        )
                    }
                },
                modifier = Modifier.testTag("player_bookmark_btn")
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Bookmark position",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // A. Cinematic Video Player Box with Swipe Gestures
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(Color.Black)
                .pointerInput(Unit) {
                        // Gesture detection
                        detectVerticalDragGestures(
                            onDragEnd = {
                                showVolumeIndicator = false
                                showBrightnessIndicator = false
                            },
                            onVerticalDrag = { change, dragAmount ->
                                val screenWidth = size.width
                                val isLeftHalf = change.position.x < screenWidth / 2f
                                
                                if (isLeftHalf) {
                                    // Adjust brightness
                                    val delta = -dragAmount / 300f
                                    localBrightness = (localBrightness + delta).coerceIn(0.1f, 1.0f)
                                    val act = context as? Activity
                                    val lp = act?.window?.attributes
                                    if (lp != null) {
                                        lp.screenBrightness = localBrightness
                                        act.window.attributes = lp
                                    }
                                    showBrightnessIndicator = true
                                    showVolumeIndicator = false
                                } else {
                                    // Adjust volume
                                    val delta = -dragAmount / 20f
                                    localVolume = (localVolume + delta).coerceIn(0f, maxVolume)
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        localVolume.toInt(),
                                        0
                                    )
                                    showVolumeIndicator = true
                                    showBrightnessIndicator = false
                                }
                            }
                        )
                    }
                    .clickable {
                        showControls = !showControls
                    }
            ) {
                // Real Android ExoPlayer View container
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false // Hide defaults, we draw ours beautifully in Compose!
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Translucent Gold Control Overlays (Animated fading on showControls status)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x73000000))
                    ) {
                        // Center Playback actions (Rewind, Play, FastForward)
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(newPos)
                                    videoPosition = newPos
                                },
                                modifier = Modifier
                                    .background(Color(0x4D000000), CircleShape)
                                    .size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                        isPlaying = false
                                    } else {
                                        exoPlayer.play()
                                        isPlaying = true
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .size(54.dp)
                                    .testTag("overlay_play_pause_btn")
                            ) {
                                CustomPlayPauseIcon(
                                    isPlaying = isPlaying,
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            IconButton(
                                onClick = {
                                    val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                                    exoPlayer.seekTo(newPos)
                                    videoPosition = newPos
                                },
                                modifier = Modifier
                                    .background(Color(0x4D000000), CircleShape)
                                    .size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White
                                )
                            }
                        }

                        // Bottom progress slider row
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0x99000000))
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatDuration(videoPosition / 1000),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatDuration(videoDuration / 1000),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val ratio = if (videoDuration > 0) videoPosition.toFloat() / videoDuration.toFloat() else 0f
                            Slider(
                                value = ratio,
                                onValueChange = { value ->
                                    val seekPos = (value * videoDuration).toLong()
                                    exoPlayer.seekTo(seekPos)
                                    videoPosition = seekPos
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color(0x73FFFFFF)
                                ),
                                modifier = Modifier
                                    .height(18.dp)
                                    .testTag("video_progress_slider")
                            )
                        }
                    }
                }

                // 3. Floating feedback overlays for gestures (Volume, Brightness)
                if (showVolumeIndicator) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color(0x99000000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Volume: ${(localVolume / maxVolume * 100).toInt()}%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (showBrightnessIndicator) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color(0x99000000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Brightness: ${(localBrightness * 100).toInt()}%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // B. Speed and Config panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback speed sector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Playback Speed: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    var expandedSpeedMenu by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            "${currentSpeed}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { expandedSpeedMenu = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("speed_dropdown")
                        )
                        DropdownMenu(
                            expanded = expandedSpeedMenu,
                            onDismissRequest = { expandedSpeedMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speedOption ->
                                DropdownMenuItem(
                                    text = { Text("${speedOption}x", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        currentSpeed = speedOption
                                        exoPlayer.setPlaybackSpeed(speedOption)
                                        expandedSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Subtitle flag, autoplay flags info
                Text(
                    text = "AutoPlay: ${if (settings.autoplay) "Enabled" else "Off"}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // C. Tabs Selection (Lesson Queue, Notes, Bookmarks)
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("QUEUE", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("NOTES", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("BOOKMARKS", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // D. Tab Panels Content switcher
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> {
                        // Lesson Queue Tab
                        if (lessons.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No lessons in this course flow structure.", color = Color(0xFFAEAEB2))
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(lessons) { les ->
                                    val isCurrent = les.id == lessonId
                                    val progressPct = progressFlow?.takeIf { les.id == it.lessonId }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                // Save current lesson progress before loading next!
                                                if (currentLesson != null) {
                                                    viewModel.saveProgress(
                                                        lessonId = currentLesson.id,
                                                        courseId = courseId,
                                                        positionMs = exoPlayer.currentPosition,
                                                        completed = exoPlayer.currentPosition >= exoPlayer.duration * 0.95f,
                                                        speed = currentSpeed
                                                    )
                                                }
                                                viewModel.selectLesson(les.id)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xFFAEAEB2),
                                            modifier = Modifier.size(22.dp)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                les.title,
                                                fontSize = 13.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "Length: ${formatDuration(les.duration.toLong())}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFAEAEB2)
                                            )
                                        }

                                        if (progressPct?.completed == true) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Lesson Completed",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color(0x0AFFFFFF))
                                }
                            }
                        }
                    }

                    1 -> {
                        // Notes Tab (Elegant local markdown reader)
                        if (currentLesson?.note.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No attachment notes assigned for this lesson.",
                                    color = Color(0xFFAEAEB2),
                                    fontSize = 12.sp
                                )
                            }
                        } else if (isNoteLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                item {
                                    MarkdownNotesRenderer(text = noteContent)
                                }
                            }
                        }
                    }

                    2 -> {
                        // Bookmarks Tab
                        val lessonBookmarks = bookmarks.filter { it.lessonId == lessonId }
                        if (lessonBookmarks.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "No bookmarks added in this session yet.",
                                        color = Color(0xFFAEAEB2),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Tap the Favorite icon in player bar to save stamps.",
                                        color = Color(0x7FAECEB2),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(lessonBookmarks) { bmrk ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                exoPlayer.seekTo(bmrk.timestamp)
                                                videoPosition = bmrk.timestamp
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    "Bookmark Stamp",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    "Timestamp: ${formatDuration(bmrk.timestamp / 1000)}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeBookmark(bmrk.id) },
                                            modifier = Modifier.testTag("delete_bookmark_${bmrk.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete bookmark",
                                                tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = Color(0x0AFFFFFF))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

// Highly reliable, hand-crafted local markdown parser with strong visual layout hierarchy
@Composable
fun MarkdownNotesRenderer(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        val lines = text.split("\n")
        var inList = false
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                continue
            }

            // A. Headings H1
            if (trimmed.startsWith("# ")) {
                val headerText = trimmed.removePrefix("# ")
                Text(
                    text = headerText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 12.dp),
                    fontFamily = FontFamily.SansSerif
                )
                inList = false
            }
            // B. Headings H2 or H3
            else if (trimmed.startsWith("## ") || trimmed.startsWith("### ")) {
                val headerText = trimmed.replace(Regex("^#+ "), "")
                Text(
                    text = headerText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontFamily = FontFamily.SansSerif
                )
                inList = false
            }
            // C. Blockquotes
            else if (trimmed.startsWith("> ")) {
                val bqText = trimmed.removePrefix("> ")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(Color(0x1F2B2B30), RoundedCornerShape(4.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = bqText,
                        fontSize = 12.sp,
                        color = Color(0xFFAEAEB2),
                        fontFamily = FontFamily.SansSerif
                    )
                }
                inList = false
            }
            // D. Bullet list items
            else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                val listText = trimmed.substring(2)
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "•",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = listText,
                        fontSize = 13.sp,
                        color = Color(0xFFAEAEB2),
                        lineHeight = 18.sp
                    )
                }
                inList = true
            }
            // E. Normal text
            else {
                Text(
                    text = trimmed,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontFamily = FontFamily.SansSerif
                )
                inList = false
            }
        }
    }
}

@Composable
fun CustomPlayPauseIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (isPlaying) {
            // Draw sleek parallel bars
            val barW = w * 0.28f
            val spacing = w * 0.16f
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.12f),
                size = androidx.compose.ui.geometry.Size(barW, h * 0.76f)
            )
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.14f + barW + spacing, h * 0.12f),
                size = androidx.compose.ui.geometry.Size(barW, h * 0.76f)
            )
        } else {
            // Draw luxury play triangle
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.22f, h * 0.12f)
                lineTo(w * 0.88f, h * 0.5f)
                lineTo(w * 0.22f, h * 0.88f)
                close()
            }
            drawPath(path = path, color = color)
        }
    }
}
