package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val uriString: String,
    val thumbnailUri: String,
    val category: String,
    val author: String = "AASHIQ+ Premium Guild",
    val isRecentlyImported: Boolean = false,
    val importTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    val orderIndex: Int
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val moduleId: String,
    val courseId: String,
    val title: String,
    val videoUri: String,
    val notePath: String?, // Markdown note or rich description text
    val durationSeconds: Long = 0L,
    val orderIndex: Int,
    val isBookmarked: Boolean = false,
    val lastWatchedTimestamp: Long = 0L,
    val playProgressPercent: Int = 0,
    val progressMs: Long = 0L,
    val subtitleUri: String? = null,
    val pdfUri: String? = null,
    val type: String = "video", // video, article, pdf, gallery, quick_note, mixed_media
    val galleryImagesJson: String? = null, // List of local or remote image URIs as JSON
    val resolution: String? = null,
    val fileSize: Long = 0L
)

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val lessonId: String,
    val courseId: String,
    val playProgressMs: Long,
    val totalDurationMs: Long,
    val lastWatchedTime: Long
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: String = "app_config",
    val darkTheme: Boolean = true,
    val amoledBlack: Boolean = true,
    val accentColorGold: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val showSubtitles: Boolean = false,
    val volumeGestureEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val doubleTapSeekEnabled: Boolean = true,
    val animationIntensityMultiplier: Float = 1.0f,
    val certificateThresholdPercent: Int = 90
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "main_user",
    val name: String = "Aashiq Ali",
    val age: Int = 22,
    val gender: String = "Male",
    val avatarUri: String = "",
    val totalWatchTimeMinutes: Long = 120L,
    val completedCoursesCount: Int = 1,
    val currentStreak: Int = 5,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey val certificateId: String,
    val userName: String,
    val courseId: String,
    val courseName: String,
    val completionDate: Long = System.currentTimeMillis(),
    val hashSignature: String
)

