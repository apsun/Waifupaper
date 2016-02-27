package com.crossbowffs.waifupaper

import android.content.Context
import android.os.Environment

class Live2DModelLoader(private val context: Context) {
    fun loadExternal(name: String) {
        Environment.getExternalStorageDirectory()
    }

    fun loadInternal(name: String) {
        context.assets.open(name)
    }
}
