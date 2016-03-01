package com.crossbowffs.waifupaper.loader

import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Manages sound assets used by a Live2D model. Once you are
 * finished using the object, you must call the release method.
 */
class SoundPoolWrapper {
    private val soundMap: MutableMap<String, Int>
    private val soundPool: SoundPool

    init {
        soundMap = hashMapOf()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build())
            .build()
    }

    internal fun loadSound(loader: FileLoaderWrapper, soundFilePath: String) {
        loader.openFileDescriptor(soundFilePath).use { it ->
            soundMap[soundFilePath] = soundPool.load(it, 1)
        }
    }

    fun playSound(soundFilePath: String) {
        soundPool.play(soundMap[soundFilePath]!!, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundMap.clear()
        soundPool.release()
    }
}
