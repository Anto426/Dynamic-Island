package com.anto426.dynamicisland

import android.app.Application
import androidx.work.Configuration

class DynamicIslandApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
