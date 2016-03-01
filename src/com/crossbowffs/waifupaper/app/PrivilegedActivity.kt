package com.crossbowffs.waifupaper.app

import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity

abstract class PrivilegedActivity : AppCompatActivity() {
    fun hasPermissions(vararg permissions: String): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun runPrivilegedAction(requestCode: Int, vararg permissions: String) {
        for (permission in permissions) {
            val permissionStatus = ContextCompat.checkSelfPermission(this, permission)
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, requestCode)
                return
            }
        }

        onRequestPermissionsResult(requestCode, true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        for (permissionStatus in grantResults) {
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                onRequestPermissionsResult(requestCode, false)
                return
            }
        }

        // Empty permissions means that the request was cancelled
        // by the user, so the permissions were not granted
        val requestCancelled = grantResults.size == 0
        onRequestPermissionsResult(requestCode, !requestCancelled)
    }

    open fun onRequestPermissionsResult(requestCode: Int, granted: Boolean) {

    }
}
