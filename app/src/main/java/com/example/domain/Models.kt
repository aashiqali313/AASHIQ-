package com.example.domain

data class Course(
    val id: String,
    val title: String,
    val description: String,
    val thumbnail: String?,
    val author: String,
    val version: Int,
    val modules: List<Module> = emptyList(),
    val courseUri: String = "" // Added when imported to store root SAF URI
)

data class Module(
    val id: String,
    val title: String,
    val lessons: List<Lesson> = emptyList()
)

data class Lesson(
    val id: String,
    val title: String,
    val video: String, // mapped from videos/lesson.mp4
    val note: String?,  // mapped from notes/lesson.md
    val subtitle: String? = null, // resolved URI string of subtitle file
    val duration: Int  // length in seconds
)

data class PlaybackProgress(
    val lessonId: String,
    val courseId: String,
    val currentPosition: Long,
    val completed: Boolean,
    val playbackSpeed: Float = 1.0f,
    val lastWatched: Long = System.currentTimeMillis()
)

data class Bookmark(
    val id: Int = 0,
    val courseId: String,
    val lessonId: String,
    val lessonTitle: String,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)

data class Settings(
    val theme: String = "DARK", // DARK, LIGHT, AMOLED
    val defaultSpeed: Float = 1.0f,
    val autoplay: Boolean = true,
    val animationsEnabled: Boolean = true
)
