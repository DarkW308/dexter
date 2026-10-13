package com.aistudio.dexter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dexter.data.model.JournalEntry
import com.aistudio.dexter.ui.components.JournalCard
import com.aistudio.dexter.ui.theme.*

@Composable
fun JournalScreen(
    notes: List<JournalEntry>,
    onAddNote: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = DexterBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DexterPrimary,
                contentColor = DexterOnPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_journal_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Note")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Voice Journal & Notes",
                color = DexterOnBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Offline captured thoughts, ideas, and reflections.",
                color = DexterOnSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No journal notes yet.\nSay \"Take a note: Meeting tomorrow at 10\" anytime.",
                        color = DexterOnSurfaceVariant,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(notes, key = { it.id }) { entry ->
                        JournalCard(entry = entry)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Note", color = DexterOnBackground) },
            text = {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("Jot down a thought or note...", color = DexterOnSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DexterOnBackground,
                        unfocusedTextColor = DexterOnBackground,
                        focusedBorderColor = DexterPrimary,
                        unfocusedBorderColor = DexterSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("journal_note_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteInput.isNotBlank()) {
                            onAddNote(noteInput)
                            noteInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DexterPrimary, contentColor = DexterOnPrimary),
                    modifier = Modifier.testTag("save_journal_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = DexterOnSurfaceVariant)
                }
            },
            containerColor = DexterSurface
        )
    }
}
