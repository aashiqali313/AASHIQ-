package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.content.Intent
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.data.database.CourseEntity
import com.example.data.database.LessonEntity
import com.example.data.database.UserProfileEntity
import com.example.data.database.CertificateEntity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCourseDetail: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToCertificatesVault: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val courses by viewModel.coursesState.collectAsState()
    val allLessons by viewModel.allLessonsState.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()

    val userProfile by viewModel.profileState.collectAsState()
    val certificates by viewModel.certificatesState.collectAsState()

    var showProfileEditDialog by remember { mutableStateOf(false) }
    var selectedCertificateToView by remember { mutableStateOf<CertificateEntity?>(null) }

    // SAF Directory Picker Launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.importLocalFolder(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeTopBar(
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToSettings = onNavigateToSettings,
                onTriggerImport = { folderPickerLauncher.launch(null) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background Subtle Warm Gold Radial Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PremiumGold.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_feed_lazycolumn"),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Import overlay/header indicator
                if (isImporting) {
                    item {
                        ImportProgressOverlay(
                            status = importStatus,
                            progress = importProgress
                        )
                    }
                }

                // Profile Dashboard Header
                item {
                    ProfileGamificationHeader(
                        profile = userProfile,
                        lessons = allLessons,
                        certificatesCount = certificates.size,
                        onEditProfileClick = { showProfileEditDialog = true }
                    )
                }

                // Certificates Vault Link
                if (certificates.isNotEmpty()) {
                    item {
                        PremiumCertificatesVaultBanner(
                            certificatesCount = certificates.size,
                            onClick = onNavigateToCertificatesVault
                        )
                    }
                }

                // Course Carousel
                if (courses.isNotEmpty()) {
                    item {
                        Text(
                            text = "RECOMMENDED MASTERCLASSES",
                            style = MaterialTheme.typography.labelMedium,
                            color = PremiumGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                        CourseHeroCarousel(
                            courses = courses,
                            onExploreCourse = { onNavigateToCourseDetail(it.id) }
                        )
                    }
                }

                // Continue Watching
                if (continueWatching.isNotEmpty()) {
                    item {
                        Text(
                            text = "CONTINUE LEARNING",
                            style = MaterialTheme.typography.labelMedium,
                            color = PremiumGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            items(continueWatching, key = { it.id }) { lesson ->
                                ContinueWatchingCard(
                                    lesson = lesson,
                                    onPlayClick = {
                                        viewModel.selectedCourseId.value = lesson.courseId
                                        viewModel.selectedLessonId.value = lesson.id
                                        onNavigateToPlayer(lesson.id)
                                    }
                                )
                            }
                        }
                    }
                }

                // All Courses Row/Grid section
                item {
                    Text(
                        text = "EXPLORE PLATFORM",
                        style = MaterialTheme.typography.labelMedium,
                        color = PremiumGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                    )
                }

                if (courses.isEmpty() && !isImporting) {
                    item {
                        EmptyCoursesPlaceholder(
                            onImportClick = { folderPickerLauncher.launch(null) }
                        )
                    }
                } else {
                    items(courses, key = { it.id }) { course ->
                        val lessonsOfCourse = allLessons.filter { it.courseId == course.id }
                        CourseRowItem(
                            course = course,
                            lessons = lessonsOfCourse,
                            onCourseClick = { onNavigateToCourseDetail(it.id) }
                        )
                    }
                }
            }

            if (showProfileEditDialog) {
                ProfileEditDialog(
                    profile = userProfile,
                    onDismiss = { showProfileEditDialog = false },
                    onSave = { name, age, gender, avatarUri ->
                        viewModel.updateProfile(name, age, gender, avatarUri)
                        showProfileEditDialog = false
                    }
                )
            }

            val cert = selectedCertificateToView
            if (cert != null) {
                CertificateViewDialog(
                    certificate = cert,
                    onDismiss = { selectedCertificateToView = null }
                )
            }

            // Real-time automatic certificate unlocked premium popup!
            val newlyUnlockedCert by viewModel.newlyUnlockedCertificate.collectAsState()
            if (newlyUnlockedCert != null) {
                CertificateUnlockedDialog(
                    certificate = newlyUnlockedCert!!,
                    onDismiss = { viewModel.newlyUnlockedCertificate.value = null }
                )
            }
        }
    }
}

@Composable
fun HomeTopBar(
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onTriggerImport: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Elegant Glow Brand Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(17.dp))
                        .border(1.dp, PremiumGold, RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = PremiumGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AASHIQ+",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = PremiumGold,
                    letterSpacing = 1.5.sp
                )
            }

            // Quick Actions Block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Import file button
                IconButton(
                    onClick = onTriggerImport,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                        .testTag("home_import_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Import local course folder",
                        tint = PremiumGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Global search button
                IconButton(
                    onClick = onNavigateToSearch,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                        .testTag("home_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Global Search",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings button
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ImportProgressOverlay(status: String, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, PremiumGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "IMPORTING COURSE FOLDER",
                    style = MaterialTheme.typography.labelMedium,
                    color = PremiumGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftGoldGlow
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = progress,
                color = PremiumGold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = status,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = SubduedGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CourseHeroCarousel(courses: List<CourseEntity>, onExploreCourse: (CourseEntity) -> Unit) {
    var activePage by remember { mutableStateOf(0) }

    // Auto rotate Hero Carousel
    LaunchedEffect(courses) {
        while (courses.size > 1) {
            delay(5000)
            activePage = (activePage + 1) % courses.size
        }
    }

    if (activePage >= courses.size) {
        activePage = 0
    }
    
    val activeCourse = courses.getOrNull(activePage) ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onExploreCourse(activeCourse) }
    ) {
        // Thumbnail Image
        AsyncImage(
            model = activeCourse.thumbnailUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                    )
                )
        )

        // Upper Category pill
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(TranslucentGlass, RoundedCornerShape(20.dp))
                .border(0.5.dp, PremiumGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = activeCourse.category.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PremiumGold,
                letterSpacing = 1.sp
            )
        }

        // Details Block
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = activeCourse.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = activeCourse.author,
                    fontSize = 11.sp,
                    color = SubduedGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Slide Indicators dots
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            courses.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == activePage) 16.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index == activePage) PremiumGold else Color.Gray.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(lesson: LessonEntity, onPlayClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(220.dp)
            .height(115.dp)
            .clickable { onPlayClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val formattedDuration = remember(lesson.durationSeconds) {
                    val hrs = lesson.durationSeconds / 3600
                    val mins = (lesson.durationSeconds % 3600) / 60
                    val secs = lesson.durationSeconds % 60
                    if (hrs > 0) String.format("%02d:%02d:%02d", hrs, mins, secs)
                    else String.format("%02d:%02d", mins, secs)
                }

                Text(
                    text = lesson.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resume",
                        fontSize = 10.sp,
                        color = PremiumGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (lesson.durationSeconds > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•  $formattedDuration",
                            fontSize = 10.sp,
                            color = SubduedGray,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Slim gold custom progress indicator bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Watched ${lesson.playProgressPercent}%",
                        fontSize = 9.sp,
                        color = SubduedGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = lesson.playProgressPercent / 100f,
                    color = PremiumGold,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                )
            }
        }
    }
}

@Composable
fun CourseRowItem(
    course: CourseEntity,
    lessons: List<LessonEntity>,
    onCourseClick: (CourseEntity) -> Unit
) {
    val totalSeconds = remember(lessons) { lessons.sumOf { it.durationSeconds } }
    val formattedDuration = remember(totalSeconds) {
        val hrs = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        if (hrs > 0) "${hrs}h ${mins}m"
        else "${mins}m"
    }

    val resolutionStr = remember(lessons) {
        lessons.firstOrNull { !it.resolution.isNullOrBlank() }?.resolution
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onCourseClick(course) }
            .testTag("course_item_card_${course.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant Rounded Thumbnail
            AsyncImage(
                model = course.thumbnailUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = course.category.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumGold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (lessons.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  ${lessons.size} LESSONS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SubduedGray,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (totalSeconds > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  $formattedDuration",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SubduedGray,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (!resolutionStr.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  $resolutionStr",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SubduedGray,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = course.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = course.description,
                    fontSize = 11.sp,
                    color = SubduedGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
fun EmptyCoursesPlaceholder(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(PremiumGold.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Drive Library is Vacant",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select any local folder on your Android unit loaded with media structures (.mp4) and notes (.md) to compile offline courses instantly on Device.",
            fontSize = 11.sp,
            color = SubduedGray,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onImportClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumGold,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "INDEX STORAGE DIRECTORY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

fun getLearningLevel(watchTimeMinutes: Long): String {
    return when {
        watchTimeMinutes < 60 -> "Initiate"
        watchTimeMinutes < 180 -> "Focused"
        watchTimeMinutes < 420 -> "Disciplined"
        watchTimeMinutes < 900 -> "Advanced"
        watchTimeMinutes < 1800 -> "Elite"
        else -> "Mastery"
    }
}

@Composable
fun ProfileGamificationHeader(
    profile: UserProfileEntity,
    lessons: List<LessonEntity>,
    certificatesCount: Int,
    onEditProfileClick: () -> Unit
) {
    val totalCompletedLessons = remember(lessons) { lessons.count { it.isCompleted } }
    
    // Smooth infinite breathing pulse for streak flame
    val infiniteTransition = rememberInfiniteTransition(label = "streak")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Calculate level tiers and progress toward next level
    val currentXP = profile.totalXP
    val levelData = remember(currentXP) {
        when {
            currentXP < 100 -> Triple("Beginner", 0L, 100L)
            currentXP < 250 -> Triple("Explorer", 100L, 250L)
            currentXP < 500 -> Triple("Disciplined", 250L, 500L)
            currentXP < 1000 -> Triple("Elite", 500L, 1000L)
            currentXP < 2000 -> Triple("Master", 1000L, 2000L)
            else -> Triple("Ascended", 2000L, 5000L)
        }
    }
    val levelName = levelData.first
    val minLevelXP = levelData.second
    val maxLevelXP = levelData.third
    val progressFraction = remember(currentXP, minLevelXP, maxLevelXP) {
        if (maxLevelXP > minLevelXP) {
            ((currentXP - minLevelXP).toFloat() / (maxLevelXP - minLevelXP).toFloat()).coerceIn(0f, 1f)
        } else 1f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalGray),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Avatar, Name, Edit Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Graphite, CircleShape)
                            .border(1.5.dp, PremiumGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.avatarUri.isNotEmpty()) {
                            AsyncImage(
                                model = profile.avatarUri,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                tint = PremiumGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WarmWhite
                            )
                        }
                        Text(
                            text = "${profile.gender}, Age ${profile.age}",
                            fontSize = 11.sp,
                            color = SubduedGray
                        )
                    }
                }
                
                IconButton(
                    onClick = onEditProfileClick,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Graphite, CircleShape)
                        .border(0.5.dp, PremiumGold.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = PremiumGold,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))

            // Tier Progress Bar Area
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "LEVEL: ${levelName.uppercase()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumGold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$currentXP / $maxLevelXP XP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SubduedGray,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Customized luxury golden progress indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Graphite, RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PremiumGold, SoftGoldGlow)
                            ),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Gamified Highlights Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Streak Card (Animated Flame)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Graphite, RoundedCornerShape(12.dp))
                        .border(0.5.dp, PremiumGold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .scale(pulseScale),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔥",
                                fontSize = 24.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${profile.currentStreak} DAYS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF9800),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "STREAK ACTIVE",
                            fontSize = 8.sp,
                            color = SubduedGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Certificates Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Graphite, RoundedCornerShape(12.dp))
                        .border(0.5.dp, PremiumGold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎓",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$certificatesCount EARNED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = PremiumGold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "OFFLINE CREDENTIALS",
                            fontSize = 8.sp,
                            color = SubduedGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Detailed Statistics Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Courses completed
                    StatItemBox(
                        modifier = Modifier.weight(1f),
                        emoji = "📚",
                        value = "${profile.completedCoursesCount}",
                        label = "COURSES COMPLETED"
                    )
                    // Lessons completed
                    StatItemBox(
                        modifier = Modifier.weight(1f),
                        emoji = "✅",
                        value = "$totalCompletedLessons",
                        label = "LESSONS COMPLETED"
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Watch Time
                    StatItemBox(
                        modifier = Modifier.weight(1f),
                        emoji = "🎥",
                        value = "${profile.totalWatchTimeMinutes}m",
                        label = "WATCHING TIME"
                    )
                    // Reading Time
                    StatItemBox(
                        modifier = Modifier.weight(1f),
                        emoji = "📖",
                        value = "${profile.readingTimeMinutes}m",
                        label = "READING TIME"
                    )
                }
            }
        }
    }
}

@Composable
fun StatItemBox(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    label: String
) {
    Row(
        modifier = modifier
            .background(Graphite.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WarmWhite
            )
            Text(
                text = label,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Medium,
                color = SubduedGray,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun PremiumCertificatesVaultBanner(
    certificatesCount: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.2.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            PremiumGold.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subtle glowing gold vault icon holder
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(PremiumGold.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, PremiumGold.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = PremiumGold,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "CERTIFICATES VAULT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = WarmWhite,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "View secure cryptographically signed sheets",
                        fontSize = 10.sp,
                        color = SubduedGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Gold outlined button indicator showing counts
            Row(
                modifier = Modifier
                    .background(PremiumGold.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .border(1.dp, PremiumGold, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$certificatesCount EARNED",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PremiumGold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun cropAndSaveOffline(context: android.content.Context, original: Bitmap, scale: Float, offsetX: Float, offsetY: Float): String? {
    return try {
        val width = original.width
        val height = original.height
        val minDim = Math.min(width, height)
        
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        matrix.postTranslate(offsetX * (width / 220f) * 0.25f, offsetY * (height / 220f) * 0.25f)
        
        val scaledBitmap = Bitmap.createBitmap(original, 0, 0, width, height, matrix, true)
        
        val outputCircle = Bitmap.createBitmap(minDim, minDim, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(outputCircle)
        val paint = android.graphics.Paint()
        val rect = android.graphics.Rect(0, 0, minDim, minDim)
        
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(minDim / 2f, minDim / 2f, minDim / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        
        val srcRect = android.graphics.Rect(
            ((scaledBitmap.width - minDim) / 2).coerceAtLeast(0),
            ((scaledBitmap.height - minDim) / 2).coerceAtLeast(0),
            ((scaledBitmap.width + minDim) / 2).coerceAtMost(scaledBitmap.width),
            ((scaledBitmap.height + minDim) / 2).coerceAtMost(scaledBitmap.height)
        )
        canvas.drawBitmap(scaledBitmap, srcRect, rect, paint)

        val avatarFile = File(context.filesDir, "profile_avatar_${System.currentTimeMillis()}.png")
        context.filesDir.listFiles { _, name -> name.startsWith("profile_avatar") }?.forEach { it.delete() }

        FileOutputStream(avatarFile).use { fos ->
            outputCircle.compress(Bitmap.CompressFormat.PNG, 95, fos)
        }
        avatarFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun SimpleCircularCropperDialog(
    imageUriStr: String,
    onDismiss: () -> Unit,
    onCropped: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap = remember(imageUriStr) {
        try {
            val uri = Uri.parse(imageUriStr)
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap == null) {
        onDismiss()
        return
    }

    var scale by remember { mutableStateOf(1.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalGray),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, PremiumGold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CROP PROFILE PICTURE",
                    style = MaterialTheme.typography.titleMedium,
                    color = PremiumGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pinch to zoom and drag to position",
                    fontSize = 11.sp,
                    color = SubduedGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                offsetX += pan.x * scale
                                offsetY += pan.y * scale
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX / 4f,
                                translationY = offsetY / 4f
                            )
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val circleRadius = size.minDimension / 2.2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        val path = Path().apply {
                            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                        }
                        val circlePath = Path().apply {
                            addOval(androidx.compose.ui.geometry.Rect(center.x - circleRadius, center.y - circleRadius, center.x + circleRadius, center.y + circleRadius))
                        }
                        val diffPath = Path.combine(PathOperation.Difference, path, circlePath)
                        drawPath(diffPath, Color.Black.copy(alpha = 0.65f))

                        drawCircle(
                            color = PremiumGold,
                            radius = circleRadius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    valueRange = 1f..4f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = PremiumGold,
                        inactiveTrackColor = Graphite,
                        thumbColor = PremiumGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = SubduedGray)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val croppedResult = cropAndSaveOffline(context, bitmap, scale, offsetX, offsetY)
                                if (croppedResult != null) {
                                    onCropped(croppedResult)
                                } else {
                                    onCropped(imageUriStr)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("APPLY CROP", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditDialog(
    profile: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, age: Int, gender: String, avatarUri: String) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var ageStr by remember { mutableStateOf(profile.age.toString()) }
    var gender by remember { mutableStateOf(profile.gender) }
    var avatarUri by remember { mutableStateOf(profile.avatarUri) }

    var selectedTempUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            selectedTempUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalGray,
        title = {
            Text(
                text = "EDIT PROFILE",
                color = PremiumGold,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circle Profile Pic Preview with Change Action
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(Graphite, CircleShape)
                        .border(2.dp, PremiumGold, CircleShape)
                        .clickable {
                            galleryLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri.isNotEmpty()) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Avatar Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PremiumGold,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    // Small floating camera badge overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .background(PremiumGold, CircleShape)
                            .border(1.5.dp, CharcoalGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Pick Image",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = "Tap circle to upload a custom picture",
                    fontSize = 10.sp,
                    color = SubduedGray,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name", color = SubduedGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = PremiumGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ageStr,
                    onValueChange = { ageStr = it },
                    label = { Text("Your Age", color = SubduedGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = PremiumGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it },
                    label = { Text("Your Gender", color = SubduedGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = PremiumGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalAge = ageStr.toIntOrNull() ?: profile.age
                    onSave(name, finalAge, gender, avatarUri)
                }
            ) {
                Text("SAVE PROFILE", color = PremiumGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = SubduedGray)
            }
        }
    )

    // Crop trigger inline Dialog!
    if (selectedTempUri != null) {
        SimpleCircularCropperDialog(
            imageUriStr = selectedTempUri!!,
            onDismiss = { selectedTempUri = null },
            onCropped = { croppedPath ->
                avatarUri = croppedPath
                selectedTempUri = null
            }
        )
    }
}

@Composable
fun CertificateViewDialog(
    certificate: CertificateEntity,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(MatteBlack, RoundedCornerShape(16.dp))
                .border(2.dp, PremiumGold, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Outer Border Gold Stamp Decor
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AASHIQ+ ACADEMY OF EXCELLENCE",
                    color = PremiumGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CERTIFICATE OF ACHIEVEMENT",
                    color = WarmWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This premium credential is super-proudly awarded to",
                    fontSize = 11.sp,
                    color = SubduedGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = certificate.userName,
                    color = PremiumGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(modifier = Modifier.width(180.dp), color = PremiumGold.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "for outstanding completion and mastery of the Masterclass",
                    fontSize = 10.sp,
                    color = SubduedGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = certificate.courseName,
                    color = WarmWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Bottom Row for Verification Signature and Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Aashiq Ali",
                            color = PremiumGold,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.width(80.dp), color = SubduedGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                        Text(
                            text = "CHIEF ARCHITECT",
                            color = SubduedGray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val formattedDate = remember(certificate.completionDate) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(certificate.completionDate))
                        }
                        Text(
                            text = formattedDate,
                            color = WarmWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.width(80.dp), color = SubduedGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                        Text(
                            text = "DATE PASSED",
                            color = SubduedGray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "CRedential fingerprint: ${certificate.hashSignature}",
                    color = PremiumGold.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Credential ID: ${certificate.certificateId}",
                    color = SubduedGray,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLOSE CREDENTIAL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CertificateUnlockedDialog(
    certificate: CertificateEntity,
    onDismiss: () -> Unit
) {
    var showScaleUp by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showScaleUp = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = showScaleUp,
                enter = scaleIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeIn(tween(500)),
                modifier = Modifier.clickable(enabled = false) {}
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalGray),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, PremiumGold)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Glowing Trophy Icon decoration
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(PremiumGold.copy(alpha = 0.15f), CircleShape)
                                .border(1.5.dp, PremiumGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = PremiumGold,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "CONGRATULATIONS",
                            style = MaterialTheme.typography.labelMedium,
                            color = PremiumGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "OFFLINE CERTIFICATE UNLOCKED",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "You have officially achieved the completion milestone for:",
                            fontSize = 12.sp,
                            color = SubduedGray,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = certificate.courseName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumGold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Brief details preview
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Graphite, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("RECIPIENT", fontSize = 8.sp, color = SubduedGray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(certificate.userName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CREDENTIAL ID", fontSize = 8.sp, color = SubduedGray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(certificate.certificateId, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CLAIM & CLOSE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
