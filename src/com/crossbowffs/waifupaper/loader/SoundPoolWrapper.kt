package com.crossbowffs.waifupaper.loader

import android.media.AudioAttributes
import android.media.SoundPool

class SoundPoolWrapper {
    private val soundPool: SoundPool

    init {
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build())
            .build()
    }

    fun loadSound(loader: FileLoaderWrapper, path: String): Int {
        return loader.openFileDescriptor(path).use { fileDescriptor ->
            fileDescriptor
                .withAssetConsumer { soundPool.load(it, 1) }
                .withParcelConsumer { soundPool.load(it.fileDescriptor, 0, it.statSize, 1) }
                .get()
        }
    }

    fun playSound(soundId: Int): Int {
        return soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}