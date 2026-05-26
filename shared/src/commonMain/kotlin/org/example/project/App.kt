package org.example.project

import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.models.*
import org.example.project.network.ApiClient

// ====== ПРЕМИУМ МИНИМАЛИСТИЧНАЯ ТЕМА ======
private val DeepBluePrimary = Color(0xFF0066FF)
private val DeepBlueDark = Color(0xFF0052CC)
private val LightBlueBg = Color(0xFFF0F7FF)
private val SuccessGreen = Color(0xFF34C759)
private val WarningOrange = Color(0xFFFF9500)
private val ErrorRed = Color(0xFFFF3B30)
private val NeutralGray = Color(0xFF8E8E93)
private val DarkGray = Color(0xFF1C1C1E)

private val PremiumColorScheme = lightColorScheme(
    primary = DeepBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FF),
    secondary = SuccessGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E9),
    tertiary = WarningOrange,
    background = Color(0xFFF8F9FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF2F4F8),
    error = ErrorRed,
    outline = Color(0xFFE5E9F0)
)

fun isValidPhone(phone: String): Boolean {
    val digitsOnly = phone.replace(Regex("[^\\d+]"), "")
    val normalized = digitsOnly.replace(Regex("^\\+"), "")
    return normalized.length in 10..15 && digitsOnly.matches(Regex("^\\+?[0-9]+$"))
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme(colorScheme = PremiumColorScheme, typography = PremiumTypography()) {
        var auth by remember { mutableStateOf<AuthResponse?>(null) }
        var tickets by remember { mutableStateOf<List<TicketResponse>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var showCreateTicket by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val platform = remember { getPlatform() }
        val apiClient = remember { ApiClient(platform.baseUrl) }
        LaunchedEffect(auth) {
            if (auth != null) {
                apiClient.setAuthToken(auth!!.token)
            }
        }

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
                .background(PremiumColorScheme.background)
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

                auth!!.role == "ADMIN" || auth!!.role == "STAFF" || auth!!.role == "CLEANER" || auth!!.role == "MASTER" -> {
                    AdminScreen(
                        apiClient = apiClient,
                        onLogout = { auth = null },
                        currentUserRole = auth!!.role
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
private fun PremiumTypography() = Typography(
    headlineLarge = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    titleLarge = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
    bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
    labelLarge = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
)

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (type: String, identifier: String, password: String?) -> Unit
) {
    var passwordVisibility by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("STAFF") }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PremiumColorScheme.background, Color.White),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                color = PremiumColorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Hotel,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Добро пожаловать",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = PremiumColorScheme.primary
            )

            Text(
                text = "Войдите в свой аккаунт",
                style = MaterialTheme.typography.bodyLarge,
                color = NeutralGray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = type == "GUEST",
                            onClick = { type = "GUEST" },
                            label = { Text("Я гость") },
                            enabled = true,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = type == "STAFF",
                            onClick = { type = "STAFF" },
                            label = { Text("Сотрудник") },
                            enabled = true,
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
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PremiumColorScheme.primary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PremiumColorScheme.primary,
                            unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f)
                        )
                    )

                    if (type != "GUEST") {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PremiumColorScheme.primary
                                )
                            },
                            trailingIcon = {
                                val image = if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                val description = if (passwordVisibility) "Скрыть пароль" else "Показать пароль"
                                IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                                    Icon(imageVector = image, contentDescription = description, tint = PremiumColorScheme.primary)
                                }
                            },
                            visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PremiumColorScheme.primary,
                                unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f)
                            )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Вход по номеру телефона, указанному при заселении",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralGray
                        )
                        if (phoneError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = phoneError!!,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            if (type == "GUEST") {
                                if (!isValidPhone(identifier)) {
                                    phoneError = "Введите корректный номер телефона (10–15 цифр)"
                                    return@Button
                                }
                                phoneError = null
                                onLogin(type, identifier, null)
                            } else {
                                if (identifier.isBlank() || password.isBlank()) {
                                    return@Button
                                }
                                onLogin(type, identifier, password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremiumColorScheme.primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Войти",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Мои заявки",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PremiumColorScheme.primary
            )
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = "Выйти", tint = NeutralGray)
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PremiumColorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Создать")
            }

            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Обновить")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PremiumColorScheme.primary)
                }
            }
            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            tickets.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = NeutralGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "У вас пока нет заявок",
                            color = NeutralGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Нажмите «Создать», чтобы оставить заявку",
                            color = NeutralGray.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tickets) { ticket ->
                        GuestTicketCard(ticket = ticket)
                    }
                }
            }
        }
    }
}

@Composable
fun GuestTicketCard(ticket: TicketResponse) {
    val statusConfig = when (ticket.status) {
        "NEW" -> StatusConfig("Новая", PremiumColorScheme.primary, PremiumColorScheme.primaryContainer)
        "IN_PROGRESS" -> StatusConfig("В работе", WarningOrange, WarningOrange.copy(alpha = 0.1f))
        "COMPLETED" -> StatusConfig("Выполнена", SuccessGreen, SuccessGreen.copy(alpha = 0.1f))
        else -> StatusConfig(ticket.status, NeutralGray, NeutralGray.copy(alpha = 0.1f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkGray
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusConfig.backgroundColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusConfig.text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = statusConfig.color,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                color = NeutralGray
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = ticket.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeutralGray.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun TicketCard(ticket: TicketResponse) {
    GuestTicketCard(ticket = ticket)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    apiClient: ApiClient,
    onLogout: () -> Unit,
    currentUserRole: String
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var tickets by remember { mutableStateOf<List<TicketResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isAdmin = currentUserRole == "ADMIN"

    fun loadTickets() {
        scope.launch {
            isLoading = true
            try {
                tickets = if (isAdmin) {
                    apiClient.getAllTickets(statusFilter)
                } else {
                    apiClient.getStaffTickets(statusFilter)
                }
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Ошибка загрузки: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(statusFilter) {
        loadTickets()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumColorScheme.background)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = PremiumColorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null,
                                tint = PremiumColorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAdmin) "Админ-панель" else "Панель сотрудника",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DarkGray
                        )
                        Text(
                            text = "${tickets.size} ${getDeclension(tickets.size)}",
                            color = NeutralGray,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Выйти", tint = NeutralGray)
                }
            }
        }
        if (isAdmin) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PremiumColorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Заявки") },
                    icon = { Icon(Icons.Default.List, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Пользователи") },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
            }
        }

        when {
            !isAdmin || selectedTab == 0 -> {
                StaffTicketsContent(
                    tickets = tickets,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    statusFilter = statusFilter,
                    onStatusFilterChange = { statusFilter = it },
                    onStatusChange = { ticket, newStatus ->
                        scope.launch {
                            isLoading = true
                            try {
                                apiClient.updateTicketStatus(ticket.id, newStatus)
                                showSnackbar = "Статус изменён на ${getStatusName(newStatus)}"
                                loadTickets()
                            } catch (e: Exception) {
                                showSnackbar = "Ошибка: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    onDelete = if (isAdmin) { { ticket ->
                        scope.launch {
                            isLoading = true
                            try {
                                apiClient.deleteTicket(ticket.id)
                                showSnackbar = "Заявка #${ticket.id} удалена"
                                loadTickets()
                            } catch (e: Exception) {
                                showSnackbar = "Ошибка удаления: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    } } else null
                )
            }
            selectedTab == 1 && isAdmin -> {
                UsersTab(
                    apiClient = apiClient,
                    onShowSnackbar = { message -> showSnackbar = message },
                    authToken = apiClient.getAuthToken()
                )
            }
        }
    }

    showSnackbar?.let { message ->
        LaunchedEffect(message) {
            delay(2000)
            showSnackbar = null
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .wrapContentWidth(),
                shape = RoundedCornerShape(8.dp),
                color = DarkGray,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showSnackbar = null }) {
                        Text("OK", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun StaffTicketsContent(
    tickets: List<TicketResponse>,
    isLoading: Boolean,
    errorMessage: String?,
    statusFilter: String?,
    onStatusFilterChange: (String?) -> Unit,
    onStatusChange: (TicketResponse, String) -> Unit,
    onDelete: ((TicketResponse) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = "Фильтр",
                color = NeutralGray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    null to "Все",
                    "NEW" to "Новые",
                    "IN_PROGRESS" to "В работе",
                    "COMPLETED" to "Выполнены"
                )
                filters.forEach { (filter, label) ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { onStatusFilterChange(filter) },
                        label = { Text(label) },
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PremiumColorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PremiumColorScheme.primary)
                    }
                }
                errorMessage != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                    ) {
                        Text(text = errorMessage, color = ErrorRed, modifier = Modifier.padding(16.dp))
                    }
                }
                tickets.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Нет заявок", color = NeutralGray)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        items(tickets, key = { it.id }) { ticket ->
                            AdminTicketCard(
                                ticket = ticket,
                                onStatusChange = { newStatus -> onStatusChange(ticket, newStatus) },
                                onDelete = if (onDelete != null) { { onDelete(ticket) } } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketsTab(
    tickets: List<TicketResponse>,
    isLoading: Boolean,
    errorMessage: String?,
    statusFilter: String?,
    onStatusFilterChange: (String?) -> Unit,
    onStatusChange: (TicketResponse, String) -> Unit,
    onDelete: (TicketResponse) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = "Фильтр",
                color = NeutralGray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    null to "Все",
                    "NEW" to "Новые",
                    "IN_PROGRESS" to "В работе",
                    "COMPLETED" to "Выполнены"
                )
                filters.forEach { (filter, label) ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { onStatusFilterChange(filter) },
                        label = { Text(label) },
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PremiumColorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PremiumColorScheme.primary)
                    }
                }
                errorMessage != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                    ) {
                        Text(text = errorMessage, color = ErrorRed, modifier = Modifier.padding(16.dp))
                    }
                }
                tickets.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Нет заявок", color = NeutralGray)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        items(tickets, key = { it.id }) { ticket ->
                            AdminTicketCard(
                                ticket = ticket,
                                onStatusChange = { newStatus -> onStatusChange(ticket, newStatus) },
                                onDelete = { onDelete(ticket) }
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun UsersTab(
    apiClient: ApiClient,
    authToken: String?,
    onShowSnackbar: (String) -> Unit
) {
    var showCreateGuestDialog by remember { mutableStateOf(false) }
    var showCreateStaffDialog by remember { mutableStateOf(false) }
    var showEditGuestDialog by remember { mutableStateOf<Guest?>(null) }
    var showEditStaffDialog by remember { mutableStateOf<StaffUser?>(null) }

    var guests by remember { mutableStateOf<List<Guest>>(emptyList()) }
    var staff by remember { mutableStateOf<List<StaffUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    fun loadUsers() {
        scope.launch {
            if (authToken == null) {
                onShowSnackbar("Ошибка: не выполнен вход")
                return@launch
            }
            isLoading = true
            try {
                val guestsResult = apiClient.getAllGuests()
                val staffResult = apiClient.getAllStaff()
                guests = guestsResult
                staff = staffResult
            } catch (e: Exception) {
                onShowSnackbar("Ошибка загрузки: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(authToken) {
        if (authToken != null) {
            loadUsers()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Управление пользователями",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PremiumColorScheme.primary,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showCreateGuestDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Добавить гостя", tint = PremiumColorScheme.primary)
                }
                IconButton(onClick = { showCreateStaffDialog = true }) {
                    Icon(Icons.Default.Badge, contentDescription = "Добавить сотрудника", tint = PremiumColorScheme.secondary)
                }
                IconButton(onClick = { loadUsers() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = NeutralGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = PremiumColorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Гости (${guests.size})") },
                icon = { Icon(Icons.Default.People, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Сотрудники (${staff.size})") },
                icon = { Icon(Icons.Default.Badge, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PremiumColorScheme.primary)
                }
            }
            selectedTab == 0 -> {
                if (guests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👥", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Нет гостей", color = NeutralGray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(guests, key = { it.id }) { guest ->
                            UserCard(
                                title = guest.fullName,
                                subtitle = "Телефон: ${guest.phone} | Комната: ${guest.roomNumber}",
                                status = if (guest.isActive) "Активен" else "Неактивен",
                                statusColor = if (guest.isActive) SuccessGreen else NeutralGray,
                                onEdit = { showEditGuestDialog = guest },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            apiClient.deleteGuest(guest.id)
                                            onShowSnackbar("Гость ${guest.fullName} удален")
                                            loadUsers()
                                        } catch (e: Exception) {
                                            onShowSnackbar("Ошибка: ${e.message}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            else -> {
                if (staff.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👔", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Нет сотрудников", color = NeutralGray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(staff, key = { it.id }) { staffMember ->
                            val isAdmin = staffMember.role == "ADMIN"
                            UserCard(
                                title = staffMember.username,
                                subtitle = "Роль: ${getRoleDisplayName(staffMember.role)}",
                                status = "Сотрудник",
                                statusColor = PremiumColorScheme.primary,
                                onEdit = if (!isAdmin) { { showEditStaffDialog = staffMember } } else null,
                                onDelete = if (!isAdmin) {
                                    {
                                        scope.launch {
                                            try {
                                                apiClient.deleteStaff(staffMember.id)
                                                onShowSnackbar("Сотрудник ${staffMember.username} удален")
                                                loadUsers()
                                            } catch (e: Exception) {
                                                onShowSnackbar("Ошибка: ${e.message}")
                                            }
                                        }
                                    }
                                } else null,
                                isAdmin = isAdmin
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateGuestDialog) {
        CreateGuestDialog(
            onDismiss = { showCreateGuestDialog = false },
            onCreate = { guest ->
                scope.launch {
                    try {
                        apiClient.createGuest(guest)
                        onShowSnackbar("Гость ${guest.fullName} успешно создан")
                        showCreateGuestDialog = false
                        loadUsers()
                    } catch (e: Exception) {
                        onShowSnackbar("Ошибка: ${e.message}")
                    }
                }
            }
        )
    }

    if (showCreateStaffDialog) {
        CreateStaffDialog(
            onDismiss = { showCreateStaffDialog = false },
            onCreate = { staffMember ->
                scope.launch {
                    try {
                        apiClient.createStaff(staffMember)
                        onShowSnackbar("Сотрудник ${staffMember.username} успешно создан")
                        showCreateStaffDialog = false
                        loadUsers()
                    } catch (e: Exception) {
                        onShowSnackbar("Ошибка: ${e.message}")
                    }
                }
            }
        )
    }

    if (showEditGuestDialog != null) {
        EditGuestDialog(
            guest = showEditGuestDialog!!,
            onDismiss = { showEditGuestDialog = null },
            onUpdate = { updatedGuest ->
                scope.launch {
                    try {
                        apiClient.updateGuest(updatedGuest.id, UpdateGuestRequest(
                            fullName = updatedGuest.fullName,
                            phone = updatedGuest.phone,
                            roomNumber = updatedGuest.roomNumber,
                            isActive = updatedGuest.isActive
                        ))
                        onShowSnackbar("Гость ${updatedGuest.fullName} обновлен")
                        showEditGuestDialog = null
                        loadUsers()
                    } catch (e: Exception) {
                        onShowSnackbar("Ошибка: ${e.message}")
                    }
                }
            }
        )
    }

    if (showEditStaffDialog != null) {
        EditStaffDialog(
            staff = showEditStaffDialog!!,
            onDismiss = { showEditStaffDialog = null },
            onUpdate = { updatedStaff ->
                scope.launch {
                    try {
                        apiClient.updateStaff(updatedStaff.id, UpdateStaffRequest(
                            username = updatedStaff.username,
                            role = updatedStaff.role
                        ))
                        onShowSnackbar("Сотрудник ${updatedStaff.username} обновлен")
                        showEditStaffDialog = null
                        loadUsers()
                    } catch (e: Exception) {
                        onShowSnackbar("Ошибка: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
fun UserCard(
    title: String,
    subtitle: String,
    status: String,
    statusColor: Color,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isAdmin: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkGray
                    )
                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PremiumColorScheme.primaryContainer
                        ) {
                            Text(
                                text = "ADMIN",
                                fontSize = 10.sp,
                                color = PremiumColorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = NeutralGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row {
                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = PremiumColorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getRoleDisplayName(role: String): String = when (role) {
    "ADMIN" -> "Администратор"
    "STAFF" -> "Сотрудник"
    "CLEANER" -> "Горничная"
    "MASTER" -> "Мастер"
    else -> role
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGuestDialog(
    guest: Guest,
    onDismiss: () -> Unit,
    onUpdate: (Guest) -> Unit
) {
    var fullName by remember { mutableStateOf(guest.fullName) }
    var phone by remember { mutableStateOf(guest.phone) }
    var roomNumber by remember { mutableStateOf(guest.roomNumber) }
    var isActive by remember { mutableStateOf(guest.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Редактировать гостя",
                fontWeight = FontWeight.Bold,
                color = PremiumColorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("ФИО гостя") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Номер телефона") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Номер комнаты") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = CheckboxDefaults.colors(checkedColor = PremiumColorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Активен", color = DarkGray)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        onUpdate(guest.copy(fullName = fullName, phone = phone, roomNumber = roomNumber, isActive = isActive))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumColorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Отмена")
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.widthIn(max = 400.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStaffDialog(
    staff: StaffUser,
    onDismiss: () -> Unit,
    onUpdate: (StaffUser) -> Unit
) {
    var username by remember { mutableStateOf(staff.username) }
    var selectedRole by remember { mutableStateOf(staff.role) }
    var expanded by remember { mutableStateOf(false) }

    val roles = listOf(
        "STAFF" to "👔 Сотрудник",
        "CLEANER" to "🧹 Горничная",
        "MASTER" to "🔧 Мастер"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Редактировать сотрудника",
                fontWeight = FontWeight.Bold,
                color = PremiumColorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Логин") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Роль", fontSize = 14.sp, color = NeutralGray)
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = roles.find { it.first == selectedRole }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PremiumColorScheme.primary,
                            unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { (role, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedRole = role
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank()) {
                        onUpdate(staff.copy(username = username, role = selectedRole))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumColorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Отмена")
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.widthIn(max = 400.dp)
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGuestDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateGuestRequest) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var roomNumber by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Добавить гостя",
                fontWeight = FontWeight.Bold,
                color = PremiumColorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("ФИО гостя") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PremiumColorScheme.primary)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { newPhone ->
                        phone = newPhone
                        phoneError = null
                    },
                    label = { Text("Номер телефона") },
                    isError = phoneError != null,
                    supportingText = { if (phoneError != null) Text(phoneError!!) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = PremiumColorScheme.primary)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Номер комнаты") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Hotel, contentDescription = null, tint = PremiumColorScheme.primary)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank() || phone.isBlank() || roomNumber.isBlank()) {
                        phoneError = "Заполните все поля"
                        return@Button
                    }
                    if (!isValidPhone(phone)) {
                        phoneError = "Введите корректный номер телефона (10–15 цифр)"
                        return@Button
                    }
                    onCreate(CreateGuestRequest(fullName, phone, roomNumber))
                },
                enabled = fullName.isNotBlank() && phone.isNotBlank() && roomNumber.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumColorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Отмена")
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.widthIn(max = 400.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStaffDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateStaffRequest) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("STAFF") }
    var expanded by remember { mutableStateOf(false) }

    val roles = listOf(
        "ADMIN" to "👑 Администратор",
        "STAFF" to "👔 Сотрудник",
        "CLEANER" to "🧹 Горничная",
        "MASTER" to "🔧 Мастер"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Добавить сотрудника",
                fontWeight = FontWeight.Bold,
                color = PremiumColorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Логин") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PremiumColorScheme.primary)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = PremiumColorScheme.primary)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Роль",
                    fontSize = 14.sp,
                    color = NeutralGray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = roles.find { it.first == selectedRole }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumColorScheme.primary)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { (role, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedRole = role
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        onCreate(CreateStaffRequest(username, password, selectedRole))
                    }
                },
                enabled = username.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumColorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Отмена")
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.widthIn(max = 400.dp)
    )
}

private fun getDeclension(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "заявка"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "заявки"
        else -> "заявок"
    }
}

private fun getStatusName(status: String): String = when (status) {
    "NEW" -> "Новая"
    "IN_PROGRESS" -> "В работе"
    "COMPLETED" -> "Выполнена"
    else -> status
}

data class StatusConfig(
    val text: String,
    val color: Color,
    val backgroundColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTicketCard(
    ticket: TicketResponse,
    onStatusChange: (String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val statusConfig = when (ticket.status) {
        "NEW" -> StatusConfig("Новая", PremiumColorScheme.primary, PremiumColorScheme.primaryContainer)
        "IN_PROGRESS" -> StatusConfig("В работе", WarningOrange, WarningOrange.copy(alpha = 0.1f))
        "COMPLETED" -> StatusConfig("Выполнена", SuccessGreen, SuccessGreen.copy(alpha = 0.1f))
        else -> StatusConfig(ticket.status, NeutralGray, NeutralGray.copy(alpha = 0.1f))
    }

    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = {
                Text(
                    text = "Детали заявки",
                    fontWeight = FontWeight.Bold,
                    color = PremiumColorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Гость", fontSize = 12.sp, color = NeutralGray)
                    Text(
                        text = ticket.guestName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text("Номер комнаты", fontSize = 12.sp, color = NeutralGray)
                    Text(
                        text = ticket.roomNumber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text("Категория", fontSize = 12.sp, color = NeutralGray)
                    Text(
                        text = ticket.categoryName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text("Статус", fontSize = 12.sp, color = NeutralGray)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusConfig.backgroundColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = statusConfig.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusConfig.color,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Text("Описание", fontSize = 12.sp, color = NeutralGray, modifier = Modifier.padding(top = 12.dp))
                    Text(
                        text = ticket.description,
                        fontSize = 14.sp,
                        color = DarkGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text("Дата создания", fontSize = 12.sp, color = NeutralGray, modifier = Modifier.padding(top = 12.dp))
                    Text(
                        text = ticket.createdAt,
                        fontSize = 14.sp,
                        color = NeutralGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Закрыть")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ticket.status != "COMPLETED") {
                        TextButton(
                            onClick = {
                                showDetailsDialog = false
                                onStatusChange("COMPLETED")
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = SuccessGreen)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Завершить")
                        }
                    }
                    if (onDelete != null) {
                        TextButton(
                            onClick = {
                                showDetailsDialog = false
                                showDeleteDialog = true
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Удалить")
                        }
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        )
    }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Удалить заявку?",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Заявка будет удалена безвозвратно.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { showDetailsDialog = true })
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = PremiumColorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = ticket.guestName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumColorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = ticket.guestName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkGray
                            )
                            Text(
                                text = "Комната ${ticket.roomNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralGray
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusConfig.backgroundColor,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = statusConfig.text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = statusConfig.color,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = ticket.categoryName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = DarkGray.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                color = NeutralGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(
                        PremiumColorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).padding(start = 8.dp),
                        tint = PremiumColorScheme.primary
                    )
                    Text(
                        text = ticket.createdAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = PremiumColorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (ticket.status != "NEW") {
                        IconButton(onClick = { onStatusChange("NEW") }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Новая",
                                tint = PremiumColorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (ticket.status != "IN_PROGRESS") {
                        IconButton(onClick = { onStatusChange("IN_PROGRESS") }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "В работе",
                                tint = WarningOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (ticket.status != "COMPLETED") {
                        IconButton(onClick = { onStatusChange("COMPLETED") }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Выполнена",
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
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
        1 to "🛏️ Уборка номера",
        2 to "🔧 Технический ремонт",
        3 to "🍽️ Рум-сервис",
        4 to "❓ Вопросы и пожелания"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Text(
                text = "Новая заявка",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PremiumColorScheme.primary
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Категория",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory.second,
                onValueChange = {},
                readOnly = true,
                label = { Text("Выберите услугу") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PremiumColorScheme.primary
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.second) },
                        onClick = {
                            selectedCategory = category
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Описание",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Что необходимо сделать?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PremiumColorScheme.primary
            ),
            placeholder = { Text("Опишите вашу проблему или пожелание...", color = NeutralGray) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (description.isNotBlank()) {
                    onCreate(selectedCategory.first, description)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = description.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumColorScheme.primary
            )
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Отправить заявку", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}