package com.harshita.iykykcollage.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object CollageExporter {

    fun save(context: Context, bitmap: Bitmap): String {
        val name = "FrameFriends_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/FrameFriends"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: error("Could not create gallery file")

        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                check(
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        94,
                        stream
                    )
                ) {
                    "Could not write collage"
                }
            } ?: error("Could not open gallery file")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completedValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }

                context.contentResolver.update(
                    uri,
                    completedValues,
                    null,
                    null
                )
            }
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }

        return name
    }

    fun share(context: Context, bitmap: Bitmap) {
        val directory = File(
            context.cacheDir,
            "shared"
        ).apply {
            mkdirs()
        }

        val file = File(directory, "framefriends.jpg")

        FileOutputStream(file).use { stream ->
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                94,
                stream
            )
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(
                shareIntent,
                "Share your collage"
            )
        )
    }
}