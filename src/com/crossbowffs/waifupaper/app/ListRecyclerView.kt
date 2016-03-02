package com.crossbowffs.waifupaper.app

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.View

class ListRecyclerView : RecyclerView {
    class RecyclerContextMenuInfo(val targetView: View, val position: Int, val id: Long) : ContextMenu.ContextMenuInfo

    private val mAdapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            updateEmptyView()
        }
    }

    private var contextMenuInfo: RecyclerContextMenuInfo? = null

    var emptyView: View? = null
        set(value) {
            emptyView = value
            updateEmptyView()
        }

    constructor(context: Context) : super(context) {
        addListDividers(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        addListDividers(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        addListDividers(context)
    }

    override fun getContextMenuInfo(): ContextMenu.ContextMenuInfo {
        return contextMenuInfo!!
    }

    override fun showContextMenuForChild(originalView: View): Boolean {
        val position = getChildAdapterPosition(originalView)
        if (position >= 0) {
            val id = adapter.getItemId(position)
            contextMenuInfo = RecyclerContextMenuInfo(originalView, position, id)
            return super.showContextMenuForChild(originalView)
        }
        return false
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<out ViewHolder>?) {
        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(mAdapterObserver)
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(mAdapterObserver)
        updateEmptyView()
    }

    private fun addListDividers(context: Context) {
        addItemDecoration(ListDividerDecoration(context))
    }

    private fun updateEmptyView() {
        val adapter = adapter
        if (emptyView != null && adapter != null) {
            val emptyViewVisible = adapter.itemCount == 0
            emptyView!!.visibility = if (emptyViewVisible) View.VISIBLE else View.GONE
            visibility = if (emptyViewVisible) View.GONE else View.VISIBLE
        }
    }
}
