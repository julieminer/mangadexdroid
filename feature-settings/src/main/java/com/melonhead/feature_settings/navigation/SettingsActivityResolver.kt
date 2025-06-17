package com.melonhead.feature_settings.navigation

import android.content.Context
import android.content.Intent
import com.melonhead.feature_settings.SettingsActivity
import com.melonhead.lib_navigation.keys.ActivityKey
import com.melonhead.lib_navigation.resolvers.ActivityResolver

class SettingsActivityResolver internal constructor():
    ActivityResolver<ActivityKey.SettingsActivity> {
    override fun intentForKey(context: Context, key: ActivityKey.SettingsActivity): Intent {
        return SettingsActivity.newIntent(context)
    }
}
