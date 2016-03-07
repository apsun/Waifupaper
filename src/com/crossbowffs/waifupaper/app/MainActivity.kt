package com.crossbowffs.waifupaper.app

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.view.ViewPager
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.crossbowffs.waifupaper.R
import com.crossbowffs.waifupaper.loader.AssetLoader
import com.crossbowffs.waifupaper.loader.FileLocation
import com.crossbowffs.waifupaper.loader.Live2DModelInfo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*

class NewAdapter(private val activity: MainActivity) : PagerArrayAdapter<Live2DModelInfo>() {
    override fun instantiateItem(container: ViewGroup?, value: Live2DModelInfo): View {
        val view = activity.layoutInflater.inflate(R.layout.listitem_model, container, false)
        val nameTextView = view.findViewById(R.id.textview_name) as TextView
        val locTextView = view.findViewById(R.id.textview_location) as TextView
        nameTextView.text = value.name
        locTextView.text = if (value.location == FileLocation.INTERNAL) "Internal" else "External"
        container?.addView(view)
        return view
    }
}

class MainActivity : PrivilegedActivity() {
    private val REQUEST_STORAGE_READ = 1
    private val PERMISSION_READ_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE

    private lateinit var adapter: NewAdapter
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (!hasPermissions(PERMISSION_READ_STORAGE)) {
            showPermissionRequestDialog()
        } else {
            runPrivilegedAction(REQUEST_STORAGE_READ, PERMISSION_READ_STORAGE)
        }

        adapter = NewAdapter(this)
        pager.adapter = adapter
        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(p0: Int) {
                setSelectedModel(adapter.getItem(p0))
            }
        })

        val behavior = BottomSheetBehavior.from(preferences_view)
        behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState != BottomSheetBehavior.STATE_COLLAPSED) {
                    activity_filter_list_create_button.hide()
                    pager.visibility = View.GONE
                    fragment_wrapper.visibility = View.VISIBLE
                } else {
                    activity_filter_list_create_button.show()
                    pager.visibility = View.VISIBLE
                    fragment_wrapper.visibility = View.GONE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
        })

        behavior.peekHeight = 200
    }

    override fun onResume() {
        super.onResume()
        wallpaper_preview_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        wallpaper_preview_view.onPause()
    }

    private fun showPermissionRequestDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.req_permission_dialog_title)
            .setMessage(R.string.req_permission_dialog_message)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { v, i ->
                runPrivilegedAction(REQUEST_STORAGE_READ, PERMISSION_READ_STORAGE)
            }
            .show()
    }

    fun setSelectedModel(modelInfo: Live2DModelInfo) {
        preferences.edit()
            .putString(PrefConsts.PREF_MODEL_NAME, "${modelInfo.name}:${modelInfo.location.name}")
            .apply()
    }

    private fun setModelList(modelList: Array<Live2DModelInfo>) {
        adapter.replaceAll(listOf(*modelList))
    }

    override fun onRequestPermissionsResult(requestCode: Int, granted: Boolean) {
        if (requestCode != REQUEST_STORAGE_READ) return
        (object : AsyncTask<Unit, Unit, Array<Live2DModelInfo>>() {
            override fun doInBackground(vararg params: Unit?): Array<Live2DModelInfo> {
                return AssetLoader.enumerateModels(this@MainActivity, granted)
            }

            override fun onPostExecute(result: Array<Live2DModelInfo>) {
                setModelList(result)
            }
        }).execute()
    }
}
