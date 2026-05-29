package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.MimeTypes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.database.*
import com.example.repository.CourseRepository
import com.example.utils.CertificateGenerator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.UUID
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

    val newlyUnlockedCertificate = MutableStateFlow<CertificateEntity?>(null)

    private fun calculateLessonsStreak(lessons: List<LessonEntity>): Int {
        val timestamps = lessons.map { it.lastWatchedTimestamp }.filter { it > 0 }
        if (timestamps.isEmpty()) return 0

        val timeZone = java.util.TimeZone.getDefault()
        val epochDays = timestamps.map { 
            (it + timeZone.getOffset(it)) / (24 * 60 * 60 * 1000L)
        }.distinct().sortedDescending()

        if (epochDays.isEmpty()) return 0

        val todayMs = System.currentTimeMillis()
        val todayEpochDay = (todayMs + timeZone.getOffset(todayMs)) / (24 * 60 * 60 * 1000L)

        val latestDay = epochDays.first()

        // If the last activity is older than yesterday, the streak is 0.
        if (todayEpochDay - latestDay > 1L) {
            return 0
        }

        var currentStreak = 1
        var expectedDay = latestDay
        for (i in 1 until epochDays.size) {
            if (epochDays[i] == expectedDay - 1) {
                currentStreak++
                expectedDay = epochDays[i]
            } else if (epochDays[i] < expectedDay - 1) {
                break
            }
        }
        return currentStreak
    }

    // User profile and certificates states - fully automated and reactive!
    val profileState: StateFlow<UserProfileEntity> = combine(
        repository.userProfile.map { it ?: UserProfileEntity() },
        repository.allCourses,
        repository.allLessonsFlow,
        repository.allHabitLogs
    ) { profile, courses, lessons, habitLogs ->
        try {
            // Total watch minutes from video-type lessons
            val watchMs = lessons.filter { it.type.lowercase() == "video" }.sumOf { it.progressMs }
            val watchMins = watchMs / 60000L

            // Reading time estimation: e.g., 5 mins per article, 8 mins per PDF lesson, 3 mins per gallery that is completed
            val completedArticles = lessons.filter { it.type.lowercase() == "article" && it.isCompleted }.size
            val completedPdfs = lessons.filter { it.type.lowercase() == "pdf" && it.isCompleted }.size
            val completedGalleries = lessons.filter { it.type.lowercase() == "gallery" && it.isCompleted }.size
            
            val calcReadingMins = (completedArticles * 5L) + (completedPdfs * 8L) + (completedGalleries * 3L)

            // Course completion logic checking all formats (video, article, pdf, gallery)
            // A course is complete if all of its lessons are completed (isCompleted = true)
            val completedCourses = courses.filter { course ->
                val courseLessons = lessons.filter { it.courseId == course.id }
                courseLessons.isNotEmpty() && courseLessons.all { it.isCompleted }
            }.size

            // Streak check
            val streak = calculateLessonsStreak(lessons)

            // XP sum from lessons
            // Video complete: 20
            // Article complete: 10
            // PDF complete: 15
            // Gallery complete: 10
            // Quiz complete: 25
            // Streak bonus: streak days * 10 XP
            val habitXP = habitLogs.sumOf { it.earnedXP.toLong() }
            val baseXP = lessons.sumOf { it.earnedXP.toLong() }
            val streakXP = streak * 10L
            val totalXP = baseXP + streakXP + habitXP

            val computedLevel = when {
                totalXP < 100 -> "Beginner"
                totalXP < 250 -> "Explorer"
                totalXP < 500 -> "Disciplined"
                totalXP < 1000 -> "Elite"
                totalXP < 2000 -> "Master"
                else -> "Ascended"
            }

            profile.copy(
                totalWatchTimeMinutes = watchMins,
                readingTimeMinutes = calcReadingMins,
                totalXP = totalXP,
                level = computedLevel,
                completedCoursesCount = completedCourses,
                currentStreak = streak
            )
        } catch (e: Exception) {
            e.printStackTrace()
            profile
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileEntity())

    val certificatesState: StateFlow<List<CertificateEntity>> = repository.allCertificates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available courses
    val coursesState: StateFlow<List<CourseEntity>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLessonsState: StateFlow<List<LessonEntity>> = repository.allLessonsFlow
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

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun initExoPlayer(): ExoPlayer? {
        if (_exoPlayer != null) return _exoPlayer
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return null
        }
        return try {
            val context = getApplication<Application>()
            val defaultDataSourceFactory = DefaultDataSource.Factory(context)
            val customDataSourceFactory = androidx.media3.datasource.DataSource.Factory {
                val upstream = defaultDataSourceFactory.createDataSource()
                AashiqDecryptingDataSource(context, upstream)
            }
            val mediaSourceFactory = DefaultMediaSourceFactory(customDataSourceFactory)

            _exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLooper(Looper.getMainLooper())
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = true
                }
            _exoPlayer
        } catch (e: Exception) {
            e.printStackTrace()
            _exoPlayer = null
            null
        }
    }

    val exoPlayer: ExoPlayer?
        @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
        get() {
            if (_exoPlayer == null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    Handler(Looper.getMainLooper()).post {
                        initExoPlayer()
                    }
                } else {
                    initExoPlayer()
                }
            }
            return _exoPlayer
        }

    init {
        // Initialize ExoPlayer safely on main thread
        viewModelScope.launch(Dispatchers.Main) {
            initExoPlayer()
        }
        viewModelScope.launch {
            // High-reliability pre-loading
            repository.prepopulateIfEmpty()
            prepopulateHabitsIfEmpty()
        }
    }

    // Load video into active player
    fun playLessonVideo(lesson: LessonEntity) {
        val player = exoPlayer ?: return
        viewModelScope.launch(Dispatchers.Main) {
            val videoUri = lesson.videoUri
            
            // App-exclusive playback restriction: Local files must be .aashiq
            val isLocal = videoUri.startsWith("content://") || videoUri.startsWith("file://")
            if (isLocal && !videoUri.endsWith(".aashiq", ignoreCase = true)) {
                player.stop()
                android.widget.Toast.makeText(
                    getApplication(),
                    "ACCESS DENIED:\nTraditional unencrypted formats (MP4, MKV) are strictly refused. Only authenticated .aashiq course video volumes are allowed.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val mediaItemBuilder = MediaItem.Builder().setUri(videoUri)
            if (videoUri.endsWith(".aashiq", ignoreCase = true)) {
                // Force progressive MP4 media source config for custom file extension
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
            }
            
            if (!lesson.subtitleUri.isNullOrEmpty()) {
                try {
                    val subUri = Uri.parse(lesson.subtitleUri)
                    val mimeType = if (lesson.subtitleUri.endsWith(".vtt", ignoreCase = true)) {
                        MimeTypes.TEXT_VTT
                    } else {
                        MimeTypes.APPLICATION_SUBRIP
                    }
                    val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                        .setMimeType(mimeType)
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            val mediaItem = mediaItemBuilder.build()
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
            
            // Call our unified completion/progress engine
            updateLessonProgressState(lessonId, percent, currentPosMs)
        }
    }

    fun updateLessonProgressState(lessonId: String, progressVal: Int, currentPosMs: Long = 0L, isComp: Boolean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val lesson = repository.getLessonById(lessonId) ?: return@launch
            var updatedLesson = lesson
            val now = System.currentTimeMillis()
            
            val wasCompleted = lesson.isCompleted
            val type = lesson.type.lowercase()

            when (type) {
                "video" -> {
                    updatedLesson = lesson.copy(
                        playProgressPercent = progressVal,
                        progressMs = if (currentPosMs > 0L) currentPosMs else lesson.progressMs,
                        lastWatchedTimestamp = now
                    )
                    if (progressVal >= 90 && !wasCompleted) {
                        updatedLesson = updatedLesson.copy(
                            isCompleted = true,
                            earnedXP = 20
                        )
                    }
                }
                "article" -> {
                    updatedLesson = lesson.copy(
                        articleProgress = progressVal,
                        lastWatchedTimestamp = now
                    )
                    if (progressVal >= 85 && !wasCompleted) {
                        updatedLesson = updatedLesson.copy(
                            isCompleted = true,
                            earnedXP = 10
                        )
                    }
                }
                "pdf" -> {
                    updatedLesson = lesson.copy(
                        pdfProgress = progressVal,
                        lastWatchedTimestamp = now
                    )
                    if (progressVal >= 80 && !wasCompleted) {
                        updatedLesson = updatedLesson.copy(
                            isCompleted = true,
                            earnedXP = 15
                        )
                    }
                }
                "gallery" -> {
                    updatedLesson = lesson.copy(
                        imageProgress = progressVal,
                        lastWatchedTimestamp = now
                    )
                    if (progressVal >= 100 && !wasCompleted) {
                        updatedLesson = updatedLesson.copy(
                            isCompleted = true,
                            earnedXP = 10
                        )
                    }
                }
                "quiz" -> {
                    updatedLesson = lesson.copy(
                        quizPassed = progressVal >= 80,
                        lastWatchedTimestamp = now
                    )
                    if (progressVal >= 80 && !wasCompleted) {
                        updatedLesson = updatedLesson.copy(
                            isCompleted = true,
                            earnedXP = 25
                        )
                    }
                }
                else -> {
                    updatedLesson = lesson.copy(
                        lastWatchedTimestamp = now
                    )
                }
            }

            if (isComp == true && !updatedLesson.isCompleted) {
                val xpReward = when (type) {
                    "video" -> 20
                    "article" -> 10
                    "pdf" -> 15
                    "gallery" -> 10
                    "quiz" -> 25
                    else -> 10
                }
                updatedLesson = updatedLesson.copy(
                    isCompleted = true,
                    earnedXP = xpReward
                )
            }

            repository.updateLesson(updatedLesson)
            
            // Trigger home screen widgets updates
            try {
                com.example.utils.WidgetUpdater.updateAllWidgets(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Checks automatic course certification!
            checkAndGenerateCertificateForCourseOfLesson(lessonId)
        }
    }

    private fun loadUriAsBitmap(uriStr: String): Bitmap? {
        if (uriStr.isBlank()) return null
        return try {
            val context = getApplication<Application>()
            val uri = Uri.parse(uriStr)
            
            // First decode with inJustDecodeBounds = true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            
            // Calculate appropriate inSampleSize for downscaling (max 512px)
            var inSampleSize = 1
            val maxDim = maxOf(options.outWidth, options.outHeight)
            if (maxDim > 512) {
                inSampleSize = maxDim / 512
            }
            
            // Second decode with actual inSampleSize
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun checkAndGenerateCertificateForCourseOfLesson(lessonId: String) {
        val lesson = repository.getLessonById(lessonId) ?: return
        val courseId = lesson.courseId
        val course = repository.getCourseById(courseId) ?: return

        // 1. Check if a certificate already exists
        val existing = repository.getCertificateByCourse(courseId)
        if (existing != null) return // Already generated!

        // 2. Fetch all lessons and calculate completion percent
        val lessons = repository.getLessonsForCourseDirect(courseId)
        if (lessons.isEmpty()) return

        val completedCount = lessons.count { it.isCompleted }
        val completionPercent = (completedCount * 100) / lessons.size

        // Only generate automatically when 100% course completed
        if (completionPercent >= 100) {
            val profile = repository.getUserProfileDirect()
            val certId = "CERT-" + UUID.randomUUID().toString().take(6).uppercase()
            val cleanSignature = "AASHIQ_CERT_${UUID.randomUUID().toString().take(8).uppercase()}"

            var certificate = CertificateEntity(
                certificateId = certId,
                userName = profile.name,
                profileImage = profile.avatarUri,
                courseId = courseId,
                courseName = course.title,
                completionDate = System.currentTimeMillis(),
                completionPercentage = 100,
                hashSignature = cleanSignature
            )

            // Render PNG and PDF
            val profileBitmap = loadUriAsBitmap(profile.avatarUri)
            try {
                val files = CertificateGenerator.generateCertificateFiles(getApplication(), certificate, profileBitmap)
                certificate = certificate.copy(
                    imagePath = files.first,
                    pdfPath = files.second
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            repository.insertCertificate(certificate)
            
            // Set newly unlocked certificate to trigger premium UI/popup!
            newlyUnlockedCertificate.value = certificate
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

    fun updateProfile(name: String, age: Int, gender: String, avatarUri: String) {
        viewModelScope.launch {
            val current = repository.getUserProfileDirect()
            val updated = current.copy(
                name = name,
                age = age,
                gender = gender,
                avatarUri = avatarUri,
                lastActiveTimestamp = System.currentTimeMillis()
            )
            repository.saveUserProfile(updated)
        }
    }

    fun claimCertificate(courseId: String, courseName: String) {
        viewModelScope.launch {
            val existing = repository.getCertificateByCourse(courseId)
            if (existing != null) return@launch
            
            val profile = repository.getUserProfileDirect()
            val cleanSignature = "AASHIQ_CERT_${UUID.randomUUID().toString().take(8).uppercase()}"
            val certificate = CertificateEntity(
                certificateId = "CERT-" + UUID.randomUUID().toString().take(6).uppercase(),
                userName = profile.name,
                courseId = courseId,
                courseName = courseName,
                completionDate = System.currentTimeMillis(),
                hashSignature = cleanSignature
            )
            repository.insertCertificate(certificate)
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

        val courseIdToTitle = courses.associate { it.id to it.title }

        return SearchResultsData(
            matchedCourses = matchedCourses,
            matchedLessons = matchedLessons,
            courseIdToTitle = courseIdToTitle,
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

    // --- Habit & Discipline Tracker Settings ---
    val selectedDate = MutableStateFlow(getTodayDateString())

    val habitsWithLogsForSelectedDate: StateFlow<List<HabitWithLog>> = combine(
        repository.allHabits,
        repository.allHabitLogs,
        selectedDate
    ) { habits, logs, date ->
        val dateLogs = logs.filter { it.date == date }
        habits.map { habit ->
            HabitWithLog(
                habit = habit,
                log = dateLogs.find { it.habitId == habit.id }
            )
        }.sortedBy { it.habit.displayOrder }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    fun setTodayDate() {
        selectedDate.value = getTodayDateString()
    }

    fun setSelectedDate(dateStr: String) {
        selectedDate.value = dateStr
    }

    fun createHabit(
        title: String,
        description: String,
        category: String,
        type: String,
        icon: String,
        color: Long,
        repeatSchedule: String,
        reminderTime: String?,
        dailyTargetValue: Float,
        targetUnit: String,
        xpReward: Int
    ) {
        viewModelScope.launch {
            val allCurrent = repository.allHabits.first()
            val maxOrder = allCurrent.maxOfOrNull { it.displayOrder } ?: 0
            val habit = HabitEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                category = category,
                type = type,
                icon = icon,
                color = color,
                repeatSchedule = repeatSchedule,
                reminderTime = reminderTime,
                dailyTargetValue = dailyTargetValue,
                targetUnit = targetUnit,
                xpReward = xpReward,
                displayOrder = maxOrder + 1
            )
            repository.insertHabit(habit)
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }

    fun deleteHabit(id: String) {
        viewModelScope.launch {
            repository.deleteHabitById(id)
        }
    }

    fun reorderHabits(reorderedList: List<HabitEntity>) {
        viewModelScope.launch {
            val updated = reorderedList.mapIndexed { index, habit ->
                habit.copy(displayOrder = index)
            }
            repository.insertHabits(updated)
        }
    }

    fun logHabitProgress(habitId: String, date: String, progressValue: Float, isCompleted: Boolean, xpReward: Int) {
        viewModelScope.launch {
            val existing = repository.getLogForHabitAndDate(habitId, date)
            val earnedXp = if (isCompleted) xpReward else 0
            val log = HabitLogEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                habitId = habitId,
                date = date,
                progressValue = progressValue,
                isCompleted = isCompleted,
                earnedXP = earnedXp,
                loggedAt = System.currentTimeMillis()
            )
            repository.insertHabitLog(log)
            try {
                com.example.utils.WidgetUpdater.updateAllWidgets(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun prepopulateHabitsIfEmpty() {
        viewModelScope.launch {
            val current = repository.allHabits.first()
            if (current.isEmpty()) {
                val samples = listOf(
                    HabitEntity(
                        id = "habit_1",
                        title = "Looksmaxxing Skincare Routine",
                        description = "Ice rolling, cleanse, moisturize & sunscreen for elite radiance.",
                        category = "Looksmaxxing",
                        type = "checkbox",
                        icon = "Face",
                        color = 0xFFD4AF37,
                        displayOrder = 0,
                        xpReward = 15
                    ),
                    HabitEntity(
                        id = "habit_2",
                        title = "Hydration Challenge",
                        description = "Drink water to stay energized and hydrated throughout the day.",
                        category = "Health",
                        type = "numeric",
                        icon = "Water",
                        color = 0xFF4A90E2,
                        dailyTargetValue = 8.0f,
                        targetUnit = "glasses",
                        displayOrder = 1,
                        xpReward = 10
                    ),
                    HabitEntity(
                        id = "habit_3",
                        title = "Cinematic Gym Core Session",
                        description = "High density push/pull or explosive functional lifting.",
                        category = "Fitness",
                        type = "timer",
                        icon = "Fitness",
                        color = 0xFFE15A5A,
                        dailyTargetValue = 45.0f, // 45 minutes
                        targetUnit = "min",
                        displayOrder = 2,
                        xpReward = 20
                    ),
                    HabitEntity(
                        id = "habit_4",
                        title = "Premium Coding & Architecture Guild",
                        description = "Study advanced system designs and write production-level clean code.",
                        category = "Learning",
                        type = "checkbox",
                        icon = "Laptop",
                        color = 0xFF8E711A,
                        displayOrder = 3,
                        xpReward = 25
                    )
                )
                repository.insertHabits(samples)
            }
        }
    }

    override fun onCleared() {
        // Releasing ExoPlayer safely on ViewModel clear to prevent heavy memory leaks
        _exoPlayer?.let { player ->
            Handler(Looper.getMainLooper()).post {
                try {
                    player.stop()
                    player.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        _exoPlayer = null
        super.onCleared()
    }
}

// Immutable state-safe holder for results query
data class SearchResultsData(
    val matchedCourses: List<CourseEntity> = emptyList(),
    val matchedLessons: List<LessonEntity> = emptyList(),
    val courseIdToTitle: Map<String, String> = emptyMap(),
    val query: String = ""
)
