package com.crossbowffs.waifupaper.utils

inline fun <T, R> T?.useNotNull(block: (T) -> R): R? {
    if (this != null) {
        return block(this)
    } else {
        return null
    }
}
