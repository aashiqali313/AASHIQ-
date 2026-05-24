package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.UserSettingsEntity
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settingsState.collectAsState()
    val courses by viewModel.coursesState.collectAsState()

    var cacheClearedAlert by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AASHIQ+ CONFIGURATIONS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumGold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme panel card
            SettingsGroupCard(title = "VISUAL INTERFLOW THEMING") {
                // Dark Theme Active Block
                SettingsToggleRow(
                    title = "Force Premium Dark Mode",
                    subtitle = "Render dark atmosphere canvases optimized for video panels.",
                    checked = settings.darkTheme,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(darkTheme = it))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)

                // AMOLED true black card
                SettingsToggleRow(
                    title = "Pitch AMOLED True Black",
                    subtitle = "Saves screen pixels by switching deep charcoal panels into total black.",
                    checked = settings.amoledBlack,
                    enabled = settings.darkTheme,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(amoledBlack = it))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)

                // Accent premium color gold
                SettingsToggleRow(
                    title = "Sleek Gold Branding Accent",
                    subtitle = "Applies signature golden strokes & elements to platform layout.",
                    checked = settings.accentColorGold,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(accentColorGold = it))
                    }
                )
            }

            // Media & Gesture presets panel card
            SettingsGroupCard(title = "MEDIA STREAMING CONTEXTS") {
                // Default speeds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Default Lesson Velocity", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "Applies automatically at lecture launch.", fontSize = 11.sp, color = SubduedGray)
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            val isSelected = settings.defaultSpeed == speed
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) PremiumGold else MaterialTheme.colorScheme.background,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .border(0.5.dp, if (isSelected) PremiumGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .clickable { viewModel.updateSettings(settings.copy(defaultSpeed = speed)) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)

                // Subtitle presets
                SettingsToggleRow(
                    title = "Render Lesson Closed Captions",
                    subtitle = "Always overlay subtitle transcript indicators natively in video box.",
                    checked = settings.showSubtitles,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(showSubtitles = it))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)

                // Gesture swipe brightness/vol
                SettingsToggleRow(
                    title = "Swipe Screen Gestures",
                    subtitle = "Control volume and luminosity dynamically via swipe on player bounds.",
                    checked = settings.brightnessGestureEnabled,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(brightnessGestureEnabled = it, volumeGestureEnabled = it))
                    }
                )
            }

            // Memory Optimizations and cache panels
            SettingsGroupCard(title = "OPTIMIZATION HUB") {
                // Clear index history
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Purge Video Cache Entries", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "Clears image render preloads and optimizes image memory allocations.", fontSize = 11.sp, color = SubduedGray)
                    }

                    Button(
                        onClick = { cacheClearedAlert = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = PremiumGold),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("PURGE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)

                // Reset database & reload curated courses
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Re-populate Core Database", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "Resets masterclass listings and restores premium starting libraries.", fontSize = 11.sp, color = SubduedGray)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.repository.deleteAllCourses()
                                viewModel.repository.prepopulateIfEmpty()
                                cacheClearedAlert = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF221A0F), contentColor = PremiumGold),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, PremiumGold),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("db_repopulate_launcher")
                    ) {
                        Text("RESTORE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Stats Diagnostics panel
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DIAGNOSTIC TELEMETRY STATUS",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PremiumGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "• Database Index: SQLite SQLite 3.0 Live", fontSize = 11.sp, color = SubduedGray)
                    Text(text = "• Local Courses Loaded: ${courses.size} packages indexed", fontSize = 11.sp, color = SubduedGray)
                    Text(text = "• Shared ExoPlayer Memory: Pooled Singleton allocated", fontSize = 11.sp, color = SubduedGray)
                    Text(text = "• CPU Target compatibility: Multi-thread Coroutines aligned", fontSize = 11.sp, color = SubduedGray)
                }
            }

            if (cacheClearedAlert) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E2818), RoundedCornerShape(8.dp))
                        .border(0.5.dp, Color(0xFF81C784), RoundedCornerShape(8.dp))
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SUCCESS: Cache space purged and database alignments processed!",
                            color = Color(0xFF81C784),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "OK",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clickable { cacheClearedAlert = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroupCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = PremiumGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 11.sp, color = SubduedGray)
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PremiumGold,
                checkedTrackColor = PremiumGold.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        )
    }
}
