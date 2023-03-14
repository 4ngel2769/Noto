package com.noto.app.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyViewHolder
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.noto.app.R
import com.noto.app.UiState
import com.noto.app.components.BaseDialogFragment
import com.noto.app.components.genericItem
import com.noto.app.databinding.MainFragmentBinding
import com.noto.app.domain.model.Folder
import com.noto.app.domain.model.FolderListSortingType
import com.noto.app.domain.model.Note
import com.noto.app.domain.model.SortingOrder
import com.noto.app.getOrDefault
import com.noto.app.util.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class MainFragment : BaseDialogFragment(isCollapsable = true) {

    private val viewModel by sharedViewModel<MainViewModel>()

    private val args by navArgs<MainFragmentArgs>()

    private lateinit var epoxyController: EpoxyController

    private lateinit var itemTouchHelper: ItemTouchHelper

    private val selectedDestinationId by lazy { navController?.lastDestinationId }

    private val popUpToDestinationId by lazy {
        when (selectedDestinationId) {
            AllNotesItemId -> R.id.allNotesFragment
            RecentNotesItemId -> R.id.recentNotesFragment
            else -> R.id.folderFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = MainFragmentBinding.inflate(inflater, container, false).withBinding {
        setupMixedTransitions()
        setupListeners()
        setupState()
    }

    private fun MainFragmentBinding.setupListeners() {
        fab.setOnClickListener {
            dismiss()
            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToNewFolderFragment())
        }

        ibMore.setOnClickListener {
            dismiss()
            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToMainDialogFragment())
        }

        ibSettings.setOnClickListener {
            dismiss()
            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToSettingsFragment())
        }

        ibSorting.setOnClickListener {
            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToFolderListViewDialogFragment())
        }
    }

    private fun MainFragmentBinding.setupState() {
        rv.edgeEffectFactory = BounceEdgeEffectFactory()
        rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        tvFoldersCount.typeface = context?.tryLoadingFontResource(R.font.nunito_semibold)
        tvFoldersCount.animationInterpolator = DefaultInterpolator()

        combine(
            viewModel.folders,
            viewModel.sortingType,
            viewModel.sortingOrder,
            viewModel.isShowNotesCount,
            viewModel.allNotes,
        ) { folders, sortingType, sortingOrder, isShowNotesCount, allNotes ->
            setupFolders(folders, sortingType, sortingOrder, isShowNotesCount, allNotes)
            setupItemTouchHelper(sortingType == FolderListSortingType.Manual)
        }.launchIn(lifecycleScope)

        viewModel.sortingType
            .onEach { sortingType ->
                rv.itemAnimator = when (sortingType) {
                    FolderListSortingType.Manual -> DefaultItemAnimator().apply {
                        addDuration = DefaultAnimationDuration
                        changeDuration = DefaultAnimationDuration
                        moveDuration = DefaultAnimationDuration
                        removeDuration = DefaultAnimationDuration
                    }
                    else -> VerticalListItemAnimator()
                }
            }
            .launchIn(lifecycleScope)

        rv.isScrollingAsFlow()
            .onEach { isScrolling -> tb.isSelected = isScrolling }
            .launchIn(lifecycleScope)

        if (isCurrentLocaleArabic()) {
            tvFoldersCount.isVisible = false
            tvFoldersCountRtl.isVisible = true
        } else {
            tvFoldersCount.isVisible = true
            tvFoldersCountRtl.isVisible = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun MainFragmentBinding.setupFolders(
        state: UiState<List<Pair<Folder, Int>>>,
        sortingType: FolderListSortingType,
        sortingOrder: SortingOrder,
        isShowNotesCount: Boolean,
        allNotes: List<Note>,
    ) {
        if (state is UiState.Success) {

            rv.withModels {
                epoxyController = this
                val folders = state.value.filterNot { it.first.isGeneral }
                val generalFolder = state.value.firstOrNull { it.first.isGeneral }
                val isManualSorting = sortingType == FolderListSortingType.Manual
                val foldersCount = folders.countRecursively()

                tvFoldersCount.text = context?.quantityStringResource(R.plurals.folders_count, foldersCount, foldersCount)
                tvFoldersCountRtl.text = context?.quantityStringResource(R.plurals.folders_count, foldersCount, foldersCount)

                generalFolder?.let {
                    folderItem {
                        id(generalFolder.first.id)
                        folder(generalFolder.first)
                        notesCount(generalFolder.second)
                        isManualSorting(isManualSorting)
                        isShowNotesCount(isShowNotesCount)
                        isSelected(generalFolder.first.id == selectedDestinationId)
                        onClickListener { _ ->
                            dismiss()
                            if (generalFolder.first.id != selectedDestinationId)
                                navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToFolderFragment(generalFolder.first.id)) {
                                    popUpTo(popUpToDestinationId) {
                                        inclusive = true
                                    }
                                }
                        }
                        onLongClickListener { _ ->
                            dismiss()
                            navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToFolderDialogFragment(generalFolder.first.id))
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
                        context.drawableResource(R.drawable.ic_round_all_notes_24)?.let { drawable ->
                            icon(drawable)
                        }
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
                        context.drawableResource(R.drawable.ic_round_schedule_24)?.let { drawable ->
                            icon(drawable)
                        }
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

                    buildFoldersModels(context, folders) { folders ->
                        folders.forEachRecursively { entry, depth ->
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
                                        navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToFolderFragment(entry.first.id)) {
                                            popUpTo(popUpToDestinationId) {
                                                inclusive = true
                                            }
                                        }
                                }
                                onLongClickListener { _ ->
                                    dismiss()
                                    navController?.navigateSafely(MainFragmentDirections.actionMainFragmentToFolderDialogFragment(entry.first.id))
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
                val itemTouchHelperCallback = FolderItemTouchHelperCallback(
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
        val folders = viewModel.folders.value.getOrDefault(emptyList())
        val model = viewHolder.model as? FolderItem
        if (model != null) {
            if (direction == ItemTouchHelper.START) {
                val parentId = folders.findRecursively { it.first.id == model.folder.parentId }?.first?.parentId
                viewModel.updateFolderParentId(model.folder, parentId)
            } else {
                val previousViewHolder = rv.findViewHolderForAdapterPosition(viewHolder.bindingAdapterPosition - 1) as EpoxyViewHolder?
                val previousModel = previousViewHolder?.model as? FolderItem?
                val parentId = folders.findRecursively {
                    val isSameParent = it.first.parentId == model.folder.parentId
                    val isPreviousSelf = it.first.id == previousModel?.folder?.id
                    val isWithinPreviousFolders = it.first.folders.findRecursively { it.first.id == previousModel?.folder?.id } != null
                    isSameParent && (isPreviousSelf || isWithinPreviousFolders)
                }?.first?.id
                if (parentId != null)
                    viewModel.updateFolderParentId(model.folder, parentId)
            }
            epoxyController.notifyModelChanged(viewHolder.bindingAdapterPosition)
        }
    }

    private fun MainFragmentBinding.onDrag() {
        rv.forEach { view ->
            val viewHolder = rv.findContainingViewHolder(view) as EpoxyViewHolder
            val model = viewHolder.model as? FolderItem
            if (model != null) viewModel.updateFolderPosition(model.folder, viewHolder.bindingAdapterPosition)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            override fun onBackPressed() {
                if (args.exit)
                    activity?.finish()
                else
                    super.onBackPressed()
            }
        }
    }

}