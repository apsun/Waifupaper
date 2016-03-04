package com.crossbowffs.waifupaper.loader

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Environment
import android.os.ParcelFileDescriptor
import com.crossbowffs.waifupaper.utils.join
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Represents the location at which a loader will look for files.
 */
enum class FileLocation {
    INTERNAL,
    EXTERNAL
}

/**
 * Loads files from either internal (assets) or external storage.
 */
abstract class FileLoader {
    /**
     * Opens the file at the specified path for reading. The caller is
     * responsible for closing the stream.
     */
    abstract fun openStream(path: String): InputStream

    /**
     * Creates a file descriptor the the file at the specified path.
     * The caller is responsible for closing the file descriptor.
     */
    abstract fun openFileDescriptor(path: String): AssetFileDescriptor

    /**
     * Returns a list of file/subdirectory names in the specified directory.
     * This method does not recursively search subdirectories.
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
class InternalFileLoader(private val context: Context): FileLoader() {
    override fun openStream(path: String): InputStream {
        return context.assets.open(path)
    }

    override fun openFileDescriptor(path: String): AssetFileDescriptor {
        return context.assets.openFd(path)
    }

    override fun enumerate(path: String): Array<String> {
        return context.assets.list(path)
    }

    override val location: FileLocation
        get() = FileLocation.INTERNAL
}

/**
 * Loads files from external storage. Using this class requires
 * the {@code READ_EXTERNAL_STORAGE} permission.
 *
 * @param basePath The directory your assets are stored in, relative
 *                 to the external storage base directory.
 */
class ExternalFileLoader(private val basePath: String): FileLoader() {
    override fun openStream(path: String): FileInputStream {
        val extDir = Environment.getExternalStorageDirectory()
        return FileInputStream(extDir.join(basePath, path))
    }

    override fun openFileDescriptor(path: String): AssetFileDescriptor {
        val extDir = Environment.getExternalStorageDirectory()
        val descriptor = ParcelFileDescriptor.open(extDir.join(basePath, path), ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(descriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    override fun enumerate(path: String): Array<String> {
        val extDir = Environment.getExternalStorageDirectory()
        return extDir.join(basePath, path).list() ?: emptyArray()
    }

    override val location: FileLocation
        get() = FileLocation.EXTERNAL
}

/**
 * Convenience wrapper around a {@link FileLoader} that automatically
 * joins loader paths with a specified base directory.
 *
 * @param loader The loader instance to wrap.
 * @param basePath The base path to load files from, relative to
 *                 the base path of the wrapped loader instance.
 */
class FileLoaderWrapper(private val loader: FileLoader, private val basePath: String) : FileLoader() {
    override fun openStream(path: String): InputStream {
        return loader.openStream(File(basePath, path).path)
    }

    override fun openFileDescriptor(path: String): AssetFileDescriptor {
        return loader.openFileDescriptor(File(basePath, path).path)
    }

    override fun enumerate(path: String): Array<String> {
        return loader.enumerate(File(basePath, path).path)
    }

    override val location: FileLocation
        get() = loader.location
}
