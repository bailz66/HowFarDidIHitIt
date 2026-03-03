package com.smacktrack.golf.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

object ShareUtil {

    fun shareShotCard(context: Context, bitmap: Bitmap) {
        try {
            val shareDir = File(context.cacheDir, "share_images")
            shareDir.mkdirs()
            val file = File(shareDir, "smacktrack_shot.png")
            file.outputStream().use { out ->
                val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                if (!ok) Log.e("ShareUtil", "Bitmap compress returned false")
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share your shot"))
        } catch (e: Exception) {
            Log.e("ShareUtil", "Failed to share shot card", e)
        }
    }
}
