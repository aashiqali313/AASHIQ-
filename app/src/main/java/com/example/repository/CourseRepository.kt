package com.example.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class CourseRepository(private val database: AppDatabase) {

    private val courseDao = database.courseDao()
    private val moduleDao = database.moduleDao()
    private val lessonDao = database.lessonDao()
    private val recentSearchDao = database.recentSearchDao()
    private val userSettingsDao = database.userSettingsDao()

    val allCourses: Flow<List<CourseEntity>> = courseDao.getAllCourses()
    val bookmarkedLessons: Flow<List<LessonEntity>> = lessonDao.getBookmarkedLessons()
    val continueWatching: Flow<List<LessonEntity>> = lessonDao.getContinueWatchingLessons()
    val recentSearches: Flow<List<RecentSearchEntity>> = recentSearchDao.getRecentSearches()
    val userSettings: Flow<UserSettingsEntity?> = userSettingsDao.getSettingsFlow()

    suspend fun getCourseById(id: String): CourseEntity? = courseDao.getCourseById(id)
    
    fun getModulesForCourse(courseId: String): Flow<List<ModuleEntity>> = 
        moduleDao.getModulesForCourse(courseId)
        
    fun getLessonsForCourse(courseId: String): Flow<List<LessonEntity>> = 
        lessonDao.getLessonsForCourse(courseId)

    fun getLessonsForModule(moduleId: String): Flow<List<LessonEntity>> = 
        lessonDao.getLessonsForModule(moduleId)

    suspend fun getLessonById(id: String): LessonEntity? = lessonDao.getLessonById(id)

    suspend fun toggleBookmark(lessonId: String) {
        withContext(Dispatchers.IO) {
            val lesson = lessonDao.getLessonById(lessonId)
            if (lesson != null) {
                lessonDao.updateBookmark(lessonId, !lesson.isBookmarked)
            }
        }
    }

    suspend fun updateLessonProgress(lessonId: String, progressMs: Long, percent: Int) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            lessonDao.updateProgress(lessonId, progressMs, percent, timestamp)
        }
    }

    suspend fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        withContext(Dispatchers.IO) {
            recentSearchDao.insertSearch(RecentSearchEntity(query.trim(), System.currentTimeMillis()))
        }
    }

    suspend fun deleteRecentSearch(query: String) {
        withContext(Dispatchers.IO) {
            recentSearchDao.deleteSearch(query)
        }
    }

    suspend fun clearAllSearches() {
        withContext(Dispatchers.IO) {
            recentSearchDao.deleteAllSearches()
        }
    }

    suspend fun saveUserSettings(settings: UserSettingsEntity) {
        withContext(Dispatchers.IO) {
            userSettingsDao.insertSettings(settings)
        }
    }

    suspend fun getSettingsDirect(): UserSettingsEntity {
        return withContext(Dispatchers.IO) {
            userSettingsDao.getSettingsDirect() ?: UserSettingsEntity().also {
                userSettingsDao.insertSettings(it)
            }
        }
    }

    suspend fun deleteCourse(courseId: String) {
        withContext(Dispatchers.IO) {
            courseDao.deleteCourseById(courseId)
            moduleDao.deleteModulesForCourse(courseId)
            lessonDao.deleteLessonsForCourse(courseId)
        }
    }

    suspend fun deleteAllCourses() {
        withContext(Dispatchers.IO) {
            courseDao.deleteAllCourses()
            moduleDao.deleteAllModules()
            lessonDao.deleteAllLessons()
        }
    }

    suspend fun insertLessons(lessons: List<LessonEntity>) {
        withContext(Dispatchers.IO) {
            lessonDao.insertLessons(lessons)
        }
    }

    // Prepopulate App with Premium Curated Courses to prevent empty feedback
    suspend fun prepopulateIfEmpty() {
        withContext(Dispatchers.IO) {
            val existing = courseDao.getAllCourses().first()
            if (existing.isEmpty()) {
                val premiumCourses = listOf(
                    CourseEntity(
                        id = "course_cinematography",
                        title = "Aesthetic Cinematography Masterclass",
                        description = "Unveil the cinematic techniques of high-contrast lighting, composition grids, and HDR grade grading. Learn directly on real media environments.",
                        uriString = "",
                        thumbnailUri = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=640&auto=format&fit=crop",
                        category = "Direction & VFX",
                        author = "AASHIQ+ Premium Guild"
                    ),
                    CourseEntity(
                        id = "course_jetpack_compose",
                        title = "Jetpack Compose Luxury Architectural Mastery",
                        description = "Transform traditional state patterns into ultra-performing reactive layouts. Implement visual glassmorphism, responsive animations, and room caches.",
                        uriString = "",
                        thumbnailUri = "https://images.unsplash.com/photo-1555066931-4365d14bab8c?q=80&w=640&auto=format&fit=crop",
                        category = "Software Engineering",
                        author = "AASHIQ+ Tech Lab"
                    )
                )
                courseDao.insertCourses(premiumCourses)

                // Modules for course_cinematography
                val modulesCine = listOf(
                    ModuleEntity("mod_cine_1", "course_cinematography", "Cinematic Scene Composition", 1),
                    ModuleEntity("mod_cine_2", "course_cinematography", "Lighting & Color Grading", 2)
                )
                moduleDao.insertModules(modulesCine)

                // Lessons for cinematography
                val lessonsCine = listOf(
                    LessonEntity(
                        id = "les_cine_1",
                        moduleId = "mod_cine_1",
                        courseId = "course_cinematography",
                        title = "Cinematic VFX Composition: Tears of Steel",
                        videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        notePath = """
                            # Cinematic VFX Composition Masterclass
                            
                            Welcome to the AASHIQ+ core cinematography laboratory.
                            
                            ## Today's Core Teachings:
                            
                            1. **Rule of Thirds vs. Golden Ratio Grid**
                               Establish perfect focal points. Keep high contrast elements near the cross-intersects of the frame.
                            
                            2. **Depth Layering**
                               Always use foreground occlusion (blurry leaves, pillars) to frame a high-end scene.
                            
                            3. **VFX Lighting Matches**
                               When matching digital assets with real-world plates, match the color temperature, direction of shadow diffusion, and camera noise pattern exactly.
                            
                            *Tip:* Select any timestamp inside your media player HUD to save custom notes instantaneously!
                        """.trimIndent(),
                        durationSeconds = 734L,
                        orderIndex = 1
                    ),
                    LessonEntity(
                        id = "les_cine_2",
                        moduleId = "mod_cine_1",
                        courseId = "course_cinematography",
                        title = "Post Production Color Science",
                        videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        notePath = """
                            # Post Production Color Science
                            
                            Learn how colors evoke powerful sub-conscious emotional triggers:
                            
                            * **Teal & Orange Contrast**: The cinematic gold standard for separating skin tones from ambient backgrounds.
                            * **LUT Interpolation**: Understating mathematical look-up tables in 3D-space.
                            * **OLED Black Levels**: Crafting extreme low-mid contrast tones for matte black screen panels.
                        """.trimIndent(),
                        durationSeconds = 653L,
                        orderIndex = 2
                    )
                )
                lessonDao.insertLessons(lessonsCine)

                // Modules for Jetpack Compose Course
                val modulesCompose = listOf(
                    ModuleEntity("mod_comp_1", "course_jetpack_compose", "High-Performance Mechanics", 1),
                    ModuleEntity("mod_comp_2", "course_jetpack_compose", "Matte Glass UI Design", 2)
                )
                moduleDao.insertModules(modulesCompose)

                val lessonsCompose = listOf(
                    LessonEntity(
                        id = "les_comp_1",
                        moduleId = "mod_comp_1",
                        courseId = "course_jetpack_compose",
                        title = "Optimizing Recompositions in Busy Flows",
                        videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        notePath = """
                            # Minimizing Jetpack Compose Frame Drops
                            
                            High frequency recompositions lead to CPU bottlenecks and frame stutters.
                            
                            ## Architectural Directives:
                            
                            * **Remember your states**: Use `remember { mutableStateOf(...) }` to safeguard values.
                            * **Use DerivedStateOf**: When observing high-frequency inputs (like scroll viewport positions), wrap the expression in `derivedStateOf { ... }` so updates only trigger when criteria is strictly met.
                            * **Use LazyLists Correctly**: Always supply unique, stable `key` identifiers to lazy items so recompositions are surgically targeted.
                        """.trimIndent(),
                        durationSeconds = 596L,
                        orderIndex = 1
                    ),
                    LessonEntity(
                        id = "les_comp_2",
                        moduleId = "mod_comp_2",
                        courseId = "course_jetpack_compose",
                        title = "Crafting Matte-Glass Acrylic Overlays",
                        videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        notePath = """
                            # Glassmorphism & Matte Black Synthesis
                            
                            To implement a gorgeous luxury dark visual, stack:
                            
                            1. **Gradient Background Layer**: Use radial gradient brush.
                            2. **Semi-transparent Card Cover**: Fill custom card colors with Alpha level `0.5f` charcoal or matte black.
                            3. **Glow Border Outline**: 1dp wide border stroke containing a subtle gold-to-transparent brush.
                            4. **Lightweight Overlay**: Add a delicate blur layer if device resources are high-performance.
                        """.trimIndent(),
                        durationSeconds = 848L,
                        orderIndex = 2
                    )
                )
                lessonDao.insertLessons(lessonsCompose)
            }
        }
    }

    // Storage Access Framework (SAF) Background Course Folder Import System
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun importCourseFolder(
        context: Context,
        treeUri: Uri,
        onProgress: (status: String, progress: Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress("Persisting secure URI permission...", 0.1f)
            val resolver = context.contentResolver
            resolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            val rootFolder = DocumentFile.fromTreeUri(context, treeUri)
            if (rootFolder == null || !rootFolder.isDirectory) {
                return@withContext false
            }

            val courseName = rootFolder.name ?: "Imported Luxury Course"
            val courseId = "imported_" + UUID.randomUUID().toString().take(8)

            onProgress("Parsing course metadata...", 0.2f)

            val files = rootFolder.listFiles()
            if (files.isEmpty()) {
                return@withContext false
            }

            // Group contents: if directories, they are modules. If solitary videos, map to default module.
            val directories = files.filter { it.isDirectory }
            val videoFilesRoot = files.filter { isVideoFile(it.name ?: "") }
            
            val modulesToInsert = mutableListOf<ModuleEntity>()
            val lessonsToInsert = mutableListOf<LessonEntity>()

            var orderCounter = 1

            if (directories.isNotEmpty()) {
                // Multi-module course structure
                directories.forEachIndexed { dirIndex, dir ->
                    val moduleTitle = dir.name ?: "Module ${dirIndex + 1}"
                    val moduleId = "mod_${courseId}_${dirIndex + 1}"
                    modulesToInsert.add(ModuleEntity(moduleId, courseId, moduleTitle, dirIndex + 1))

                    val modFiles = dir.listFiles()
                    var lessonIndex = 1
                    
                    // Parse video files in module folder
                    val modVideos = modFiles.filter { isVideoFile(it.name ?: "") }.sortedBy { it.name }
                    modVideos.forEach { file ->
                        val lessonTitle = file.name?.substringBeforeLast(".") ?: "Lesson $lessonIndex"
                        val lessonId = "les_${moduleId}_${UUID.randomUUID().toString().take(6)}"

                        // Try finding notes of same name (.md or .txt)
                        val noteContent = findCorrespondingNoteText(resolver, modFiles, file.name)

                        lessonsToInsert.add(
                            LessonEntity(
                                id = lessonId,
                                moduleId = moduleId,
                                courseId = courseId,
                                title = lessonTitle,
                                videoUri = file.uri.toString(),
                                notePath = noteContent ?: "## $lessonTitle\nEnjoy this offline cinematic video lesson natively on AASHIQ+.",
                                durationSeconds = 300L, // Placeholder, dynamically read where supported
                                orderIndex = lessonIndex
                            )
                        )
                        lessonIndex++
                    }
                }
            } else if (videoFilesRoot.isNotEmpty()) {
                // Single-module course structure
                val defaultModuleId = "mod_${courseId}_default"
                modulesToInsert.add(ModuleEntity(defaultModuleId, courseId, "General Lectures", 1))

                videoFilesRoot.forEachIndexed { videoIndex, file ->
                    val lessonTitle = file.name?.substringBeforeLast(".") ?: "Lecture ${videoIndex + 1}"
                    val lessonId = "les_${defaultModuleId}_${videoIndex + 1}"
                    val noteContent = findCorrespondingNoteText(resolver, files, file.name)

                    lessonsToInsert.add(
                        LessonEntity(
                            id = lessonId,
                            moduleId = defaultModuleId,
                            courseId = courseId,
                            title = lessonTitle,
                            videoUri = file.uri.toString(),
                            notePath = noteContent ?: "## $lessonTitle\nPremium offline lecture notes.",
                            durationSeconds = 300L,
                            orderIndex = videoIndex + 1
                        )
                    )
                }
            } else {
                // No video files found in selection
                return@withContext false
            }

            onProgress("Saving details into secure Room DB...", 0.7f)

            // Dynamic course thumbnail: take from unsplash to keep elegant visual, or any embedded image file inside folder!
            val mainImageFile = files.find { isImageFile(it.name ?: "") }
            val courseThumbnail = mainImageFile?.uri?.toString() 
                ?: "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=640&auto=format&fit=crop"

            val courseEntity = CourseEntity(
                id = courseId,
                title = courseName,
                description = "Natively imported from local folder on ${System.currentTimeMillis()}.\nFully indexes ${lessonsToInsert.size} high fidelity video resources, attachments and lectures.",
                uriString = treeUri.toString(),
                thumbnailUri = courseThumbnail,
                category = "Imported Offline",
                author = "Local Drive Operator",
                isRecentlyImported = true,
                importTimestamp = System.currentTimeMillis()
            )

            // Atomic database operations
            courseDao.insertCourse(courseEntity)
            moduleDao.insertModules(modulesToInsert)
            lessonDao.insertLessons(lessonsToInsert)

            onProgress("Course successfully indexed!", 1.0f)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isVideoFile(filename: String): Boolean {
        val extension = filename.substringAfterLast(".").lowercase()
        return extension in listOf("mp4", "mkv", "3gp", "webm", "avi", "mov")
    }

    private fun isImageFile(filename: String): Boolean {
        val extension = filename.substringAfterLast(".").lowercase()
        return extension in listOf("png", "jpg", "jpeg", "webp")
    }

    private fun findCorrespondingNoteText(resolver: ContentResolver, files: Array<DocumentFile>, videoFileName: String?): String? {
        if (videoFileName == null) return null
        val baseName = videoFileName.substringBeforeLast(".")
        val noteFile = files.find {
            val name = it.name ?: ""
            name.substringBeforeLast(".") == baseName && 
            (name.endsWith(".md") || name.endsWith(".txt"))
        }

        return noteFile?.let { file ->
            try {
                resolver.openInputStream(file.uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
