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
    val bookmarkedLessons by viewModel.bookmarkedLessons.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()

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

                // Bookmarks collection preview
                if (bookmarkedLessons.isNotEmpty()) {
                    item {
                        Text(
                            text = "SAVED MOMENTS",
                            style = MaterialTheme.typography.labelMedium,
                            color = PremiumGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                        ) {
                            items(bookmarkedLessons, key = { it.id }) { lesson ->
                                BookmarkChipItem(
                                    lesson = lesson,
                                    onClick = {
                                        viewModel.selectedCourseId.value = lesson.courseId
                                        viewModel.selectedLessonId.value = lesson.id
                                        onNavigateToPlayer(lesson.id)
                                    }
                                )
                            }
                        }
                    }
                }
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
        border = BorderStroke(0.5.dp, Color(0xFF222222)),
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
                        .background(Color(0xFF161616), RoundedCornerShape(17.dp))
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
                        .background(Color(0xFF141414), CircleShape)
                        .border(0.5.dp, Color(0xFF333333), CircleShape)
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
                        .background(Color(0xFF141414), CircleShape)
                        .border(0.5.dp, Color(0xFF333333), CircleShape)
                        .testTag("home_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Global Search",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings button
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF141414), CircleShape)
                        .border(0.5.dp, Color(0xFF333333), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
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
            .background(Color(0xFF111111), RoundedCornerShape(12.dp))
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
                trackColor = Color(0xFF222222),
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
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
            .background(Color(0xFF121212))
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
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, Color(0xFF252525)),
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
                    color = Color.White,
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
                    trackColor = Color(0xFF292929),
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        border = BorderStroke(0.5.dp, Color(0xFF2A2A2A)),
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
                    .border(0.5.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF221F14), RoundedCornerShape(4.dp))
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
                    color = Color.White,
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
fun BookmarkChipItem(lesson: LessonEntity, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF161616),
        border = BorderStroke(0.5.dp, Color(0xFF333333)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .width(170.dp)
            .height(70.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF261D0C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = lesson.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Launch saved play",
                    fontSize = 10.sp,
                    color = SubduedGray
                )
            }
        }
    }
}

@Composable
fun EmptyCoursesPlaceholder(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(Color(0xFF111111), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF252525), RoundedCornerShape(16.dp))
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFF1B1A12), CircleShape),
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
            color = Color.White,
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
