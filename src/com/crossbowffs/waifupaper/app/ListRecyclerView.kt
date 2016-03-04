package com.crossbowffs.waifupaper.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.View

private class ListDividerDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val divider: Drawable

    init {
        val attributes = context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider))
        divider = attributes.getDrawable(0)
        attributes.recycle()
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount
        for (i in 0..childCount - 1) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight
            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        outRect.set(0, 0, 0, divider.intrinsicHeight)
    }
}

class ListRecyclerView : RecyclerView {
    class RecyclerContextMenuInfo(val targetView: View, val position: Int, val id: Long) : ContextMenu.ContextMenuInfo

    private val adapterObserver = object : RecyclerView.AdapterDataObserver() {
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
        oldAdapter?.unregisterAdapterDataObserver(adapterObserver)
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(adapterObserver)
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
