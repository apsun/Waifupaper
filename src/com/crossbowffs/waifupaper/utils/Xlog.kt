package com.crossbowffs.waifupaper.utils

import android.util.Log

object Xlog {
    private val LOG_LEVEL = Log.DEBUG
    private val MERGE_TAGS = true
    private val APPLICATION_TAG = "Waifupaper"

    private fun log(priority: Int, tag: String, message: String, vararg args: Any) {
        if (priority < LOG_LEVEL) {
            return
        }

        var vtag = tag
        if (MERGE_TAGS) {
            vtag = APPLICATION_TAG
        }

        var vmessage = String.format(message, *args)
        if (args.size > 0 && args[args.size - 1] is Throwable) {
            val throwable = args[args.size - 1] as Throwable
            val stacktraceStr = Log.getStackTraceString(throwable)
            vmessage = message + '\n' + stacktraceStr
        }

        Log.println(priority, vtag, vmessage)
    }

    fun v(tag: String, message: String, vararg args: Any) {
        log(Log.VERBOSE, tag, message, *args)
    }

    fun d(tag: String, message: String, vararg args: Any) {
        log(Log.DEBUG, tag, message, *args)
    }

    fun i(tag: String, message: String, vararg args: Any) {
        log(Log.INFO, tag, message, *args)
    }

    fun w(tag: String, message: String, vararg args: Any) {
        log(Log.WARN, tag, message, *args)
    }

    fun e(tag: String, message: String, vararg args: Any) {
        log(Log.ERROR, tag, message, *args)
    }
}

fun Any.logv(message: String, vararg args: Any) {
    Xlog.v(this.javaClass.simpleName, message, args)
}

fun Any.logd(message: String, vararg args: Any) {
    Xlog.d(this.javaClass.simpleName, message, args)
}

fun Any.logi(message: String, vararg args: Any) {
    Xlog.i(this.javaClass.simpleName, message, args)
}

fun Any.logw(message: String, vararg args: Any) {
    Xlog.w(this.javaClass.simpleName, message, args)
}

fun Any.loge(message: String, vararg args: Any) {
    Xlog.e(this.javaClass.simpleName, message, args)
}
