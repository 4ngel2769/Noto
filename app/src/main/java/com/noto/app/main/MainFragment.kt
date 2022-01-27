package com.noto.app.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyViewHolder
import com.noto.app.*
import com.noto.app.databinding.BaseDialogFragmentBinding
import com.noto.app.databinding.MainFragmentBinding
import com.noto.app.domain.model.Folder
import com.noto.app.domain.model.LibraryListSortingType
import com.noto.app.domain.model.Note
import com.noto.app.domain.model.SortingOrder
import com.noto.app.util.*
import kotlinx.coroutines.flow.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : BaseDialogFragment(isCollapsable = true) {

    private val viewModel by viewModel<MainViewModel>()

    private lateinit var epoxyController: EpoxyController

    private lateinit var itemTouchHelper: ItemTouchHelper

    private val selectedDestinationId by lazy { navController?.lastDestinationId }

    private val popUpToDestinationId by lazy {
        when (selectedDestinationId) {
            AllNotesItemId -> R.id.allNotesFragment
            RecentNotesItemId -> R.id.recentNotesFragment
            else -> R.id.libraryFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = MainFragmentBinding.inflate(inflater, container, false).withBinding {
        setupBaseDialogFragment()
        setupListeners()
        setupState()
    }

    private fun MainFragmentBinding.setupBaseDialogFragment() = BaseDialogFragmentBinding.bind(root).apply {
        tvDialogTitle.text = context?.stringResource(R.string.app_name)
    }

    private fun MainFragmentBinding.setupListeners() {
        fab.setOnClickListener {
            dismiss()
            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToNewLibraryDialogFragment())
        }

        ibMore.setOnClickListener {
            dismiss()
            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToMainDialogFragment())
        }
    }

    private fun MainFragmentBinding.setupState() {
        rv.edgeEffectFactory = BounceEdgeEffectFactory()
        rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        combine(
            viewModel.libraries,
            viewModel.sortingType,
            viewModel.sortingOrder,
            viewModel.isShowNotesCount,
            viewModel.allNotes,
        ) { libraries, sortingType, sortingOrder, isShowNotesCount, allNotes ->
            setupLibraries(libraries, sortingType, sortingOrder, isShowNotesCount, allNotes)
            setupItemTouchHelper(sortingType == LibraryListSortingType.Manual)
        }.launchIn(lifecycleScope)

        viewModel.sortingType
            .onEach { sortingType ->
                rv.itemAnimator = when (sortingType) {
                    LibraryListSortingType.Manual -> DefaultItemAnimator().apply {
                        addDuration = DefaultAnimationDuration
                        changeDuration = DefaultAnimationDuration
                        moveDuration = DefaultAnimationDuration
                        removeDuration = DefaultAnimationDuration
                    }
                    else -> VerticalListItemAnimator()
                }
            }
            .launchIn(lifecycleScope)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun MainFragmentBinding.setupLibraries(
        state: UiState<List<Pair<Folder, Int>>>,
        sortingType: LibraryListSortingType,
        sortingOrder: SortingOrder,
        isShowNotesCount: Boolean,
        allNotes: List<Note>,
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
                    librariesCount(libraries.countRecursively())
                    onClickListener { _ ->
                        dismiss()
                        navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToLibraryListSortingDialogFragment())
                    }
                }

                inboxLibrary?.let {
                    folderItem {
                        id(inboxLibrary.first.id)
                        folder(inboxLibrary.first)
                        notesCount(inboxLibrary.second)
                        isManualSorting(isManualSorting)
                        isShowNotesCount(isShowNotesCount)
                        isSelected(inboxLibrary.first.id == selectedDestinationId)
                        onClickListener { _ ->
                            dismiss()
                            if (inboxLibrary.first.id != selectedDestinationId)
                                navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToLibraryFragment(inboxLibrary.first.id)) {
                                    popUpTo(popUpToDestinationId) {
                                        inclusive = true
                                    }
                                }
                        }
                        onLongClickListener { _ ->
                            dismiss()
                            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToLibraryDialogFragment(inboxLibrary.first.id))
                            true
                        }
                        onDragHandleTouchListener { _, _ -> false }
                    }
                }

                context?.let { context ->

                    genericItem {
                        id("all_notes")
                        notesCount(allNotes.count())
                        title(context.stringResource(R.string.all_notes))
                        icon(context.drawableResource(R.drawable.ic_round_all_inbox_24))
                        isManualSorting(isManualSorting)
                        isShowNotesCount(isShowNotesCount)
                        isSelected(AllNotesItemId == selectedDestinationId)
                        onClickListener { _ ->
                            dismiss()
                            if (selectedDestinationId != AllNotesItemId)
                                navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToAllNotesFragment()) {
                                    popUpTo(popUpToDestinationId) {
                                        inclusive = true
                                    }
                                }
                        }
                    }

                    genericItem {
                        id("recent_notes")
                        title(context.getString(R.string.recent_notes))
                        icon(context.drawableResource(R.drawable.ic_round_schedule_24))
                        notesCount(allNotes.filterRecentlyAccessed().count())
                        isManualSorting(isManualSorting)
                        isShowNotesCount(isShowNotesCount)
                        isSelected(RecentNotesItemId == selectedDestinationId)
                        onClickListener { _ ->
                            dismiss()
                            if (selectedDestinationId != RecentNotesItemId)
                                navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToRecentNotesFragment()) {
                                    popUpTo(popUpToDestinationId) {
                                        inclusive = true
                                    }
                                }
                        }
                    }

                    buildLibrariesModels(context, libraries) { libraries ->
                        libraries.forEachRecursively { entry, depth ->
                            folderItem {
                                id(entry.first.id)
                                folder(entry.first)
                                notesCount(entry.second)
                                isManualSorting(isManualSorting)
                                isShowNotesCount(isShowNotesCount)
                                isSelected(entry.first.id == selectedDestinationId)
                                depth(depth)
                                onClickListener { _ ->
                                    dismiss()
                                    if (entry.first.id != selectedDestinationId)
                                        navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToLibraryFragment(entry.first.id)) {
                                            popUpTo(popUpToDestinationId) {
                                                inclusive = true
                                            }
                                        }
                                }
                                onLongClickListener { _ ->
                                    dismiss()
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

    private fun MainFragmentBinding.setupItemTouchHelper(isManualSorting: Boolean) {
        if (isManualSorting) {
            if (this@MainFragment::epoxyController.isInitialized) {
                val itemTouchHelperCallback = LibraryItemTouchHelperCallback(
                    epoxyController,
                    onSwipe = { viewHolder, direction -> onSwipe(viewHolder, direction) },
                    onDrag = { onDrag() }
                )
                itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
                    .apply { attachToRecyclerView(rv) }
            }
        } else {
            if (this@MainFragment::itemTouchHelper.isInitialized) {
                itemTouchHelper.attachToRecyclerView(null)
            }
        }
    }

    private fun MainFragmentBinding.onSwipe(viewHolder: EpoxyViewHolder, direction: Int) {
        val libraries = viewModel.libraries.value.getOrDefault(emptyList())
        val model = viewHolder.model as? FolderItem
        if (model != null) {
            if (direction == ItemTouchHelper.START) {
                val parentId = libraries.findRecursively { it.first.id == model.folder.parentId }?.first?.parentId
                viewModel.updateLibraryParentId(model.folder, parentId)
            } else {
                val previousViewHolder = rv.findViewHolderForAdapterPosition(viewHolder.bindingAdapterPosition - 1) as EpoxyViewHolder?
                val previousModel = previousViewHolder?.model as? FolderItem?
                val parentId = libraries.findRecursively {
                    val isSameParent = it.first.parentId == model.folder.parentId
                    val isPreviousSelf = it.first.id == previousModel?.folder?.id
                    val isWithinPreviousLibraries = it.first.libraries.findRecursively { it.first.id == previousModel?.folder?.id } != null
                    isSameParent && (isPreviousSelf || isWithinPreviousLibraries)
                }?.first?.id
                if (parentId != null)
                    viewModel.updateLibraryParentId(model.folder, parentId)
            }
            epoxyController.notifyModelChanged(viewHolder.bindingAdapterPosition)
        }
    }

    private fun MainFragmentBinding.onDrag() {
        rv.forEach { view ->
            val viewHolder = rv.findContainingViewHolder(view) as EpoxyViewHolder
            val model = viewHolder.model as? FolderItem
            if (model != null) viewModel.updateLibraryPosition(model.folder, viewHolder.bindingAdapterPosition)
        }
    }
}