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
                        """.trimIndent(),
                        durationSeconds = 734L,
                        orderIndex = 1,
                        type = "video"
                    ),
                    LessonEntity(
                        id = "les_cine_2",
                        moduleId = "mod_cine_1",
                        courseId = "course_cinematography",
                        title = "Post Production Color Science",
                        videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        notePath = """
                            # Post Production Color Science
                            
                            In cinematic storytelling, color science is not decorative—it is a physiological coordinate system designed to guide sub-conscious emotional response.
                            
                            :::info
                            Colors translate directly into cerebral triggers, manipulating ambient tension, focus hierarchy and psychological safety index.
                            :::
                            
                            ## 1. Advanced Cinematic Color Grading Tech
                            
                            | Spectrum Accent | Emotional Valence | Target Narrative Environment |
                            | |-- | |-- |
                            | Gold Teal Contrast | Immersion, Safety | Modern Blockbuster Action Sequences |
                            | Emerald Slate | Paranoia, Isolation | Suspenseful Sci-Fi Thriller Plates |
                            | Rich Warm Amber | Nostalgia, Melancholic | Period Biopic Retrospectives |
                            
                            :::comparison
                            [SDR BASE REC. 709 GRID] | [HDR WIDE COLOR COLUMNS]
                            Linear Rec.709 colors with standard 8-bit dynamic depth levels where dark channels merge. | Extended BT.2020 color gamut with 10-bit dark sub-levels delivering ultra deep luminance.
                            :::
                            
                            :::flashcard
                            Front: LUT Interpolation 3D
                            Back: A mathematical matrix mapping a 10-bit color channel into custom stylistic color spaces.
                            :::
                            
                            ## 2. Dynamic Performance Quiz
                            
                            :::quiz
                            Question: Why is Teal and Orange the industry standard color pairing?
                            A) Because they are easiest to render in digital post-production.
                            B) Because human skin tones sit in the warm orange spectrum, and its complementary color is cool contrast teal.
                            C) It is a legacy analog tape layout constraint.
                            Answer: B
                            Explanation: Orange skin tones contrasted against complementary blue-teal backgrounds maximizes skin tone focus and separation.
                            :::
                            
                            ## 3. Post Production Safety Directives
                            
                            :::expandable Pro Editing Secret Checklist
                            - [ ] Calibrate grading monitor to Rec.2020.
                            - [ ] Enforce Matte Black low contrast ceilings.
                            - [ ] Apply fine grain analog noise in active 4K composite plates.
                            :::
                        """.trimIndent(),
                        durationSeconds = 653L,
                        orderIndex = 2,
                        type = "mixed_media"
                    ),
                    LessonEntity(
                        id = "les_cine_3",
                        moduleId = "mod_cine_2",
                        courseId = "course_cinematography",
                        title = "HDR Cine Grading Standards Handbook",
                        videoUri = "",
                        notePath = "Cinematographer's reference manual for HDR mastering protocols, color gamut spaces, and Rec.2020 calibrating schemas.",
                        durationSeconds = 0L,
                        orderIndex = 3,
                        type = "pdf",
                        pdfUri = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
                    ),
                    LessonEntity(
                        id = "les_cine_4",
                        moduleId = "mod_cine_2",
                        courseId = "course_cinematography",
                        title = "Visual Scene Moodboards & Layouts",
                        videoUri = "",
                        notePath = "A curated database of cinematic layout design inspirations, framing examples, color pairings, and creative references.",
                        durationSeconds = 0L,
                        orderIndex = 4,
                        type = "gallery",
                        galleryImagesJson = """["https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=640","https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=640","https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=640"]"""
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
                        orderIndex = 1,
                        type = "quick_note"
                    ),
                    LessonEntity(
                        id = "les_comp_2",
                        moduleId = "mod_comp_2",
                        courseId = "course_jetpack_compose",
                        title = "Crafting Matte-Glass Acrylic Overlays",
                        videoUri = "",
                        notePath = """
                            {
                              "title": "Aesthetic Matte-Glass Overlays",
                              "category": "Jetpack Compose Luxury Mechanics",
                              "sections": [
                                {
                                  "type": "text",
                                  "content": "To implement high-end visual glassmorphism inside a luxury dark application, stack overlapping drawing canvas layers together."
                                },
                                {
                                  "type": "callout",
                                  "style": "tip",
                                  "content": "Always use edge-to-edge drawing for premium immersive experiences."
                                },
                                {
                                  "type": "columns",
                                  "columns": [
                                    "**Gradient Foundation Layer**\nBuild radial or sweeping gradient brushes using custom drawing scopes inside `drawBehind` modifiers.",
                                    "**Selective Acrylic Blur**\nStack a semi-transparent card cover with Alpha parameter set of `0.15f` to `0.35f` to let underlying shadows dissolve beautifully."
                                  ]
                                },
                                {
                                  "type": "comparison",
                                  "left_header": "STANDARD CARD STATS",
                                  "right_header": "MATTE-GLASS SHEETS",
                                  "left_text": "Solid flat charcoal background with simple gray outline borders.",
                                  "right_text": "Radial color gradients, semi-translucence card surface layers, and custom thin gold borders."
                                },
                                {
                                  "type": "quiz",
                                  "question": "Which Compose modifier best supports custom radial drawing on background canvases without creating redundant recomposition states?",
                                  "options": [
                                    "Modifier.background()",
                                    "Modifier.drawBehind()",
                                    "Modifier.paint()"
                                  ],
                                  "answer_index": 1,
                                  "explanation": "Modifier.drawBehind allows you to draw directly onto the canvas, bypassing layout measurements entirely for massive render optimization."
                                },
                                {
                                  "type": "expandable",
                                  "title": "Under-the-Hood Compositing Directives",
                                  "content": "To deliver silk-like 120Hz glass effects on low-power devices, avoid active run-time blurred captures. Instead, approximate with premium static gradient pre-renders and light vector grain layers."
                                }
                              ]
                            }
                        """.trimIndent(),
                        durationSeconds = 0L,
                        orderIndex = 2,
                        type = "article"
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

            // Group contents: if directories, they are modules. If solitary videos/files, map to default module.
            val directories = files.filter { it.isDirectory }
            val modulesToInsert = mutableListOf<ModuleEntity>()
            val lessonsToInsert = mutableListOf<LessonEntity>()

            if (directories.isNotEmpty()) {
                // Multi-module course structure
                directories.forEachIndexed { dirIndex, dir ->
                    val moduleTitle = dir.name ?: "Module ${dirIndex + 1}"
                    val moduleId = "mod_${courseId}_${dirIndex + 1}"
                    modulesToInsert.add(ModuleEntity(moduleId, courseId, moduleTitle, dirIndex + 1))

                    val modFiles = dir.listFiles()
                    val addedLessons = parseDirectoryFiles(resolver, modFiles, moduleId, courseId)
                    lessonsToInsert.addAll(addedLessons)
                }
            } else {
                // Single-module course structure
                val defaultModuleId = "mod_${courseId}_default"
                modulesToInsert.add(ModuleEntity(defaultModuleId, courseId, "General Lectures", 1))
                val addedLessons = parseDirectoryFiles(resolver, files, defaultModuleId, courseId)
                lessonsToInsert.addAll(addedLessons)
            }

            if (lessonsToInsert.isEmpty()) {
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
        return extension in listOf("mp4", "mkv", "3gp", "webm", "avi", "mov", "aashiq")
    }

    private fun isImageFile(filename: String): Boolean {
        val extension = filename.substringAfterLast(".").lowercase()
        return extension in listOf("png", "jpg", "jpeg", "webp")
    }

    private fun findCorrespondingSubtitleUri(files: Array<DocumentFile>, videoFileName: String?): String? {
        if (videoFileName == null) return null
        val baseName = videoFileName.substringBeforeLast(".")
        val subFile = files.find {
            val name = it.name ?: ""
            name.substringBeforeLast(".") == baseName && 
            (name.endsWith(".srt") || name.endsWith(".vtt"))
        }
        return subFile?.uri?.toString()
    }

    private fun findCorrespondingPdfUri(files: Array<DocumentFile>, videoFileName: String?): String? {
        if (videoFileName == null) return null
        val baseName = videoFileName.substringBeforeLast(".")
        // Find PDF with same name first
        var pdfFile = files.find {
            val name = it.name ?: ""
            name.substringBeforeLast(".") == baseName && name.endsWith(".pdf")
        }
        // Fallback: If no PDF with same name, find any PDF in the same folder
        if (pdfFile == null) {
            pdfFile = files.find { (it.name ?: "").endsWith(".pdf") }
        }
        return pdfFile?.uri?.toString()
    }

    private fun findCorrespondingNoteText(resolver: ContentResolver, files: Array<DocumentFile>, videoFileName: String?): String? {
        if (videoFileName == null) return null
        val baseName = videoFileName.substringBeforeLast(".")
        val noteFile = files.find {
            val name = it.name ?: ""
            name.substringBeforeLast(".") == baseName && 
            (name.endsWith(".md") || name.endsWith(".txt") || name.endsWith(".html") || name.endsWith(".aashiqnote") || name.endsWith(".json"))
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

    private fun parseDirectoryFiles(
        resolver: ContentResolver,
        files: Array<DocumentFile>,
        moduleId: String,
        courseId: String
    ): List<LessonEntity> {
        val lessons = mutableListOf<LessonEntity>()
        val processedNames = mutableSetOf<String>() // Keep track of base names that are associated with a video

        val videoFiles = files.filter { isVideoFile(it.name ?: "") }.sortedBy { it.name }
        var index = 1

        // 1. Parse Video lessons first
        videoFiles.forEach { file ->
            val filename = file.name ?: ""
            val baseName = filename.substringBeforeLast(".")
            processedNames.add(baseName)

            val lessonTitle = baseName
            val lessonId = "les_${moduleId}_${UUID.randomUUID().toString().take(6)}"
            val noteContent = findCorrespondingNoteText(resolver, files, filename)
            val subtitleUri = findCorrespondingSubtitleUri(files, filename)
            val pdfUri = findCorrespondingPdfUri(files, filename)

            val type = if (noteContent?.trim()?.startsWith("{") == true && noteContent.trim().endsWith("}")) {
                "mixed_media"
            } else {
                "video"
            }

            lessons.add(
                LessonEntity(
                    id = lessonId,
                    moduleId = moduleId,
                    courseId = courseId,
                    title = lessonTitle,
                    videoUri = file.uri.toString(),
                    notePath = noteContent ?: "## $lessonTitle\nEnjoy this offline cinematic video lesson natively on AASHIQ+.",
                    durationSeconds = 300L,
                    orderIndex = index++,
                    subtitleUri = subtitleUri,
                    pdfUri = pdfUri,
                    type = type
                )
            )
        }

        // 2. Parse standalone PDF files as standalone PDF lessons
        val pdfFiles = files.filter { (it.name ?: "").endsWith(".pdf") }
        pdfFiles.forEach { file ->
            val filename = file.name ?: ""
            val baseName = filename.substringBeforeLast(".")
            if (baseName !in processedNames) {
                // Standalone PDF!
                val lessonId = "les_${moduleId}_pdf_${UUID.randomUUID().toString().take(6)}"
                lessons.add(
                    LessonEntity(
                        id = lessonId,
                        moduleId = moduleId,
                        courseId = courseId,
                        title = baseName,
                        videoUri = "",
                        notePath = "PDF handbook: $baseName. Access documentation directly inside AASHIQ+.",
                        durationSeconds = 0L,
                        orderIndex = index++,
                        pdfUri = file.uri.toString(),
                        type = "pdf"
                    )
                )
            }
        }

        // 3. Parse standalone markdown/html/txt files as Article lessons
        val noteFiles = files.filter {
            val name = it.name ?: ""
            name.endsWith(".md") || name.endsWith(".txt") || name.endsWith(".html") || name.endsWith(".aashiqnote") || name.endsWith(".json")
        }
        noteFiles.forEach { file ->
            val filename = file.name ?: ""
            val baseName = filename.substringBeforeLast(".")
            if (baseName !in processedNames) {
                // Standalone Article/Note!
                val content = try {
                    resolver.openInputStream(file.uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).use { reader ->
                            reader.readText()
                        }
                    }
                } catch (e: Exception) {
                    null
                }

                val finalContent = content ?: "Premium study material: $baseName."
                val isJson = finalContent.trim().startsWith("{") && finalContent.trim().endsWith("}")
                val type = if (isJson) "article" else if (finalContent.length < 500) "quick_note" else "article"

                val lessonId = "les_${moduleId}_art_${UUID.randomUUID().toString().take(6)}"
                lessons.add(
                    LessonEntity(
                        id = lessonId,
                        moduleId = moduleId,
                        courseId = courseId,
                        title = baseName,
                        videoUri = "",
                        notePath = finalContent,
                        durationSeconds = 0L,
                        orderIndex = index++,
                        type = type
                    )
                )
            }
        }

        return lessons
    }
}
