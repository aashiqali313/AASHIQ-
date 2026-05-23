package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.Course
import com.example.domain.Lesson
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: AppViewModel,
    onNavigateToCourse: (String) -> Unit,
    onNavigateToPlayer: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val courses by viewModel.allCourses.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    // Filter matching results: Courses & Lessons
    val matchedCourses = remember(searchQuery, courses) {
        if (searchQuery.isBlank()) emptyList()
        else courses.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val matchedLessons = remember(searchQuery, courses) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val results = mutableListOf<Pair<Course, Lesson>>()
            for (course in courses) {
                for (module in course.modules) {
                    for (lesson in module.lessons) {
                        if (lesson.title.contains(searchQuery, ignoreCase = true) ||
                            (lesson.note?.contains(searchQuery, ignoreCase = true) == true)
                        ) {
                            results.add(course to lesson)
                        }
                    }
                }
            }
            results
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search courses, lessons, notes...", color = Color(0x80FFFFFF), fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_input"),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear text", tint = Color.LightGray)
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("search_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // A. Recent Searches Subtitle Bar
            if (recentSearches.isNotEmpty() && searchQuery.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent searches",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "CLEAR ALL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252),
                            modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentSearches) { term ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0x10FFFFFF), RoundedCornerShape(100))
                                    .clickable {
                                        viewModel.setSearchQuery(term)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(term, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // B. Filter Panel Options / Results Lists
            if (searchQuery.isEmpty()) {
                // Show informative empty guidance panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0x30FFFFFF),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "INSTANT SEARCH ACTIVE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            "Type keywords to crawl through courses, lesson lectures & notes fully offline",
                            fontSize = 11.sp,
                            color = Color(0xFFAEAEB2),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp)
                        )
                    }
                }
            } else if (matchedCourses.isEmpty() && matchedLessons.isEmpty()) {
                // High fidelity search result fallback empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0x40FF1744),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "NO SEARCH RESULTS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "We couldn't locate any matches for '$searchQuery'. Please check spelling or verify course imports.",
                            fontSize = 11.sp,
                            color = Color(0xFFAEAEB2),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                // Store query search dynamically on result match
                LaunchedEffect(searchQuery) {
                    viewModel.addRecentSearch(searchQuery)
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (matchedCourses.isNotEmpty()) {
                        item {
                            Text(
                                "Matched Courses",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                                letterSpacing = 1.sp
                            )
                        }

                        items(matchedCourses) { course ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToCourse(course.id) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(course.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Text(course.author, style = MaterialTheme.typography.bodySmall, color = Color(0xFFAEAEB2))
                                }
                            }
                            HorizontalDivider(color = Color(0x0AFFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    if (matchedLessons.isNotEmpty()) {
                        item {
                            Text(
                                "Matched Lesson Playlists & Notes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                                letterSpacing = 1.sp
                            )
                        }

                        items(matchedLessons) { (course, lesson) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onNavigateToPlayer(course.id, lesson.id)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lesson.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Text(
                                        "Inside course: ${course.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFAEAEB2),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.DarkGray
                                )
                            }
                            HorizontalDivider(color = Color(0x0AFFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}
