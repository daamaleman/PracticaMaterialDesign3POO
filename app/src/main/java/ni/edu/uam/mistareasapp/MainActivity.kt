package ni.edu.uam.mistareasapp

// ============================================================
// TaskApp — Material Design 3 con Jetpack Compose
// Pantalla: Registro de Tareas
// ============================================================


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ────────────────────────────────────────────────────────────
// 1. MODELO DE DATOS
// ────────────────────────────────────────────────────────────
enum class Priority(val label: String, val color: Color) {
    HIGH("En espera",  Color(0xFFB3261E)),
    MEDIUM("Iniciado", Color(0xFFE8A000)),
    LOW("Finalizado",  Color(0xFF386A20))
}

data class Task(
    val id: Int,
    val text: String,
    val description: String,
    val priority: Priority,
    val fechaInicioMillis: Long,
    val fechaFinMillis: Long,
    val isDone: Boolean = false
)

private fun formatDate(millis: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
}

private fun normalizePriority(priority: Priority): Priority {
    return if (priority == Priority.LOW) Priority.MEDIUM else priority
}

private fun nextPriority(priority: Priority): Priority {
    return when (priority) {
        Priority.HIGH -> Priority.MEDIUM
        Priority.MEDIUM -> Priority.LOW
        Priority.LOW -> Priority.HIGH
    }
}

// ────────────────────────────────────────────────────────────
// 2. TEMA — Material Design 3 (colorScheme + typography)
// ────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary         = Color(0xFF6750A4),
    onPrimary       = Color.White,
    primaryContainer= Color(0xFFEADDFF),
    secondary       = Color(0xFF625B71),
    surface         = Color(0xFFFFFBFE),
    onSurface       = Color(0xFF1C1B1F),
    background      = Color(0xFFFFFBFE),
    error           = Color(0xFFB3261E)
)

private val DarkColorScheme = darkColorScheme(
    primary         = Color(0xFFD0BCFF),
    onPrimary       = Color(0xFF381E72),
    primaryContainer= Color(0xFF4F378B),
    secondary       = Color(0xFFCCC2DC),
    surface         = Color(0xFF1C1B1F),
    onSurface       = Color(0xFFE6E1E5),
    background      = Color(0xFF1C1B1F)
)

@Composable
fun TaskAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography(),          // MD3 typography por defecto
        content     = content
    )
}

// ────────────────────────────────────────────────────────────
// 3. PANTALLA PRINCIPAL — Scaffold + estado
// ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen() {
    val context = LocalContext.current
    // ── Estado reactivo con remember ──────────────────────────
    var taskText       by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var taskDone       by remember { mutableStateOf(false) }
    var editingTaskId  by remember { mutableStateOf<Int?>(null) }
    var tasks          by remember { mutableStateOf(listOf<Task>()) }
    var nextId         by remember { mutableIntStateOf(1) }
    var snackMessage   by remember { mutableStateOf<String?>(null) }
    var darkMode       by remember { mutableStateOf(false) }
    var isLoaded       by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val (loadedTasks, loadedNextId) = loadTasksFromPrefs(context)
        tasks = loadedTasks
        nextId = loadedNextId
        isLoaded = true
    }

    LaunchedEffect(tasks, nextId, isLoaded) {
        if (isLoaded) {
            saveTasksToPrefs(context, tasks, nextId)
        }
    }

    // Mostrar snackbar cuando cambie el mensaje
    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackMessage = null
        }
    }

    TaskAppTheme(darkTheme = darkMode) {
        // ── Scaffold — estructura base MD3 ─────────────────────
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Mis Tareas",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        // Botón modo oscuro — componente interactivo #2
                        IconButton(onClick = { darkMode = !darkMode }) {
                            Text(
                                text = if (darkMode) "☀" else "🌙",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor    = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {

                // ── Tarjeta de entrada ──────────────────────────
                item { InputCard(
                    taskText        = taskText,
                    onTextChange    = { taskText = it },
                    descriptionText = descriptionText,
                    onDescriptionChange = { descriptionText = it },
                    selectedPriority = selectedPriority,
                    onPriorityChange = { selectedPriority = it },
                    startDateMillis = startDateMillis,
                    onStartDateChange = { startDateMillis = it },
                    endDateMillis = endDateMillis,
                    onEndDateChange = { endDateMillis = it },
                    isDone = taskDone,
                    onDoneChange = { taskDone = it },
                    isEditing = editingTaskId != null,
                    onCancelEdit = {
                        editingTaskId = null
                        taskText = ""
                        descriptionText = ""
                        selectedPriority = Priority.MEDIUM
                        startDateMillis = null
                        endDateMillis = null
                        taskDone = false
                    },
                    onAddTask = {
                        when {
                            taskText.isBlank() -> {
                                snackMessage = "Escribe el nombre de la tarea"
                            }
                            descriptionText.isBlank() -> {
                                snackMessage = "Escribe la descripcion"
                            }
                            startDateMillis == null || endDateMillis == null -> {
                                snackMessage = "Selecciona fecha de inicio y fin"
                            }
                            endDateMillis!! < startDateMillis!! -> {
                                snackMessage = "La fecha fin no puede ser menor que la fecha inicio"
                            }
                            else -> {
                                val normalizedPriority = normalizePriority(selectedPriority)
                                val currentEditingId = editingTaskId
                                if (currentEditingId == null) {
                                    tasks = tasks + Task(
                                        id       = nextId++,
                                        text     = taskText.trim(),
                                        description = descriptionText.trim(),
                                        priority = normalizedPriority,
                                        fechaInicioMillis = startDateMillis!!,
                                        fechaFinMillis = endDateMillis!!,
                                        isDone = taskDone
                                    )
                                    snackMessage = "✓ Tarea \"${taskText.take(20)}\" agregada"
                                } else {
                                    tasks = tasks.map {
                                        if (it.id == currentEditingId) {
                                            it.copy(
                                                text = taskText.trim(),
                                                description = descriptionText.trim(),
                                                priority = normalizedPriority,
                                                fechaInicioMillis = startDateMillis!!,
                                                fechaFinMillis = endDateMillis!!,
                                                isDone = taskDone
                                            )
                                        } else it
                                    }
                                    snackMessage = "Tarea actualizada"
                                }
                                taskText = ""
                                descriptionText = ""
                                selectedPriority = Priority.MEDIUM
                                startDateMillis = null
                                endDateMillis = null
                                taskDone = false
                                editingTaskId = null
                            }
                        }
                    }
                )}

                // ── Tarjeta de progreso ─────────────────────────
                item { ProgressCard(tasks) }

                item {
                    val allSelected = tasks.isNotEmpty() && tasks.all { it.isDone }
                    FilledTonalButton(
                        onClick = {
                            tasks = tasks.map { it.copy(isDone = !allSelected) }
                            snackMessage = if (allSelected) {
                                "Todas las tareas fueron deseleccionadas"
                            } else {
                                "Todas las tareas fueron seleccionadas"
                            }
                        },
                        enabled = tasks.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (allSelected) "Deseleccionar todas" else "Seleccionar todas")
                    }
                }

                // ── Lista de tareas ─────────────────────────────
                if (tasks.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task     = task,
                            onToggle = {
                                tasks = tasks.map {
                                    // El checkbox izquierdo solo permite desmarcar una tarea ya finalizada.
                                    if (it.id == task.id) {
                                        if (it.isDone) it.copy(isDone = false) else it
                                    } else it
                                }
                            },
                            onPriorityChipClick = {
                                tasks = tasks.map {
                                    if (it.id != task.id) {
                                        it
                                    } else {
                                        val newPriority = nextPriority(it.priority)
                                        // Si sale de Finalizado, se limpia la marca de completada.
                                        it.copy(
                                            priority = newPriority,
                                            isDone = if (newPriority == Priority.LOW) it.isDone else false
                                        )
                                    }
                                }
                            },
                            onConfirmDoneClick = {
                                tasks = tasks.map {
                                    if (it.id == task.id && it.priority == Priority.LOW) {
                                        it.copy(isDone = true)
                                    } else it
                                }
                            },
                            onEdit = {
                                editingTaskId = task.id
                                taskText = task.text
                                descriptionText = task.description
                                selectedPriority = normalizePriority(task.priority)
                                startDateMillis = task.fechaInicioMillis
                                endDateMillis = task.fechaFinMillis
                                taskDone = task.isDone
                            },
                            onDelete = {
                                tasks = tasks.filter { it.id != task.id }
                                snackMessage = "Tarea eliminada"
                                if (editingTaskId == task.id) {
                                    editingTaskId = null
                                    taskText = ""
                                    descriptionText = ""
                                    selectedPriority = Priority.MEDIUM
                                    startDateMillis = null
                                    endDateMillis = null
                                    taskDone = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// 4. COMPONENTE — Tarjeta de ingreso (OutlinedTextField + Chips + Button)
// ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputCard(
    taskText: String,
    onTextChange: (String) -> Unit,
    descriptionText: String,
    onDescriptionChange: (String) -> Unit,
    selectedPriority: Priority,
    onPriorityChange: (Priority) -> Unit,
    startDateMillis: Long?,
    onStartDateChange: (Long?) -> Unit,
    endDateMillis: Long?,
    onEndDateChange: (Long?) -> Unit,
    isDone: Boolean,
    onDoneChange: (Boolean) -> Unit,
    isEditing: Boolean,
    onCancelEdit: () -> Unit,
    onAddTask: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text  = "NUEVA TAREA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // OutlinedTextField — componente interactivo #1
            OutlinedTextField(
                value         = taskText,
                onValueChange = onTextChange,
                label         = { Text("Nombre de la tarea") },
                placeholder   = { Text("Escribe aqui...") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                trailingIcon  = {
                    if (taskText.isNotEmpty()) {
                        IconButton(onClick = { onTextChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    focusedLabelColor    = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            OutlinedTextField(
                value = descriptionText,
                onValueChange = onDescriptionChange,
                label = { Text("Descripcion") },
                placeholder = { Text("Describe la tarea...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    focusedLabelColor    = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            DatePickerField(
                label = "Fecha inicio",
                selectedDateMillis = startDateMillis,
                onDateSelected = onStartDateChange
            )

            DatePickerField(
                label = "Fecha fin",
                selectedDateMillis = endDateMillis,
                onDateSelected = onEndDateChange
            )

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Completada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isDone,
                        onCheckedChange = onDoneChange
                    )
                }
            }

            // Priority chips
            Text(
                text  = "Prioridad",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            val visiblePriorities = Priority.entries.filter { it != Priority.LOW }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                visiblePriorities.forEach { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick  = { onPriorityChange(priority) },
                        label    = { Text(priority.label) },
                        leadingIcon = if (selectedPriority == priority) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }
            }

            // Botón principal — FilledButton MD3
            Button(
                onClick  = onAddTask,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Filled.Edit else Icons.Filled.Add,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Guardar cambios" else "Agregar tarea",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (isEditing) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar edicion")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDateMillis: Long?,
    onDateSelected: (Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDateMillis?.let { formatDate(it) } ?: "",
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text("Seleccionar fecha") },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Abrir calendario")
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            focusedLabelColor    = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        onDateSelected(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ────────────────────────────────────────────────────────────
// 5. COMPONENTE — Tarjeta de progreso (Surface + LinearProgressIndicator)
// ────────────────────────────────────────────────────────────
@Composable
fun ProgressCard(tasks: List<Task>) {
    val done  = tasks.count { it.isDone }
    val total = tasks.size
    val pct   = if (total > 0) done.toFloat() / total else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.medium,
        color    = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Progreso del día",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${(pct * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth(),
                color    = MaterialTheme.colorScheme.primary
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "$done completadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "$total total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// 6. COMPONENTE — Ítem de tarea (Card + Checkbox + Badge)
// ────────────────────────────────────────────────────────────
@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onPriorityChipClick: () -> Unit, onConfirmDoneClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors   = CardDefaults.cardColors(
            containerColor = if (task.isDone)
                MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked         = task.isDone,
                onCheckedChange = { onToggle() },
                colors          = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text  = task.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isDone)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatDate(task.fechaInicioMillis)} - ${formatDate(task.fechaFinMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                // Badge de prioridad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SuggestionChip(
                        onClick = onPriorityChipClick,
                        label   = { Text(task.priority.label, style = MaterialTheme.typography.labelSmall) },
                        colors  = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = task.priority.color.copy(alpha = 0.15f),
                            labelColor     = task.priority.color
                        )
                    )

                    if (task.priority == Priority.LOW) {
                        IconButton(
                            onClick = onConfirmDoneClick,
                            enabled = !task.isDone
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = if (task.isDone) "Tarea finalizada" else "Finalizar tarea",
                                tint = if (task.isDone) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Editar tarea",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar tarea",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// 7. COMPONENTE — Estado vacío
// ────────────────────────────────────────────────────────────
@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Text(
            text  = "Sin tareas por ahora",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text  = "Agrega tu primera tarea arriba",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// ────────────────────────────────────────────────────────────
// 8. PUNTO DE ENTRADA
// ────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TaskScreen() }
    }
}

private const val PREFS_NAME = "task_app_prefs"
private const val KEY_TASKS = "tasks_json"
private const val KEY_NEXT_ID = "next_id"

private fun taskToJson(task: Task): JSONObject {
    return JSONObject().apply {
        put("id", task.id)
        put("text", task.text)
        put("description", task.description)
        put("priority", task.priority.name)
        put("fechaInicioMillis", task.fechaInicioMillis)
        put("fechaFinMillis", task.fechaFinMillis)
        put("isDone", task.isDone)
    }
}

private fun taskFromJson(json: JSONObject): Task? {
    return try {
        Task(
            id = json.getInt("id"),
            text = json.getString("text"),
            description = json.getString("description"),
            priority = Priority.valueOf(json.getString("priority")),
            fechaInicioMillis = json.getLong("fechaInicioMillis"),
            fechaFinMillis = json.getLong("fechaFinMillis"),
            isDone = json.optBoolean("isDone", false)
        )
    } catch (_: Exception) {
        null
    }
}

private fun saveTasksToPrefs(context: android.content.Context, tasks: List<Task>, nextId: Int) {
    val jsonArray = JSONArray()
    tasks.forEach { jsonArray.put(taskToJson(it)) }

    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_TASKS, jsonArray.toString())
        .putInt(KEY_NEXT_ID, nextId)
        .apply()
}

private fun loadTasksFromPrefs(context: android.content.Context): Pair<List<Task>, Int> {
    return try {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TASKS, null)
        if (raw.isNullOrBlank()) {
            emptyList<Task>() to 1
        } else {
            val jsonArray = JSONArray(raw)
            val loadedTasks = buildList {
                for (i in 0 until jsonArray.length()) {
                    taskFromJson(jsonArray.getJSONObject(i))?.let { add(it) }
                }
            }
            val storedNextId = prefs.getInt(KEY_NEXT_ID, 1)
            val safeNextId = maxOf(storedNextId, (loadedTasks.maxOfOrNull { it.id } ?: 0) + 1)
            loadedTasks to safeNextId
        }
    } catch (_: Exception) {
        emptyList<Task>() to 1
    }
}
