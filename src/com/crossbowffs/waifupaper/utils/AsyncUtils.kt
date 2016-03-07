package com.crossbowffs.waifupaper.utils

import android.os.AsyncTask
import android.os.Handler
import java.util.concurrent.Executor

fun async(block: () -> Unit) {
    (object : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            block()
        }
    }).execute()
}

fun async(executor: Executor, block: () -> Unit) {
    (object : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            block()
        }
    }).executeOnExecutor(executor)
}

fun async(handler: Handler, block: () -> Unit) {
    handler.post(block)
}
