package com.anto426.dynamicisland.plugins

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

open class PluginPermission(
	var name: String,
	var description: String,
	var requestIntent: Intent,
	val granted: MutableState<Boolean> = mutableStateOf(false),
) {
	open fun checkPermission(context: Context) : Boolean { return false }
}