package com.manichord.mgit.models

data class GitHubRepo(
    val name: String,
    val fullName: String,
    val description: String?,
    val cloneUrl: String,
    val stars: Int,
    val language: String?
)
