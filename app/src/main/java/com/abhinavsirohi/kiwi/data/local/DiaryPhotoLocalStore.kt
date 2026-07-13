package com.abhinavsirohi.kiwi.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlin.math.max

data class StoredDiaryPhoto(
    val path: String,
    val width: Int,
    val height: Int,
    val byteSize: Long,
    val mimeType: String = "image/jpeg",
)

interface DiaryPhotoLocalStore {
    fun importCompressed(sourceUri: String, localId: String): StoredDiaryPhoto
    fun readBytes(path: String): ByteArray
    fun delete(path: String): Boolean
}

class AndroidDiaryPhotoLocalStore(private val context: Context) : DiaryPhotoLocalStore {
    override fun importCompressed(sourceUri: String, localId: String): StoredDiaryPhoto {
        val uri = Uri.parse(sourceUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Selected photo could not be opened" }
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Selected file is not a supported image" }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Selected photo could not be opened" }
            requireNotNull(BitmapFactory.decodeStream(input, null, options)) { "Selected photo could not be decoded" }
        }
        val scaled = scaleToFit(decoded)
        if (scaled !== decoded) decoded.recycle()
        val storedWidth = scaled.width
        val storedHeight = scaled.height

        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        val destination = File(directory, "$localId.jpg")
        try {
            destination.outputStream().buffered().use { output ->
                check(scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "Photo compression failed"
                }
            }
        } finally {
            scaled.recycle()
        }
        return StoredDiaryPhoto(
            path = destination.absolutePath,
            width = storedWidth,
            height = storedHeight,
            byteSize = destination.length(),
        )
    }

    override fun readBytes(path: String): ByteArray = File(path).readBytes()

    override fun delete(path: String): Boolean {
        val file = File(path)
        return !file.exists() || file.delete()
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (max(width / sample, height / sample) > MAX_DIMENSION * 2) sample *= 2
        return sample
    }

    private fun scaleToFit(bitmap: Bitmap): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / largest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        const val DIRECTORY = "diary-photos"
        const val MAX_DIMENSION = 1600
        const val JPEG_QUALITY = 82
    }
}
