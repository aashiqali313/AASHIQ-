package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY importTimestamp DESC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseById(id: String): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourseById(courseId: String)

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()
}

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules WHERE courseId = :courseId ORDER BY orderIndex ASC")
    fun getModulesForCourse(courseId: String): Flow<List<ModuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<ModuleEntity>)

    @Query("DELETE FROM modules WHERE courseId = :courseId")
    suspend fun deleteModulesForCourse(courseId: String)

    @Query("DELETE FROM modules")
    suspend fun deleteAllModules()
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons")
    fun getAllLessonsFlow(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE moduleId = :moduleId ORDER BY orderIndex ASC")
    fun getLessonsForModule(moduleId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex ASC")
    fun getLessonsForCourse(courseId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex ASC")
    suspend fun getLessonsForCourseDirect(courseId: String): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun getLessonById(id: String): LessonEntity?

    @Query("SELECT * FROM lessons WHERE isBookmarked = 1 ORDER BY lastWatchedTimestamp DESC")
    fun getBookmarkedLessons(): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Update
    suspend fun updateLesson(lesson: LessonEntity)

    @Query("UPDATE lessons SET isBookmarked = :isBookmarked WHERE id = :lessonId")
    suspend fun updateBookmark(lessonId: String, isBookmarked: Boolean)

    @Query("UPDATE lessons SET progressMs = :progressMs, playProgressPercent = :percent, lastWatchedTimestamp = :timestamp WHERE id = :lessonId")
    suspend fun updateProgress(lessonId: String, progressMs: Long, percent: Int, timestamp: Long)

    @Query("SELECT * FROM lessons WHERE lastWatchedTimestamp > 0 ORDER BY lastWatchedTimestamp DESC")
    fun getContinueWatchingLessons(): Flow<List<LessonEntity>>
    
    @Query("DELETE FROM lessons WHERE courseId = :courseId")
    suspend fun deleteLessonsForCourse(courseId: String)

    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
}

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 15")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun deleteAllSearches()
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 'app_config' LIMIT 1")
    fun getSettingsFlow(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 'app_config' LIMIT 1")
    suspend fun getSettingsDirect(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: UserSettingsEntity)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 'main_user' LIMIT 1")
    fun getProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'main_user' LIMIT 1")
    suspend fun getProfileDirect(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)
}

@Dao
interface CertificateDao {
    @Query("SELECT * FROM certificates ORDER BY completionDate DESC")
    fun getAllCertificates(): Flow<List<CertificateEntity>>

    @Query("SELECT * FROM certificates WHERE courseId = :courseId LIMIT 1")
    suspend fun getCertificateByCourse(courseId: String): CertificateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificate(certificate: CertificateEntity)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY displayOrder ASC, createdAt DESC")
    fun getAllHabitsFlow(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY displayOrder ASC, createdAt DESC")
    suspend fun getAllHabitsDirect(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    @Update
    suspend fun updateHabit(habit: HabitEntity)
}

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getLogsForDateFlow(date: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    suspend fun getLogsForDateDirect(date: String): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLogForHabitAndDate(habitId: String, date: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun getLogsForHabitFlow(habitId: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getLogsForHabitDirect(habitId: String): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs")
    fun getAllLogsFlow(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllLogsDirect(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: String)
}

