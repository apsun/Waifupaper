package com.crossbowffs.waifupaper.loader

import android.content.res.AssetFileDescriptor
import android.os.ParcelFileDescriptor
import java.io.Closeable

class MultiFileDescriptor(private val descriptor: Closeable) : Closeable {
    override fun close() {
        descriptor.close()
    }

    fun <T> withAssetConsumer(consumer: (AssetFileDescriptor) -> T): FileDescriptorConsumer<T> {
        return FileDescriptorConsumer<T>(descriptor).withAssetConsumer(consumer)
    }

    fun <T> withParcelConsumer(consumer: (ParcelFileDescriptor) -> T): FileDescriptorConsumer<T> {
        return FileDescriptorConsumer<T>(descriptor).withParcelConsumer(consumer)
    }
}

class FileDescriptorConsumer<T>(private val descriptor: Closeable) {
    private var assetConsumer: ((AssetFileDescriptor) -> T)? = null
    private var parcelConsumer: ((ParcelFileDescriptor) -> T)? = null

    fun withAssetConsumer(consumer: (AssetFileDescriptor) -> T): FileDescriptorConsumer<T> {
        assetConsumer = consumer
        return this
    }

    fun withParcelConsumer(consumer: (ParcelFileDescriptor) -> T): FileDescriptorConsumer<T> {
        parcelConsumer = consumer
        return this
    }

    fun get(): T {
        if (descriptor is AssetFileDescriptor) { return assetConsumer!!(descriptor) }
        if (descriptor is ParcelFileDescriptor) { return parcelConsumer!!(descriptor) }
        throw AssertionError("Unknown file descriptor type")
    }
}
