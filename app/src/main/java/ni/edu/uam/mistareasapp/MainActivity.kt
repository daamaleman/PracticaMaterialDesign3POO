package ni.edu.uam.mistareasapp

// ============================================================
// TaskApp — Material Design 3 con Jetpack Compose
// Pantalla: Registro de Tareas
// ============================================================

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

// ────────────────────────────────────────────────────────────
// 1. MODELO DE DATOS
// ────────────────────────────────────────────────────────────
enum class Priority(val label: String, val color: Color) {
    HIGH("Alta",  Color(0xFFB3261E)),
    MEDIUM("Media", Color(0xFFE8A000)),
    LOW("Baja",  Color(0xFF386A20))
}

data class Task(
    val id: Int,
    val text: String,
    val priority: Priority,
    val isDone: Boolean = false
)

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
    // ── Estado reactivo con remember ──────────────────────────
    var taskText       by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var tasks          by remember { mutableStateOf(listOf<Task>()) }
    var nextId         by remember { mutableIntStateOf(1) }
    var snackMessage   by remember { mutableStateOf<String?>(null) }
    var darkMode       by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

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
                            Icon(
                                imageVector = if (darkMode) Icons.Filled.LightMode
                                else Icons.Filled.DarkMode,
                                contentDescription = if (darkMode) "Modo claro"
                                else "Modo oscuro"
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
                    selectedPriority = selectedPriority,
                    onPriorityChange = { selectedPriority = it },
                    onAddTask = {
                        if (taskText.isNotBlank()) {
                            tasks = tasks + Task(
                                id       = nextId++,
                                text     = taskText.trim(),
                                priority = selectedPriority
                            )
                            snackMessage = "✓ Tarea \"${taskText.take(20)}\" agregada"
                            taskText = ""
                        } else {
                            snackMessage = "Escribe el nombre de la tarea"
                        }
                    }
                )}

                // ── Tarjeta de progreso ─────────────────────────
                item { ProgressCard(tasks) }

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
                                    if (it.id == task.id) it.copy(isDone = !it.isDone) else it
                                }
                            },
                            onDelete = {
                                tasks = tasks.filter { it.id != task.id }
                                snackMessage = "Tarea eliminada"
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
    selectedPriority: Priority,
    onPriorityChange: (Priority) -> Unit,
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
                placeholder   = { Text("Escribe aquí...") },
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

            // Priority chips
            Text(
                text  = "Prioridad",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick  = { onPriorityChange(priority) },
                        label    = { Text(priority.label) },
                        leadingIcon = if (selectedPriority == priority) {{
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }} else null
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
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar tarea", style = MaterialTheme.typography.labelLarge)
            }
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
fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
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
                // Badge de prioridad
                SuggestionChip(
                    onClick = {},
                    label   = { Text(task.priority.label, style = MaterialTheme.typography.labelSmall) },
                    colors  = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = task.priority.color.copy(alpha = 0.15f),
                        labelColor     = task.priority.color
                    )
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
            imageVector = Icons.Filled.CheckBox,
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
// En MainActivity.kt:
//
// class MainActivity : ComponentActivity() {
//     override fun onCreate(savedInstanceState: Bundle?) {
//         super.onCreate(savedInstanceState)
//         setContent { TaskScreen() }
//     }
// }