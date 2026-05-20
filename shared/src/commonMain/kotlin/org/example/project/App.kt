package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.models.*
import org.example.project.network.ApiClient

// ====== НАСТРОЙКА СИНЕЙ ТЕМЫ (Material 3) ======
private val DeepBluePrimary = Color(0xFF005FAF)
private val LightBlueSecondary = Color(0xFFE1F5FE)
private val MintTertiary = Color(0xFFE8F5E9)
private val GraySurface = Color(0xFFF5F5F5)
private val BackgroundColor = Color(0xFFFAFAFA)

private val BlueColorScheme = lightColorScheme(
    primary = DeepBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E8FF),
    background = BackgroundColor,
    surface = Color.White,
    error = Color(0xFFBA1A1A)
)

@Composable
fun App() {
    MaterialTheme(colorScheme = BlueColorScheme) {
        var auth by remember { mutableStateOf<AuthResponse?>(null) }
        var tickets by remember { mutableStateOf<List<TicketResponse>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var showCreateTicket by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val platform = remember { getPlatform() }
        val apiClient = remember { ApiClient(platform.baseUrl) }

        LaunchedEffect(auth, showCreateTicket) {
            if (auth != null && auth!!.role == "GUEST" && !showCreateTicket) {
                isLoading = true
                try {
                    tickets = apiClient.getGuestTickets(auth!!.guestId!!)
                    errorMessage = null
                } catch (e: Exception) {
                    errorMessage = "Ошибка загрузки: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            when {
                auth == null -> {
                    LoginScreen(
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onLogin = { type, identifier, password ->
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val response = apiClient.login(
                                        LoginRequest(type, identifier, password)
                                    )
                                    auth = response
                                } catch (e: Exception) {
                                    errorMessage = "Ошибка входа: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    )
                }

                auth!!.role == "ADMIN" || auth!!.role == "STAFF" -> {
                    AdminScreen(
                        apiClient = apiClient,
                        onLogout = { auth = null }
                    )
                }

                showCreateTicket -> {
                    CreateTicketScreen(
                        onCreate = { categoryId, description ->
                            scope.launch {
                                isLoading = true
                                try {
                                    apiClient.createTicket(auth!!.guestId!!, CreateTicketRequest(categoryId, description))
                                    showCreateTicket = false
                                    tickets = apiClient.getGuestTickets(auth!!.guestId!!)
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = "Ошибка создания: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        onBack = { showCreateTicket = false }
                    )
                }

                else -> {
                    GuestTicketsScreen(
                        tickets = tickets,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onCreateClick = { showCreateTicket = true },
                        onRefresh = {
                            scope.launch {
                                isLoading = true
                                try {
                                    tickets = apiClient.getGuestTickets(auth!!.guestId!!)
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = "Ошибка загрузки: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        onLogout = { auth = null }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (type: String, identifier: String, password: String?) -> Unit
) {
    var type by remember { mutableStateOf("STAFF") }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HotelApp Вход",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = type == "GUEST",
                        onClick = { type = "GUEST" },
                        label = { Text("Я гость") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterChip(
                        selected = type == "STAFF",
                        onClick = { type = "STAFF" },
                        label = { Text("Сотрудник") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text(if (type == "GUEST") "Номер телефона" else "Логин") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (type != "GUEST") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Вход по номеру телефона, указанному при заселении",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        val pass = if (type == "GUEST") null else password
                        onLogin(type, identifier, pass)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Войти в личный кабинет", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun GuestTicketsScreen(
    tickets: List<TicketResponse>,
    isLoading: Boolean,
    errorMessage: String?,
    onCreateClick: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Мои заявки",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onLogout) {
                Text("Выйти", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCreateClick,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Создать заявку")
            }

            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Обновить")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
            tickets.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("У вас пока нет активных заявок", color = Color.Gray)
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(tickets) { ticket ->
                        TicketCard(ticket = ticket)
                    }
                }
            }
        }
    }
}

@Composable
fun TicketCard(ticket: TicketResponse) {
    val cardColor = when (ticket.status) {
        "NEW" -> LightBlueSecondary
        "IN_PROGRESS" -> MintTertiary
        "COMPLETED" -> GraySurface
        else -> Color.White
    }

    val statusText = when (ticket.status) {
        "NEW" -> "Новая"
        "IN_PROGRESS" -> "В работе"
        "COMPLETED" -> "Выполнена"
        else -> ticket.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = ticket.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = when (ticket.status) {
                        "NEW" -> DeepBluePrimary
                        "IN_PROGRESS" -> Color(0xFF2E7D32)
                        else -> Color.Gray
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = ticket.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Создана: ${ticket.createdAt}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

// ====== ИСПРАВЛЕНО: ТЕПЕРЬ ТУТ ВЫПАДАЮЩИЙ СПИСОК (DROPDOWN MENU) ======
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketScreen(
    onCreate: (categoryId: Int, description: String) -> Unit,
    onBack: () -> Unit
) {
    // Список категорий, соответствующий твоей структуре БД
    val categories = listOf(
        1 to "Уборка номера",
        2 to "Технический ремонт",
        3 to "Рум-сервис",
        4 to "Вопросы"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(categories[0]) } // По умолчанию первая
    var description by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Новая заявка",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Выберите категорию:", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Material 3 Выпадающий список (ExposedDropdownMenuBox)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory.second, // Отображаем название текстом
                onValueChange = {},
                readOnly = true, // Запрещаем ввод с клавиатуры
                label = { Text("Категория услуги") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.second) },
                        onClick = {
                            selectedCategory = category
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Что необходимо сделать?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Назад")
            }

            Button(
                onClick = {
                    if (description.isNotBlank()) {
                        onCreate(selectedCategory.first, description) // Передаем чистый ID в бэкенд
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Отправить")
            }
        }
    }
}

@Composable
fun AdminScreen(
    apiClient: ApiClient,
    onLogout: () -> Unit
) {
    var tickets by remember { mutableStateOf<List<TicketResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statusFilter) {
        isLoading = true
        try {
            tickets = apiClient.getAllTickets(statusFilter)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Ошибка загрузки: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Админ-Панель",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onLogout) {
                Text("Выйти", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ====== ИСПРАВЛЕНО: ТЕПЕРЬ ТЕКСТ «В РАБОТЕ» НЕ СЪЕЗЖАЕТ ======
        // Добавили горизонтальный скролл. На маленьких экранах чипы можно плавно листать пальцем, они сохраняют красивую ширину
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(null to "Все", "NEW" to "Новые", "IN_PROGRESS" to "В работе", "COMPLETED" to "Готово")
            filters.forEach { (filter, label) ->
                FilterChip(
                    selected = statusFilter == filter,
                    onClick = { statusFilter = filter },
                    label = {
                        Text(
                            text = label,
                            maxLines = 1 // Строго запрещаем перенос на другую строку
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tickets) { ticket ->
                    AdminTicketCard(ticket = ticket)
                }
            }
        }
    }
}

@Composable
fun AdminTicketCard(ticket: TicketResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically // Выравниваем текст по центру строки
            ) {
                // Ограничиваем имя гостя, чтобы оно не выталкивало статус
                Text(
                    text = "${ticket.guestName} (Комн. ${ticket.roomNumber})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (ticket.status) {
                        "NEW" -> "Новая"
                        "IN_PROGRESS" -> "В работе"
                        "COMPLETED" -> "Выполнена"
                        else -> ticket.status
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, // Чтобы статус внутри карточки тоже никогда не рвало на куски
                    color = when (ticket.status) {
                        "NEW" -> DeepBluePrimary
                        "IN_PROGRESS" -> Color(0xFF2E7D32)
                        else -> Color.Gray
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Категория: ${ticket.categoryName}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = ticket.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}