package com.example.hoppie.features.home

internal data class RecentFile(
    val name: String,
    val size: String
)

internal val sampleRecentFiles = listOf(
    RecentFile("Lecture10.pdf", "12.8 MB"),
    RecentFile("Lecture10.docx", "12.8 MB"),
    RecentFile("Lecture10.png", "12.8 MB")
)
