package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.HabitEntity
import com.example.data.database.HabitLogEntity
import com.example.data.database.HabitWithLog
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun DisciplineTrackerScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val habitsWithLogs by viewModel.habitsWithLogsForSelectedDate.collectAsState()
    val allHabitLogs by viewModel.repository.allHabitLogs.collectAsState(initial = emptyList())
    val selectedDate by viewModel.selectedDate.collectAsState()
    val profile by viewModel.profileState.collectAsState()

    var showCreateHabitDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<HabitEntity?>(null) }
    var showAnalyticsSection by remember { mutableStateOf(true) }

    // Selected week computation
    val calendarWeeks = remember { getCalendarDaysOfCurrentWeek() }

    // Calculations
    val completedCount = habitsWithLogs.count { it.log?.isCompleted == true }
    val totalCount = habitsWithLogs.size
    val completionPercent = if (totalCount > 0) {
        ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt()
    } else {
        0
    }

    // Compute active habit streak
    val habitStreakCount = remember(allHabitLogs) {
        calculateHabitStreak(allHabitLogs)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MatteBlack,
        topBar = {
            Surface(
                color = MatteBlack,
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.03f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PremiumGold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DISCIPLINE ENGINE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PremiumGold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "AASHIQS+ OFFLINE OS v1.4",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SubduedGray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { showCreateHabitDialog = true },
                        modifier = Modifier
                            .background(PremiumGold.copy(alpha = 0.1f), CircleShape)
                            .border(1.dp, PremiumGold.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Habit",
                            tint = PremiumGold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Radial Glow Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PremiumGold.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            radius = 1200f
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Immersive Status Analytics Ring Header
                item {
                    GlassmorphicAnalyticsHeader(
                        completionPercent = completionPercent,
                        completedCount = completedCount,
                        totalCount = totalCount,
                        streak = habitStreakCount,
                        totalXP = profile.totalXP,
                        level = profile.level
                    )
                }

                // Interactive 7-Day Week Calendar Strip
                item {
                    Text(
                        text = "TEMPORAL TIMELINE",
                        style = MaterialTheme.typography.labelMedium,
                        color = PremiumGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    WeeklyCalendarStrip(
                        days = calendarWeeks,
                        selectedDate = selectedDate,
                        allLogs = allHabitLogs,
                        totalHabitsCount = totalCount,
                        onDateSelected = { dateStr ->
                            viewModel.setSelectedDate(dateStr)
                        }
                    )
                }

                // Analytics consistency heatmap toggleable matrix
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CONSISTENCY MATRIX",
                            style = MaterialTheme.typography.labelMedium,
                            color = PremiumGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        IconButton(
                            onClick = { showAnalyticsSection = !showAnalyticsSection }
                        ) {
                            Icon(
                                imageVector = if (showAnalyticsSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Matrix",
                                tint = PremiumGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showAnalyticsSection,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        DisciplineConsistencyHeatmap(
                            allLogs = allHabitLogs,
                            habitsCount = totalCount
                        )
                    }
                }

                // Habits Label Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DAILY HYPER-HABITS",
                            style = MaterialTheme.typography.labelMedium,
                            color = PremiumGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Text(
                            text = if (selectedDate == viewModel.getTodayDateString()) "TODAY" else selectedDate,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WarmWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (habitsWithLogs.isEmpty()) {
                    item {
                        EmptyHabitNotificationCard(
                            onCreateClick = { showCreateHabitDialog = true }
                        )
                    }
                } else {
                    itemsIndexed(habitsWithLogs, key = { _, h -> h.habit.id }) { index, habitWithLog ->
                        HabitItemInteractiveCard(
                            habitWithLog = habitWithLog,
                            selectedDate = selectedDate,
                            onProgressLogged = { progressVal, isComplete ->
                                viewModel.logHabitProgress(
                                    habitId = habitWithLog.habit.id,
                                    date = selectedDate,
                                    progressValue = progressVal,
                                    isCompleted = isComplete,
                                    xpReward = habitWithLog.habit.xpReward
                                )
                            },
                            onMoveUp = if (index > 0) {
                                {
                                    val newList = habitsWithLogs.map { it.habit }.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    viewModel.reorderHabits(newList)
                                }
                            } else null,
                            onMoveDown = if (index < habitsWithLogs.size - 1) {
                                {
                                    val newList = habitsWithLogs.map { it.habit }.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    viewModel.reorderHabits(newList)
                                }
                            } else null,
                            onEditClick = {
                                habitToEdit = habitWithLog.habit
                            },
                            onDeleteClick = {
                                viewModel.deleteHabit(habitWithLog.habit.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog for Habit Creation
    if (showCreateHabitDialog) {
        HabitCustomizerDialog(
            habit = null,
            onDismiss = { showCreateHabitDialog = false },
            onSave = { h ->
                viewModel.createHabit(
                    title = h.title,
                    description = h.description,
                    category = h.category,
                    type = h.type,
                    icon = h.icon,
                    color = h.color,
                    repeatSchedule = h.repeatSchedule,
                    reminderTime = h.reminderTime,
                    dailyTargetValue = h.dailyTargetValue,
                    targetUnit = h.targetUnit,
                    xpReward = h.xpReward
                )
                showCreateHabitDialog = false
            }
        )
    }

    // Dialog for Habit Editing
    if (habitToEdit != null) {
        HabitCustomizerDialog(
            habit = habitToEdit,
            onDismiss = { habitToEdit = null },
            onSave = { updated ->
                viewModel.updateHabit(updated)
                habitToEdit = null
            }
        )
    }
}

// ==========================================
// COMPOSABLE UI MODULES
// ==========================================

@Composable
fun GlassmorphicAnalyticsHeader(
    completionPercent: Int,
    completedCount: Int,
    totalCount: Int,
    streak: Int,
    totalXP: Long,
    level: String
) {
    Surface(
        color = CharcoalGray.copy(alpha = 0.55f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 15.dp, shape = RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DISCIPLINE RANK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumGold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Lv. $level",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = WarmWhite
                        )
                        Text(
                            text = "Accumulated $totalXP XP overall",
                            fontSize = 11.sp,
                            color = SubduedGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Circular Progress Ring Area
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Track
                            drawCircle(
                                color = Color.White.copy(alpha = 0.05f),
                                radius = size.minDimension / 2.3f,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            // Progress
                            drawArc(
                                color = PremiumGold,
                                startAngle = -90f,
                                sweepAngle = (completionPercent.toFloat() / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx()),
                                topLeft = androidx.compose.ui.geometry.Offset((size.width - size.minDimension / 1.15f) / 2, (size.height - size.minDimension / 1.15f) / 2),
                                size = androidx.compose.ui.geometry.Size(size.minDimension / 1.15f, size.minDimension / 1.15f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$completionPercent%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PremiumGold
                            )
                            Text(
                                text = "CORE SCORE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = WarmWhite,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.06f))

                // Consistency Statistics indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PremiumGold.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, PremiumGold.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = "Streak",
                                tint = PremiumGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "$streak DAYS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = WarmWhite
                            )
                            Text(
                                text = "HABIT STREAK",
                                fontSize = 9.sp,
                                color = SubduedGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.04f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = "Completed",
                                tint = PremiumGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "$completedCount / $totalCount",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = WarmWhite
                            )
                            Text(
                                text = "COMPLETED",
                                fontSize = 9.sp,
                                color = SubduedGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyCalendarStrip(
    days: List<Pair<Date, String>>,
    selectedDate: String,
    allLogs: List<HabitLogEntity>,
    totalHabitsCount: Int,
    onDateSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        days.forEach { (date, formattedString) ->
            val isSelected = formattedString == selectedDate
            val isToday = formattedString == todayStr

            val dayName = SimpleDateFormat("E", Locale.US).format(date).take(1).uppercase()
            val dayOfMonth = SimpleDateFormat("d", Locale.US).format(date)

            // Calculate progress percentage for this specific day
            val dayLogs = allLogs.filter { it.date == formattedString }
            val completedLogsCount = dayLogs.count { it.isCompleted }
            val totalDayHabits = if (totalHabitsCount > 0) totalHabitsCount else 4
            val progressOnDay = if (totalDayHabits > 0) {
                completedLogsCount.toFloat() / totalDayHabits.toFloat()
            } else {
                0f
            }.coerceIn(0f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) PremiumGold.copy(alpha = 0.12f)
                        else if (isToday) Color.White.copy(alpha = 0.03f)
                        else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) PremiumGold.copy(alpha = 0.5f)
                        else if (isToday) PremiumGold.copy(alpha = 0.15f)
                        else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onDateSelected(formattedString) }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = dayName,
                    fontSize = 11.sp,
                    color = if (isSelected) PremiumGold else SubduedGray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayOfMonth,
                    fontSize = 15.sp,
                    color = if (isSelected) PremiumGold else WarmWhite,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Beautiful micro dot representing consistency percentage
                Box(
                    modifier = Modifier.size(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = if (progressOnDay >= 1f) PremiumGold
                            else if (progressOnDay > 0f) PremiumGold.copy(alpha = 0.45f)
                            else Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DisciplineConsistencyHeatmap(
    allLogs: List<HabitLogEntity>,
    habitsCount: Int
) {
    Surface(
        color = CharcoalGray.copy(alpha = 0.4f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "RETROSPECTIVE COMPLIANCE (LAST 28 DAYS)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PremiumGold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Renders 4 columns of 7 blocks representing the calendar consistency
            val columnsCount = 7
            val rowsCount = 4
            val totalBlocks = columnsCount * rowsCount

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -totalBlocks + 1)

            val gridDates = remember(allLogs) {
                List(totalBlocks) {
                    val date = calendar.time
                    val dateStr = sdf.format(date)
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    dateStr
                }
            }

            // Consistency level indicator rows
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val blockWidth = (maxWidth - (6.dp * (columnsCount - 1))) / columnsCount
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in 0 until rowsCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0 until columnsCount) {
                                val blockIdx = row * columnsCount + col
                                val dateStr = gridDates.getOrElse(blockIdx) { "" }

                                val dLogs = allLogs.filter { it.date == dateStr }
                                val cCount = dLogs.count { it.isCompleted }
                                val effectiveHabitsCount = if (habitsCount > 0) habitsCount else 4
                                val complRatio = if (effectiveHabitsCount > 0) {
                                    cCount.toFloat() / effectiveHabitsCount.toFloat()
                                } else 0f

                                val color = when {
                                    complRatio >= 1.0f -> PremiumGold
                                    complRatio >= 0.75f -> PremiumGold.copy(alpha = 0.7f)
                                    complRatio >= 0.5f -> PremiumGold.copy(alpha = 0.45f)
                                    complRatio >= 0.25f -> PremiumGold.copy(alpha = 0.25f)
                                    complRatio > 0f -> PremiumGold.copy(alpha = 0.1f)
                                    else -> Color.White.copy(alpha = 0.04f)
                                }

                                TooltipArea(dateStr, cCount, effectiveHabitsCount) {
                                    Box(
                                        modifier = Modifier
                                            .width(blockWidth)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(color)
                                            .border(
                                                width = 0.5.dp,
                                                color = if (complRatio >= 1.0f) SoftGoldGlow.copy(alpha = 0.35f) else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
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
fun TooltipArea(
    dateStr: String,
    completed: Int,
    total: Int,
    content: @Composable () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    showTooltip = true
                    tryAwaitRelease()
                    showTooltip = false
                }
            )
        }
    ) {
        content()
        if (showTooltip) {
            Box(
                modifier = Modifier
                    .offset(y = (-35).dp)
                    .background(CharcoalGray, RoundedCornerShape(6.dp))
                    .border(0.5.dp, PremiumGold, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text = "$dateStr: $completed/$total completed",
                    color = PremiumGold,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitItemInteractiveCard(
    habitWithLog: HabitWithLog,
    selectedDate: String,
    onProgressLogged: (Float, Boolean) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val habit = habitWithLog.habit
    val log = habitWithLog.log
    val coroutineScope = rememberCoroutineScope()

    val currentProgress = log?.progressValue ?: 0.0f
    val isCompleted = log?.isCompleted == true

    // Hold-to-Complete State
    var holdTriggerProgress by remember { mutableStateOf(0f) }
    val isHoldingActive = remember { mutableStateOf(false) }

    // Floating UI state actions visible
    var isExpandedByTap by remember { mutableStateOf(false) }

    // Launch Hold timer
    LaunchedEffect(isHoldingActive.value) {
        if (isHoldingActive.value) {
            while (holdTriggerProgress < 1.0f && isHoldingActive.value) {
                delay(20)
                holdTriggerProgress += 0.04f
            }
            if (holdTriggerProgress >= 1.0f) {
                onProgressLogged(1.0f, true)
                holdTriggerProgress = 0f
                isHoldingActive.value = false
            }
        } else {
            // Cool spring reset
            while (holdTriggerProgress > 0f) {
                delay(15)
                holdTriggerProgress = (holdTriggerProgress - 0.1f).coerceAtLeast(0f)
            }
        }
    }

    Surface(
        color = CharcoalGray.copy(alpha = if (isCompleted) 0.65f else 0.45f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCompleted) PremiumGold.copy(alpha = 0.45f)
            else Color.White.copy(alpha = 0.06f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpandedByTap = !isExpandedByTap }
            .shadow(
                elevation = if (isExpandedByTap) 10.dp else 2.dp,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Category-based Accent Color indicators
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(habit.color))
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = habit.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(habit.color).copy(alpha = 0.85f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = habit.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) PremiumGold else WarmWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Checkbox habit or other specific log controls
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(46.dp)
                ) {
                    if (habit.type == "checkbox") {
                        // Hold to Complete circular button visual representation
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) PremiumGold.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (isCompleted) PremiumGold else Color.White.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                                .pointerInput(isCompleted) {
                                    detectTapGestures(
                                        onPress = {
                                            if (!isCompleted) {
                                                try {
                                                    isHoldingActive.value = true
                                                    tryAwaitRelease()
                                                } finally {
                                                    isHoldingActive.value = false
                                                }
                                            }
                                        },
                                        onTap = {
                                            onProgressLogged(
                                                if (isCompleted) 0f else 1f,
                                                !isCompleted
                                            )
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (holdTriggerProgress > 0f) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = PremiumGold,
                                        startAngle = -90f,
                                        sweepAngle = holdTriggerProgress * 360f,
                                        useCenter = false,
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.Done else Icons.Default.Star,
                                contentDescription = "Hold to Complete",
                                tint = if (isCompleted) PremiumGold else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            tint = PremiumGold,
                            contentDescription = "Done",
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        // Dynamic standard mini ring for numbers
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.03f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (habit.type == "timer") Icons.Default.Timer else Icons.Default.TrendingUp,
                                tint = Color(habit.color).copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp),
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            if (!habit.description.isBlank()) {
                Text(
                    text = habit.description,
                    fontSize = 11.sp,
                    color = SubduedGray,
                    lineHeight = 15.sp
                )
            }

            // Custom UI elements based on Habit Log Type
            when (habit.type) {
                "numeric" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Logged: ${currentProgress.toInt()} / ${habit.dailyTargetValue.toInt()} ${habit.targetUnit}",
                            fontSize = 12.sp,
                            color = WarmWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val nextVal = (currentProgress - 1f).coerceAtLeast(0f)
                                    val done = nextVal >= habit.dailyTargetValue
                                    onProgressLogged(nextVal, done)
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White.copy(alpha = 0.03f), CircleShape)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Text("-", color = WarmWhite, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }

                            IconButton(
                                onClick = {
                                    val nextVal = currentProgress + 1f
                                    val done = nextVal >= habit.dailyTargetValue
                                    onProgressLogged(nextVal, done)
                                },
                                modifier = Modifier
                                        .size(28.dp)
                                        .background(PremiumGold.copy(alpha = 0.05f), CircleShape)
                                        .border(0.5.dp, PremiumGold.copy(alpha = 0.25f), CircleShape)
                            ) {
                                Text("+", color = PremiumGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }

                    // Progress bar slider preview
                    LinearProgressIndicator(
                        progress = (currentProgress / habit.dailyTargetValue).coerceIn(0f, 1f),
                        color = PremiumGold,
                        trackColor = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                "timer" -> {
                    // Stopwatch timer section in composition
                    var activeTimerValue by remember { mutableStateOf(currentProgress) }
                    var isStopwatchRunning by remember { mutableStateOf(false) }

                    LaunchedEffect(isStopwatchRunning) {
                        if (isStopwatchRunning) {
                            while (isStopwatchRunning) {
                                delay(1000)
                                activeTimerValue += 1f
                                onProgressLogged(
                                    activeTimerValue,
                                    activeTimerValue >= habit.dailyTargetValue
                                )
                            }
                        }
                    }

                    val minsFormatted = (activeTimerValue / 60).toInt()
                    val secsFormatted = (activeTimerValue % 60).toInt()
                    val displayTime = String.format("%02d:%02d", minsFormatted, secsFormatted)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Elapsed: $displayTime",
                                fontSize = 12.sp,
                                color = if (activeTimerValue >= habit.dailyTargetValue) PremiumGold else WarmWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val unitText = if (habit.type == "timer") {
                                val seconds = habit.dailyTargetValue.toInt()
                                val h = seconds / 3600
                                val m = (seconds % 3600) / 60
                                val s = seconds % 60
                                if (h > 0) String.format("/ %02dh %02dm %02ds", h, m, s)
                                else String.format("/ %02dm %02ds", m, s)
                            } else {
                                "/ ${habit.dailyTargetValue.toInt()} ${habit.targetUnit}"
                            }
                            Text(
                                text = unitText,
                                fontSize = 11.sp,
                                color = SubduedGray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isStopwatchRunning = !isStopwatchRunning },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (isStopwatchRunning) Color.Red.copy(alpha = 0.15f)
                                        else PremiumGold.copy(alpha = 0.1f),
                                        CircleShape
                                    )
                                    .border(
                                        0.5.dp,
                                        if (isStopwatchRunning) Color.Red else PremiumGold.copy(alpha = 0.35f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isStopwatchRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    tint = if (isStopwatchRunning) Color.Red else PremiumGold,
                                    modifier = Modifier.size(16.dp),
                                    contentDescription = null
                                )
                            }

                            // Manual Input button
                            IconButton(
                                onClick = {
                                    val done = !isCompleted
                                    onProgressLogged(
                                        if (done) habit.dailyTargetValue else 0f,
                                        done
                                    )
                                    activeTimerValue = if (done) habit.dailyTargetValue else 0f
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.03f), CircleShape)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isCompleted) Icons.Default.Close else Icons.Default.DoneAll,
                                    tint = WarmWhite,
                                    modifier = Modifier.size(14.dp),
                                    contentDescription = null
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = (activeTimerValue / habit.dailyTargetValue).coerceIn(0f, 1f),
                        color = PremiumGold,
                        trackColor = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            // Expanded administrative row layout
            AnimatedVisibility(
                visible = isExpandedByTap,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Reorder controls
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onMoveUp?.invoke() },
                                enabled = onMoveUp != null,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = if (onMoveUp != null) PremiumGold else SubduedGray.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onMoveDown?.invoke() },
                                enabled = onMoveDown != null,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = if (onMoveDown != null) PremiumGold else SubduedGray.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Edit / Delete Controls
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.03f), CircleShape)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = PremiumGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier
                                    .background(Color.Red.copy(alpha = 0.1f), CircleShape)
                                    .border(0.5.dp, Color.Red.copy(alpha = 0.35f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHabitNotificationCard(
    onCreateClick: () -> Unit
) {
    Surface(
        color = CharcoalGray.copy(alpha = 0.35f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = PremiumGold.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "NO HABITS DEFINED",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PremiumGold,
                letterSpacing = 1.sp
            )

            Text(
                text = "AASHIQS+ Discipline Engine has detected clean states. Prepare your daily discipline targets immediately.",
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = SubduedGray,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "INITIALIZE TARGET",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

// Dialog to handle full configurations of create/modify habits
@Composable
fun HabitCustomizerDialog(
    habit: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (HabitEntity) -> Unit
) {
    var title by remember { mutableStateOf(habit?.title ?: "") }
    var description by remember { mutableStateOf(habit?.description ?: "") }
    var category by remember { mutableStateOf(habit?.category ?: "Learning") }
    var type by remember { mutableStateOf(habit?.type ?: "checkbox") }
    var icon by remember { mutableStateOf(habit?.icon ?: "Star") }
    var color by remember { mutableStateOf(habit?.color ?: 0xFFD4AF37) }
    var targetValue by remember { mutableStateOf(habit?.dailyTargetValue?.toString() ?: "1.0") }
    var targetUnit by remember { mutableStateOf(habit?.targetUnit ?: "times") }
    var xpValue by remember { mutableStateOf(habit?.xpReward?.toString() ?: "10") }

    val categoriesList = listOf("Looksmaxxing", "Fitness", "Learning", "Productivity", "Health", "Spiritual", "Custom")
    val typesList = listOf("checkbox", "numeric", "timer")

    val curatedColors = listOf(
        0xFFD4AF37, // Gold
        0xFF8E711A, // Bronze
        0xFFE15A5A, // Crimson Gym
        0xFF4A90E2, // Aqua Blue
        0xFF52D3A2, // Emerald
        0xFF9013FE, // Amethyst
        0xFFF5A623  // Sunset Sunset
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MatteBlack,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(25.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = if (habit == null) "INITIALIZE NEW HABIT" else "CONFIGURE DAILY HABIT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PremiumGold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize details to map correct targets on your dashboard.",
                        fontSize = 11.sp,
                        color = SubduedGray
                    )
                }

                // Title Input
                item {
                    Text("HABIT TITLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g., Read Philosophy for 30 minutes", color = SubduedGray.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WarmWhite,
                            unfocusedTextColor = WarmWhite,
                            focusedBorderColor = PremiumGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )
                }

                // Description Input
                item {
                    Text("SHORT DESCRIPTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("e.g. Focus on deep work with zero notifications", color = SubduedGray.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WarmWhite,
                            unfocusedTextColor = WarmWhite,
                            focusedBorderColor = PremiumGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                // Category Selection Carousel
                item {
                    Text("CATEGORIZATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoriesList.size) { index ->
                            val cat = categoriesList[index]
                            val isSel = cat == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSel) PremiumGold.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.03f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) PremiumGold else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { category = cat }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) PremiumGold else WarmWhite
                                )
                            }
                        }
                    }
                }

                // Type of Habit selection
                item {
                    Text("METRIC LOGGER TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        typesList.forEach { typeOption ->
                            val isSel = typeOption == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSel) PremiumGold.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.03f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) PremiumGold else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        type = typeOption
                                        if (typeOption == "timer") {
                                            targetValue = "1800" // 30 minutes in seconds
                                            targetUnit = "sec"
                                        } else if (typeOption == "numeric") {
                                            targetValue = "8"
                                            targetUnit = "glasses"
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeOption.uppercase(),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) PremiumGold else WarmWhite
                                )
                            }
                        }
                    }
                }

                // If non-checkbox, target values
                if (type != "checkbox") {
                    item {
                        if (type == "timer") {
                            // Premium Glassmorphic Duration Picker
                            var pickedHours by remember { 
                                val totalSecs = targetValue.toFloatOrNull()?.toInt() ?: 1800
                                mutableStateOf(totalSecs / 3600) 
                            }
                            var pickedMinutes by remember { 
                                val totalSecs = targetValue.toFloatOrNull()?.toInt() ?: 1800
                                mutableStateOf((totalSecs % 3600) / 60) 
                            }
                            var pickedSeconds by remember { 
                                val totalSecs = targetValue.toFloatOrNull()?.toInt() ?: 1800
                                mutableStateOf(totalSecs % 60) 
                            }

                            // Automatically synchronize targetValue and targetUnit
                            LaunchedEffect(pickedHours, pickedMinutes, pickedSeconds) {
                                val totalSecs = (pickedHours * 3600) + (pickedMinutes * 60) + pickedSeconds
                                targetValue = totalSecs.toString()
                                targetUnit = "sec"
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "DURATION CONFIGURATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumGold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Clock text displays
                                Text(
                                    text = String.format("%02d hr | %02d min | %02d sec", pickedHours, pickedMinutes, pickedSeconds),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PremiumGold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Row of selectors
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // HOURS COLUMN
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("HOURS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SubduedGray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        IconButton(
                                            onClick = { pickedHours = (pickedHours + 1).coerceAtMost(23) },
                                            modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.03f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Add Hour", tint = PremiumGold, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = String.format("%02d", pickedHours),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarmWhite,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        IconButton(
                                            onClick = { pickedHours = (pickedHours - 1).coerceAtLeast(0) },
                                            modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.03f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sub Hour", tint = PremiumGold, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Separator
                                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.15f))

                                    // MINUTES COLUMN
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("MINUTES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SubduedGray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        IconButton(
                                            onClick = { pickedMinutes = (pickedMinutes + 1).coerceAtMost(59) },
                                            modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.03f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Add Minute", tint = PremiumGold, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = String.format("%02d", pickedMinutes),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarmWhite,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        IconButton(
                                            onClick = { pickedMinutes = (pickedMinutes - 1).coerceAtLeast(0) },
                                            modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.03f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sub Minute", tint = PremiumGold, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Separator
                                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.15f))

                                    // SECONDS COLUMN
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("SECONDS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SubduedGray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        IconButton(
                                            onClick = { pickedSeconds = (pickedSeconds + 1).coerceAtMost(59) },
                                            modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.03f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Add Second", tint = PremiumGold, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = String.format("%02d", pickedSeconds),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarmWhite,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        IconButton(
                                            onClick = { pickedSeconds = (pickedSeconds - 1).coerceAtLeast(0) },
                                            modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.03f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sub Second", tint = PremiumGold, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = Color.White.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "QUICK SECURE PRESETS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SubduedGray,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Preset Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val presets = listOf(
                                        Pair("5 Min", 300),
                                        Pair("10 Min", 600),
                                        Pair("15 Min", 900),
                                        Pair("30 Min", 1800),
                                        Pair("1 Hr", 3600)
                                    )
                                    presets.forEach { (label, durationSecs) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.03f))
                                                .border(0.5.dp, PremiumGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    pickedHours = durationSecs / 3600
                                                    pickedMinutes = (durationSecs % 3600) / 60
                                                    pickedSeconds = durationSecs % 60
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PremiumGold
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DAILY TARGET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = targetValue,
                                        onValueChange = { targetValue = it },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = WarmWhite,
                                            unfocusedTextColor = WarmWhite,
                                            focusedBorderColor = PremiumGold,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("UNIT TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = targetUnit,
                                        onValueChange = { targetUnit = it },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = WarmWhite,
                                            unfocusedTextColor = WarmWhite,
                                            focusedBorderColor = PremiumGold,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Experience Reward Value & Custom Color Selection Row
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("XP REWARD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = xpValue,
                                onValueChange = { xpValue = it },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = WarmWhite,
                                    unfocusedTextColor = WarmWhite,
                                    focusedBorderColor = PremiumGold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("GLOW COLOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                curatedColors.forEach { cLong ->
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(cLong))
                                            .border(
                                                width = if (color == cLong) 1.5.dp else 0.dp,
                                                color = WarmWhite,
                                                shape = CircleShape
                                            )
                                            .clickable { color = cLong }
                                    )
                                }
                            }
                        }
                    }
                }

                // Spacer and actions
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmWhite),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CANCEL")
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val checkedTargetVal = targetValue.toFloatOrNull() ?: 1.0f
                                    val checkedXPVal = xpValue.toIntOrNull() ?: 10
                                    onSave(
                                        HabitEntity(
                                            id = habit?.id ?: UUID.randomUUID().toString(),
                                            title = title,
                                            description = description,
                                            category = category,
                                            type = type,
                                            icon = icon,
                                            color = color,
                                            repeatSchedule = "daily",
                                            reminderTime = "08:00",
                                            dailyTargetValue = checkedTargetVal,
                                            targetUnit = targetUnit,
                                            xpReward = checkedXPVal,
                                            displayOrder = habit?.displayOrder ?: 0
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, contentColor = Color.Black),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("COMPLETE INSTANCE", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// CORE COMPUTATION HELPERS
// ==========================================

fun getCalendarDaysOfCurrentWeek(): List<Pair<Date, String>> {
    val list = mutableListOf<Pair<Date, String>>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    for (i in 0 until 7) {
        list.add(Pair(calendar.time, sdf.format(calendar.time)))
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    return list
}

fun calculateHabitStreak(logs: List<HabitLogEntity>): Int {
    if (logs.isEmpty()) return 0
    
    val dateGroups = logs.filter { it.isCompleted }.groupBy { it.date }
    if (dateGroups.isEmpty()) return 0

    val timeZone = TimeZone.getDefault()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Parse each completed date to epoch days
    val completedEpochDays = dateGroups.keys.mapNotNull { dStr ->
        try {
            val date = sdf.parse(dStr)
            date?.let { (it.time + timeZone.getOffset(it.time)) / (24 * 60 * 60 * 1000L) }
        } catch (e: Exception) {
            null
        }
    }.distinct().sortedDescending()

    if (completedEpochDays.isEmpty()) return 0

    val todayMs = System.currentTimeMillis()
    val todayEpochDay = (todayMs + timeZone.getOffset(todayMs)) / (24 * 60 * 60 * 1000L)
    val latestDay = completedEpochDays.first()

    // If latest completed day is older than yesterday, streak is broken
    if (todayEpochDay - latestDay > 1L) {
        return 0
    }

    var streak = 1
    var expectedDay = latestDay
    for (i in 1 until completedEpochDays.size) {
        if (completedEpochDays[i] == expectedDay - 1) {
            streak++
            expectedDay = completedEpochDays[i]
        } else if (completedEpochDays[i] < expectedDay - 1) {
            break
        }
    }
    return streak
}
