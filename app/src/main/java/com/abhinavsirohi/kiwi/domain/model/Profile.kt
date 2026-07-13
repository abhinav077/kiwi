package com.abhinavsirohi.kiwi.domain.model

data class Profile(
    val localId: String,
    val userId: String,
    val preferredName: String,
    val createdAt: Long,
    val updatedAt: Long,
)
