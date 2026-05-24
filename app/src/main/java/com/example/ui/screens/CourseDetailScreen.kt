package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.domain.Course
import com.example.domain.Lesson
import com.example.domain.PlaybackProgress
import com.example.viewmodel.AppViewModel
import com.example.ui.components.AashiqLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String, String) -> Unit
) {
    val activeCourse by viewModel.activeCourse.collectAsState()
    val progressMap by viewModel.getProgressForCourse(courseId).collectAsState(initial = emptyMap())

    // Trigger loading active course detail
    LaunchedEffect(courseId) {
        viewModel.selectCourse(courseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Details", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("detail_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val course = activeCourse
            if (course == null) {
                // Skeleton loading state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val totalLessons = course.modules.flatMap { it.lessons }
                val totalLessonsCount = totalLessons.size
                val completedLessonsCount = totalLessons.count { progressMap[it.id]?.completed == true }
                
                val progressPercent = if (totalLessonsCount > 0) {
                    (completedLessonsCount.toFloat() / totalLessonsCount.toFloat())
                } else 0f

                // Find resume lesson
                val resumeLessonId = remember(course, progressMap) {
                    val lastWatched = progressMap.values.maxByOrNull { it.lastWatched }
                    if (lastWatched != null && totalLessons.any { it.id == lastWatched.lessonId }) {
                        lastWatched.lessonId
                    } else {
                        // Default to first lesson of first module
                        totalLessons.firstOrNull()?.id
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // 1. Large Header Banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color(0xFF131316))
                        ) {
                            if (course.thumbnail != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = course.thumbnail),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AashiqLogo(modifier = Modifier.size(140.dp))
                                }
                            }
                            
                            // Bottom absolute overlay gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color(0xBF000000), Color(0xFF0F0F11)),
                                            startY = 100f
                                        )
                                    )
                            )
                        }
                    }

                    // 2. Title and Dynamic stats
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                course.author.uppercase(),
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                course.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Stats Grid Card with Progress Ring
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(14.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Progress Ring
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { 1f },
                                            color = Color(0x20FFFFFF),
                                            strokeWidth = 6.dp,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        CircularProgressIndicator(
                                            progress = { progressPercent },
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 6.dp,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Text(
                                            text = "${(progressPercent * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(18.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Progress status",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFAEAEB2)
                                        )
                                        Text(
                                            "$completedLessonsCount of $totalLessonsCount lessons finished",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            "Offline cached media ready",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp
                                        )
                                    }

                                    if (resumeLessonId != null) {
                                        IconButton(
                                            onClick = { onNavigateToPlayer(courseId, resumeLessonId) },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .size(44.dp)
                                                .testTag("resume_play_btn")
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Resume",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Description",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                course.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // 3. Lesson Accordion header
                    item {
                        Text(
                            "Course Modules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // 4. Modules / Lessons layout
                    if (course.modules.isEmpty()) {
                        item {
                            Text(
                                "No modules specified for this learning series.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(course.modules) { module ->
                            var isExpanded by remember { mutableStateOf(true) }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                // Accordion Header Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { isExpanded = !isExpanded }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                  ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            module.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Expandable Content Block
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        module.lessons.forEachIndexed { index, lesson ->
                                            val isCompleted = progressMap[lesson.id]?.completed == true
                                            val position = progressMap[lesson.id]?.currentPosition ?: 0L
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onNavigateToPlayer(courseId, lesson.id) }
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Completed tick or Play marker
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            color = if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isCompleted) {
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            contentDescription = "Finished",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = (index + 1).toString(),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFAEAEB2)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        lesson.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isCompleted) Color.White.copy(alpha = 0.7f) else Color.White
                                                    )
                                                    Text(
                                                        "Length: ${formatDuration(lesson.duration.toLong())}" +
                                                        if (position > 0 && !isCompleted) " • resume at ${formatDuration(position / 1000)}" else "",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFAEAEB2)
                                                    )
                                                }

                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Start Lesson",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            
                                            if (index < module.lessons.lastIndex) {
                                                HorizontalDivider(
                                                    color = Color(0x10FFFFFF),
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
