package com.manichord.mgit.models

enum class AccountType {
    GITHUB,
    GITLAB,
    BITBUCKET,
    CUSTOM
}

data class Account(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val username: String,
    val token: String,
    val type: AccountType = AccountType.GITHUB
)
