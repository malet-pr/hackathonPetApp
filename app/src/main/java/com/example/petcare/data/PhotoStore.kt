package com.example.petcare.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Copies picker content into private storage; paths from outside this directory are never read or removed. */
class PhotoStore(private val context: Context) {
    private val directory get() = File(context.filesDir, "photos")
    private val demoDirectory get() = File(context.filesDir, "demo-photos")

    suspend fun import(uri: Uri, demo: Boolean = false): String = withContext(Dispatchers.IO) {
        val destination = if (demo) demoDirectory else directory
        destination.mkdirs()
        val target = File(destination, "${UUID.randomUUID()}.image")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_BYTES) throw IOException("Image is too large (10 MB maximum).")
                        output.write(buffer, 0, count)
                    }
                }
            } ?: throw IOException("Could not open the selected image.")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.path, bounds)
            if (target.length() == 0L || bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                bounds.outWidth > MAX_DIMENSION || bounds.outHeight > MAX_DIMENSION) {
                throw IOException("That image could not be read. Choose another one.")
            }
            target.absolutePath
        } catch (error: Exception) {
            target.delete()
            throw error
        }
    }

    suspend fun thumbnail(path: String, maxSide: Int = 720): Bitmap? = withContext(Dispatchers.IO) {
        val file = ownedFile(path) ?: return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    suspend fun discard(path: String?) = withContext(Dispatchers.IO) {
        path?.let(::ownedFile)?.delete()
        Unit
    }

    suspend fun clearDemo() = withContext(Dispatchers.IO) {
        demoDirectory.listFiles()?.forEach { it.delete() }
        demoDirectory.delete()
        Unit
    }

    fun isDemoPath(path: String): Boolean =
        File(path).parentFile?.canonicalFile == demoDirectory.canonicalFile

    private fun ownedFile(path: String): File? {
        val file = File(path)
        return file.takeIf { (it.parentFile?.canonicalFile == directory.canonicalFile ||
            it.parentFile?.canonicalFile == demoDirectory.canonicalFile) && it.isFile }
    }

    companion object {
        private const val MAX_BYTES = 10L * 1024 * 1024
        private const val MAX_DIMENSION = 20_000
    }
}
