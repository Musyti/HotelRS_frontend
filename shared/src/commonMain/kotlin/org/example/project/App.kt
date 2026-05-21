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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russhwolf.settings.Settings
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
        val settings = remember { Settings() }

        // Явно указываем тип String?, чтобы убрать ошибку типов Any
        val savedToken: String? = settings.getStringOrNull("auth_token")
        val savedRole: String? = settings.getStringOrNull("auth_role")

        var auth by remember {
            mutableStateOf(
                if (savedToken != null && savedRole != null) {
                    AuthResponse(token = savedToken, role = savedRole, guestId = null)
                } else null
            )
        }

        var tickets by remember { mutableStateOf<List<TicketResponse>>(emptyList()) }
        var isLoading by rememberSaveable { mutableStateOf(false) }
        var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
        var showCreateTicket by rememberSaveable { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val platform = remember { getPlatform() }
        val apiClient = remember { ApiClient(platform.baseUrl) }

        LaunchedEffect(auth, showCreateTicket) {
            if (auth != null && auth!!.role == "GUEST" && !showCreateTicket) {
                isLoading = true
                try {
                    tickets = apiClient.getGuestTickets(auth!!.token)
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
                                    settings.putString("auth_token", response.token)
                                    settings.putString("auth_role", response.role)
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
                        authToken = auth!!.token,
                        onLogout = {
                            settings.remove("auth_token")
                            settings.remove("auth_role")
                            auth = null
                        }
                    )
                }

                showCreateTicket -> {
                    CreateTicketScreen(
                        onCreate = { categoryId, description ->
                            scope.launch {
                                isLoading = true
                                try {
                                    apiClient.createTicket(auth!!.token, CreateTicketRequest(categoryId, description))
                                    showCreateTicket = false
                                    tickets = apiClient.getGuestTickets(auth!!.token)
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
                                    tickets = apiClient.getGuestTickets(auth!!.token)
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = "Ошибка загрузки: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        onLogout = {
                            settings.remove("auth_token")
                            settings.remove("auth_role")
                            auth = null
                        }
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
    var type by rememberSaveable { mutableStateOf("STAFF") }
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

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

        val currentError = errorMessage
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            currentError != null -> {
                Text(text = currentError, color = MaterialTheme.colorScheme.error)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketScreen(
    onCreate: (categoryId: Int, description: String) -> Unit,
    onBack: () -> Unit
) {
    val categories = listOf(
        1 to "Уборка номера",
        2 to "Технический ремонт",
        3 to "Рум-сервис",
        4 to "Вопросы"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var description by remember { mutableStateOf("") }

    // ДОБАВЛЕНО: Флаг отправки, чтобы предотвратить спам кликами
    var isSending by remember { mutableStateOf(false) }

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

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (!isSending) expanded = !expanded }, // Блокируем меню при отправке
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory.second,
                onValueChange = {},
                readOnly = true,
                label = { Text("Категория услуги") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSending
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
            shape = RoundedCornerShape(12.dp),
            enabled = !isSending // Отключаем ввод во время отправки
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSending
            ) {
                Text("Назад")
            }

            Button(
                onClick = {
                    if (description.isNotBlank() && !isSending) {
                        isSending = true // Сразу блокируем кнопку
                        onCreate(selectedCategory.first, description)
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSending && description.isNotBlank()
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Отправить")
                }
            }
        }
    }
}

// ====== ОБНОВЛЕННАЯ АДМИН-ПАНЕЛЬ С ИНТЕРАКТИВОМ ======
@Composable
fun AdminScreen(
    apiClient: ApiClient,
    authToken: String,
    onLogout: () -> Unit
) {
    var tickets by remember { mutableStateOf<List<TicketResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val refreshTickets = {
        isLoading = true
        scope.launch {
            try {
                tickets = apiClient.getAllTickets(authToken, statusFilter)
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Ошибка загрузки: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(statusFilter) {
        refreshTickets()
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
                    label = { Text(text = label, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentError = errorMessage
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (currentError != null) {
            Text(text = currentError, color = MaterialTheme.colorScheme.error)
        } else if (tickets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Заявок в этой категории нет", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tickets) { ticket ->
                    AdminTicketCard(
                        ticket = ticket,
                        onStatusChange = { newStatus ->
                            scope.launch {
                                try {
                                    // ИСПРАВЛЕНО: Передаем authToken первым параметром!
                                    apiClient.updateTicketStatus(authToken, ticket.id, newStatus)
                                    refreshTickets() // Перезапрашиваем список, чтобы статус обновился на экране
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = "Не удалось обновить статус: ${e.message}"
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                try {
                                    // ИСПРАВЛЕНО: Передаем authToken для удаления
                                    apiClient.deleteTicket(authToken, ticket.id)
                                    refreshTickets()
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = "Не удалось удалить заявку: ${e.message}"
                                }
                            }
                        }
                    )
                }
            }

        }
    }
}

// ====== ОБНОВЛЕННАЯ КАРТОЧКА АДМИНА С КНОПКАМИ ДЕЙСТВИЙ ======
@Composable
fun AdminTicketCard(
    ticket: TicketResponse,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    maxLines = 1,
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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp) // ИСПРАВЛЕНО: HorizontalDivider вместо Divider
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (ticket.status) {
                        "NEW" -> {
                            Button(
                                onClick = { onStatusChange("IN_PROGRESS") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                // ИСПРАВЛЕНО: Использование текста вместо Vector-иконки
                                Text("▶ В работу", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        "IN_PROGRESS" -> {
                            Button(
                                onClick = { onStatusChange("COMPLETED") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                // ИСПРАВЛЕНО: Использование текста вместо Vector-иконки
                                Text("✓ Завершить", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        "COMPLETED" -> {
                            Text(
                                text = "Архивировано",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ИСПРАВЛЕНО: Текстовая кнопка-эмодзи вместо IconButton со сломанным ресурсом
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("❌", fontSize = 16.sp)
                }
            }
        }
    }
}