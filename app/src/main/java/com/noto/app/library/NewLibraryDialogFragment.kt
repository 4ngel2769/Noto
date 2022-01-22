package com.noto.app.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.noto.app.BaseDialogFragment
import com.noto.app.R
import com.noto.app.databinding.BaseDialogFragmentBinding
import com.noto.app.databinding.NewLibraryDialogFragmentBinding
import com.noto.app.domain.model.Layout
import com.noto.app.domain.model.Library
import com.noto.app.domain.model.NewNoteCursorPosition
import com.noto.app.domain.model.NotoColor
import com.noto.app.util.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NewLibraryDialogFragment : BaseDialogFragment() {

    private val viewModel by viewModel<LibraryViewModel> { parametersOf(args.libraryId) }

    private val args by navArgs<NewLibraryDialogFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        NewLibraryDialogFragmentBinding.inflate(inflater, container, false).withBinding {
            val baseDialogFragment = setupBaseDialogFragment()
            setupState(baseDialogFragment)
            setupListeners()
        }

    private fun NewLibraryDialogFragmentBinding.setupBaseDialogFragment() = BaseDialogFragmentBinding.bind(root).apply {
        context?.let { context ->
            when (args.libraryId) {
                0L -> {
                    tvDialogTitle.text = context.stringResource(R.string.new_library)
                }
                Library.InboxId -> {
                    tvDialogTitle.text = context.stringResource(R.string.edit_inbox)
                    btnCreate.text = context.stringResource(R.string.done)
                }
                else -> {
                    tvDialogTitle.text = context.stringResource(R.string.edit_library)
                    btnCreate.text = context.stringResource(R.string.done)
                }
            }
        }
    }

    private fun NewLibraryDialogFragmentBinding.setupState(baseDialogFragment: BaseDialogFragmentBinding) {
        rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rv.clipToOutline = true
        rv.itemAnimator = HorizontalListItemAnimator()
        rv.edgeEffectFactory = BounceEdgeEffectFactory()

        if (args.libraryId == 0L) {
            et.requestFocus()
            activity?.showKeyboard(root)
        }

        if (args.libraryId == Library.InboxId) {
            et.setText(context?.stringResource(R.string.inbox))
            til.isVisible = false
            tvLibraryTitle.isVisible = false
            tvLibraryColor.text = context?.stringResource(R.string.inbox_color)
            tvLibraryLayout.text = context?.stringResource(R.string.inbox_layout)
        }

        viewModel.library
            .onEach { library -> setupLibrary(library, baseDialogFragment) }
            .launchIn(lifecycleScope)

        viewModel.notoColors
            .onEach { pairs -> setupNotoColors(pairs) }
            .launchIn(lifecycleScope)
    }

    private fun NewLibraryDialogFragmentBinding.setupListeners() {
        btnCreate.setOnClickListener {
            val title = et.text.toString()
            if (title.isBlank()) {
                til.isErrorEnabled = true
                context?.let { context ->
                    til.error = context.stringResource(R.string.empty_title)
                }
            } else {
                val layout = tlLibraryLayout.selectedTabPosition.let {
                    when (it) {
                        0 -> Layout.Linear
                        else -> Layout.Grid
                    }
                }
                val cursorPosition = tlNewNoteCursorPosition.selectedTabPosition.let {
                    when (it) {
                        0 -> NewNoteCursorPosition.Body
                        else -> NewNoteCursorPosition.Title
                    }
                }
                activity?.hideKeyboard(root)
                updatePinnedShortcut(title)
                viewModel.createOrUpdateLibrary(
                    title,
                    layout,
                    sNotePreviewSize.value.toInt(),
                    cursorPosition,
                    swShowNoteCreationDate.isChecked,
                ).invokeOnCompletion {
                    context?.updateAllWidgetsData()
                    context?.updateLibraryListWidgets()
                    context?.updateNoteListWidgets(viewModel.library.value.id)
                    dismiss()
                }
            }
        }
    }

    private fun NewLibraryDialogFragmentBinding.setupLibrary(library: Library, baseDialogFragment: BaseDialogFragmentBinding) {
        rv.smoothScrollToPosition(library.color.ordinal)
        val layoutTab = when (library.layout) {
            Layout.Linear -> tlLibraryLayout.getTabAt(0)
            Layout.Grid -> tlLibraryLayout.getTabAt(1)
        }
        val cursorPositionTab = when (library.newNoteCursorPosition) {
            NewNoteCursorPosition.Body -> tlNewNoteCursorPosition.getTabAt(0)
            NewNoteCursorPosition.Title -> tlNewNoteCursorPosition.getTabAt(1)
        }
        tlLibraryLayout.selectTab(layoutTab)
        tlNewNoteCursorPosition.selectTab(cursorPositionTab)
        swShowNoteCreationDate.isChecked = library.isShowNoteCreationDate
        context?.let { context ->
            et.setText(library.getTitle(context))
            et.setSelection(library.getTitle(context).length)
            if (library.id != 0L) {
                val color = context.colorResource(library.color.toResource())
                val colorStateList = color.toColorStateList()
                baseDialogFragment.tvDialogTitle.setTextColor(color)
                baseDialogFragment.vHead.background?.mutate()?.setTint(color)
                tlLibraryLayout.setSelectedTabIndicatorColor(color)
                tlNewNoteCursorPosition.setSelectedTabIndicatorColor(color)
                sNotePreviewSize.value = library.notePreviewSize.toFloat()
                tlLibraryLayout.tabRippleColor = colorStateList
                tlNewNoteCursorPosition.tabRippleColor = colorStateList
                sNotePreviewSize.trackActiveTintList = colorStateList
                sNotePreviewSize.thumbTintList = colorStateList
                sNotePreviewSize.tickInactiveTintList = colorStateList
                swShowNoteCreationDate.setupColors(thumbCheckedColor = color, trackCheckedColor = color)
            } else {
                swShowNoteCreationDate.setupColors()
            }
        }
    }

    private fun NewLibraryDialogFragmentBinding.setupNotoColors(pairs: List<Pair<NotoColor, Boolean>>) {
        rv.withModels {
            pairs.forEach { pair ->
                notoColorItem {
                    id(pair.first.ordinal)
                    notoColor(pair.first)
                    isChecked(pair.second)
                    onClickListener { _ ->
                        viewModel.selectNotoColor(pair.first)
                    }
                }
            }
        }
    }

    private fun NewLibraryDialogFragmentBinding.updatePinnedShortcut(title: String) {
        val library = viewModel.library.value.copy(
            title = title,
            color = viewModel.notoColors.value.first { it.second }.first
        )
        context?.let { context ->
            ShortcutManagerCompat.updateShortcuts(context, listOf(context.createPinnedShortcut(library)))
        }
    }
}