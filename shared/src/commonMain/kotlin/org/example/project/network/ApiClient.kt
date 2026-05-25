package org.example.project.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
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
    private var authToken: String? = null

    fun setAuthToken(token: String) {
        this.authToken = token
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        defaultRequest {
            url(baseUrl)
            // Добавляем заголовок авторизации, если токен есть
            authToken?.let {
                header("Authorization", "Bearer $it")
            }
        }
    }



    // Авторизация - сохраняем токен
    suspend fun login(request: LoginRequest): AuthResponse {
        val response = client.post {
            url {
                appendPathSegments("api", "auth", "login")
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<AuthResponse>()

        // Сохраняем токен для последующих запросов
        setAuthToken(response.token)

        return response
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

    // Получение всех заявок (для админа) - теперь с токеном
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

    // Удаление заявки
    suspend fun deleteTicket(ticketId: Int): Map<String, String> {
        return client.delete {
            url {
                appendPathSegments("api", "tickets", ticketId.toString())
            }
        }.body()
    }

    // Добавьте эти методы в ApiClient.kt

    // Создание гостя (админ)
    suspend fun createGuest(guest: CreateGuestRequest): Map<String, String> {
        return client.post {
            url {
                appendPathSegments("api", "admin", "guests")
            }
            contentType(ContentType.Application.Json)
            setBody(guest)
        }.body()
    }

    // Создание сотрудника (админ)
    suspend fun createStaff(staff: CreateStaffRequest): Map<String, String> {
        return client.post {
            url {
                appendPathSegments("api", "admin", "staff")
            }
            contentType(ContentType.Application.Json)
            setBody(staff)
        }.body()
    }
}