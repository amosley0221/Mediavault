package com.mediavault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mediavault.app.data.Emulators
import com.mediavault.app.data.GameSystem
import com.mediavault.app.data.Thumbnails

/**
 * Asked once per console: which emulator runs this? The answer is remembered unless the
 * user unticks it, so tapping a ROM afterwards goes straight into the game.
 */
@Composable
fun EmulatorPickerDialog(
    system: GameSystem,
    romTitle: String?,
    currentPackage: String?,
    onDismiss: () -> Unit,
    onPick: (packageName: String, remember: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val candidates = remember(system.id) { Emulators.candidatesFor(context, system) }
    var rememberChoice by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Mv.Card)
                .padding(20.dp),
        ) {
            Text(
                text = "Open with",
                color = Mv.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = romTitle?.let { "$it · ${system.label}" } ?: system.label,
                color = Mv.Secondary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(14.dp))

            if (candidates.isEmpty()) {
                Text(
                    text = "No emulator for ${system.label} is installed. Install one — RetroArch " +
                        "covers most systems — and it will show up here.",
                    color = Mv.Muted,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    candidates.forEach { emulator ->
                        EmulatorRow(
                            label = emulator.label,
                            packageName = emulator.packageName,
                            selected = emulator.packageName == currentPackage,
                        ) { onPick(emulator.packageName, rememberChoice) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { rememberChoice = !rememberChoice },
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (rememberChoice) LocalAccent.current else Color(0xFFE6E6EA)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (rememberChoice) {
                            Text(text = "✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Always use this for ${system.label}",
                        color = Mv.Muted,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Cancel",
                    color = LocalAccent.current,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onDismiss() }.padding(6.dp),
                )
            }
        }
    }
}

@Composable
private fun EmulatorRow(
    label: String,
    packageName: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    CardSurface(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkBox(
                letter = label.take(1).uppercase(),
                gradient = 0x1B3A4BL to 0x7AD9F5L,
                artUri = Thumbnails.APP_ICON_SCHEME + packageName,
                letterSize = 14,
                thumbWidth = 96,
                thumbHeight = 96,
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = Mv.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Text(text = "✓", color = Mv.Watched, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
