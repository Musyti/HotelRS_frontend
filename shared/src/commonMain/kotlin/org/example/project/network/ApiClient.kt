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
            // Динамически подтягиваем актуальный токен авторизации для каждого запроса
            authToken?.let {
                header("Authorization", "Bearer $it")
            }
        }
    }


    // Авторизация - сохраняем полученный токен
    suspend fun login(request: LoginRequest): AuthResponse {
        val response = client.post {
            url { appendPathSegments("api", "auth", "login") }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<AuthResponse>()

        println("=== LOGIN SUCCESS ===")
        println("Получен токен: ${response.token.take(50)}...")
        println("Роль: ${response.role}, guestId: ${response.guestId}")

        setAuthToken(response.token)
        println("Токен сохранён в ApiClient. authToken = ${authToken?.take(50)}")

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

    // Получение заявок для сотрудников на смене
    suspend fun getStaffTickets(status: String? = null): List<TicketResponse> {
        return client.get {
            url {
                appendPathSegments("api", "staff", "tickets")
                if (status != null) {
                    parameters.append("status", status)
                }
            }
        }.body()
    }

    // Получение всех гостей (админ)
    suspend fun getAllGuests(): List<Guest> {
        return client.get {
            url {
                appendPathSegments("api", "admin", "guests")
            }
        }.body()
    }
    fun isTokenPresent(): Boolean = !authToken.isNullOrEmpty()
    fun getAuthToken(): String? = authToken

    // Получение всех сотрудников (админ)
    suspend fun getAllStaff(): List<StaffUser> {
        val token = authToken  // сохраняем в локальную переменную

        println("=== getAllStaff() ===")
        println("Токен из authToken: ${token?.take(50)}")

        return client.get {
            url {
                appendPathSegments("api", "admin", "staff")
            }
            // Принудительно добавляем токен в заголовок
            if (token != null) {
                header("Authorization", "Bearer $token")
            } else {
                println("⚠️ Токен NULL! Запрос без авторизации")
            }
        }.body()
    }

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

    // Обновление гостя (админ)
    suspend fun updateGuest(guestId: Int, guest: UpdateGuestRequest): Map<String, String> {
        return client.put {
            url {
                appendPathSegments("api", "admin", "guests", guestId.toString())
            }
            contentType(ContentType.Application.Json)
            setBody(guest)
        }.body()
    }

    // Обновление сотрудника (админ)
    suspend fun updateStaff(staffId: Int, staff: UpdateStaffRequest): Map<String, String> {
        return client.put {
            url {
                appendPathSegments("api", "admin", "staff", staffId.toString())
            }
            contentType(ContentType.Application.Json)
            setBody(staff)
        }.body()
    }

    // Обновление статуса заявки (сотрудник/админ)
    suspend fun updateTicketStatus(ticketId: Int, status: String): Map<String, String> {
        return client.put {
            url {
                appendPathSegments("api", "tickets", ticketId.toString(), "status")
            }
            contentType(ContentType.Application.Json)
            setBody(mapOf("status" to status))
        }.body()
    }

    // Удаление заявки (админ)
    suspend fun deleteTicket(ticketId: Int): Map<String, String> {
        return client.delete {
            url {
                appendPathSegments("api", "tickets", ticketId.toString())
            }
        }.body()
    }

    // Удаление гостя (админ)
    suspend fun deleteGuest(guestId: Int): Map<String, String> {
        return client.delete {
            url {
                appendPathSegments("api", "admin", "guests", guestId.toString())
            }
        }.body()
    }

    // Удаление сотрудника (админ)
    suspend fun deleteStaff(staffId: Int): Map<String, String> {
        return client.delete {
            url {
                appendPathSegments("api", "admin", "staff", staffId.toString())
            }
        }.body()
    }
}