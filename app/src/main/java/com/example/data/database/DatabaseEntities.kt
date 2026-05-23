package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val thumbnail: String?,
    val author: String,
    val version: Int,
    val courseUri: String,
    val dateImported: Long = System.currentTimeMillis()
)

@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey val id: String, // courseId + "_" + title
    val courseId: String,
    val title: String
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String, // lessonId
    val moduleId: String,
    val courseId: String,
    val title: String,
    val videoPath: String,
    val notePath: String?,
    val subtitlePath: String? = null,
    val duration: Int
)

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val lessonId: String,
    val courseId: String,
    val currentPosition: Long,
    val completed: Boolean,
    val playbackSpeed: Float = 1.0f,
    val lastWatched: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: String,
    val lessonId: String,
    val lessonTitle: String,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1, // Row ID 1 is the active singleton settings row
    val theme: String = "DARK",
    val defaultSpeed: Float = 1.0f,
    val autoplay: Boolean = true,
    val animationsEnabled: Boolean = true
)
