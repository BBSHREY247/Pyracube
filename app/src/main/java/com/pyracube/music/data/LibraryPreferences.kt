package com.pyracube.music.data

import android.content.Context

class LibraryPreferences(
    context: Context
) {
    private val preferences =
        context.getSharedPreferences(
            "pyracube_library",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val SORT_FIELD = "sort_field"
        private const val SORT_ASCENDING = "sort_ascending"
        private const val DEFAULT_SORT_FIELD = "TITLE"
        private const val DEFAULT_SORT_ASCENDING = true
    }

    fun getSortField(): String {
        return preferences.getString(SORT_FIELD, DEFAULT_SORT_FIELD)
            ?: DEFAULT_SORT_FIELD
    }

    fun isSortAscending(): Boolean {
        return preferences.getBoolean(SORT_ASCENDING, DEFAULT_SORT_ASCENDING)
    }

    fun saveSort(field: String, ascending: Boolean) {
        preferences.edit()
            .putString(SORT_FIELD, field)
            .putBoolean(SORT_ASCENDING, ascending)
            .apply()
    }
}
