package com.crossbowffs.waifupaper.loader

import android.content.Context
import com.crossbowffs.waifupaper.utils.join
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Allows assets to be loaded from either asset or external storage.
 * This is needed because Android does not allow apps to "build" paths
 * to assets using {@link java.io.File}, and only allows reading files
 * through {@link java.io.InputStream} objects.
 */
internal abstract class FileLoader {
    abstract fun openStream(path: String): InputStream

    class AssetFileLoader(private val context: Context, private val name: String): FileLoader() {
        override fun openStream(path: String): InputStream {
            return context.assets.open(File(name, path).path)
        }
    }

    class ExternalFileLoader(private val basePath: File): FileLoader() {
        override fun openStream(path: String): InputStream {
            return FileInputStream(basePath.join(path))
        }
    }
}
