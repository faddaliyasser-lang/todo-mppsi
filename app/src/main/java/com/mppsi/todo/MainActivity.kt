package com.mppsi.todo

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

data class SubjectGroup(
    val id: String,
    val nameAr: String,
    val nameFr: String,
    val colorHex: Long
)

data class TodoTask(
    val id: String,
    val title: String,
    val scope: String = "day",
    val date: String,
    val weekStart: String,
    val time: String = "08:00",
    val reminderMinutes: Int = 60,
    val groupIds: List<String> = emptyList(),
    val customColors: List<Long> = listOf(0xFF4F46E5),
    val notes: String = "",
    val completed: Boolean = false
)

val PALETTE_COLORS = listOf(
    0xFF4F46E5 to ("نيلي (رياضيات)" to "Indigo"),
    0xFF0EA5E9 to ("سماوي (فيزياء)" to "Ciel"),
    0xFF10B981 to ("زمردي (علوم المهندس)" to "Émeraude"),
    0xFF8B5CF6 to ("بنفسجي (إعلاميات)" to "Violet"),
    0xFFF59E0B to ("كهرماني (كيمياء/لغات)" to "Ambre"),
    0xFFE11D48 to ("أحمر (مباريات تجريبية)" to "Rose"),
    0xFFF97316 to ("برتقالي" to "Orange"),
    0xFF14B8A6 to ("تركوازي" to "Turquoise"),
    0xFFEC4899 to ("فوشيا" to "Fuchsia"),
    0xFF84CC16 to ("ليموني" to "Citron"),
    0xFF06B6D4 to ("أزرق فاتح" to "Cyan"),
    0xFF64748B to ("رمادي" to "Ardoise")
)

fun getMondayOfWeek(dateStr: String): String {
    val ld = LocalDate.parse(dateStr)
    return ld.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
}

fun getWeekDaysList(mondayStr: String): List<String> {
    val start = LocalDate.parse(mondayStr)
    return (0..6).map { start.plusDays(it.toLong()).toString() }
}

fun getIsoWeekNumber(dateStr: String): Int {
    val ld = LocalDate.parse(dateStr)
    return ld.get(WeekFields.ISO.weekOfWeekBasedYear())
}

fun getOfficialMPPSIData(): Pair<List<SubjectGroup>, List<TodoTask>> {
    val groups = listOf(
        SubjectGroup("grp_math", "الرياضيات (M)", "Mathématiques (M)", 0xFF4F46E5),
        SubjectGroup("grp_phys", "الفيزياء (P)", "Physique (P)", 0xFF0EA5E9),
        SubjectGroup("grp_si", "علوم المهندس (SI)", "Sciences Ingénieur (SI)", 0xFF10B981),
        SubjectGroup("grp_info", "المعلوميات (Info)", "Informatique (Info)", 0xFF8B5CF6),
        SubjectGroup("grp_lang_chim", "الكيمياء واللغات", "Chimie & Langues", 0xFFF59E0B),
        SubjectGroup("grp_concours", "مباريات تجريبية", "Concours Blancs", 0xFFE11D48)
    )

    data class RawEntry(
        val scope: String,
        val date: String,
        val title: String,
        val groups: List<String>,
        val colors: List<Long>,
        val note: String
    )

    val raw = listOf(
        RawEntry("week", "2026-09-21", "DS : M — Semaine N°3", listOf("grp_math"), listOf(0xFF4F46E5), "Semaine du 21.9 (N°3)"),
        RawEntry("week", "2026-09-28", "DS : P — Semaine N°4", listOf("grp_phys"), listOf(0xFF0EA5E9), "Semaine du 28.9 (N°4)"),
        RawEntry("week", "2026-10-05", "DS : SI — Semaine N°5", listOf("grp_si"), listOf(0xFF10B981), "Semaine du 5.10 (N°5)"),
        RawEntry("week", "2026-10-12", "DS : Fr et Chimie — Semaine N°6", listOf("grp_lang_chim"), listOf(0xFFF59E0B), "Semaine du 12.10 (N°6)"),
        RawEntry("day", "2026-10-26", "1er concours (MP-PSI)", listOf("grp_concours"), listOf(0xFFE11D48), "Semaine du 26.10 (N°7)"),
        RawEntry("week", "2026-11-16", "DS : M — Semaine N°10", listOf("grp_math"), listOf(0xFF4F46E5), "Semaine du 16.11 (N°10)"),
        RawEntry("week", "2026-11-23", "DS : P — Semaine N°11", listOf("grp_phys"), listOf(0xFF0EA5E9), "Semaine du 23.11 (N°11)"),
        RawEntry("week", "2026-11-30", "DS : Info — Semaine N°12", listOf("grp_info"), listOf(0xFF8B5CF6), "Semaine du 30.11 (N°12)"),
        RawEntry("day", "2026-12-14", "2ème concours (MP-PSI)", listOf("grp_concours"), listOf(0xFFE11D48), "Semaine du 14.12 (N°13)"),
        RawEntry("week", "2026-12-28", "DS : M — Semaine N°15", listOf("grp_math"), listOf(0xFF4F46E5), "Semaine du 28.12 (N°15)"),
        RawEntry("week", "2027-01-04", "DS : Chimie et Fr — Semaine N°16", listOf("grp_lang_chim"), listOf(0xFFF59E0B), "Semaine du 4.1.2027 (N°16)"),
        RawEntry("week", "2027-01-11", "DS : P — Semaine N°17", listOf("grp_phys"), listOf(0xFF0EA5E9), "Semaine du 11.1 (N°17)"),
        RawEntry("week", "2027-01-18", "DS : SI et Info — Semaine N°18", listOf("grp_si", "grp_info"), listOf(0xFF10B981, 0xFF8B5CF6), "Semaine du 18.1 (N°18)"),
        RawEntry("day", "2027-02-08", "3ème concours (MP-PSI)", listOf("grp_concours"), listOf(0xFFE11D48), "Semaine du 8.2 (N°20)"),
        RawEntry("week", "2027-02-22", "DS : M et Fr — Semaine N°22", listOf("grp_math", "grp_lang_chim"), listOf(0xFF4F46E5, 0xFFF59E0B), "Semaine du 22.2 (N°22)"),
        RawEntry("week", "2027-03-01", "DS : SI et Ang — Semaine N°23", listOf("grp_si", "grp_lang_chim"), listOf(0xFF10B981, 0xFFF59E0B), "Semaine du 1.3 (N°23)"),
        RawEntry("week", "2027-03-08", "DS : P et Traduction — Semaine N°24", listOf("grp_phys", "grp_lang_chim"), listOf(0xFF0EA5E9, 0xFFF59E0B), "Semaine du 8.3 (N°24)"),
        RawEntry("week", "2027-03-29", "DS : Chimie — Semaine N°26", listOf("grp_lang_chim"), listOf(0xFFF59E0B), "Semaine du 29.3 (N°26)"),
        RawEntry("week", "2027-04-05", "DS : M — Semaine N°27", listOf("grp_math"), listOf(0xFF4F46E5), "Semaine du 5.4 (N°27)"),
        RawEntry("week", "2027-04-12", "DS : P — Semaine N°28", listOf("grp_phys"), listOf(0xFF0EA5E9), "Semaine du 12.4 (N°28)")
    )

    val tasks = raw.mapIndexed { idx, r ->
        TodoTask(
            id = "mppsi_${idx + 1}",
            title = r.title,
            scope = r.scope,
            date = r.date,
            weekStart = getMondayOfWeek(r.date),
            time = "08:00",
            reminderMinutes = 60,
            groupIds = r.groups,
            customColors = r.colors,
            notes = r.note
        )
    }
    return groups to tasks
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "TO DO"
        val time = intent.getStringExtra("time") ?: ""
        val id = intent.getIntExtra("notifId", 1001)

        val channelId = "todo_mppsi_reminders"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rappels & DS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, MainActivity::class.java)
        val pendingTap = PendingIntent.getActivity(
            context, id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("TO DO • $time")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .build()

        nm.notify(id, notification)
    }
}

fun scheduleTaskNotification(context: Context, task: TodoTask) {
    if (task.completed || task.reminderMinutes < 0) return
    try {
        val ld = LocalDate.parse(task.date)
        val lt = LocalTime.parse(task.time)
        val triggerMillis = LocalDateTime.of(ld, lt)
            .minusMinutes(task.reminderMinutes.toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", task.title)
            putExtra("time", task.time)
            putExtra("notifId", task.id.hashCode())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    } catch (_: Exception) {
    }
}

fun sendInstantTestNotification(context: Context, isAr: Boolean) {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("title", if (isAr) "تجربة التنبيه والصوت بنجاح!" else "Test de notification réussi !")
        putExtra("time", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
        putExtra("notifId", 9999)
    }
    context.sendBroadcast(intent)
    try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
    } catch (_: Exception) {
    }
}

class TodoStorage(context: Context) {
    private val prefs = context.getSharedPreferences("todo_mppsi_native_v1", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadLang(): String = prefs.getString("lang", "ar") ?: "ar"
    fun saveLang(lang: String) = prefs.edit().putString("lang", lang).apply()

    fun loadDarkTheme(): Boolean = prefs.getBoolean("dark", false)
    fun saveDarkTheme(dark: Boolean) = prefs.edit().putBoolean("dark", dark).apply()

    fun isInitialized(): Boolean = prefs.getBoolean("initialized", false)
    fun setInitialized(v: Boolean) = prefs.edit().putBoolean("initialized", v).apply()

    fun loadGroups(): List<SubjectGroup> {
        val json = prefs.getString("groups", null) ?: return emptyList()
        val type = object : TypeToken<List<SubjectGroup>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveGroups(groups: List<SubjectGroup>) {
        prefs.edit().putString("groups", gson.toJson(groups)).apply()
    }

    fun loadTasks(): List<TodoTask> {
        val json = prefs.getString("tasks", null) ?: return emptyList()
        val type = object : TypeToken<List<TodoTask>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveTasks(tasks: List<TodoTask>) {
        prefs.edit().putString("tasks", gson.toJson(tasks)).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoAppRoot()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoAppRoot() {
    val context = LocalContext.current
    val storage = remember { TodoStorage(context) }
    val scope = rememberCoroutineScope()

    var lang by remember { mutableStateOf(storage.loadLang()) }
    var isDark by remember { mutableStateOf(storage.loadDarkTheme()) }
    val isAr = lang == "ar"

    val groups = remember { mutableStateListOf<SubjectGroup>() }
    val tasks = remember { mutableStateListOf<TodoTask>() }

    LaunchedEffect(Unit) {
        if (!storage.isInitialized()) {
            val (initGroups, initTasks) = getOfficialMPPSIData()
            groups.addAll(initGroups)
            tasks.addAll(initTasks)
            storage.saveGroups(groups)
            storage.saveTasks(tasks)
            storage.setInitialized(true)
        } else {
            groups.addAll(storage.loadGroups())
            tasks.addAll(storage.loadTasks())
        }
        tasks.forEach { scheduleTaskNotification(context, it) }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) sendInstantTestNotification(context, isAr)
    }

    fun persistAll() {
        storage.saveGroups(groups)
        storage.saveTasks(tasks)
        tasks.forEach { scheduleTaskNotification(context, it) }
    }

    val pagerState = rememberPagerState(pageCount = { 4 })
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("upcoming") }
    var selectedGroupFilter by remember { mutableStateOf("all") }
    var selectedColorFilter by remember { mutableStateOf<Long?>(null) }

    var calendarCursorMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedCalendarDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var weekDrawerMonday by remember { mutableStateOf<String?>(null) }

    var groupsScopeFilter by remember { mutableStateOf("all") }
    var expandedGroupId by remember { mutableStateOf<String?>(null) }

    var editingTask by remember { mutableStateOf<TodoTask?>(null) }
    var showTaskModal by remember { mutableStateOf(false) }
    var showGroupModal by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = Color(0xFF6366F1),
            background = Color(0xFF020617),
            surface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFF1E293B)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF4F46E5),
            background = Color(0xFFF8FAFC),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF1F5F9)
        )
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF4F46E5), Color(0xFF9333EA))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "TO DO",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        if (isAr) "مذكّر المواعيد والمهام" else "Calendrier & Rappels MP-PSI",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    sendInstantTestNotification(context, isAr)
                                }
                            }) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                            }

                            IconButton(onClick = {
                                isDark = !isDark
                                storage.saveDarkTheme(isDark)
                            }) {
                                Icon(
                                    if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                    contentDescription = "Theme"
                                )
                            }

                            Button(
                                onClick = {
                                    lang = if (isAr) "fr" else "ar"
                                    storage.saveLang(lang)
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(if (isAr) "FR" else "عربي", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            editingTask = null
                            showTaskModal = true
                        },
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(28.dp))
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        val navItems = listOf(
                            (if (isAr) "الرئيسية" else "Accueil") to Icons.Outlined.CheckCircle,
                            (if (isAr) "التقويم" else "Calendrier") to Icons.Outlined.CalendarMonth,
                            (if (isAr) "المجموعات" else "Matières") to Icons.Outlined.Layers,
                            (if (isAr) "الإعدادات" else "Paramètres") to Icons.Outlined.Tune
                        )
                        navItems.forEachIndexed { idx, (label, icon) ->
                            NavigationBarItem(
                                selected = pagerState.currentPage == idx,
                                onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) { page ->
                    when (page) {
                        0 -> HomeTabScreen(
                            isAr = isAr,
                            tasks = tasks,
                            groups = groups,
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            statusFilter = statusFilter,
                            onStatusFilterChange = { statusFilter = it },
                            selectedGroupFilter = selectedGroupFilter,
                            onGroupFilterChange = { selectedGroupFilter = it },
                            selectedColorFilter = selectedColorFilter,
                            onColorFilterChange = { selectedColorFilter = it },
                            onToggleComplete = { id ->
                                val idx = tasks.indexOfFirst { it.id == id }
                                if (idx != -1) {
                                    tasks[idx] = tasks[idx].copy(completed = !tasks[idx].completed)
                                    persistAll()
                                }
                            },
                            onEditTask = { task ->
                                editingTask = task
                                showTaskModal = true
                            },
                            onDeleteTask = { id ->
                                tasks.removeAll { it.id == id }
                                persistAll()
                            },
                            onConvertToDay = { id, dateStr ->
                                val idx = tasks.indexOfFirst { it.id == id }
                                if (idx != -1) {
                                    tasks[idx] = tasks[idx].copy(
                                        scope = "day",
                                        date = dateStr,
                                        weekStart = getMondayOfWeek(dateStr)
                                    )
                                    persistAll()
                                }
                            },
                            onConvertToWeek = { id ->
                                val idx = tasks.indexOfFirst { it.id == id }
                                if (idx != -1) {
                                    tasks[idx] = tasks[idx].copy(
                                        scope = "week",
                                        weekStart = getMondayOfWeek(tasks[idx].date)
                                    )
                                    persistAll()
                                }
                            }
                        )

                        1 -> CalendarTabScreen(
                            isAr = isAr,
                            tasks = tasks,
                            groups = groups,
                            cursorMonth = calendarCursorMonth,
                            onMonthChange = { calendarCursorMonth = it },
                            selectedDate = selectedCalendarDate,
                            onSelectDate = { selectedCalendarDate = it },
                            onOpenWeekDrawer = { mondayStr -> weekDrawerMonday = mondayStr },
                            onToggleComplete = { id ->
                                val idx = tasks.indexOfFirst { it.id == id }
                                if (idx != -1) {
                                    tasks[idx] = tasks[idx].copy(completed = !tasks[idx].completed)
                                    persistAll()
                                }
                            },
                            onEditTask = { editingTask = it; showTaskModal = true },
                            onDeleteTask = { id -> tasks.removeAll { it.id == id }; persistAll() },
                            onConvertToDay = { id, d ->
                                val idx = tasks.indexOfFirst { it.id == id }
                                if (idx != -1) {
                                    tasks[idx] = tasks[idx].copy(scope = "day", date = d, weekStart = getMondayOfWeek(d))
                                    persistAll()
                                }
                            },
                            onConvertToWeek = { id ->
                                val idx = tasks.indexOfFirst { it.id == id }
                                if (idx != -1) {
                                    tasks[idx] = tasks[idx].copy(scope = "week", weekStart = getMondayOfWeek(tasks[idx].date))
                                    persistAll()
                                }
                            }
                        )

                        2 -> GroupsTabScreen(
                            isAr = isAr,
                            groups = groups,
                            tasks = tasks,
                            scopeFilter = groupsScopeFilter,
                            onScopeFilterChange = { groupsScopeFilter = it },
                            expandedGroupId = expandedGroupId,
                            onToggleExpand = { gid ->
                                expandedGroupId = if (expandedGroupId == gid) null else gid
                            },
                            onOpenNewGroup = { showGroupModal = true },
                            onRestoreDefaultGroups = {
                                val (defGroups, _) = getOfficialMPPSIData()
                                val existingIds = groups.map { it.id }.toSet()
                                defGroups.filter { it.id !in existingIds }.forEach { groups.add(it) }
                                persistAll()
                            },
                            onDeleteGroup = { gid ->
                                groups.removeAll { it.id == gid }
                                for (i in tasks.indices) {
                                    tasks[i] = tasks[i].copy(groupIds = tasks[i].groupIds.filter { it != gid })
                                }
                                persistAll()
                            },
                            onToggleComplete = { id ->
                                val idx = tasks.indexOfFirst { it.id == id }
                                if (idx != -1) {
                                    tasks[idx] = tasks[idx].copy(completed = !tasks[idx].completed)
                                    persistAll()
                                }
                            },
                            onEditTask = { editingTask = it; showTaskModal = true },
                            onDeleteTask = { id -> tasks.removeAll { it.id == id }; persistAll() },
                            onConvertToDay = { id, d ->
                                val idx = tasks.indexOfFirst { it.id == id }
                                if (idx != -1) {
                                    tasks[idx] = tasks[idx].copy(scope = "day", date = d, weekStart = getMondayOfWeek(d))
                                    persistAll()
                                }
                            },
                            onConvertToWeek = { id ->
                                val idx = tasks.indexOfFirst { it.id == id }
                                if (idx != -1) {
                                    tasks[idx] = tasks[idx].copy(scope = "week", weekStart = getMondayOfWeek(tasks[idx].date))
                                    persistAll()
                                }
                            }
                        )

                        3 -> SettingsTabScreen(
                            isAr = isAr,
                            isDark = isDark,
                            onToggleDark = {
                                isDark = !isDark
                                storage.saveDarkTheme(isDark)
                            },
                            onToggleLang = {
                                lang = if (isAr) "fr" else "ar"
                                storage.saveLang(lang)
                            },
                            onEnableNotif = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    sendInstantTestNotification(context, isAr)
                                }
                            },
                            onTestNotif = { sendInstantTestNotification(context, isAr) },
                            onImportMPPSI = {
                                val (defGroups, defTasks) = getOfficialMPPSIData()
                                groups.clear(); groups.addAll(defGroups)
                                tasks.clear(); tasks.addAll(defTasks)
                                persistAll()
                            },
                            onOpenReset = { showResetDialog = true }
                        )
                    }
                }
            }

            if (showTaskModal) {
                TaskEditorSheet(
                    isAr = isAr,
                    existingTask = editingTask,
                    groups = groups,
                    defaultDate = selectedCalendarDate,
                    onDismiss = { showTaskModal = false },
                    onSave = { generatedTasks ->
                        if (editingTask != null && generatedTasks.isNotEmpty()) {
                            val idx = tasks.indexOfFirst { it.id == editingTask!!.id }
                            if (idx != -1) tasks[idx] = generatedTasks.first()
                        } else {
                            tasks.addAll(generatedTasks)
                        }
                        persistAll()
                        showTaskModal = false
                    }
                )
            }

            if (weekDrawerMonday != null) {
                WeekDrawerSheet(
                    isAr = isAr,
                    mondayStr = weekDrawerMonday!!,
                    tasks = tasks,
                    groups = groups,
                    onDismiss = { weekDrawerMonday = null },
                    onToggleComplete = { id ->
                        val idx = tasks.indexOfFirst { it.id == id }
                        if (idx != -1) {
                            tasks[idx] = tasks[idx].copy(completed = !tasks[idx].completed)
                            persistAll()
                        }
                    },
                    onEditTask = { editingTask = it; showTaskModal = true },
                    onDeleteTask = { id -> tasks.removeAll { it.id == id }; persistAll() },
                    onConvertToDay = { id, d ->
                        val idx = tasks.indexOfFirst { it.id == id }
                        if (idx != -1) {
                            tasks[idx] = tasks[idx].copy(scope = "day", date = d, weekStart = getMondayOfWeek(d))
                            persistAll()
                        }
                    },
                    onConvertToWeek = { id ->
                        val idx = tasks.indexOfFirst { it.id == id }
                        if (idx != -1) {
                            tasks[idx] = tasks[idx].copy(scope = "week", weekStart = getMondayOfWeek(tasks[idx].date))
                            persistAll()
                        }
                    }
                )
            }

            if (showGroupModal) {
                NewGroupDialog(
                    isAr = isAr,
                    onDismiss = { showGroupModal = false },
                    onCreate = { name, colorHex ->
                        groups.add(
                            SubjectGroup(
                                id = "grp_${System.currentTimeMillis()}",
                                nameAr = name,
                                nameFr = name,
                                colorHex = colorHex
                            )
                        )
                        persistAll()
                        showGroupModal = false
                    }
                )
            }

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = {
                        Text(
                            if (isAr) "هل أنت متأكد من مسح كل شيء؟" else "Tout effacer ?",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            if (isAr) "سيتم حذف جميع المواعيد والمجموعات نهائياً."
                            else "Tous vos rendez-vous et matières seront supprimés définitivement."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                tasks.clear()
                                groups.clear()
                                persistAll()
                                showResetDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                        ) {
                            Text(if (isAr) "نعم، امسح الكل" else "Oui, tout effacer")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text(if (isAr) "إلغاء" else "Annuler")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HomeTabScreen(
    isAr: Boolean,
    tasks: List<TodoTask>,
    groups: List<SubjectGroup>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit,
    selectedGroupFilter: String,
    onGroupFilterChange: (String) -> Unit,
    selectedColorFilter: Long?,
    onColorFilterChange: (Long?) -> Unit,
    onToggleComplete: (String) -> Unit,
    onEditTask: (TodoTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onConvertToDay: (String, String) -> Unit,
    onConvertToWeek: (String) -> Unit
) {
    val todayStr = LocalDate.now().toString()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val nextTaskItem = remember(tasks.toList(), nowMillis) {
        tasks.filter { !it.completed }.mapNotNull { t ->
            try {
                val ts = LocalDateTime.of(LocalDate.parse(t.date), LocalTime.parse(t.time))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (ts > nowMillis) t to ts else null
            } catch (_: Exception) {
                null
            }
        }.minByOrNull { it.second }
    }

    val filteredTasks = remember(
        tasks.toList(), searchQuery, statusFilter, selectedGroupFilter, selectedColorFilter
    ) {
        tasks.filter { task ->
            val passStatus = when (statusFilter) {
                "today" -> task.date == todayStr
                "upcoming" -> !task.completed && task.date >= todayStr
                "completed" -> task.completed
                else -> true
            }
            val passGroup = selectedGroupFilter == "all" || task.groupIds.contains(selectedGroupFilter)
            val passColor = selectedColorFilter == null || task.customColors.contains(selectedColorFilter)
            val q = searchQuery.trim().lowercase()
            val passQuery = q.isEmpty() ||
                task.title.lowercase().contains(q) ||
                task.notes.lowercase().contains(q) ||
                task.date.contains(q)

            passStatus && passGroup && passColor && passQuery
        }.sortedWith(compareBy({ it.date }, { it.time }))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (nextTaskItem != null) {
                val (nextTask, targetTs) = nextTaskItem
                val diffSecs = ((targetTs - nowMillis) / 1000).coerceAtLeast(0)
                val d = diffSecs / 86400
                val h = (diffSecs % 86400) / 3600
                val m = (diffSecs % 3600) / 60
                val s = diffSecs % 60

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isAr) "أقرب موعد قادم" else "Prochaine échéance",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.16f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                            Text(
                                "${nextTask.date} • ${nextTask.time}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            nextTask.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "%02d".format(d) to (if (isAr) "يوم" else "jours"),
                                    "%02d".format(h) to (if (isAr) "ساعة" else "heures"),
                                    "%02d".format(m) to (if (isAr) "دقيقة" else "min"),
                                    "%02d".format(s) to (if (isAr) "ثانية" else "sec")
                                ).forEach { (num, unit) ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.16f))
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(num, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                        Text(unit, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        placeholder = {
                            Text(
                                if (isAr) "ابحث عن DS، مباراة، مادة..." else "Rechercher un DS, concours...",
                                fontSize = 13.sp
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val chips = listOf(
                            "upcoming" to (if (isAr) "القادمة" else "À venir"),
                            "all" to (if (isAr) "الكل" else "Tous"),
                            "today" to (if (isAr) "اليوم" else "Aujourd'hui"),
                            "completed" to (if (isAr) "المكتملة" else "Terminés")
                        )
                        items(chips) { (key, label) ->
                            FilterChip(
                                selected = statusFilter == key,
                                onClick = { onStatusFilterChange(key) },
                                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedGroupFilter == "all",
                                onClick = { onGroupFilterChange("all") },
                                label = { Text(if (isAr) "كل المواد" else "Toutes matières", fontSize = 11.sp) }
                            )
                        }
                        items(groups) { grp ->
                            FilterChip(
                                selected = selectedGroupFilter == grp.id,
                                onClick = {
                                    onGroupFilterChange(if (selectedGroupFilter == grp.id) "all" else grp.id)
                                },
                                leadingIcon = {
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(grp.colorHex))
                                    )
                                },
                                label = {
                                    Text(if (isAr) grp.nameAr else grp.nameFr, fontSize = 11.sp)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isAr) "لا توجد مواعيد في هذه القائمة" else "Aucun élément dans cette liste",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                TaskItemCard(
                    isAr = isAr,
                    task = task,
                    groups = groups,
                    onToggleComplete = { onToggleComplete(task.id) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task.id) },
                    onConvertToDay = { d -> onConvertToDay(task.id, d) },
                    onConvertToWeek = { onConvertToWeek(task.id) }
                )
            }
        }
    }
}

@Composable
fun TaskItemCard(
    isAr: Boolean,
    task: TodoTask,
    groups: List<SubjectGroup>,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConvertToDay: (String) -> Unit,
    onConvertToWeek: () -> Unit
) {
    val isWeekScope = task.scope == "week"
    val primaryColor = Color(task.customColors.firstOrNull() ?: 0xFF4F46E5)
    val weekDays = remember(task.weekStart) { getWeekDaysList(task.weekStart) }

    Card(
        shape = RoundedCornerShape(18.dp),
        border = if (isWeekScope) BorderStroke(1.5.dp, Color(0xFF0EA5E9).copy(alpha = 0.55f)) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .heightIn(min = 92.dp)
                    .background(primaryColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = task.completed,
                        onCheckedChange = { onToggleComplete() },
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scopeLabel = if (isWeekScope) {
                        if (isAr) "أسبوع #${getIsoWeekNumber(task.weekStart)}" else "Sem. #${getIsoWeekNumber(task.weekStart)}"
                    } else {
                        "${task.date} • ${task.time}"
                    }

                    Text(
                        text = scopeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWeekScope) Color(0xFF0284C7) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                (if (isWeekScope) Color(0xFF0EA5E9) else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )

                    task.groupIds.mapNotNull { gid -> groups.find { it.id == gid } }.forEach { grp ->
                        Text(
                            text = if (isAr) grp.nameAr else grp.nameFr,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(grp.colorHex),
                            modifier = Modifier
                                .background(Color(grp.colorHex).copy(alpha = 0.12f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (task.notes.isNotBlank()) {
                    Text(
                        task.notes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }

                if (isWeekScope) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (isAr) "تحويل إلى يوم محدد في هذا الأسبوع:" else "Convertir en jour précis :",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(weekDays) { dStr ->
                                val ld = LocalDate.parse(dStr)
                                OutlinedButton(
                                    onClick = { onConvertToDay(dStr) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("${ld.dayOfMonth}/${ld.monthValue}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = onConvertToWeek,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                if (isAr) "إرجاع إلى نطاق أسبوع" else "Repasser en semaine",
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarTabScreen(
    isAr: Boolean,
    tasks: List<TodoTask>,
    groups: List<SubjectGroup>,
    cursorMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    onOpenWeekDrawer: (String) -> Unit,
    onToggleComplete: (String) -> Unit,
    onEditTask: (TodoTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onConvertToDay: (String, String) -> Unit,
    onConvertToWeek: (String) -> Unit
) {
    val firstOfMonth = cursorMonth.atDay(1)
    val startMonday = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastOfMonth = cursorMonth.atEndOfMonth()
    val totalDays = ChronoUnit.DAYS.between(startMonday, lastOfMonth).toInt() + 1
    val totalWeeks = (totalDays + 6) / 7

    val selectedDayTasks = tasks.filter { it.scope == "day" && it.date == selectedDate }
        .sortedBy { it.time }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onMonthChange(cursorMonth.minusMonths(1)) }) {
                            Icon(
                                if (isAr) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                null
                            )
                        }
                        Text(
                            "${cursorMonth.month.name} ${cursorMonth.year}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { onMonthChange(cursorMonth.plusMonths(1)) }) {
                            Icon(
                                if (isAr) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null
                            )
                        }
                    }

                    val headers = if (isAr)
                        listOf("إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت", "أحد")
                    else
                        listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

                    Row(modifier = Modifier.fillMaxWidth()) {
                        headers.forEach { h ->
                            Text(
                                h,
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    for (w in 0 until totalWeeks) {
                        val rowMonday = startMonday.plusWeeks(w.toLong())
                        val mondayStr = rowMonday.toString()
                        val weekScopeTasks = tasks.filter { it.scope == "week" && it.weekStart == mondayStr }
                        val frameColorHex = weekScopeTasks.firstOrNull()?.customColors?.firstOrNull()
                        val frameColor = frameColorHex?.let { Color(it) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (frameColor != null) 2.dp else 1.dp,
                                    color = frameColor ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .background(frameColor?.copy(alpha = 0.08f) ?: Color.Transparent)
                                .clickable { onOpenWeekDrawer(mondayStr) }
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    (if (isAr) "أسبوع #" else "Sem. #") + getIsoWeekNumber(mondayStr),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = frameColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                if (weekScopeTasks.isNotEmpty()) {
                                    Text(
                                        weekScopeTasks.first().title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = frameColor ?: MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (d in 0..6) {
                                    val dayDate = rowMonday.plusDays(d.toLong())
                                    val dStr = dayDate.toString()
                                    val isCurrentMonth = dayDate.month == cursorMonth.month
                                    val isSelected = dStr == selectedDate
                                    val dayDots = tasks.filter { it.date == dStr }
                                        .flatMap { it.customColors }.distinct().take(3)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isCurrentMonth) 0.7f else 0.25f)
                                            )
                                            .clickable { onSelectDate(dStr) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                "${dayDate.dayOfMonth}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = if (isCurrentMonth) 1f else 0.35f)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                dayDots.forEach { cHex ->
                                                    Box(
                                                        Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else Color(cHex))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                (if (isAr) "مواعيد يوم: " else "Échéances du : ") + selectedDate,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

        if (selectedDayTasks.isEmpty()) {
            item {
                Text(
                    if (isAr) "لا توجد مواعيد بيوم محدد في هذا التاريخ." else "Aucun rendez-vous précis à cette date.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        } else {
            items(selectedDayTasks, key = { it.id }) { task ->
                TaskItemCard(
                    isAr = isAr,
                    task = task,
                    groups = groups,
                    onToggleComplete = { onToggleComplete(task.id) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task.id) },
                    onConvertToDay = { d -> onConvertToDay(task.id, d) },
                    onConvertToWeek = { onConvertToWeek(task.id) }
                )
            }
        }
    }
}

@Composable
fun GroupsTabScreen(
    isAr: Boolean,
    groups: List<SubjectGroup>,
    tasks: List<TodoTask>,
    scopeFilter: String,
    onScopeFilterChange: (String) -> Unit,
    expandedGroupId: String?,
    onToggleExpand: (String) -> Unit,
    onOpenNewGroup: () -> Unit,
    onRestoreDefaultGroups: () -> Unit,
    onDeleteGroup: (String) -> Unit,
    onToggleComplete: (String) -> Unit,
    onEditTask: (TodoTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onConvertToDay: (String, String) -> Unit,
    onConvertToWeek: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isAr) "المواد والمجموعات" else "Matières & Groupes",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onRestoreDefaultGroups, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text(if (isAr) "استعادة" else "Restaurer", fontSize = 11.sp)
                    }
                    Button(onClick = onOpenNewGroup, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text(if (isAr) "+ مادة" else "+ Matière", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "all" to (if (isAr) "الكل" else "Tout"),
                    "week" to (if (isAr) "بالأسابيع" else "Par semaine"),
                    "day" to (if (isAr) "بالأيام" else "Par jour")
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = scopeFilter == key,
                        onClick = { onScopeFilterChange(key) },
                        label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }
        }

        items(groups, key = { it.id }) { grp ->
            val grpTasks = tasks.filter { it.groupIds.contains(grp.id) }.filter {
                scopeFilter == "all" || it.scope == scopeFilter
            }.sortedBy { it.date }
            val isExpanded = expandedGroupId == grp.id

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(grp.colorHex).copy(alpha = 0.12f))
                            .clickable { onToggleExpand(grp.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(grp.colorHex)))
                            Text(if (isAr) grp.nameAr else grp.nameFr, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${grpTasks.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(grp.colorHex)
                            )
                            IconButton(onClick = { onDeleteGroup(grp.id) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Outlined.Delete, null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                            }
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                null
                            )
                        }
                    }

                    if (isExpanded) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (grpTasks.isEmpty()) {
                                Text(
                                    if (isAr) "لا توجد مواعيد في هذه المادة" else "Aucun élément",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            } else {
                                grpTasks.forEach { t ->
                                    TaskItemCard(
                                        isAr = isAr,
                                        task = t,
                                        groups = groups,
                                        onToggleComplete = { onToggleComplete(t.id) },
                                        onEdit = { onEditTask(t) },
                                        onDelete = { onDeleteTask(t.id) },
                                        onConvertToDay = { d -> onConvertToDay(t.id, d) },
                                        onConvertToWeek = { onConvertToWeek(t.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabScreen(
    isAr: Boolean,
    isDark: Boolean,
    onToggleDark: () -> Unit,
    onToggleLang: () -> Unit,
    onEnableNotif: () -> Unit,
    onTestNotif: () -> Unit,
    onImportMPPSI: () -> Unit,
    onOpenReset: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                if (isAr) "الإعدادات والبيانات" else "Paramètres & Données",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }

        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isAr) "إشعارات التذكير والصوت" else "Notifications & Son",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onEnableNotif, modifier = Modifier.weight(1f)) {
                            Text(if (isAr) "تفعيل الإشعارات" else "Activer", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = onTestNotif, modifier = Modifier.weight(1f)) {
                            Text(if (isAr) "تجربة التنبيه" else "Tester", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isAr) "الوضع الليلي (Dark Mode)" else "Mode Sombre", fontWeight = FontWeight.Bold)
                        Switch(checked = isDark, onCheckedChange = { onToggleDark() })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isAr) "لغة التطبيق (AR / FR)" else "Langue (AR / FR)", fontWeight = FontWeight.Bold)
                        Button(onClick = onToggleLang) {
                            Text(if (isAr) "العربية (RTL)" else "Français (LTR)", fontSize = 12.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isAr) "جدول MP-PSI 2026-2027" else "Calendrier MP-PSI", fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = onImportMPPSI) {
                            Text(if (isAr) "استيراد الجدول" else "Importer", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onOpenReset,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    if (isAr) "مسح جميع البيانات وتصفير التطبيق (Reset)" else "Tout effacer (Reset complet)",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    isAr: Boolean,
    existingTask: TodoTask?,
    groups: List<SubjectGroup>,
    defaultDate: String,
    onDismiss: () -> Unit,
    onSave: (List<TodoTask>) -> Unit
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var scopeType by remember { mutableStateOf(existingTask?.scope ?: "day") }
    var dateStr by remember { mutableStateOf(existingTask?.date ?: defaultDate) }
    var timeStr by remember { mutableStateOf(existingTask?.time ?: "08:00") }
    var repeatWeeks by remember { mutableIntStateOf(1) }
    var reminderMin by remember { mutableIntStateOf(existingTask?.reminderMinutes ?: 60) }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    val selectedGroups = remember {
        mutableStateListOf<String>().apply {
            if (existingTask != null) addAll(existingTask.groupIds)
            else if (groups.isNotEmpty()) add(groups.first().id)
        }
    }
    var selectedColor by remember {
        mutableLongStateOf(existingTask?.customColors?.firstOrNull() ?: groups.firstOrNull()?.colorHex ?: 0xFF4F46E5)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (existingTask != null) (if (isAr) "تعديل الموعد" else "Modifier")
                else (if (isAr) "إضافة موعد جديد" else "Nouveau rendez-vous / DS"),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (isAr) "عنوان الموعد أو الامتحان *" else "Titre du DS *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = scopeType == "day",
                    onClick = { scopeType = "day" },
                    label = { Text(if (isAr) "يوم محدد" else "Jour précis", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = scopeType == "week",
                    onClick = { scopeType = "week" },
                    label = { Text(if (isAr) "أسبوع كامل" else "Semaine complète", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("YYYY-MM-DD") },
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = timeStr,
                    onValueChange = { timeStr = it },
                    label = { Text("HH:MM") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            if (existingTask == null) {
                Column {
                    Text(
                        (if (isAr) "تكرار أوتوماتيكي لعدة أسابيع: " else "Répétition semaines : ") + repeatWeeks,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = repeatWeeks.toFloat(),
                        onValueChange = { repeatWeeks = it.toInt().coerceIn(1, 28) },
                        valueRange = 1f..28f
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val options = listOf(
                    0 to (if (isAr) "في الوقت" else "À l'heure"),
                    15 to (if (isAr) "قبل 15 د" else "15 min avant"),
                    60 to (if (isAr) "قبل ساعة" else "1h avant"),
                    -1 to (if (isAr) "بدون" else "Sans")
                )
                items(options) { (mins, lbl) ->
                    FilterChip(
                        selected = reminderMin == mins,
                        onClick = { reminderMin = mins },
                        label = { Text(lbl, fontSize = 11.sp) }
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(groups) { grp ->
                    val active = selectedGroups.contains(grp.id)
                    FilterChip(
                        selected = active,
                        onClick = {
                            if (active) selectedGroups.remove(grp.id)
                            else {
                                selectedGroups.add(grp.id)
                                selectedColor = grp.colorHex
                            }
                        },
                        label = { Text(if (isAr) grp.nameAr else grp.nameFr, fontSize = 11.sp) }
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PALETTE_COLORS) { (hex, _) ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (selectedColor == hex) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(if (isAr) "ملاحظات (اختياري)" else "Notes (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val parsedDate = try { LocalDate.parse(dateStr.trim()) } catch (_: Exception) { LocalDate.now() }
                    val validTime = try { LocalTime.parse(timeStr.trim()).toString() } catch (_: Exception) { "08:00" }

                    val count = if (existingTask != null) 1 else repeatWeeks
                    val baseId = System.currentTimeMillis()
                    val list = (0 until count).map { w ->
                        val d = parsedDate.plusWeeks(w.toLong()).toString()
                        val suffix = if (count > 1) " (${if (isAr) "أسبوع" else "Sem."} ${w + 1}/$count)" else ""
                        TodoTask(
                            id = existingTask?.id ?: "tsk_${baseId}_$w",
                            title = title.trim() + suffix,
                            scope = scopeType,
                            date = d,
                            weekStart = getMondayOfWeek(d),
                            time = validTime,
                            reminderMinutes = reminderMin,
                            groupIds = selectedGroups.toList(),
                            customColors = listOf(selectedColor),
                            notes = notes.trim(),
                            completed = existingTask?.completed ?: false
                        )
                    }
                    onSave(list)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isAr) "حفظ الموعد" else "Enregistrer", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekDrawerSheet(
    isAr: Boolean,
    mondayStr: String,
    tasks: List<TodoTask>,
    groups: List<SubjectGroup>,
    onDismiss: () -> Unit,
    onToggleComplete: (String) -> Unit,
    onEditTask: (TodoTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onConvertToDay: (String, String) -> Unit,
    onConvertToWeek: (String) -> Unit
) {
    val weekDays = remember(mondayStr) { getWeekDaysList(mondayStr) }
    val weekTasks = tasks.filter { it.weekStart == mondayStr || it.date in weekDays }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                (if (isAr) "مواعيد الأسبوع #" else "Contenu de la semaine #") + getIsoWeekNumber(mondayStr),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
            Text(
                "${weekDays.first()} → ${weekDays.last()}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            if (weekTasks.isEmpty()) {
                Text(
                    if (isAr) "لا توجد مواعيد مسجلة في هذا الأسبوع." else "Aucun élément cette semaine.",
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(weekTasks, key = { it.id }) { t ->
                        TaskItemCard(
                            isAr = isAr,
                            task = t,
                            groups = groups,
                            onToggleComplete = { onToggleComplete(t.id) },
                            onEdit = { onEditTask(t) },
                            onDelete = { onDeleteTask(t.id) },
                            onConvertToDay = { d -> onConvertToDay(t.id, d) },
                            onConvertToWeek = { onConvertToWeek(t.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun NewGroupDialog(
    isAr: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var colorHex by remember { mutableLongStateOf(PALETTE_COLORS.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAr) "إنشاء مجموعة / مادة" else "Nouvelle matière", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(if (isAr) "مثال: الرياضيات..." else "Ex: Mathématiques...") },
                    singleLine = true
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PALETTE_COLORS) { (hex, _) ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(if (colorHex == hex) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onCreate(name.trim(), colorHex) }) {
                Text(if (isAr) "حفظ" else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isAr) "إلغاء" else "Annuler") }
        }
    )
}
