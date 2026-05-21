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
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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

    // Получение заявок гостя
    suspend fun getGuestTickets(token: String): List<TicketResponse> {
        return client.get {
            url {
                // Добавляем "guest", чтобы путь стал /api/guest/tickets
                appendPathSegments("api", "guest", "tickets")
            }
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    // Метод создания заявки (гость)
    suspend fun createTicket(token: String, request: CreateTicketRequest): String {
                return client.post("$baseUrl/api/tickets") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.bodyAsText() // Просто читаем текстовый ответ сервера, чтобы Ktor не спотыкался о десериализацию
    }

    // Получение всех заявок (для админа) - ИСПРАВЛЕНО ТУТ
    suspend fun getAllTickets(token: String, status: String?): List<TicketResponse> {
        return client.get {
            url {
                appendPathSegments("api", "admin", "tickets") // Путь настраиваем тут
                if (!status.isNullOrBlank()) {
                    parameters.append("status", status)      // Параметр добавляется безопасно
                }
            }
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    // Обновление статуса заявки
    suspend fun updateTicketStatus(token: String, ticketId: Int, newStatus: String): String {
                return client.put("$baseUrl/api/tickets/$ticketId/status") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    // Передаем статус в теле запроса, как просит бэкенд
                    setBody(mapOf("status" to newStatus))
                }.body() // Или bodyAsText(), если используешь Ktor 2.x/3.x
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
    suspend fun deleteTicket(token: String, ticketId: Int): String {
        return client.delete("$baseUrl/api/tickets/$ticketId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }
}