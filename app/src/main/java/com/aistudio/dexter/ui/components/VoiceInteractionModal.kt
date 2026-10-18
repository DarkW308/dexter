package com.aistudio.dexter.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.aistudio.dexter.R
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.dexter.engine.OrbVisualState
import com.aistudio.dexter.engine.ParsedIntent
import com.aistudio.dexter.ui.theme.*

@Composable
fun VoiceInteractionModal(
    visible: Boolean,
    orbState: OrbVisualState,
    transcript: String,
    lastResponse: String,
    pendingConfirmation: ParsedIntent?,
    audioIntensity: Float = 0f,
    onDismiss: () -> Unit,
    onConfirmAction: (ParsedIntent) -> Unit,
    onCancelConfirmation: () -> Unit,
    onSubmitText: (String) -> Unit
) {
    if (!visible) return

    var manualInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DexterBackground.copy(alpha = 0.96f))
                .padding(24.dp)
        ) {
            // Top Bar with Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DexterSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when (orbState) {
                                        OrbVisualState.LISTENING -> DexterPrimaryGlow
                                        OrbVisualState.PROCESSING -> DexterPrimary
                                        OrbVisualState.SPEAKING -> DexterSuccess
                                        OrbVisualState.ERROR -> DexterError
                                        OrbVisualState.IDLE -> DexterPrimary.copy(alpha = 0.5f)
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (orbState) {
                                OrbVisualState.LISTENING -> "Listening..."
                                OrbVisualState.PROCESSING -> "Processing..."
                                OrbVisualState.SPEAKING -> "Dexter Speaking"
                                OrbVisualState.ERROR -> "Action Clarification"
                                OrbVisualState.IDLE -> "Ready"
                            },
                            color = DexterOnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_voice_modal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice Modal",
                        tint = DexterOnSurfaceVariant
                    )
                }
            }

            // Center: Large Voice Orb & Transcription
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                VoiceOrb(
                    state = orbState,
                    audioIntensity = audioIntensity,
                    size = 230.dp,
                    onTap = {
                        // toggle or restart listening
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Spoken / Transcribed Text
                val displayText = when {
                    transcript.isNotBlank() -> "\"$transcript\""
                    lastResponse.isNotBlank() -> lastResponse
                    orbState == OrbVisualState.LISTENING -> "Say \"Hey Dexter\" or speak your request..."
                    else -> "Tap the orb or speak naturally"
                }

                Text(
                    text = displayText,
                    color = DexterOnBackground,
                    fontSize = 18.sp,
                    fontStyle = if (transcript.isNotBlank()) FontStyle.Italic else FontStyle.Normal,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                // Confirmation Card if an action requires explicit approval
                AnimatedVisibility(
                    visible = pendingConfirmation != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (pendingConfirmation != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DexterSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Confirm Action",
                                    color = DexterPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pendingConfirmation.confirmationPrompt
                                        ?: "Are you sure you want to execute this action?",
                                    color = DexterOnBackground,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onCancelConfirmation,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("cancel_action_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = DexterOnSurfaceVariant
                                        )
                                    ) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = { onConfirmAction(pendingConfirmation) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("confirm_action_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DexterPrimary,
                                            contentColor = DexterOnPrimary
                                        )
                                    ) {
                                        Text("Confirm")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Fallback Text Input (Zero-Barrier Natural Language)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .imePadding(),
                shape = RoundedCornerShape(24.dp),
                color = DexterSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mic),
                        contentDescription = "Voice Input",
                        tint = DexterPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        placeholder = {
                            Text(
                                "Or type phrase (e.g., spent 350 on lunch)",
                                color = DexterOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = DexterOnBackground,
                            unfocusedTextColor = DexterOnBackground
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (manualInput.isNotBlank()) {
                                    onSubmitText(manualInput)
                                    manualInput = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_modal_text_input")
                    )
                    IconButton(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                onSubmitText(manualInput)
                                manualInput = ""
                            }
                        },
                        modifier = Modifier.testTag("voice_modal_send_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_send),
                            contentDescription = "Send",
                            tint = if (manualInput.isNotBlank()) DexterPrimary else DexterOnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
