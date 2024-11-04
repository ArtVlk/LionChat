package com.example.lionchat.services

import com.example.lionchat.entities.User

interface UserRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(name: String, email: String, password: String): Result<Unit>
}