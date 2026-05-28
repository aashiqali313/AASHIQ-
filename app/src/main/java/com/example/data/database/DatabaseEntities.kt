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
    val type: String = "video", // video, article, pdf, gallery, quick_note, mixed_media, quiz
    val galleryImagesJson: String? = null, // List of local or remote image URIs as JSON
    val resolution: String? = null,
    val fileSize: Long = 0L,
    val isCompleted: Boolean = false,
    val articleProgress: Int = 0, // scroll percentage 0..100
    val pdfProgress: Int = 0, // opened pages count or percentage
    val imageProgress: Int = 0, // viewed images index or count or percentage
    val quizPassed: Boolean = false,
    val earnedXP: Int = 0
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
    val readingTimeMinutes: Long = 0L,
    val totalXP: Long = 0L,
    val level: String = "Beginner",
    val completedCoursesCount: Int = 1,
    val currentStreak: Int = 5,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey val certificateId: String,
    val userName: String,
    val profileImage: String = "",
    val courseId: String,
    val courseName: String,
    val completionDate: Long = System.currentTimeMillis(),
    val completionPercentage: Int = 100,
    val hashSignature: String,
    val imagePath: String? = null,
    val pdfPath: String? = null
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val category: String, // Looksmaxxing, Fitness, Learning, Productivity, Health, Spiritual, Custom
    val type: String = "checkbox", // checkbox, numeric, timer, streak
    val icon: String = "Star",
    val color: Long = 0xFFD4AF37,
    val repeatSchedule: String = "daily", // comma-separated days or "daily"
    val reminderTime: String? = null, // e.g., "08:00"
    val dailyTargetValue: Float = 1.0f,
    val targetUnit: String = "times",
    val xpReward: Int = 10,
    val displayOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_logs")
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: String, // format "yyyy-MM-dd"
    val progressValue: Float = 0.0f,
    val isCompleted: Boolean = false,
    val earnedXP: Int = 0,
    val loggedAt: Long = System.currentTimeMillis()
)

data class HabitWithLog(
    val habit: HabitEntity,
    val log: HabitLogEntity?
)



