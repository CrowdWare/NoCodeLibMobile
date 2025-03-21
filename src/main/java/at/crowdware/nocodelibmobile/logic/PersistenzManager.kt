/****************************************************************************
 * Copyright (C) 2025 CrowdWare
 *
 * This file is part of NoCodeLibMobile.
 *
 *  NoCodeLibMobile is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  NoCodeLibMobile is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with NoCodeLibMobile.  If not, see <http://www.gnu.org/licenses/>.
 *
 ****************************************************************************/
package at.crowdware.nocodelibmobile.logic

import android.content.Context

object PersistanceManager{
    private const val LANGUAGE_CODE_PREF = "language_code_pref"
    private const val APP_PREFS = "app_prefs"

    fun getLanguage(context: Context): String? {
        val preferences = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        return preferences.getString(LANGUAGE_CODE_PREF, "")
    }

    fun setLanguageCode(context: Context, language: String) {
        val preferences = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        preferences.edit().putString(LANGUAGE_CODE_PREF, language).apply()
    }
}
