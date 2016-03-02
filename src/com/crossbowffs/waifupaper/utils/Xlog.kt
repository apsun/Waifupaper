package com.crossbowffs.waifupaper.utils

import android.util.Log

object Xlog {
    private val LOG_LEVEL = Log.VERBOSE
    private val MERGE_TAGS = true
    private val APPLICATION_TAG = "Waifupaper"

    private fun log(priority: Int, tag: String, message: String, throwable: Throwable? = null) {
        if (priority < LOG_LEVEL) {
            return
        }

        var vtag = tag
        if (MERGE_TAGS) {
            vtag = APPLICATION_TAG
        }

        var vmessage = message
        if (throwable != null) {
            vmessage += '\n' + Log.getStackTraceString(throwable)
        }

        Log.println(priority, vtag, vmessage)
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.VERBOSE, tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.DEBUG, tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.INFO, tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.ERROR, tag, message, throwable)
    }
}

fun getNamedParentClass(javaClass: Class<*>): Class<*> {
    var cls = javaClass
    while (cls.isAnonymousClass) {
        cls = cls.enclosingClass
    }
    return cls
}

fun Any.logv(message: String, throwable: Throwable? = null) {
    Xlog.v(getNamedParentClass(this.javaClass).simpleName, message, throwable)
}

fun Any.logd(message: String, throwable: Throwable? = null) {
    Xlog.d(getNamedParentClass(this.javaClass).simpleName, message, throwable)
}

fun Any.logi(message: String, throwable: Throwable? = null) {
    Xlog.i(getNamedParentClass(this.javaClass).simpleName, message, throwable)
}

fun Any.logw(message: String, throwable: Throwable? = null) {
    Xlog.w(getNamedParentClass(this.javaClass).simpleName, message, throwable)
}

fun Any.loge(message: String, throwable: Throwable? = null) {
    Xlog.e(getNamedParentClass(this.javaClass).simpleName, message, throwable)
}
