package com.livefree.feature.qr

import android.content.Context
import androidx.print.PrintHelper

/** Opens the system print dialog, which also offers "Save as PDF". */
fun printQrKey(context: Context, content: String, caption: String) {
    PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }
        .printBitmap("Live Free key", QrBitmaps.printable(content, caption))
}
