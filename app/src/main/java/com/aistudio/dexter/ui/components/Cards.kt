package com.aistudio.dexter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.aistudio.dexter.R
import com.aistudio.dexter.data.model.*
import com.aistudio.dexter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BriefingCard(
    briefing: DailyBriefing?,
    onPlayBriefing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("briefing_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DexterSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌅", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Today's Briefing",
                        color = DexterOnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FilledTonalIconButton(
                    onClick = onPlayBriefing,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("play_briefing_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = DexterPrimary.copy(alpha = 0.15f),
                        contentColor = DexterPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Speak Briefing",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (briefing != null) {
                Text(
                    text = "${briefing.pendingTasksCount} tasks pending · ${if (briefing.nextAlarmLabel != null) "1 reminder · " else ""}₹${briefing.todaySpentMinor / 100} spent today",
                    color = DexterOnSurfaceVariant,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DexterSurfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                if (briefing.topTaskTitle != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Text(text = "📋", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next: ${briefing.topTaskTitle}",
                            color = DexterOnSurface,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (briefing.nextAlarmLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Text(text = "⏰", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reminder: ${briefing.nextAlarmLabel}",
                            color = DexterOnSurface,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Text(
                    text = "Loading today's briefing...",
                    color = DexterOnSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    onToggleComplete: () -> Unit,
    onCancelTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    val priorityBarColor = when (task.priority) {
        Priority.CRITICAL -> PriorityCritical
        Priority.HIGH -> PriorityHigh
        Priority.NORMAL -> PriorityNormal
        Priority.LOW -> PriorityLow
        Priority.SOMEDAY -> PrioritySomeday
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DexterSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 4dp vertical priority color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(priorityBarColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = priorityBarColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = task.priority.name,
                                color = priorityBarColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "#${task.id}",
                            color = DexterOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = task.title,
                        color = if (isCompleted) DexterOnSurfaceVariant else DexterOnBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTime(task.createdAt),
                        color = DexterOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Circular complete checkbox
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) DexterSuccess.copy(alpha = 0.2f)
                            else DexterSurfaceVariant
                        )
                        .clickable { onToggleComplete() }
                        .testTag("task_check_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = DexterSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: FinanceTransaction,
    modifier: Modifier = Modifier
) {
    val isDebit = transaction.type == "DEBIT"
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("finance_item_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DexterSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDebit) DexterError.copy(alpha = 0.15f)
                            else DexterSuccess.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDebit) "↓" else "↑",
                        color = if (isDebit) DexterError else DexterSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.description,
                        color = DexterOnBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${transaction.category} · ${formatTime(transaction.createdAt)}",
                        color = DexterOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = "${if (isDebit) "-" else "+"}₹${transaction.amountRupees}",
                color = if (isDebit) DexterOnBackground else DexterSuccess,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun JournalCard(
    entry: JournalEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("journal_item_${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DexterSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mic),
                        contentDescription = "Voice captured",
                        tint = DexterPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Voice Note",
                        color = DexterPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = formatTime(entry.createdAt),
                    color = DexterOnSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "\"${entry.content}\"",
                color = DexterOnBackground,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

private fun formatTime(epochSecs: Long): String {
    val date = Date(epochSecs * 1000)
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(date)
}
