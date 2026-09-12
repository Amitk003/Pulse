package com.pulse.app

import android.app.Activity
import android.content.Context

internal object ArenaLauncher {
    const val arenaPackage = "com.pulse.arena"

    enum class Result {
        LAUNCHED,
        NOT_INSTALLED
    }

    fun launch(context: Context): Result {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(arenaPackage)
            ?: return Result.NOT_INSTALLED

        if (context !is Activity) {
            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launchIntent)
        return Result.LAUNCHED
    }
}
