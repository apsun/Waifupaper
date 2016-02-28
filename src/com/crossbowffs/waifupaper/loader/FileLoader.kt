package com.crossbowffs.waifupaper.loader

import android.content.Context
import com.crossbowffs.waifupaper.utils.join
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

internal abstract class FileLoader {
    abstract fun openStream(path: String): InputStream

    class AssetFileLoader(private val context: Context): FileLoader() {
        override fun openStream(path: String): InputStream {
            return context.assets.open(path)
        }
    }

    class ExternalFileLoader(private val basePath: File): FileLoader() {
        override fun openStream(path: String): InputStream {
            return FileInputStream(basePath.join(path))
        }
    }
}
