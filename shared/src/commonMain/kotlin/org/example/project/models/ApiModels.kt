package org.example.project.models

import kotlinx.serialization.Serializable

// Совпадает с серверным LoginRequest
@Serializable
data class LoginRequest(
    val type: String,
    val identifier: String,
    val password: String? = null
)

// Совпадает с серверным AuthResponse
@Serializable
data class AuthResponse(
    val token: String,
    val role: String,
    val guestId: Int? = null
)

// Совпадает с серверным CreateTicketRequest
@Serializable
data class CreateTicketRequest(
    val categoryId: Int,
    val description: String
)

// Совпадает с серверным TicketResponse (НЕ TicketDto!)
@Serializable
data class TicketResponse(
    val id: Int,
    val guestName: String,
    val roomNumber: String,
    val categoryName: String,
    val description: String,
    val status: String,
    val createdAt: String
)

// Для создания сотрудника (если понадобится)
@Serializable
data class CreateStaffRequest(
    val username: String,
    val password: String,
    val role: String
)

// Для обновления статуса
@Serializable
data class UpdateStatusRequest(
    val status: String,
    val assignedStaffId: Int? = null
)