package com.noto.app.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyViewHolder
import com.noto.app.BaseDialogFragment
import com.noto.app.R
import com.noto.app.UiState
import com.noto.app.allnotes.allNotesItem
import com.noto.app.databinding.BaseDialogFragmentBinding
import com.noto.app.databinding.MainFragmentBinding
import com.noto.app.domain.model.Library
import com.noto.app.domain.model.LibraryListSortingType
import com.noto.app.domain.model.SortingOrder
import com.noto.app.map
import com.noto.app.util.*
import kotlinx.coroutines.flow.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : BaseDialogFragment(isCollapsable = true) {

    private val viewModel by viewModel<MainViewModel>()

    private lateinit var epoxyController: EpoxyController

    private lateinit var itemTouchHelper: ItemTouchHelper

    private val selectedLibraryId by lazy { navController?.lastLibraryId }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        MainFragmentBinding.inflate(inflater, container, false).withBinding {
            setupBaseDialogFragment()
            setupListeners()
            setupState()
        }

    private fun MainFragmentBinding.setupBaseDialogFragment() = BaseDialogFragmentBinding.bind(root).apply {
        tvDialogTitle.text = context?.stringResource(R.string.app_name)
    }

    private fun MainFragmentBinding.setupListeners() {
        fab.setOnClickListener {
            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToNewLibraryDialogFragment())
        }

        ibArchive.setOnClickListener {
            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToMainArchiveFragment())
        }

        ibVault.setOnClickListener {
            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToMainVaultFragment())
        }

        ibSettings.setOnClickListener {
            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToSettingsFragment())
        }
    }

    private fun MainFragmentBinding.setupState() {
        rv.edgeEffectFactory = BounceEdgeEffectFactory()
        rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rv.itemAnimator = VerticalListItemAnimator()

        combine(
            viewModel.libraries,
            viewModel.sortingType,
            viewModel.sortingOrder,
            viewModel.isShowNotesCount,
            viewModel.allNotesCount,
        ) { libraries, sortingType, sortingOrder, isShowNotesCount, allNotesCount ->
            setupLibraries(libraries.map { it.sorted(sortingType, sortingOrder) }, sortingType, sortingOrder, isShowNotesCount, allNotesCount)
            setupItemTouchHelper()
        }.launchIn(lifecycleScope)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun MainFragmentBinding.setupLibraries(
        state: UiState<List<Pair<Library, Int>>>,
        sortingType: LibraryListSortingType,
        sortingOrder: SortingOrder,
        isShowNotesCount: Boolean,
        allNotesCount: Int,
    ) {
        if (state is UiState.Success) {
            rv.withModels {
                epoxyController = this
                val libraries = state.value.filterNot { it.first.isInbox }
                val inboxLibrary = state.value.firstOrNull { it.first.isInbox }
                val isManualSorting = sortingType == LibraryListSortingType.Manual

                libraryListSortingItem {
                    id(0)
                    sortingType(sortingType)
                    sortingOrder(sortingOrder)
                    librariesCount(libraries.size)
                    onClickListener { _ ->
                        navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToLibraryListSortingDialogFragment())
                    }
                }

                inboxLibrary?.let {
                    libraryItem {
                        id(inboxLibrary.first.id)
                        library(inboxLibrary.first)
                        notesCount(inboxLibrary.second)
                        isManualSorting(isManualSorting)
                        isShowNotesCount(isShowNotesCount)
                        isSelected(inboxLibrary.first.id == selectedLibraryId)
                        onClickListener { _ ->
                            if (inboxLibrary.first.id != selectedLibraryId)
                                navigateToLibraryFragment(inboxLibrary.first.id)
                            dismiss()
                        }
                        onLongClickListener { _ ->
                            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToLibraryDialogFragment(inboxLibrary.first.id))
                            true
                        }
                        onDragHandleTouchListener { _, _ -> false }
                    }
                }

                allNotesItem {
                    id("all_notes")
                    notesCount(allNotesCount)
                    isManualSorting(isManualSorting)
                    isShowNotesCount(isShowNotesCount)
                    isSelected(AllNotesItemId == selectedLibraryId)
                    onClickListener { _ ->
                        if (selectedLibraryId != AllNotesItemId)
                            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToAllNotesFragment())
                        dismiss()
                    }
                }

                context?.let { context ->
                    buildLibrariesModels(context, libraries) { libraries ->
                        libraries.forEach { entry ->
                            libraryItem {
                                id(entry.first.id)
                                library(entry.first)
                                notesCount(entry.second)
                                isManualSorting(isManualSorting)
                                isShowNotesCount(isShowNotesCount)
                                isSelected(entry.first.id == selectedLibraryId)
                                onClickListener { _ ->
                                    if (entry.first.id != selectedLibraryId)
                                        navigateToLibraryFragment(entry.first.id)
                                    dismiss()
                                }
                                onLongClickListener { _ ->
                                    navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToLibraryDialogFragment(entry.first.id))
                                    true
                                }
                                onDragHandleTouchListener { view, event ->
                                    if (event.action == MotionEvent.ACTION_DOWN)
                                        rv.findContainingViewHolder(view)?.let { viewHolder ->
                                            if (this@MainFragment::itemTouchHelper.isInitialized)
                                                itemTouchHelper.startDrag(viewHolder)
                                        }
                                    view.performClick()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun MainFragmentBinding.setupItemTouchHelper() {
        if (this@MainFragment::epoxyController.isInitialized) {
            val itemTouchHelperCallback = LibraryItemTouchHelperCallback(epoxyController) {
                rv.forEach { view ->
                    val viewHolder = rv.findContainingViewHolder(view) as EpoxyViewHolder
                    val model = viewHolder.model as? LibraryItem
                    if (model != null) viewModel.updateLibraryPosition(model.library, viewHolder.bindingAdapterPosition)
                }
            }
            itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
                .apply { attachToRecyclerView(rv) }
        }
    }

    private fun navigateToLibraryFragment(libraryId: Long) {
        navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToLibraryFragment(libraryId)) {
            popUpTo(if (selectedLibraryId == AllNotesItemId) R.id.allNotesFragment else R.id.libraryFragment) {
                inclusive = true
            }
        }
    }
}