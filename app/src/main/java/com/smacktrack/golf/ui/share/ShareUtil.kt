package com.smacktrack.golf.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File

object ShareUtil {

    fun shareShotCard(context: Context, bitmap: Bitmap) {
        val shareDir = File(context.cacheDir, "share_images")
        shareDir.mkdirs()
        val file = File(shareDir, "smacktrack_shot.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share your shot"))
    }
}
