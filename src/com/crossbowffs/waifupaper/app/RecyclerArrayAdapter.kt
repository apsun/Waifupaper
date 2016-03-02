package com.crossbowffs.waifupaper.app

import android.support.v7.widget.RecyclerView

abstract class RecyclerArrayAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    private val objects = mutableListOf<T>()

    var notifyOnChange = true

    init {
        setHasStableIds(true)
    }

    fun add(value: T) {
        objects.add(value)
        if (notifyOnChange) {
            notifyDataSetChanged()
        }
    }

    fun addAll(values: List<T>) {
        for (v in values) {
            objects.add(v)
        }
        if (notifyOnChange) {
            notifyDataSetChanged()
        }
    }

    fun clear() {
        objects.clear()
        if (notifyOnChange) {
            notifyDataSetChanged()
        }
    }

    fun replaceAll(values: List<T>) {
        objects.clear()
        for (v in values) {
            objects.add(v)
        }
        if (notifyOnChange) {
            notifyDataSetChanged()
        }
    }

    fun getItem(position: Int): T {
        return objects[position]
    }

    override fun getItemCount(): Int {
        return objects.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(vh: VH, i: Int) {
        onBindViewHolder(vh, getItem(i))
    }

    abstract fun onBindViewHolder(vh: VH, value: T)
}
