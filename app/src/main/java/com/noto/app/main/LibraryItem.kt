package com.noto.app.main

import android.annotation.SuppressLint
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMarginsRelative
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.noto.app.R
import com.noto.app.databinding.LibraryItemBinding
import com.noto.app.domain.model.Library
import com.noto.app.util.*

@SuppressLint("NonConstantResourceId")
@EpoxyModelClass(layout = R.layout.library_item)
abstract class LibraryItem : EpoxyModelWithHolder<LibraryItem.Holder>() {

    @EpoxyAttribute
    lateinit var library: Library

    @EpoxyAttribute
    open var isManualSorting: Boolean = false

    @EpoxyAttribute
    open var isShowNotesCount: Boolean = true

    @EpoxyAttribute
    var notesCount: Int = 0

    @EpoxyAttribute
    open var isClickable: Boolean = true

    @EpoxyAttribute
    open var isLongClickable: Boolean = true

    @EpoxyAttribute
    open var isSelected: Boolean = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    lateinit var onClickListener: View.OnClickListener

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    lateinit var onLongClickListener: View.OnLongClickListener

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    lateinit var onDragHandleTouchListener: View.OnTouchListener

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(holder: Holder) = with(holder.binding) {
        root.context?.let { context ->
            val color = context.colorResource(library.color.toResource())
            val selectedColorStateList = color.withDefaultAlpha().toColorStateList()
            val rippleDrawable = root.background as RippleDrawable
            rippleDrawable.setColor(selectedColorStateList)
            tvNotesCount.text = notesCount.toString()
            tvTitle.setTextColor(color)
            tvNotesCount.setTextColor(color)
            ibDrag.drawable?.mutate()?.setTint(color)
            root.backgroundTintList = if (isSelected)
                selectedColorStateList
            else
                context.attributeColoResource(R.attr.notoBackgroundColor).toColorStateList()
            if (library.isInbox) {
                tvTitle.text = context.stringResource(R.string.inbox)
                ivIcon.setImageDrawable(context.drawableResource(R.drawable.ic_round_inbox_24))
            } else {
                tvTitle.text = library.title
                ivIcon.setImageDrawable(context.drawableResource(R.drawable.ic_round_folder_24))
            }
            ivIcon.imageTintList = color.toColorStateList()
        }
        ibDrag.visibility = when {
            isManualSorting && !library.isInbox -> View.VISIBLE
            isManualSorting && library.isInbox -> View.INVISIBLE
            else -> View.GONE
        }
        ibDrag.setOnTouchListener(onDragHandleTouchListener)
        root.setOnClickListener(onClickListener)
        root.setOnLongClickListener(onLongClickListener)
        root.isClickable = isClickable
        root.isLongClickable = isLongClickable
        tvNotesCount.isVisible = isShowNotesCount
        tvTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMarginsRelative(end = if (!isShowNotesCount && !isManualSorting) 16.dp else 8.dp)
        }
        tvNotesCount.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMarginsRelative(end = if (isManualSorting) 8.dp else 0.dp)
        }
    }

    class Holder : EpoxyHolder() {
        lateinit var binding: LibraryItemBinding
            private set

        override fun bindView(itemView: View) {
            binding = LibraryItemBinding.bind(itemView)
        }
    }
}