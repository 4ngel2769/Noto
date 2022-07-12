package com.noto.app.util

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.noto.app.R
import com.noto.app.domain.model.Folder
import com.noto.app.domain.model.Icon
import com.noto.app.widget.NoteListWidgetConfigActivity
import com.noto.app.widget.NoteListWidgetService

fun Context.createNoteListWidgetRemoteViews(
    appWidgetId: Int,
    isHeaderEnabled: Boolean,
    isEditWidgetButtonEnabled: Boolean,
    isAppIconEnabled: Boolean,
    isNewFolderButtonEnabled: Boolean,
    widgetRadius: Int,
    folder: Folder,
    isEmpty: Boolean,
    icon: Icon,
): RemoteViews {
    val color = colorResource(folder.color.toResource())
    val activityName = icon.toActivityAliasName()
    return RemoteViews(packageName, R.layout.note_list_widget).apply {
        setTextViewText(R.id.tv_folder_title, folder.getTitle(this@createNoteListWidgetRemoteViews))
        setTextColor(R.id.tv_folder_title, color)
        setViewVisibility(R.id.ll_header, if (isHeaderEnabled) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.iv_edit_widget, if (isEditWidgetButtonEnabled) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.iv_app_icon, if (isAppIconEnabled) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.fab, if (isNewFolderButtonEnabled) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.fab, if (isNewFolderButtonEnabled) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.tv_placeholder, if (isEmpty) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.lv, if (isEmpty) View.GONE else View.VISIBLE)
        setOnClickPendingIntent(R.id.iv_edit_widget, createEditWidgetButtonPendingIntent(appWidgetId, folder.id))
        setOnClickPendingIntent(R.id.ib_fab, createNewNoteButtonPendingIntent(appWidgetId, folder.id, activityName))
        setOnClickPendingIntent(R.id.iv_app_icon, createAppLauncherPendingIntent(appWidgetId, activityName))
        setOnClickPendingIntent(R.id.tv_folder_title, createFolderLauncherPendingIntent(appWidgetId, folder.id, activityName))
        setRemoteAdapter(R.id.lv, createNoteListServiceIntent(appWidgetId, folder.id))
        setPendingIntentTemplate(R.id.lv, createNoteItemPendingIntent(appWidgetId, activityName))
        setInt(R.id.ll, SetBackgroundResourceMethodName, widgetRadius.toWidgetShapeId())
        setInt(R.id.ll_header, SetBackgroundResourceMethodName, widgetRadius.toWidgetHeaderShapeId())
        setInt(R.id.iv_fab, SetColorFilterMethodName, color)
        setInt(R.id.iv_app_icon, SetImageResource, icon.toResource())
        if (isAppIconEnabled)
            setViewPadding(R.id.tv_folder_title, 0.dp, 16.dp, 0.dp, 16.dp)
        else
            setViewPadding(R.id.tv_folder_title, 16.dp, 16.dp, 16.dp, 16.dp)
    }
}

private fun Context.createEditWidgetButtonPendingIntent(appWidgetId: Int, folderId: Long): PendingIntent? {
    val intent = Intent(
        AppWidgetManager.ACTION_APPWIDGET_CONFIGURE,
        null,
        this,
        NoteListWidgetConfigActivity::class.java
    ).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra(Constants.FolderId, folderId)
        data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
    }
    return PendingIntent.getActivity(this, appWidgetId, intent, PendingIntentFlags)
}

private fun Context.createFolderLauncherPendingIntent(appWidgetId: Int, folderId: Long, activityName: String): PendingIntent? {
    val intent = Intent(Constants.Intent.ActionOpenFolder, null).apply {
        setClassName(this@createFolderLauncherPendingIntent, activityName)
        putExtra(Constants.FolderId, folderId)
    }
    return PendingIntent.getActivity(this, appWidgetId, intent, PendingIntentFlags)
}

private fun Context.createNewNoteButtonPendingIntent(appWidgetId: Int, folderId: Long, activityName: String): PendingIntent? {
    val intent = Intent(Constants.Intent.ActionCreateNote, null).apply {
        setClassName(this@createNewNoteButtonPendingIntent, activityName)
        putExtra(Constants.FolderId, folderId)
    }
    return PendingIntent.getActivity(this, appWidgetId, intent, PendingIntentFlags)
}

private fun Context.createNoteListServiceIntent(appWidgetId: Int, folderId: Long): Intent {
    return Intent(this, NoteListWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra(Constants.FolderId, folderId)
        data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
    }
}

private fun Context.createNoteItemPendingIntent(appWidgetId: Int, activityName: String): PendingIntent? {
    val intent = Intent(Constants.Intent.ActionOpenNote, null).apply {
        setClassName(this@createNoteItemPendingIntent, activityName)
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
    }
    return PendingIntent.getActivity(this, appWidgetId, intent, PendingIntentFlags)
}