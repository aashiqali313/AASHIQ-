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
import androidx.compose.ui.text.withStyle
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
    
    var selectedFilter by remember { mutableStateOf("All") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SearchTopBar(
                query = query,
                onQueryChange = { 
                    viewModel.searchQuery.value = it 
                    // Reset filter back to All when query empty to prevent dead states
                    if (it.isBlank()) {
                        selectedFilter = "All"
                    }
                },
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
                // Elegant horizontal in-place filter chips (All, Courses, Lectures, Notes)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("All", "Courses", "Lectures", "Notes")
                    items(filters) { filter ->
                        SearchFilterChip(
                            text = filter.uppercase(),
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter }
                        )
                    }
                }

                val matchedCourses = searchResults.matchedCourses
                val matchedLessons = searchResults.matchedLessons
                val courseIdToTitle = searchResults.courseIdToTitle

                // Apply in-memory filtering based on selected search filter
                val filteredCourses = remember(matchedCourses, selectedFilter) {
                    if (selectedFilter == "All" || selectedFilter == "Courses") {
                        matchedCourses
                    } else {
                        emptyList()
                    }
                }

                val filteredLessons = remember(matchedLessons, selectedFilter) {
                    when (selectedFilter) {
                        "All" -> matchedLessons
                        "Lectures" -> matchedLessons.filter { it.type == "video" }
                        "Notes" -> matchedLessons.filter { it.type != "video" }
                        else -> emptyList()
                    }
                }

                // Group the matched lessons by courseId for clear structured representation
                val groupedLessons = remember(filteredLessons) {
                    filteredLessons.groupBy { it.courseId }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("search_results_lazy"),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Header counts matching
                    item {
                        val totalResults = filteredCourses.size + filteredLessons.size
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text(
                                text = "FOUND $totalResults MATCHES FOR \"${query.uppercase()}\"",
                                style = MaterialTheme.typography.labelMedium,
                                color = PremiumGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    if (filteredCourses.isEmpty() && filteredLessons.isEmpty()) {
                        item {
                            EmptySearchResultFeedback(query = query)
                        }
                    } else {
                        // Section: Courses
                        if (filteredCourses.isNotEmpty()) {
                            item {
                                SectionSubHeader(title = "INDEXED MASTERCLASSES (${filteredCourses.size})")
                            }
                            items(filteredCourses, key = { it.id }) { course ->
                                SearchCourseResultCard(
                                    course = course,
                                    query = query,
                                    onSelect = {
                                        focusManager.clearFocus()
                                        viewModel.selectedCourseId.value = course.id
                                        onNavigateToCourseDetail(course.id)
                                    }
                                )
                            }
                        }

                        // Section: Lessons / Notes grouped by Course Name
                        if (groupedLessons.isNotEmpty()) {
                            item {
                                val sectionHeaderTitle = when (selectedFilter) {
                                    "Lectures" -> "LECTURES BY COURSE (${filteredLessons.size})"
                                    "Notes" -> "LEARNING NOTES BY COURSE (${filteredLessons.size})"
                                    else -> "LECTURES & NOTES BY COURSE (${filteredLessons.size})"
                                }
                                SectionSubHeader(title = sectionHeaderTitle)
                            }

                            groupedLessons.forEach { (courseId, lessons) ->
                                val courseTitle = courseIdToTitle[courseId] ?: "Independent Learning"
                                
                                // Group sub-header for this specific Course
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = PremiumGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = courseTitle.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PremiumGold,
                                                letterSpacing = 1.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                items(lessons, key = { it.id }) { lesson ->
                                    val courseTitle = courseIdToTitle[lesson.courseId]
                                    SearchLessonResultCard(
                                        lesson = lesson,
                                        courseTitle = courseTitle,
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
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
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
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            
            // Search Input Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
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
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable { onSelect(text) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = SubduedGray,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onDelete(text) }
            )
        }
    }
}

@Composable
fun SearchFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) PremiumGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            0.5.dp,
            if (selected) PremiumGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (selected) PremiumGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    modifier: Modifier = Modifier
) {
    if (query.isBlank() || !text.lowercase().contains(query.lowercase())) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
    } else {
        val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
            val lowerText = text.lowercase()
            val lowerQuery = query.trim().lowercase()
            var startIdx = 0
            while (startIdx < text.length) {
                val matchIdx = lowerText.indexOf(lowerQuery, startIdx)
                if (matchIdx == -1) {
                    append(text.substring(startIdx))
                    break
                } else {
                    if (matchIdx > startIdx) {
                        append(text.substring(startIdx, matchIdx))
                    }
                    withStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = PremiumGold,
                            fontWeight = FontWeight.ExtraBold,
                            background = PremiumGold.copy(alpha = 0.15f)
                        )
                    ) {
                        append(text.substring(matchIdx, matchIdx + lowerQuery.length))
                    }
                    startIdx = matchIdx + lowerQuery.length
                }
            }
        }
        Text(
            text = annotatedString,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
    }
}

@Composable
fun SearchCourseResultCard(course: CourseEntity, query: String, onSelect: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
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
                HighlightedText(
                    text = course.title,
                    query = query,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(text = "Course Module Collection • ${course.category}", fontSize = 10.sp, color = SubduedGray)
            }
        }
    }
}

@Composable
fun SearchLessonResultCard(
    lesson: LessonEntity,
    courseTitle: String?,
    query: String,
    onSelect: () -> Unit
) {
    val notesContainQuery = lesson.notePath?.lowercase()?.contains(query.lowercase()) == true

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(if (notesContainQuery) PremiumGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (notesContainQuery) Icons.Default.Description else Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = PremiumGold,
                            modifier = Modifier.size(11.dp)
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

                // Show a gorgeous, highly visible Course Badge at the top of the card
                if (!courseTitle.isNullOrEmpty()) {
                    Surface(
                        color = PremiumGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, PremiumGold.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = PremiumGold,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = courseTitle.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = PremiumGold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            // Highlighted Lesson Title
            HighlightedText(
                text = lesson.title,
                query = query,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Direct Course Name identification to completely eliminate user confusion
            if (!courseTitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "In Course",
                        tint = SubduedGray,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "From Course: ",
                        color = SubduedGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = courseTitle,
                        color = PremiumGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (notesContainQuery) {
                Spacer(modifier = Modifier.height(6.dp))
                val rawNote = lesson.notePath ?: ""
                val snippetIndex = rawNote.lowercase().indexOf(query.lowercase())
                val start = (snippetIndex - 20).coerceAtLeast(0)
                val end = (snippetIndex + 40).coerceAtMost(rawNote.length)
                val snippet = "..." + rawNote.substring(start, end).replace("\n", " ") + "..."

                // Highlight match in notes snippet
                HighlightedText(
                    text = snippet,
                    query = query,
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
        Icon(imageVector = Icons.Default.SavedSearch, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Index Query Engine Live",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
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
            color = MaterialTheme.colorScheme.onBackground
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
