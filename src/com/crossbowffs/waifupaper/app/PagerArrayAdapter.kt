package com.crossbowffs.waifupaper.app

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup

abstract class PagerArrayAdapter<T> : PagerAdapter() {
    private val objects = mutableListOf<T>()

    var notifyOnChange = true

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

    override fun getCount(): Int {
        return objects.size
    }

    override fun instantiateItem(container: ViewGroup?, position: Int): Any? {
        return instantiateItem(container, getItem(position))
    }

    override fun destroyItem(container: ViewGroup?, position: Int, obj: Any?) {
        container?.removeView(obj as View?)
    }

    override fun isViewFromObject(view: View?, obj: Any?): Boolean {
        return view == obj
    }

    abstract fun instantiateItem(container: ViewGroup?, value: T): View
}
