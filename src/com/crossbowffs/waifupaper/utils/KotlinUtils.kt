package com.crossbowffs.waifupaper.utils

inline fun <T> T?.useNotNull(block: (T) -> Unit) {
    if (this != null) {
        block(this)
    }
}
