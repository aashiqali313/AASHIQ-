package com.example.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.database.*
import com.example.data.model.CourseJson
import com.example.domain.Bookmark
import com.example.domain.Course
import com.example.domain.CourseImportException
import com.example.domain.Lesson
import com.example.domain.Module
import com.example.domain.PlaybackProgress
import com.example.domain.Settings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class CourseRepository(
    private val context: Context,
    private val courseDao: CourseDao,
    private val progressDao: PlaybackProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val settingsDao: SettingsDao
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val jsonAdapter = moshi.adapter(CourseJson::class.java)

    // Exposed Flows mapped to domain models
    val allCourses: Flow<List<Course>> = courseDao.getAllCourses().map { entities ->
        entities.map { entity ->
            Course(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                thumbnail = getFileUriStringIfPresent(entity.courseUri, entity.thumbnail),
                author = entity.author,
                version = entity.version,
                courseUri = entity.courseUri
            )
        }
    }.flowOn(Dispatchers.IO)

    fun getCourseDetail(courseId: String): Flow<Course?> = combine(
        flow { emit(courseDao.getCourseById(courseId)) },
        courseDao.getModulesForCourse(courseId),
        courseDao.getLessonsForCourse(courseId)
    ) { courseEntity, moduleEntities, lessonEntities ->
        if (courseEntity == null) return@combine null

        val lessonsByModule = lessonEntities.groupBy { it.moduleId }
        val modules = moduleEntities.map { modEntity ->
            val modLessons = lessonsByModule[modEntity.id]?.map { lesEntity ->
                Lesson(
                    id = lesEntity.id,
                    title = lesEntity.title,
                    video = getFileUriStringIfPresent(courseEntity.courseUri, lesEntity.videoPath) ?: "",
                    note = lesEntity.notePath, // markdown note relative path or text
                    subtitle = getFileUriStringIfPresent(courseEntity.courseUri, lesEntity.subtitlePath),
                    duration = lesEntity.duration
                )
            } ?: emptyList()
            Module(
                id = modEntity.id,
                title = modEntity.title,
                lessons = modLessons
            )
        }

        Course(
            id = courseEntity.id,
            title = courseEntity.title,
            description = courseEntity.description,
            thumbnail = getFileUriStringIfPresent(courseEntity.courseUri, courseEntity.thumbnail),
            author = courseEntity.author,
            version = courseEntity.version,
            modules = modules,
            courseUri = courseEntity.courseUri
        )
    }.flowOn(Dispatchers.IO)

    fun getLessonsForCourse(courseId: String): Flow<List<Lesson>> = flow {
        val course = courseDao.getCourseById(courseId) ?: return@flow
        courseDao.getLessonsForCourse(courseId).collect { entities ->
            val lessons = entities.map { entity ->
                Lesson(
                    id = entity.id,
                    title = entity.title,
                    video = getFileUriStringIfPresent(course.courseUri, entity.videoPath) ?: "",
                    note = entity.notePath,
                    subtitle = getFileUriStringIfPresent(course.courseUri, entity.subtitlePath),
                    duration = entity.duration
                )
            }
            emit(lessons)
        }
    }.flowOn(Dispatchers.IO)

    // Playback Progress
    fun getProgressForCourse(courseId: String): Flow<Map<String, PlaybackProgress>> =
        progressDao.getProgressForCourse(courseId).map { entities ->
            entities.associate { entity ->
                entity.lessonId to PlaybackProgress(
                    lessonId = entity.lessonId,
                    courseId = entity.courseId,
                    currentPosition = entity.currentPosition,
                    completed = entity.completed,
                    playbackSpeed = entity.playbackSpeed,
                    lastWatched = entity.lastWatched
                )
            }
        }.flowOn(Dispatchers.IO)

    fun getProgressForLesson(lessonId: String): Flow<PlaybackProgress?> =
        progressDao.getProgressForLesson(lessonId).map { entity ->
            entity?.let {
                PlaybackProgress(
                    lessonId = it.lessonId,
                    courseId = it.courseId,
                    currentPosition = it.currentPosition,
                    completed = it.completed,
                    playbackSpeed = it.playbackSpeed,
                    lastWatched = it.lastWatched
                )
            }
        }.flowOn(Dispatchers.IO)

    suspend fun saveProgress(progress: PlaybackProgress) = withContext(Dispatchers.IO) {
        progressDao.insertProgress(
            PlaybackProgressEntity(
                lessonId = progress.lessonId,
                courseId = progress.courseId,
                currentPosition = progress.currentPosition,
                completed = progress.completed,
                playbackSpeed = progress.playbackSpeed,
                lastWatched = progress.lastWatched
            )
        )
    }

    fun getContinueWatching(): Flow<List<PlaybackProgress>> =
        progressDao.getContinueWatching().map { entities ->
            entities.map { entity ->
                PlaybackProgress(
                    lessonId = entity.lessonId,
                    courseId = entity.courseId,
                    currentPosition = entity.currentPosition,
                    completed = entity.completed,
                    playbackSpeed = entity.playbackSpeed,
                    lastWatched = entity.lastWatched
                )
            }
        }.flowOn(Dispatchers.IO)

    // Bookmarks
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks().map { entities ->
        entities.map { entity ->
            Bookmark(
                id = entity.id,
                courseId = entity.courseId,
                lessonId = entity.lessonId,
                lessonTitle = entity.lessonTitle,
                timestamp = entity.timestamp,
                createdAt = entity.createdAt
            )
        }
    }.flowOn(Dispatchers.IO)

    fun getBookmarksForCourse(courseId: String): Flow<List<Bookmark>> =
        bookmarkDao.getBookmarksForCourse(courseId).map { entities ->
            entities.map { entity ->
                Bookmark(
                    id = entity.id,
                    courseId = entity.courseId,
                    lessonId = entity.lessonId,
                    lessonTitle = entity.lessonTitle,
                    timestamp = entity.timestamp,
                    createdAt = entity.createdAt
                )
            }
        }.flowOn(Dispatchers.IO)

    fun getBookmarksForLesson(lessonId: String): Flow<List<Bookmark>> =
        bookmarkDao.getBookmarksForLesson(lessonId).map { entities ->
            entities.map { entity ->
                Bookmark(
                    id = entity.id,
                    courseId = entity.courseId,
                    lessonId = entity.lessonId,
                    lessonTitle = entity.lessonTitle,
                    timestamp = entity.timestamp,
                    createdAt = entity.createdAt
                )
            }
        }.flowOn(Dispatchers.IO)

    suspend fun addBookmark(bookmark: Bookmark) = withContext(Dispatchers.IO) {
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                courseId = bookmark.courseId,
                lessonId = bookmark.lessonId,
                lessonTitle = bookmark.lessonTitle,
                timestamp = bookmark.timestamp,
                createdAt = bookmark.createdAt
            )
        )
    }

    suspend fun deleteBookmark(bookmarkId: Int) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(bookmarkId)
    }

    // Settings
    val settings: Flow<Settings> = settingsDao.getSettings().map { entity ->
        if (entity == null) {
            Settings()
        } else {
            Settings(
                theme = entity.theme,
                defaultSpeed = entity.defaultSpeed,
                autoplay = entity.autoplay,
                animationsEnabled = entity.animationsEnabled
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateSettings(settings: Settings) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdateSettings(
            SettingsEntity(
                theme = settings.theme,
                defaultSpeed = settings.defaultSpeed,
                autoplay = settings.autoplay,
                animationsEnabled = settings.animationsEnabled
            )
        )
    }

    suspend fun deleteCourseCascade(courseId: String) = withContext(Dispatchers.IO) {
        courseDao.deleteCourseCascade(courseId)
    }

    // Core Course Parsing & Validation System
    suspend fun importCourseFromUri(treeUri: Uri): String = withContext(Dispatchers.IO) {
        val rootDir = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw CourseImportException("Could not open picked folder directory.")

        // 1. Locate and read course.json
        val courseJsonFile = rootDir.findFile("course.json")
            ?: throw CourseImportException("File 'course.json' missing at root of course directory.")

        val jsonBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(courseJsonFile.uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        jsonBuilder.append(line)
                    }
                }
            }
        } catch (e: Exception) {
            throw CourseImportException("Failed reading 'course.json': ${e.message}")
        }

        val jsonStr = jsonBuilder.toString()
        val courseJson = try {
            jsonAdapter.fromJson(jsonStr)
        } catch (e: Exception) {
            throw CourseImportException("YAML/JSON schema syntax error inside 'course.json': ${e.message}")
        } ?: throw CourseImportException("Invalid empty document structure in 'course.json'.")

        // 2. Validate Metadata Structure
        val id = courseJson.id
            ?: throw CourseImportException("Missing field: course 'id' is empty in course.json.")
        if (id.trim().isEmpty()) {
            throw CourseImportException("Field 'id' in course.json cannot be empty whitespace.")
        }

        val title = courseJson.title
            ?: throw CourseImportException("Missing field: course 'title' in course.json.")
        val description = courseJson.description ?: "Offline local educational series"
        val author = courseJson.author ?: "Aashiq"
        val version = courseJson.version ?: 1

        // 3. File existence checklist
        // A. Thumbnail
        val rawThumbPath = courseJson.thumbnail
        if (!rawThumbPath.isNullOrEmpty()) {
            if (!rawThumbPath.startsWith("http://") && !rawThumbPath.startsWith("https://") && !rawThumbPath.startsWith("content://")) {
                val thumbFile = findFileByRelativePath(context, treeUri.toString(), rawThumbPath)
                if (thumbFile == null || !thumbFile.exists()) {
                    throw CourseImportException("Import folder missing thumbnail file: '$rawThumbPath'")
                }
            }
        }

        // B. Check lessons modules and verify files
        val modulesList = courseJson.modules
            ?: throw CourseImportException("Missing modules block or modules list in course.json.")

        val moduleEntities = ArrayList<ModuleEntity>()
        val lessonEntities = ArrayList<LessonEntity>()
        val seenLessonIds = HashSet<String>()

        for ((mIndex, modJson) in modulesList.withIndex()) {
            val modTitle = modJson.title ?: "Module ${mIndex + 1}"
            val moduleId = "${id}_mod_${mIndex + 1}"

            moduleEntities.add(
                ModuleEntity(
                    id = moduleId,
                    courseId = id,
                    title = modTitle
                )
            )

            val lessonsList = modJson.lessons
                ?: throw CourseImportException("Module '$modTitle' is missing lessons specification.")

            for (lesJson in lessonsList) {
                val lesId = lesJson.id ?: throw CourseImportException(
                    "Lesson missing 'id' inside Module '$modTitle'."
                )
                if (seenLessonIds.contains(lesId)) {
                    throw CourseImportException("Duplicate lesson IDs detected across course: '$lesId'")
                }
                seenLessonIds.add(lesId)

                val lesTitle = lesJson.title ?: "Untitled Lesson"
                val videoPath = lesJson.video ?: throw CourseImportException(
                    "Lesson '$lesTitle' inside Module '$modTitle' is missing video file path ('video')."
                )

                // Validate video format and file existence/integrity
                if (!videoPath.startsWith("http://") && !videoPath.startsWith("https://") && !videoPath.startsWith("content://")) {
                    val videoFile = findFileByRelativePath(context, treeUri.toString(), videoPath)
                    if (videoFile == null || !videoFile.exists()) {
                        throw CourseImportException("Lesson '$lesTitle' references missing video file: '$videoPath'")
                    }
                    if (videoFile.length() == 0L) {
                        throw CourseImportException("Lesson '$lesTitle' video file is corrupt or empty: '$videoPath'")
                    }
                }

                // Subtitle Validation and Integrity Check
                val subPath = lesJson.subtitle
                if (!subPath.isNullOrEmpty()) {
                    if (!subPath.startsWith("http://") && !subPath.startsWith("https://") && !subPath.startsWith("content://")) {
                        val subFile = findFileByRelativePath(context, treeUri.toString(), subPath)
                        if (subFile == null || !subFile.exists()) {
                            throw CourseImportException("Lesson '$lesTitle' references missing subtitle file: '$subPath'")
                        }
                        if (subFile.length() == 0L) {
                            throw CourseImportException("Lesson '$lesTitle' subtitle file is empty: '$subPath'")
                        }
                    }
                }

                // Optional MD note Validation and Integrity Check
                val notePath = lesJson.note
                if (!notePath.isNullOrEmpty()) {
                    if (!notePath.startsWith("http://") && !notePath.startsWith("https://") && !notePath.startsWith("content://")) {
                        val noteFile = findFileByRelativePath(context, treeUri.toString(), notePath)
                        if (noteFile == null || !noteFile.exists()) {
                            throw CourseImportException("Lesson '$lesTitle' references missing note file: '$notePath'")
                        }
                        if (noteFile.length() == 0L) {
                            throw CourseImportException("Lesson '$lesTitle' note file is empty: '$notePath'")
                        }
                    }
                }

                lessonEntities.add(
                    LessonEntity(
                        id = lesId,
                        moduleId = moduleId,
                        courseId = id,
                        title = lesTitle,
                        videoPath = videoPath,
                        notePath = notePath,
                        subtitlePath = subPath,
                        duration = lesJson.duration ?: 0
                    )
                )
            }
        }

        // Import validated items to Room
        courseDao.insertCourse(
            CourseEntity(
                id = id,
                title = title,
                description = description,
                thumbnail = rawThumbPath,
                author = author,
                version = version,
                courseUri = treeUri.toString()
            )
        )
        courseDao.insertModules(moduleEntities)
        courseDao.insertLessons(lessonEntities)

        return@withContext id
    }

    // Resolves SAF note file content
    suspend fun readNoteContent(courseUriStr: String, notePath: String?): String = withContext(Dispatchers.IO) {
        if (notePath.isNullOrEmpty()) return@withContext ""
        val noteFile = findFileByRelativePath(context, courseUriStr, notePath)
            ?: return@withContext "Note file not found: $notePath"

        val sb = StringBuilder()
        try {
            context.contentResolver.openInputStream(noteFile.uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext "Failed to load note: ${e.message}"
        }
        return@withContext sb.toString()
    }

    // Local SAF path explorer
    fun getFileUriStringIfPresent(courseUriStr: String, relativePath: String?): String? {
        if (relativePath.isNullOrEmpty()) return null
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://") || relativePath.startsWith("content://")) {
            return relativePath
        }
        val doc = findFileByRelativePath(context, courseUriStr, relativePath)
        return doc?.uri?.toString()
    }

    private fun findFileByRelativePath(context: Context, treeUri: String, relativePath: String): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return null
        if (relativePath.isEmpty()) return root

        var current: DocumentFile = root
        // Normalize split delimiters
        val parts = relativePath.replace("\\", "/").split("/").filter { it.isNotEmpty() && it != "." }
        for (part in parts) {
            val next = current.findFile(part) ?: return null
            current = next
        }
        return current
    }
}
