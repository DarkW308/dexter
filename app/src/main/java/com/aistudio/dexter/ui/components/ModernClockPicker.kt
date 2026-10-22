package com.aistudio.dexter.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dexter.ui.theme.DexterOnBackground
import com.aistudio.dexter.ui.theme.DexterPrimary
import com.aistudio.dexter.ui.theme.DexterSurface
import com.aistudio.dexter.ui.theme.DexterSurfaceVariant
import java.text.SimpleDateFormat
import java.util.*

data class ClockTime(
    val hour24: Int,
    val minute: Int
) {
    val hour12: Int
        get() = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }

    val isAm: Boolean
        get() = hour24 < 12

    val amPmString: String
        get() = if (isAm) "AM" else "PM"

    val formattedString: String
        get() = String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPmString)

    fun toMillisToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        fun fromFormattedString(timeStr: String, fallbackHour: Int = 8, fallbackMinute: Int = 0): ClockTime {
            return try {
                val clean = timeStr.trim().uppercase()
                val isPm = clean.endsWith("PM")
                val isAm = clean.endsWith("AM")
                val numPart = clean.replace("AM", "").replace("PM", "").trim()
                val parts = numPart.split(":")
                var h = parts[0].toInt()
                val m = if (parts.size > 1) parts[1].toInt() else 0
                if (isPm && h < 12) h += 12
                if (isAm && h == 12) h = 0
                ClockTime(h.coerceIn(0, 23), m.coerceIn(0, 59))
            } catch (e: Exception) {
                ClockTime(fallbackHour, fallbackMinute)
            }
        }

        fun fromMillis(millis: Long): ClockTime {
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            return ClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }

        fun now(): ClockTime {
            val cal = Calendar.getInstance()
            return ClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }
    }
}

/**
 * ModernClockTimePicker
 *
 * A modern, clock-style time picker component for alarms, timetables, sleep logs, and habit reminders.
 * Displays prominent interactive digital time cards with circular clock dialog picker and quick presets.
 */
@Composable
fun ModernClockTimePicker(
    label: String,
    time: ClockTime,
    onTimeChanged: (ClockTime) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<Pair<String, ClockTime>> = emptyList()
) {
    val context = LocalContext.current

    val openTimePicker = {
        val picker = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                onTimeChanged(ClockTime(selectedHour, selectedMinute))
            },
            time.hour24,
            time.minute,
            false // 12-hour AM/PM clock
        )
        picker.show()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFA0A5BD),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Clock Card Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DexterSurface)
                .border(1.dp, Color(0xFF2A344A), RoundedCornerShape(16.dp))
                .clickable { openTimePicker() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Clock Icon & Prominent Time Digits
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(DexterPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ClockFaceIcon(
                            modifier = Modifier.size(20.dp),
                            tint = DexterPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Hour and Minute Digits
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%02d", time.hour12),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = DexterOnBackground
                        )
                        Text(
                            text = " : ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DexterPrimary
                        )
                        Text(
                            text = String.format(Locale.US, "%02d", time.minute),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = DexterOnBackground
                        )
                    }
                }

                // AM / PM Segmented Control
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E2638))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AM Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (time.isAm) DexterPrimary else Color.Transparent)
                            .clickable {
                                if (!time.isAm) {
                                    val newH = if (time.hour24 >= 12) time.hour24 - 12 else time.hour24
                                    onTimeChanged(ClockTime(newH, time.minute))
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (time.isAm) Color.Black else Color(0xFFA0A5BD)
                        )
                    }

                    // PM Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (!time.isAm) DexterPrimary else Color.Transparent)
                            .clickable {
                                if (time.isAm) {
                                    val newH = if (time.hour24 < 12) time.hour24 + 12 else time.hour24
                                    onTimeChanged(ClockTime(newH, time.minute))
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!time.isAm) Color.Black else Color(0xFFA0A5BD)
                        )
                    }
                }
            }
        }

        // Quick Preset Chips (if provided)
        if (presets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { (presetLabel, presetTime) ->
                    val isSelected = time.hour24 == presetTime.hour24 && time.minute == presetTime.minute
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) DexterPrimary.copy(alpha = 0.2f) else DexterSurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) DexterPrimary else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onTimeChanged(presetTime) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = presetLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DexterPrimary else DexterOnBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClockFaceIcon(
    modifier: Modifier = Modifier,
    tint: Color = DexterPrimary
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.8.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth
        val centerOffset = Offset(size.width / 2f, size.height / 2f)

        // Outer circular bezel
        drawCircle(
            color = tint,
            radius = radius,
            center = centerOffset,
            style = Stroke(width = strokeWidth)
        )

        // Center hub
        drawCircle(
            color = tint,
            radius = strokeWidth * 1.1f,
            center = centerOffset
        )

        // Hour hand pointing to 10 o'clock
        drawLine(
            color = tint,
            start = centerOffset,
            end = Offset(centerOffset.x - radius * 0.35f, centerOffset.y - radius * 0.35f),
            strokeWidth = strokeWidth * 1.3f,
            cap = StrokeCap.Round
        )

        // Minute hand pointing to 2 o'clock
        drawLine(
            color = tint,
            start = centerOffset,
            end = Offset(centerOffset.x + radius * 0.5f, centerOffset.y - radius * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
