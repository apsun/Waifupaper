package com.crossbowffs.waifupaper.app

import android.os.Bundle
import com.crossbowffs.waifupaper.R
import com.crossbowffs.waifupaper.loader.Live2DModelLoader
import kotlinx.android.synthetic.main.toolbar.*

class MainActivity : PrivilegedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        for (model in Live2DModelLoader.enumerateModels(this)) {
            println("${model.name} -> ${model.modelPath}")
        }
    }
}
