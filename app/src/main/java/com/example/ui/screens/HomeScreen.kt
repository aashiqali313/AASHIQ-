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
    onNavigateToPlayer: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val courses by viewModel.coursesState.collectAsState()
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
                        onEditProfileClick = { showProfileEditDialog = true }
                    )
                }

                // Certificates Row
                if (certificates.isNotEmpty()) {
                    item {
                        CertificatesHorizontalRow(
                            certificates = certificates,
                            onViewCertificate = { selectedCertificateToView = it }
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
                        CourseRowItem(
                            course = course,
                            onCourseClick = { onNavigateToCourseDetail(it.id) }
                        )
                    }
                }
            }

            if (showProfileEditDialog) {
                ProfileEditDialog(
                    profile = userProfile,
                    onDismiss = { showProfileEditDialog = false },
                    onSave = { name, age, gender, watchTime, streak ->
                        viewModel.updateProfile(name, age, gender, watchTime, streak)
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
                Text(
                    text = lesson.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Resume",
                    fontSize = 10.sp,
                    color = PremiumGold,
                    fontWeight = FontWeight.SemiBold
                )
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
fun CourseRowItem(course: CourseEntity, onCourseClick: (CourseEntity) -> Unit) {
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
    onEditProfileClick: () -> Unit
) {
    val level = getLearningLevel(profile.totalWatchTimeMinutes)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalGray),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left avatar / Welcome
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Graphite, CircleShape)
                            .border(1.5.dp, PremiumGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = PremiumGold,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarmWhite
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${profile.gender}, Age ${profile.age}",
                                fontSize = 11.sp,
                                color = SubduedGray
                            )
                        }
                    }
                }
                
                // Edit profile button
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats blocks row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Level Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "RANK LEVEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = SubduedGray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .background(PremiumGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, PremiumGold, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = PremiumGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = level,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumGold
                        )
                    }
                }
                
                // Streak Counter
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LEARNING STREAK",
                        style = MaterialTheme.typography.labelSmall,
                        color = SubduedGray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFF9800).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile.currentStreak} Days",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                // Total Watch metrics
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "WATCH TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = SubduedGray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .background(SubtleElectricBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, SubtleElectricBlue, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${profile.totalWatchTimeMinutes} Mins",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SubtleElectricBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CertificatesHorizontalRow(
    certificates: List<CertificateEntity>,
    onViewCertificate: (CertificateEntity) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "CLAIMED CERTIFICATES",
            style = MaterialTheme.typography.labelMedium,
            color = PremiumGold,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(certificates) { cert ->
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .clickable { onViewCertificate(cert) },
                    colors = CardDefaults.cardColors(containerColor = CharcoalGray),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Graphite, RoundedCornerShape(6.dp))
                                .border(0.5.dp, PremiumGold.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                tint = PremiumGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cert.courseName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = cert.certificateId,
                            fontSize = 10.sp,
                            color = PremiumGold,
                            fontFamily = FontFamily.Monospace
                        )
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
    onSave: (name: String, age: Int, gender: String, watchTime: Long, streak: Int) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var ageStr by remember { mutableStateOf(profile.age.toString()) }
    var gender by remember { mutableStateOf(profile.gender) }
    var watchTimeStr by remember { mutableStateOf(profile.totalWatchTimeMinutes.toString()) }
    var streakStr by remember { mutableStateOf(profile.currentStreak.toString()) }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                OutlinedTextField(
                    value = watchTimeStr,
                    onValueChange = { watchTimeStr = it },
                    label = { Text("Total Watch Time (Minutes)", color = SubduedGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = PremiumGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = streakStr,
                    onValueChange = { streakStr = it },
                    label = { Text("Streak Days (Daily Streaks)", color = SubduedGray) },
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
                    val finalWatchTime = watchTimeStr.toLongOrNull() ?: profile.totalWatchTimeMinutes
                    val finalStreak = streakStr.toIntOrNull() ?: profile.currentStreak
                    onSave(name, finalAge, gender, finalWatchTime, finalStreak)
                }
            ) {
                Text("SAVE", color = PremiumGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = SubduedGray)
            }
        }
    )
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
