package com.altankoc.nidado.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetPrefKeys {
    val SELECTED_PAGE = intPreferencesKey("selected_page")
    val GRADIENT_ID = stringPreferencesKey("gradient_id")
    val LAST_COMPLETED_ITEM = intPreferencesKey("last_completed_item")
    val LAST_MUTED_LIST = intPreferencesKey("last_muted_list")
    val LAST_MUTED_STATE = booleanPreferencesKey("last_muted_state")
}