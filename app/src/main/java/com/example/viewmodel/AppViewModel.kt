package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.domain.Bookmark
import com.example.domain.Course
import com.example.domain.PlaybackProgress
import com.example.domain.Settings
import com.example.repository.CourseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = CourseRepository(
        context = application,
        courseDao = database.courseDao(),
        progressDao = database.playbackProgressDao(),
        bookmarkDao = database.bookmarkDao(),
        settingsDao = database.settingsDao()
    )

    // UI state flows
    val allCourses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<Settings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    val continueWatching: StateFlow<List<PlaybackProgress>> = repository.getContinueWatching()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Interactive states
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active course detail state
    private val _activeCourseId = MutableStateFlow<String?>(null)
    val activeCourse: StateFlow<Course?> = _activeCourseId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getCourseDetail(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active playing lesson state
    private val _activeLessonId = MutableStateFlow<String?>(null)
    val activeLessonId: StateFlow<String?> = _activeLessonId.asStateFlow()

    // Note renderer content
    private val _activeNoteContent = MutableStateFlow("")
    val activeNoteContent: StateFlow<String> = _activeNoteContent.asStateFlow()

    private val _isNoteLoading = MutableStateFlow(false)
    val isNoteLoading: StateFlow<Boolean> = _isNoteLoading.asStateFlow()

    // Recent search storage in memory
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addRecentSearch(query: String) {
        if (query.isNotBlank()) {
            val list = _recentSearches.value.toMutableList()
            list.remove(query)
            list.add(0, query)
            _recentSearches.value = list.take(5) // Limit to 5
        }
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }

    fun selectCourse(courseId: String?) {
        _activeCourseId.value = courseId
    }

    fun selectLesson(lessonId: String?) {
        _activeLessonId.value = lessonId
    }

    fun clearImportError() {
        _importError.value = null
    }

    // Load active lesson markdown notes asynchronously
    fun loadLessonNote(courseUri: String, notePath: String?) {
        if (notePath.isNullOrEmpty()) {
            _activeNoteContent.value = ""
            return
        }
        viewModelScope.launch {
            _isNoteLoading.value = true
            val content = repository.readNoteContent(courseUri, notePath)
            _activeNoteContent.value = content
            _isNoteLoading.value = false
        }
    }

    // Actions
    fun importCourse(treeUri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            try {
                repository.importCourseFromUri(treeUri)
            } catch (e: Exception) {
                _importError.value = e.message ?: "An unknown database file verification error occurred."
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            repository.deleteCourseCascade(courseId)
            if (_activeCourseId.value == courseId) {
                _activeCourseId.value = null
            }
        }
    }

    fun saveProgress(lessonId: String, courseId: String, positionMs: Long, completed: Boolean, speed: Float) {
        viewModelScope.launch {
            repository.saveProgress(
                PlaybackProgress(
                    lessonId = lessonId,
                    courseId = courseId,
                    currentPosition = positionMs,
                    completed = completed,
                    playbackSpeed = speed,
                    lastWatched = System.currentTimeMillis()
                )
            )
        }
    }

    fun addBookmark(courseId: String, lessonId: String, lessonTitle: String, timestampMs: Long) {
        viewModelScope.launch {
            repository.addBookmark(
                Bookmark(
                    courseId = courseId,
                    lessonId = lessonId,
                    lessonTitle = lessonTitle,
                    timestamp = timestampMs
                )
            )
        }
    }

    fun removeBookmark(bookmarkId: Int) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
        }
    }

    fun updateTheme(themeStr: String) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(theme = themeStr))
        }
    }

    fun updateAutoplay(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(autoplay = enabled))
        }
    }

    fun updateDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(defaultSpeed = speed))
        }
    }

    fun updateAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(animationsEnabled = enabled))
        }
    }

    fun getProgressForCourse(courseId: String): Flow<Map<String, PlaybackProgress>> {
        return repository.getProgressForCourse(courseId)
    }

    fun getProgressForLesson(lessonId: String): Flow<PlaybackProgress?> {
        return repository.getProgressForLesson(lessonId)
    }
}
