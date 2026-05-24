package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import com.example.data.database.CourseEntity
import com.example.data.database.LessonEntity

@Composable
fun SearchScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCourseDetail: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SearchTopBar(
                query = query,
                onQueryChange = { viewModel.searchQuery.value = it },
                onSearchSubmit = { viewModel.searchTriggered(it) },
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Recent Searches Row
            if (recentSearches.isNotEmpty() && query.isBlank()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT SEARCH HISTORY",
                            style = MaterialTheme.typography.labelMedium,
                            color = PremiumGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = { viewModel.clearSearchHistory() }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear History", tint = Color.Red, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentSearches, key = { it.query }) { item ->
                            RecentChipItem(
                                text = item.query,
                                onSelect = { viewModel.searchTriggered(it) },
                                onDelete = { viewModel.deleteSearch(it) }
                            )
                        }
                    }
                }
            }

            // Results Listing
            if (query.isBlank()) {
                // Search onboarding tutorial instructions
                SearchOnboardingVisual()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("search_results_lazy"),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Header counts matching
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = "MATCHES FOUND FOR \"${query.uppercase()}\"",
                                style = MaterialTheme.typography.labelMedium,
                                color = PremiumGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    val matchedCourses = searchResults.matchedCourses
                    val matchedLessons = searchResults.matchedLessons

                    if (matchedCourses.isEmpty() && matchedLessons.isEmpty()) {
                        item {
                            EmptySearchResultFeedback(query = query)
                        }
                    } else {
                        // Section: Courses
                        if (matchedCourses.isNotEmpty()) {
                            item {
                                SectionSubHeader(title = "INDEXED MASTERCLASSES (${matchedCourses.size})")
                            }
                            items(matchedCourses, key = { it.id }) { course ->
                                SearchCourseResultCard(
                                    course = course,
                                    onSelect = {
                                        focusManager.clearFocus()
                                        viewModel.selectedCourseId.value = course.id
                                        onNavigateToCourseDetail(course.id)
                                    }
                                )
                            }
                        }

                        // Section: Lessons / Notes / Modules
                        if (matchedLessons.isNotEmpty()) {
                            item {
                                SectionSubHeader(title = "LECTURES & NOTES FOUND (${matchedLessons.size})")
                            }
                            items(matchedLessons, key = { it.id }) { lesson ->
                                SearchLessonResultCard(
                                    lesson = lesson,
                                    query = query,
                                    onSelect = {
                                        focusManager.clearFocus()
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
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onBack: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(0.5.dp, Color(0xFF222222)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            // Search Input Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(Color(0xFF141414), RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PremiumGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search course, modules, lessons, notes...", fontSize = 12.sp, color = SubduedGray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit(query) }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_text_input")
                    )

                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionSubHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = SubduedGray,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
fun RecentChipItem(text: String, onSelect: (String) -> Unit, onDelete: (String) -> Unit) {
    Surface(
        color = Color(0xFF121212),
        border = BorderStroke(0.5.dp, Color(0xFF333333)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable { onSelect(text) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, color = Color.White, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onDelete(text) }
            )
        }
    }
}

@Composable
fun SearchCourseResultCard(course: CourseEntity, onSelect: () -> Unit) {
    Surface(
        color = Color(0xFF121212),
        border = BorderStroke(0.5.dp, Color(0xFF222222)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null, tint = PremiumGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = course.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "Course Module Collection • ${course.category}", fontSize = 10.sp, color = SubduedGray)
            }
        }
    }
}

@Composable
fun SearchLessonResultCard(lesson: LessonEntity, query: String, onSelect: () -> Unit) {
    val notesContainQuery = lesson.notePath?.lowercase()?.contains(query.lowercase()) == true

    Surface(
        color = Color(0xFF121212),
        border = BorderStroke(0.5.dp, Color(0xFF222222)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(if (notesContainQuery) Color(0xFF2E240D) else Color(0xFF1F1F1F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (notesContainQuery) Icons.Default.Description else Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = PremiumGold,
                        modifier = Modifier.size(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (notesContainQuery) "NOTE MATCH" else "LECTURE VIDEO",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PremiumGold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = lesson.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            if (notesContainQuery) {
                Spacer(modifier = Modifier.height(4.dp))
                val rawNote = lesson.notePath ?: ""
                val snippetIndex = rawNote.lowercase().indexOf(query.lowercase())
                val start = (snippetIndex - 20).coerceAtLeast(0)
                val end = (snippetIndex + 40).coerceAtMost(rawNote.length)
                val snippet = "..." + rawNote.substring(start, end).replace("\n", " ") + "..."

                Text(
                    text = snippet,
                    fontSize = 11.sp,
                    color = SubduedGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SearchOnboardingVisual() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.SavedSearch, contentDescription = null, tint = Color(0xFF222222), modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Index Query Engine Live",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Initiate typing to instantly isolate high-precision video timestamps, custom lecture code blocks, and exported PDF matches natively.",
            fontSize = 11.sp,
            color = SubduedGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun EmptySearchResultFeedback(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.SearchOff, contentDescription = null, tint = PremiumGold, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Zero Matches Isolated",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "We scanned internal database records but couldn't isolate matching items for \"$query\". Verify spelling or import supplementary folder libraries.",
            color = SubduedGray,
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}
