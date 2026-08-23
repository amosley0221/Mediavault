package com.mediavault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mediavault.app.data.MediaItem

/**
 * Cover options for one ROM: pick an image off the phone, or look it up in the libretro
 * archive. The lookup is the only thing in MediaVault that reaches the network, so it is
 * never automatic.
 */
@Composable
fun BoxArtDialog(
    rom: MediaItem,
    hasCustomArt: Boolean,
    fetching: Boolean,
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onDownload: () -> Unit,
    onClear: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Mv.Card)
                .padding(20.dp),
        ) {
            Text(text = "Box art", color = Mv.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(text = rom.title, color = Mv.Secondary, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))

            ActionRow(
                title = "Choose an image",
                subtitle = "Pick a picture from this phone",
                enabled = !fetching,
                onClick = onPickImage,
            )
            Spacer(Modifier.height(8.dp))
            ActionRow(
                title = if (fetching) "Looking…" else "Download cover",
                subtitle = "Search the libretro thumbnail archive by filename",
                enabled = !fetching,
                onClick = onDownload,
            )
            if (hasCustomArt) {
                Spacer(Modifier.height(8.dp))
                ActionRow(
                    title = "Remove cover",
                    subtitle = "Go back to the placeholder",
                    enabled = !fetching,
                    onClick = onClear,
                )
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
private fun ActionRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    CardSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Mv.Text else Mv.Secondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = subtitle, color = Mv.Secondary, fontSize = 11.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(text = "›", color = Mv.Secondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
