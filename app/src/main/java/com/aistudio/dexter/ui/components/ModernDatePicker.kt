package com.aistudio.dexter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dexter.ui.theme.DexterOnBackground
import com.aistudio.dexter.ui.theme.DexterPrimary
import com.aistudio.dexter.ui.theme.DexterSurface
import com.aistudio.dexter.ui.theme.DexterSurfaceVariant
import java.text.SimpleDateFormat
import java.util.*

/**
 * ModernDeadlinePickerField
 *
 * A modern, accessible Material 3 date picker field that supports:
 * - A clean display field showing the chosen target date (or placeholder)
 * - Tapping anywhere on the field or calendar icon opens a full Material 3 calendar modal
 * - The calendar dialog includes year selection mode (tap year header to choose any year)
 * - Fast preset chips (1 Week, 1 Month, 3 Months, End of Year) for quick zero-tap deadlines
 * - Clear button to reset the deadline
 */
@Composable
fun ModernDeadlinePickerField(
    deadline: String,
    onDeadlineChange: (String) -> Unit,
    label: String = "Target Deadline",
    placeholder: String = "Select target deadline...",
    modifier: Modifier = Modifier,
    testTagPrefix: String = "deadline"
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main Date Input Box (Clickable)
        OutlinedTextField(
            value = deadline,
            onValueChange = { onDeadlineChange(it) },
            readOnly = true, // Read-only so tapping opens the modern calendar
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                IconButton(
                    onClick = { showDatePickerDialog = true },
                    modifier = Modifier.testTag("${testTagPrefix}_icon_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Open Calendar",
                        tint = DexterPrimary
                    )
                }
            },
            trailingIcon = {
                if (deadline.isNotBlank()) {
                    IconButton(
                        onClick = { onDeadlineChange("") },
                        modifier = Modifier.testTag("${testTagPrefix}_clear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Deadline",
                            tint = Color(0xFFA0A5BD),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DexterOnBackground,
                unfocusedTextColor = DexterOnBackground,
                focusedBorderColor = DexterPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                focusedContainerColor = DexterSurfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = DexterSurfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePickerDialog = true }
                .testTag("${testTagPrefix}_input")
        )

        // Quick Deadline Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFA0A5BD)
            )

            val presets = listOf(
                "1 Week" to 7,
                "2 Weeks" to 14,
                "1 Month" to 30,
                "3 Months" to 90,
                "End of Year" to -1
            )

            presets.forEach { (labelStr, daysOffset) ->
                Surface(
                    onClick = {
                        val cal = Calendar.getInstance()
                        if (daysOffset == -1) {
                            cal.set(Calendar.MONTH, Calendar.DECEMBER)
                            cal.set(Calendar.DAY_OF_MONTH, 31)
                        } else {
                            cal.add(Calendar.DAY_OF_YEAR, daysOffset)
                        }
                        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        onDeadlineChange(sdf.format(cal.time))
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = DexterSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelStr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = DexterPrimary
                        )
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        ModernDatePickerDialog(
            currentDateString = deadline,
            onDateSelected = { _, formattedDate ->
                onDeadlineChange(formattedDate)
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }
}

/**
 * ModernDatePickerDialog
 *
 * Material 3 DatePickerDialog featuring full month and year calendar navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDatePickerDialog(
    currentDateString: String = "",
    onDateSelected: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse current date or use today
    val initialMillis = remember(currentDateString) {
        parseDateToUtcMillis(currentDateString) ?: System.currentTimeMillis()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = IntRange(2020, 2040)
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = utcMillis
                        }
                        val localCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, calUtc.get(Calendar.YEAR))
                            set(Calendar.MONTH, calUtc.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, calUtc.get(Calendar.DAY_OF_MONTH))
                        }
                        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        val formatted = sdf.format(localCal.time)
                        onDateSelected(localCal.timeInMillis, formatted)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Select", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA0A5BD))
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = DexterSurface,
            titleContentColor = DexterOnBackground,
            headlineContentColor = DexterOnBackground,
            weekdayContentColor = Color(0xFFA0A5BD),
            subheadContentColor = DexterOnBackground,
            yearContentColor = DexterOnBackground,
            currentYearContentColor = DexterPrimary,
            selectedYearContentColor = Color.Black,
            selectedYearContainerColor = DexterPrimary,
            dayContentColor = DexterOnBackground,
            selectedDayContentColor = Color.Black,
            selectedDayContainerColor = DexterPrimary,
            todayContentColor = DexterPrimary,
            todayDateBorderColor = DexterPrimary
        )
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = DexterSurface,
                titleContentColor = DexterOnBackground,
                headlineContentColor = DexterOnBackground,
                weekdayContentColor = Color(0xFFA0A5BD),
                subheadContentColor = DexterOnBackground,
                yearContentColor = DexterOnBackground,
                currentYearContentColor = DexterPrimary,
                selectedYearContentColor = Color.Black,
                selectedYearContainerColor = DexterPrimary,
                dayContentColor = DexterOnBackground,
                selectedDayContentColor = Color.Black,
                selectedDayContainerColor = DexterPrimary,
                todayContentColor = DexterPrimary,
                todayDateBorderColor = DexterPrimary
            )
        )
    }
}

private fun parseDateToUtcMillis(dateStr: String): Long? {
    if (dateStr.isBlank()) return null
    val formats = listOf(
        "MMM d, yyyy",
        "MMM dd, yyyy",
        "MMMM d, yyyy",
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "MM/dd/yyyy"
    )
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            val parsed = sdf.parse(dateStr)
            if (parsed != null) {
                val cal = Calendar.getInstance().apply { time = parsed }
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.YEAR, cal.get(Calendar.YEAR))
                    set(Calendar.MONTH, cal.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return utcCal.timeInMillis
            }
        } catch (_: Exception) {}
    }
    return null
}
