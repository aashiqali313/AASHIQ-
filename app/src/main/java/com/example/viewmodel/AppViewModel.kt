package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.database.*
import com.example.repository.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = CourseRepository(db)

    // User configurations
    val settingsState: StateFlow<UserSettingsEntity> = repository.userSettings
        .map { it ?: UserSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettingsEntity())

    // All available courses
    val coursesState: StateFlow<List<CourseEntity>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Books, settings, continue watching
    val bookmarkedLessons: StateFlow<List<LessonEntity>> = repository.bookmarkedLessons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching: StateFlow<List<LessonEntity>> = repository.continueWatching
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<RecentSearchEntity>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active state identifiers
    val selectedCourseId = MutableStateFlow<String?>(null)
    val selectedLessonId = MutableStateFlow<String?>(null)
    
    // Active course entity
    val activeCourse: StateFlow<CourseEntity?> = selectedCourseId
        .flatMapLatest { id ->
            if (id == null) flowOf<CourseEntity?>(null)
            else flow { emit(repository.getCourseById(id)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Modules under active course
    val activeModules: StateFlow<List<ModuleEntity>> = selectedCourseId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getModulesForCourse(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Lessons under active course
    val activeLessons: StateFlow<List<LessonEntity>> = selectedCourseId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getLessonsForCourse(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active playing lesson
    val activeLesson: StateFlow<LessonEntity?> = selectedLessonId
        .flatMapLatest { id ->
            if (id == null) flowOf<LessonEntity?>(null)
            else flow { emit(repository.getLessonById(id)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Search Engine
    val searchQuery = MutableStateFlow("")
    
    // Realtime compiled search result state covering courses, modules, lessons, notes, tags
    val searchResults: StateFlow<SearchResultsData> = searchQuery
        .debounce(150)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchResultsData())
            } else {
                combine(
                    coursesState,
                    rowLessonsFlow() // helper flow of all lessons in database
                ) { courses, allLessons ->
                    filterSearchResults(query, courses, allLessons)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResultsData())

    // Import states
    val isImporting = MutableStateFlow(false)
    val importProgress = MutableStateFlow(0.0f)
    val importStatus = MutableStateFlow("")
    val importSuccess = MutableStateFlow(false)

    // Shared ExoPlayer instance for performance-optimized instant video loading
    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() {
            if (_exoPlayer == null) {
                _exoPlayer = ExoPlayer.Builder(getApplication())
                    .setHandleAudioBecomingNoisy(true)
                    .build().apply {
                        repeatMode = Player.REPEAT_MODE_OFF
                        playWhenReady = true
                    }
            }
            return _exoPlayer
        }

    init {
        viewModelScope.launch {
            // High-reliability pre-loading
            repository.prepopulateIfEmpty()
        }
    }

    // Load video into active player
    fun playLessonVideo(lesson: LessonEntity) {
        val player = exoPlayer ?: return
        viewModelScope.launch(Dispatchers.Main) {
            val mediaItem = MediaItem.fromUri(lesson.videoUri)
            player.setMediaItem(mediaItem)
            
            // Auto resume / Continue Watching check
            if (lesson.progressMs > 0 && lesson.playProgressPercent < 95) {
                player.seekTo(lesson.progressMs)
            } else {
                player.seekTo(0)
            }
            player.prepare()
            player.play()
        }
    }

    // Save video progress in Room database
    fun savePlayingProgress(lessonId: String, currentPosMs: Long, totalDurationMs: Long) {
        if (totalDurationMs <= 0) return
        val percent = ((currentPosMs * 100) / totalDurationMs).coerceIn(0, 100).toInt()
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLessonProgress(lessonId, currentPosMs, percent)
        }
    }

    // Folder local import flow
    fun importLocalFolder(treeUri: Uri) {
        viewModelScope.launch {
            isImporting.value = true
            importSuccess.value = false
            importProgress.value = 0.0f
            importStatus.value = "Initializing secure connection..."

            val success = repository.importCourseFolder(
                getApplication(),
                treeUri
            ) { status, progress ->
                importStatus.value = status
                importProgress.value = progress
            }

            isImporting.value = false
            importSuccess.value = success
            if (!success) {
                importStatus.value = "Import failed. Verify video content or folder permissions."
            } else {
                importStatus.value = "Import Complete!"
            }
        }
    }

    fun toggleBookmark(lessonId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(lessonId)
        }
    }

    fun searchTriggered(query: String) {
        searchQuery.value = query
        viewModelScope.launch {
            repository.addRecentSearch(query)
        }
    }

    fun deleteSearch(query: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearAllSearches()
        }
    }

    fun updateSettings(settings: UserSettingsEntity) {
        viewModelScope.launch {
            repository.saveUserSettings(settings)
        }
    }

    // Helper: Flow emitting all lessons in the DB for lightning-fast search
    private fun rowLessonsFlow(): Flow<List<LessonEntity>> {
        return coursesState.flatMapLatest { courses ->
            if (courses.isEmpty()) {
                flowOf(emptyList())
            } else {
                val listFlows = courses.map { repository.getLessonsForCourse(it.id) }
                combine(listFlows) { arrays ->
                    arrays.flatMap { it.toList() }
                }
            }
        }
    }

    // Index-match filtering search
    private fun filterSearchResults(
        query: String,
        courses: List<CourseEntity>,
        lessons: List<LessonEntity>
    ): SearchResultsData {
        val q = query.trim().lowercase()
        
        val matchedCourses = courses.filter {
            it.title.lowercase().contains(q) || 
            it.description.lowercase().contains(q) ||
            it.category.lowercase().contains(q)
        }

        val matchedLessons = lessons.filter {
            it.title.lowercase().contains(q) || 
            (it.notePath != null && it.notePath.lowercase().contains(q))
        }

        return SearchResultsData(
            matchedCourses = matchedCourses,
            matchedLessons = matchedLessons,
            query = query
        )
    }

    // Public database triggers to avoid layout thread leaks
    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
        }
    }

    fun deleteAllCourses() {
        viewModelScope.launch {
            repository.deleteAllCourses()
        }
    }

    fun saveTimestampNoteForLesson(lessonId: String, note: String) {
        viewModelScope.launch {
            val current = repository.getLessonById(lessonId)
            if (current != null) {
                repository.insertLessons(listOf(current.copy(notePath = note)))
                // Trigger flow refresh
                selectedLessonId.value = lessonId
            }
        }
    }

    override fun onCleared() {
        // Releasing ExoPlayer safely on ViewModel clear to prevent heavy memory leaks
        _exoPlayer?.let { player ->
            player.stop()
            player.release()
        }
        _exoPlayer = null
        super.onCleared()
    }
}

// Immutable state-safe holder for results query
data class SearchResultsData(
    val matchedCourses: List<CourseEntity> = emptyList(),
    val matchedLessons: List<LessonEntity> = emptyList(),
    val query: String = ""
)
