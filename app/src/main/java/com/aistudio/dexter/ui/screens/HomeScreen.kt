package com.aistudio.dexter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dexter.data.model.DailyBriefing
import com.aistudio.dexter.engine.OrbVisualState
import com.aistudio.dexter.ui.components.BriefingCard
import com.aistudio.dexter.ui.components.VoiceOrb
import com.aistudio.dexter.ui.theme.*

@Composable
fun HomeScreen(
    orbState: OrbVisualState,
    briefing: DailyBriefing?,
    audioIntensity: Float = 0f,
    avatarEmoji: String = "🤖",
    onTapOrb: () -> Unit,
    onPlayBriefing: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DEXTER",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                color = DexterOnBackground
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DexterSurfaceVariant)
                    .clickable { onOpenProfile() }
                    .testTag("btn_home_profile_avatar"),
                contentAlignment = Alignment.Center
            ) {
                Text(avatarEmoji, fontSize = 18.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Center Voice Orb
        VoiceOrb(
            state = orbState,
            audioIntensity = audioIntensity,
            size = 200.dp,
            onTap = onTapOrb
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic State / Action Hint
        Text(
            text = when (orbState) {
                OrbVisualState.LISTENING -> "Listening..."
                OrbVisualState.PROCESSING -> "Synthesizing..."
                OrbVisualState.SPEAKING -> "Speaking..."
                OrbVisualState.ERROR -> "Clarification Needed"
                OrbVisualState.IDLE -> "Tap to speak"
            },
            color = when (orbState) {
                OrbVisualState.LISTENING -> DexterPrimaryGlow
                OrbVisualState.SPEAKING -> DexterSuccess
                OrbVisualState.ERROR -> DexterError
                else -> DexterOnSurfaceVariant
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Suggestion Chips (Voice Prompts)
        val suggestions = listOf(
            "What's the status of project Website?",
            "Create project Website with deadline Dec 31",
            "Add milestone Launch to Website",
            "What are my pending tasks?",
            "Daily briefing",
            "Spent 350 on lunch",
            "Remind me in 30 mins"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (prompt in suggestions) {
                SuggestionChip(
                    onClick = { onQuickPrompt(prompt) },
                    label = { Text(prompt, fontSize = 12.sp, color = DexterOnSurface) },
                    shape = RoundedCornerShape(14.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = DexterSurfaceVariant
                    ),
                    border = null,
                    modifier = Modifier.testTag("prompt_chip_${prompt.take(10)}")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Briefing Card
        BriefingCard(
            briefing = briefing,
            onPlayBriefing = onPlayBriefing
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}
