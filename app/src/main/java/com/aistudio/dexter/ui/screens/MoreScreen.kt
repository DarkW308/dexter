package com.aistudio.dexter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dexter.data.model.AlarmItem
import com.aistudio.dexter.data.model.AuditLog
import com.aistudio.dexter.data.model.GoalItem
import com.aistudio.dexter.data.model.ProjectItem
import com.aistudio.dexter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MoreScreen(
    goals: List<GoalItem>,
    projects: List<ProjectItem>,
    alarms: List<AlarmItem>,
    auditLogs: List<AuditLog>,
    onCancelAlarms: () -> Unit
) {
    var selectedSection by remember { mutableStateOf("Overview") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Section Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Overview", "Goals", "Alarms", "Audit Logs").forEach { section ->
                FilterChip(
                    selected = selectedSection == section,
                    onClick = { selectedSection = section },
                    label = { Text(section, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DexterPrimary,
                        selectedLabelColor = DexterOnPrimary,
                        containerColor = DexterSurface,
                        labelColor = DexterOnSurface
                    ),
                    modifier = Modifier.testTag("more_tab_${section.lowercase().replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedSection) {
            "Overview" -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎯", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Active Goals (${goals.size})", color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                if (goals.isEmpty()) {
                                    Text("No goals yet. Say \"Set a goal to read 12 books\".", color = DexterOnSurfaceVariant, fontSize = 13.sp)
                                } else {
                                    goals.take(3).forEach { goal ->
                                        Text("• ${goal.name}", color = DexterOnSurface, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⏰", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Reminders & Alarms (${alarms.size})", color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    }
                                    if (alarms.isNotEmpty()) {
                                        TextButton(onClick = onCancelAlarms) {
                                            Text("Cancel All", color = DexterError, fontSize = 12.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                if (alarms.isEmpty()) {
                                    Text("No active alarms. Say \"Remind me in 30 minutes to call mom\".", color = DexterOnSurfaceVariant, fontSize = 13.sp)
                                } else {
                                    alarms.take(3).forEach { alarm ->
                                        Text("• ${alarm.label} at ${formatAlarmTime(alarm.alarmTime)}", color = DexterOnSurface, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚙️", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("System Diagnostics", color = DexterOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("• Storage: SQLite WAL Mode (Zero data loss)", color = DexterOnSurface, fontSize = 13.sp)
                                Text("• Engine: Native Kotlin NLU Fast-Path", color = DexterOnSurface, fontSize = 13.sp)
                                Text("• Privacy: 100% On-Device & Offline", color = DexterSuccess, fontSize = 13.sp)
                                Text("• Model Sandbox: Deterministic Action Boundary", color = DexterOnSurface, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            "Goals" -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(goals) { goal ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(goal.name, color = DexterOnBackground, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    Text("Status: ${goal.status} · ${goal.priority.name}", color = DexterOnSurfaceVariant, fontSize = 12.sp)
                                }
                                Text("🎯", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            "Alarms" -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(alarms) { alarm ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(alarm.label, color = DexterOnBackground, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    Text(formatAlarmTime(alarm.alarmTime), color = DexterPrimary, fontSize = 13.sp)
                                }
                                Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = DexterPrimary)
                            }
                        }
                    }
                }
            }

            "Audit Logs" -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(auditLogs) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${log.actionType} [${log.entityType}]",
                                        color = DexterPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = formatAlarmTime(log.createdAt),
                                        color = DexterOnSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.message,
                                    color = DexterOnSurface,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatAlarmTime(epochSecs: Long): String {
    val date = Date(epochSecs * 1000)
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(date)
}
