package com.noto.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.noto.app.BaseActivity
import com.noto.app.R
import com.noto.app.databinding.NoteListWidgetConfigActivityBinding
import com.noto.app.label.labelItem
import com.noto.app.main.SelectFolderDialogFragment
import com.noto.app.util.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NoteListWidgetConfigActivity : BaseActivity() {

    private val viewModel by viewModel<NoteListWidgetConfigViewModel> { parametersOf(appWidgetId) }

    private val appWidgetId by lazy {
        intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private val folderId by lazy { intent?.getLongExtra(Constants.FolderId, 0) ?: 0 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NoteListWidgetConfigActivityBinding.inflate(layoutInflater).withBinding {
            setContentView(root)
            if (folderId == 0L)
                showSelectFolderDialog(false)
            else
                viewModel.getWidgetData(folderId)
            setupState()
            setupListeners()
        }
    }

    private fun NoteListWidgetConfigActivityBinding.setupState() {
        setResult(Activity.RESULT_CANCELED)
        widget.lv.dividerHeight = 16.dp
        widget.lv.setPaddingRelative(8.dp, 16.dp, 8.dp, 100.dp)
        widget.root.clipToOutline = true
        rv.edgeEffectFactory = BounceEdgeEffectFactory()

        viewModel.isWidgetCreated
            .onEach { isCreated ->
                if (isCreated) {
                    tb.title = stringResource(R.string.edit_notes_widget)
                    btnCreate.text = stringResource(R.string.done)
                }
            }
            .launchIn(lifecycleScope)

        combine(viewModel.folder, viewModel.notes, viewModel.labels) { folder, notes, labels ->
            val filteredNotes = notes.filter { it.second.containsAll(labels.filterSelected()) }
            val color = colorResource(folder.color.toResource())
            val colorStateList = color.toColorStateList()
            tvFilterLabels.isVisible = labels.isNotEmpty()
            rv.isVisible = labels.isNotEmpty()
            divider2.root.isVisible = labels.isNotEmpty()
            widget.tvFolderTitle.text = folder.getTitle(this@NoteListWidgetConfigActivity)
            widget.tvFolderTitle.setTextColor(color)
            widget.fab.background?.setTint(color)
            widget.ivFab.setColorFilter(color)
            sWidgetRadius.trackActiveTintList = colorStateList
            sWidgetRadius.thumbTintList = colorStateList
            sWidgetRadius.tickInactiveTintList = colorStateList
            sWidgetRadius.trackInactiveTintList = color.withDefaultAlpha().toColorStateList()
            listOf(swWidgetHeader, swEditWidget, swAppIcon, swNewFolder)
                .onEach { it.setupColors(thumbCheckedColor = color, trackCheckedColor = color) }
            listOf(divider1, divider2, divider3, divider4)
                .onEach { divider -> divider.root.background?.mutate()?.setTint(color.withDefaultAlpha()) }
            if (filteredNotes.isEmpty()) {
                widget.lv.isVisible = false
                widget.tvPlaceholder.isVisible = true
            } else {
                widget.lv.isVisible = true
                widget.tvPlaceholder.isVisible = false
                widget.lv.adapter = NoteListWidgetAdapter(
                    this@NoteListWidgetConfigActivity,
                    R.layout.note_list_widget,
                    filteredNotes,
                    folder.isShowNoteCreationDate,
                    folder.color,
                    folder.notePreviewSize,
                )
            }

            rv.withModels {
                labels.forEach { entry ->
                    labelItem {
                        id(entry.key.id)
                        label(entry.key)
                        isSelected(entry.value)
                        color(folder.color)
                        onClickListener { _ ->
                            if (entry.value)
                                viewModel.deselectLabel(entry.key.id)
                            else
                                viewModel.selectLabel(entry.key.id)
                        }
                        onLongClickListener { _ -> false }
                    }
                }
            }
        }.launchIn(lifecycleScope)

        viewModel.isWidgetHeaderEnabled
            .onEach { isEnabled ->
                widget.llHeader.isVisible = isEnabled
                swWidgetHeader.isChecked = isEnabled
                swAppIcon.isVisible = isEnabled
                swEditWidget.isVisible = isEnabled
            }
            .launchIn(lifecycleScope)

        viewModel.isEditWidgetButtonEnabled
            .onEach { isEnabled ->
                widget.llEditWidget.isVisible = isEnabled
                swEditWidget.isChecked = isEnabled
            }
            .launchIn(lifecycleScope)

        viewModel.isAppIconEnabled
            .onEach { isEnabled ->
                widget.ivAppIcon.isVisible = isEnabled
                swAppIcon.isChecked = isEnabled
                if (isEnabled)
                    widget.tvFolderTitle.setPadding(0.dp, 16.dp, 0.dp, 16.dp)
                else
                    widget.tvFolderTitle.setPadding(16.dp)
            }
            .launchIn(lifecycleScope)

        viewModel.isNewFolderButtonEnabled
            .onEach { isEnabled ->
                widget.fab.isVisible = isEnabled
                swNewFolder.isChecked = isEnabled
            }
            .launchIn(lifecycleScope)

        viewModel.widgetRadius
            .onEach { radius ->
                sWidgetRadius.value = radius.toFloat()
                widget.ll.background = drawableResource(radius.toWidgetShapeId())
                widget.llHeader.background = drawableResource(radius.toWidgetHeaderShapeId())
            }
            .launchIn(lifecycleScope)
    }

    private fun NoteListWidgetConfigActivityBinding.setupListeners() {
        tvSelectFolder.setOnClickListener {
            showSelectFolderDialog(true)
        }

        swWidgetHeader.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsWidgetHeaderEnabled(isChecked)
        }

        swEditWidget.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsEditWidgetButtonEnabled(isChecked)
        }

        swAppIcon.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsAppIconEnabled(isChecked)
        }

        swNewFolder.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsNewFolderButtonEnabled(isChecked)
        }

        sWidgetRadius.addOnChangeListener { _, value, _ ->
            viewModel.setWidgetRadius(value.toInt())
        }

        btnCreate.setOnClickListener {
//            sendBroadcast() Maybe we can send broadcast to NoteListWidgetProvider instead of updating it manually
            viewModel.createOrUpdateWidget()
            val appWidgetManager = AppWidgetManager.getInstance(this@NoteListWidgetConfigActivity)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv)
            appWidgetManager.updateAppWidget(
                appWidgetId,
                createNoteListWidgetRemoteViews(
                    appWidgetId,
                    viewModel.isWidgetHeaderEnabled.value,
                    viewModel.isEditWidgetButtonEnabled.value,
                    viewModel.isAppIconEnabled.value,
                    viewModel.isNewFolderButtonEnabled.value,
                    viewModel.widgetRadius.value,
                    viewModel.folder.value,
                    viewModel.notes.value.isEmpty(),
                )
            )
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

    private fun showSelectFolderDialog(isDismissible: Boolean) {
        val args = bundleOf(
            Constants.FilteredFolderIds to longArrayOf(),
            Constants.IsDismissible to isDismissible,
            Constants.SelectedFolderId to viewModel.folder.value.id,
        )
        SelectFolderDialogFragment { folderId -> viewModel.getWidgetData(folderId) }
            .apply { arguments = args }
            .show(supportFragmentManager, null)
    }
}