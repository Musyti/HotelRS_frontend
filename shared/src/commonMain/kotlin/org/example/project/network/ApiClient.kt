package org.example.project.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.models.*

class ApiClient(
    private val baseUrl: String
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        defaultRequest {
            url(baseUrl)
        }
    }

    // Авторизация
    suspend fun login(request: LoginRequest): AuthResponse {
        return client.post {
            url {
                appendPathSegments("api", "auth", "login")
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // Создание заявки (для гостя)
    suspend fun createTicket(guestId: Int, request: CreateTicketRequest): Map<String, String> {
        return client.post {
            url {
                appendPathSegments("api", "tickets")
            }
            contentType(ContentType.Application.Json)
            header("X-Guest-Id", guestId.toString())
            setBody(request)
        }.body()
    }

    // Получение заявок гостя
    suspend fun getGuestTickets(guestId: Int): List<TicketResponse> {
        return client.get {
            url {
                appendPathSegments("api", "guest", "tickets")
            }
            header("X-Guest-Id", guestId.toString())
        }.body()
    }

    // Получение всех заявок (для админа)
    suspend fun getAllTickets(status: String? = null): List<TicketResponse> {
        return client.get {
            url {
                appendPathSegments("api", "admin", "tickets")
                if (status != null) {
                    parameters.append("status", status)
                }
            }
        }.body()
    }

    // Обновление статуса заявки
    suspend fun updateTicketStatus(ticketId: Int, status: String): Map<String, String> {
        return client.put {
            url {
                appendPathSegments("api", "tickets", ticketId.toString(), "status")
            }
            contentType(ContentType.Application.Json)
            setBody(mapOf("status" to status))
        }.body()
    }

    // Создание сотрудника (для админа)
    suspend fun createStaff(request: CreateStaffRequest): Map<String, String> {
        return client.post {
            url {
                appendPathSegments("api", "admin", "staff")
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}