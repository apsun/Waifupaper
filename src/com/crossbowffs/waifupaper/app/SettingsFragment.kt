package com.crossbowffs.waifupaper.app

import android.os.Bundle
import android.preference.PreferenceFragment
import com.crossbowffs.waifupaper.R

class SettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings)
    }
}
