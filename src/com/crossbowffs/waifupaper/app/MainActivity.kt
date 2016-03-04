package com.crossbowffs.waifupaper.app

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.crossbowffs.waifupaper.R
import com.crossbowffs.waifupaper.loader.AssetLoader
import com.crossbowffs.waifupaper.loader.FileLocation
import com.crossbowffs.waifupaper.loader.Live2DModelInfo
import com.crossbowffs.waifupaper.utils.loge
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*

class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val nameTextView: TextView
    val locTextView: TextView

    init {
        nameTextView = itemView.findViewById(R.id.textview_name) as TextView
        locTextView = itemView.findViewById(R.id.textview_location) as TextView
    }
}

class MainAdapter(private val activity: MainActivity) : RecyclerArrayAdapter<Live2DModelInfo, MainViewHolder>() {
    override fun onCreateViewHolder(group: ViewGroup?, i: Int): MainViewHolder? {
        return MainViewHolder(activity.layoutInflater.inflate(R.layout.listitem_model, group, false))
    }

    override fun onBindViewHolder(vh: MainViewHolder, value: Live2DModelInfo) {
        vh.nameTextView.text = value.name
        vh.locTextView.text = if (value.location == FileLocation.INTERNAL) "Internal" else "External"
        vh.itemView.setOnClickListener {
            activity.setSelectedModel(value)
            Toast.makeText(activity, "Selected model: ${value.name}", Toast.LENGTH_SHORT).show()
        }
    }
}

class MainActivity : PrivilegedActivity() {
    private val REQUEST_STORAGE_READ = 1
    private val PERMISSION_READ_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE

    private lateinit var adapter: MainAdapter
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

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        adapter = MainAdapter(this)
        activity_main_list.adapter = adapter
        activity_main_list.layoutManager = LinearLayoutManager(this)
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
            .putString("selectedModel", "${modelInfo.name}:${modelInfo.location.name}")
            .apply()
    }

    private fun setModelList(modelList: Array<Live2DModelInfo>) {
        adapter.replaceAll(listOf(*modelList))
    }

    override fun onRequestPermissionsResult(requestCode: Int, granted: Boolean) {
        if (requestCode != REQUEST_STORAGE_READ) return
        (object : AsyncTask<Void, Void, Array<Live2DModelInfo>>() {
            override fun doInBackground(vararg p0: Void?): Array<Live2DModelInfo> {
                loge("Granted?: $granted")
                return AssetLoader.enumerateModels(this@MainActivity, granted)
            }

            override fun onPostExecute(result: Array<Live2DModelInfo>?) {
                setModelList(result!!)
            }
        }).execute()
    }
}
