package com.crossbowffs.waifupaper.app

import android.app.AlertDialog
import android.os.Bundle
import com.crossbowffs.waifupaper.R
import com.crossbowffs.waifupaper.loader.Live2DModelLoader
import kotlinx.android.synthetic.main.toolbar.*

class MainActivity : PrivilegedActivity() {
    private val REQUEST_STORAGE_READ = 1
    private val PERMISSION_READ_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (!hasPermissions(PERMISSION_READ_STORAGE)) {
            showPermissionRequestDialog()
        } else {
            runPrivilegedAction(REQUEST_STORAGE_READ, PERMISSION_READ_STORAGE)
        }
    }

    private fun showPermissionRequestDialog() {
        AlertDialog.Builder(this)
            .setTitle("I can haz permissions?")
            .setMessage("To use 3rd party models, we'll need permissions to read your external storage!")
            .setCancelable(false)
            .setPositiveButton("Got it!") { v, i ->
                runPrivilegedAction(REQUEST_STORAGE_READ, PERMISSION_READ_STORAGE)
            }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, granted: Boolean) {
        if (requestCode != REQUEST_STORAGE_READ) return
        for (model in Live2DModelLoader.enumerateModels(this, granted)) {
            println("${model.name} -> ${model.modelPath}")
        }
    }
}
