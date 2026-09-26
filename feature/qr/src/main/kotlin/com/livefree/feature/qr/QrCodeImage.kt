package com.livefree.feature.qr

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun QrCodeImage(content: String, contentDescription: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { QrBitmaps.qrCode(content, 600).asImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        filterQuality = FilterQuality.None,
    )
}
