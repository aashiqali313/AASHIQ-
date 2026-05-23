package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY dateImported DESC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseById(id: String): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<ModuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourseOnly(courseId: String)

    @Query("DELETE FROM modules WHERE courseId = :courseId")
    suspend fun deleteModulesForCourse(courseId: String)

    @Query("DELETE FROM lessons WHERE courseId = :courseId")
    suspend fun deleteLessonsForCourse(courseId: String)

    @Transaction
    suspend fun deleteCourseCascade(courseId: String) {
        deleteLessonsForCourse(courseId)
        deleteModulesForCourse(courseId)
        deleteCourseOnly(courseId)
    }

    @Query("SELECT * FROM modules WHERE courseId = :courseId")
    fun getModulesForCourse(courseId: String): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM lessons WHERE courseId = :courseId")
    fun getLessonsForCourse(courseId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE courseId = :courseId")
    suspend fun getLessonsForCourseSync(courseId: String): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    fun getLessonById(lessonId: String): Flow<LessonEntity?>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getLessonByIdSync(lessonId: String): LessonEntity?
}

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE courseId = :courseId")
    fun getProgressForCourse(courseId: String): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE lessonId = :lessonId LIMIT 1")
    fun getProgressForLesson(lessonId: String): Flow<PlaybackProgressEntity?>

    @Query("SELECT * FROM playback_progress WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getProgressForLessonSync(lessonId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress ORDER BY lastWatched DESC")
    fun getContinueWatching(): Flow<List<PlaybackProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: PlaybackProgressEntity)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE courseId = :courseId ORDER BY timestamp ASC")
    fun getBookmarksForCourse(courseId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE lessonId = :lessonId ORDER BY timestamp ASC")
    fun getBookmarksForLesson(lessonId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Int)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SettingsEntity)
}
