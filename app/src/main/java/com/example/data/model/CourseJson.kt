package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CourseJson(
    val id: String?,
    val title: String?,
    val author: String?,
    val description: String?,
    val thumbnail: String?,
    val version: Int?,
    val modules: List<ModuleJson>?
)

@JsonClass(generateAdapter = true)
data class ModuleJson(
    val title: String?,
    val lessons: List<LessonJson>?
)

@JsonClass(generateAdapter = true)
data class LessonJson(
    val id: String?,
    val title: String?,
    val video: String?,
    val note: String?,
    val subtitle: String?,
    val duration: Int?
)
