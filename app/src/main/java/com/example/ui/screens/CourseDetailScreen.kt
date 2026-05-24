package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.CourseEntity
import com.example.data.database.LessonEntity
import com.example.data.database.ModuleEntity
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

@Composable
fun CourseDetailScreen(
    viewModel: AppViewModel,
    courseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val course by viewModel.activeCourse.collectAsState()
    val modules by viewModel.activeModules.collectAsState()
    val lessons by viewModel.activeLessons.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(courseId) {
        viewModel.selectedCourseId.value = courseId
    }

    val activeCourse = course ?: return

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Unindex Course Library?", color = Color.White) },
            text = { Text("This will erase custom bookmarks, watch progress, and indexed modules from local database space.", color = SubduedGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.selectedCourseId.value = null
                        onNavigateBack()
                        viewModel.deleteCourse(courseId)
                    }
                ) {
                    Text("UNINDEX", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("KEEP", color = Color.White)
                }
            },
            containerColor = Color(0xFF141414),
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("course_detail_lazycolumn"),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Course Header Cover Section
                item {
                    CourseDetailedHeader(
                        course = activeCourse,
                        lessonsCount = lessons.size,
                        onBack = onNavigateBack,
                        onDeleteClick = { showDeleteDialog = true }
                    )
                }

                // Modules and lessons items
                if (modules.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PremiumGold, strokeWidth = 2.dp)
                        }
                    }
                } else {
                    itemsIndexed(modules, key = { _, item -> item.id }) { index, module ->
                        ModuleExpansionHeader(
                            module = module,
                            moduleIndex = index + 1
                        )

                        // Filter lessons matching this specific module
                        val moduleLessons = lessons.filter { it.moduleId == module.id }
                        if (moduleLessons.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "No indexed videos in module folder.",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SubduedGray
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                moduleLessons.forEach { lesson ->
                                    LessonRowItem(
                                        lesson = lesson,
                                        onLessonClick = {
                                            viewModel.selectedLessonId.value = lesson.id
                                            onNavigateToPlayer(lesson.id)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CourseDetailedHeader(
    course: CourseEntity,
    lessonsCount: Int,
    onBack: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        // Thumbnail back cover
        AsyncImage(
            model = course.thumbnailUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark Radial Matte shadow to establish cinematic layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 1.0f)
                        )
                    )
                )
        )

        // Navigation Back Button & Delete Action Row Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .border(0.5.dp, Color(0xFF333333), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .border(0.5.dp, Color(0xFF333333), CircleShape)
                    .testTag("delete_course_metadata")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Unindex Course",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Details Description block
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF2E240D), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = course.category,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumGold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = course.title,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = course.description,
                fontSize = 11.sp,
                color = SubduedGray,
                lineHeight = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = PremiumGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$lessonsCount Lessons",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SoftGoldGlow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = course.author,
                        fontSize = 11.sp,
                        color = SubduedGray
                    )
                }
            }
        }
    }
}

@Composable
fun ModuleExpansionHeader(module: ModuleEntity, moduleIndex: Int) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "M O D U L E  ${moduleIndex.toString().padStart(2, '0')}".uppercase(),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = PremiumGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = module.title,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Divider(
                color = Color(0xFF222222),
                thickness = 0.5.dp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun LessonRowItem(lesson: LessonEntity, onLessonClick: () -> Unit) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, Color(0xFF252525)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLessonClick() }
            .testTag("lesson_item_row_${lesson.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number or Check mark if played/completed
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (lesson.playProgressPercent >= 90) Color(0xFF1E2818) else Color(0xFF181818),
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (lesson.playProgressPercent >= 90) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text = lesson.orderIndex.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumGold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = lesson.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "READY FOR OFFLINE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Bold
                    )

                    if (lesson.playProgressPercent > 0) {
                        Text(
                            text = "${lesson.playProgressPercent}% Watched",
                            fontSize = 9.sp,
                            color = SoftGoldGlow
                        )
                    }

                    if (lesson.isBookmarked) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Saved",
                            tint = PremiumGold,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            // Small play action icon
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFF1E1E1E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(start = 1.dp)
                )
            }
        }
    }
}
