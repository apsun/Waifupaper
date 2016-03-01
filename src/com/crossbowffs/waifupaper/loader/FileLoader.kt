package com.crossbowffs.waifupaper.loader

import android.content.Context
import android.os.Environment
import com.crossbowffs.waifupaper.utils.join
import java.io.FileInputStream
import java.io.InputStream

/**
 * Represents the location at which a loader will look for files.
 */
enum class FileLocation {
    ASSETS,
    EXTERNAL
}

/**
 * Loads files from either internal (assets) or external storage.
 */
abstract class FileLoader {
    /**
     * Opens the file at the specified path for reading.
     */
    abstract fun openStream(path: String): InputStream

    /**
     * Returns a list of files/subdirectories in the specified directory.
     * This method is not recursive.
     */
    abstract fun enumerate(path: String): Array<String>

    /**
     * The location at which files loaded by this loader are stored.
     */
    abstract val location: FileLocation
}

/**
 * Loads files from internal storage (assets directory within the APK).
 */
class AssetFileLoader(private val context: Context): FileLoader() {
    override fun openStream(path: String): InputStream {
        return context.assets.open(path)
    }

    override fun enumerate(path: String): Array<String> {
        return context.assets.list(path)
    }

    override val location: FileLocation
        get() = FileLocation.ASSETS
}

/**
 * Loads files from external storage. Using this class requires
 * the {@code READ_EXTERNAL_STORAGE} permission.
 *
 * @param basePath The directory your assets are stored in, relative
 *                 to the external storage base directory.
 */
class ExternalFileLoader(private val basePath: String): FileLoader() {
    override fun openStream(path: String): InputStream {
        val extDir = Environment.getExternalStorageDirectory()
        return FileInputStream(extDir.join(basePath, path))
    }

    override fun enumerate(path: String): Array<String> {
        val extDir = Environment.getExternalStorageDirectory()
        return extDir.join(path).list() ?: emptyArray()
    }

    override val location: FileLocation
        get() = FileLocation.EXTERNAL
}
