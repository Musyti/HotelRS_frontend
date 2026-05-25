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
    var type by remember { mutableStateOf("STAFF") }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            // Logo Area
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
                    // Role Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = type == "GUEST",  // добавлено
                            onClick = { type = "GUEST" },
                            label = { Text("Я гость") },
                            enabled = true,  // добавлено
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = type == "STAFF",  // добавлено
                            onClick = { type = "STAFF" },
                            label = { Text("Сотрудник") },
                            enabled = true,  // добавлено
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Identifier Field
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
                            val pass = if (type == "GUEST") null else password
                            onLogin(type, identifier, pass)
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
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var tickets by remember { mutableStateOf<List<TicketResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadTickets() {
        scope.launch {
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
    }

    LaunchedEffect(statusFilter) {
        loadTickets()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumColorScheme.background)
    ) {
        // Header
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
                            text = "Админ-панель",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DarkGray
                        )
                        Text(
                            text = if (selectedTab == 0) "${tickets.size} ${getDeclension(tickets.size)}" else "Управление пользователями",
                            color = NeutralGray,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Выйти", tint = NeutralGray)  // <-- Icons.Default
                }
            }
        }

        // Табы - используем PrimaryTabRow
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = PremiumColorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Заявки") },
                icon = { Icon(Icons.Default.List, contentDescription = null) }  // <-- Icons.Default
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Пользователи") },
                icon = { Icon(Icons.Default.People, contentDescription = null) }
            )
        }

        // Контент вкладок
        when (selectedTab) {
            0 -> TicketsTab(
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
                onDelete = { ticket ->
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
                }
            )
            1 -> UsersTab(
                apiClient = apiClient,
                onShowSnackbar = { message -> showSnackbar = message }
            )
        }
    }

    // Snackbar
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
        // Фильтры
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

        // Список заявок
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
    onShowSnackbar: (String) -> Unit
) {
    var showCreateGuestDialog by remember { mutableStateOf(false) }
    var showCreateStaffDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Заголовок
        Text(
            text = "Управление пользователями",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PremiumColorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Карточка добавления гостя
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { showCreateGuestDialog = true })
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = PremiumColorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = PremiumColorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Добавить гостя",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkGray
                    )
                    Text(
                        text = "Создать нового гостя в системе",
                        fontSize = 14.sp,
                        color = NeutralGray
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = NeutralGray
                )
            }
        }

        // Карточка добавления сотрудника
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { showCreateStaffDialog = true })
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = PremiumColorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = PremiumColorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Добавить сотрудника",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkGray
                    )
                    Text(
                        text = "Создать сотрудника с определенной ролью",
                        fontSize = 14.sp,
                        color = NeutralGray
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = NeutralGray
                )
            }
        }
    }

    // Диалог создания гостя - передаем scope
    if (showCreateGuestDialog) {
        CreateGuestDialog(
            onDismiss = { showCreateGuestDialog = false },
            onCreate = { guest ->
                // Запускаем корутину здесь, в колбэке
                scope.launch {
                    try {
                        val response = apiClient.createGuest(guest)
                        onShowSnackbar("Гость ${guest.fullName} успешно создан")
                        showCreateGuestDialog = false
                    } catch (e: Exception) {
                        onShowSnackbar("Ошибка: ${e.message}")
                    }
                }
            }
        )
    }

    // Диалог создания сотрудника - передаем scope
    if (showCreateStaffDialog) {
        CreateStaffDialog(
            onDismiss = { showCreateStaffDialog = false },
            onCreate = { staff ->
                // Запускаем корутину здесь, в колбэке
                scope.launch {
                    try {
                        val response = apiClient.createStaff(staff)
                        onShowSnackbar("Сотрудник ${staff.username} успешно создан")
                        showCreateStaffDialog = false
                    } catch (e: Exception) {
                        onShowSnackbar("Ошибка: ${e.message}")
                    }
                }
            }
        )
    }
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
                    onValueChange = { phone = it },
                    label = { Text("Номер телефона") },
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
                    if (fullName.isNotBlank() && phone.isNotBlank() && roomNumber.isNotBlank()) {
                        onCreate(CreateGuestRequest(fullName, phone, roomNumber))
                    }
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

                // Выбор роли
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
    onDelete: () -> Unit
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val statusConfig = when (ticket.status) {
        "NEW" -> StatusConfig("Новая", PremiumColorScheme.primary, PremiumColorScheme.primaryContainer)
        "IN_PROGRESS" -> StatusConfig("В работе", WarningOrange, WarningOrange.copy(alpha = 0.1f))
        "COMPLETED" -> StatusConfig("Выполнена", SuccessGreen, SuccessGreen.copy(alpha = 0.1f))
        else -> StatusConfig(ticket.status, NeutralGray, NeutralGray.copy(alpha = 0.1f))
    }

    // Диалог с деталями заявки
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
                    // Гость
                    Text(
                        text = "Гость",
                        fontSize = 12.sp,
                        color = NeutralGray
                    )
                    Text(
                        text = ticket.guestName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Номер комнаты
                    Text(
                        text = "Номер комнаты",
                        fontSize = 12.sp,
                        color = NeutralGray
                    )
                    Text(
                        text = ticket.roomNumber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Категория
                    Text(
                        text = "Категория",
                        fontSize = 12.sp,
                        color = NeutralGray
                    )
                    Text(
                        text = ticket.categoryName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Статус
                    Text(
                        text = "Статус",
                        fontSize = 12.sp,
                        color = NeutralGray
                    )
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

                    // Описание
                    Text(
                        text = "Описание",
                        fontSize = 12.sp,
                        color = NeutralGray,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = ticket.description,
                        fontSize = 14.sp,
                        color = DarkGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Дата создания
                    Text(
                        text = "Дата создания",
                        fontSize = 12.sp,
                        color = NeutralGray,
                        modifier = Modifier.padding(top = 12.dp)
                    )
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
                // Кнопки действий в диалоге (опционально)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (ticket.status != "COMPLETED") {
                        TextButton(
                            onClick = {
                                showDetailsDialog = false
                                onStatusChange("COMPLETED")
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = SuccessGreen)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Завершить")
                        }
                    }
                    TextButton(
                        onClick = {
                            showDetailsDialog = false
                            showDeleteDialog = true
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить")
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        )
    }

    // Диалог удаления
    if (showDeleteDialog) {
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
                detectTapGestures(
                    onTap = { showDetailsDialog = true }
                )
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        // ... остальной код карточки без изменений ...
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
                        modifier = Modifier
                            .size(14.dp)
                            .padding(start = 8.dp),
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

                // Кнопки быстрого действия
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (ticket.status != "NEW") {
                        IconButton(
                            onClick = { onStatusChange("NEW") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Новая",
                                tint = PremiumColorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (ticket.status != "IN_PROGRESS") {
                        IconButton(
                            onClick = { onStatusChange("IN_PROGRESS") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "В работе",
                                tint = WarningOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (ticket.status != "COMPLETED") {
                        IconButton(
                            onClick = { onStatusChange("COMPLETED") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Выполнена",
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
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