package Akari.Twitter.share.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import Akari.Twitter.share.data.PrefManager

class ConfigProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "Akari.Twitter.share.provider"
        const val METHOD_GET_CONFIG = "getConfig"
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        if (method == METHOD_GET_CONFIG) {
            val bundle = Bundle()
            bundle.putString(PrefManager.KEY_CUSTOM_S, PrefManager.getCustomS(ctx))
            bundle.putBoolean(PrefManager.KEY_IS_ENABLED, PrefManager.isEnabled(ctx))
            bundle.putBoolean(PrefManager.KEY_CLEAN_TRACKING, PrefManager.isCleanTracking(ctx))
            return bundle
        }
        return super.call(method, arg, extras)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}