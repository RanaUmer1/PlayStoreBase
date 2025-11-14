package com.professor.playstorebaseproject.utils

import android.os.SystemClock
import android.view.View
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.reflect.Type

/**

Created by Umer Javed
Senior Android Developer
Created on 12/06/2025 12:47 pm
Email: umerr8019@gmail.com

 */


fun mainCoroutine(work: suspend (() -> Unit)): Job {
    return CoroutineScope(Dispatchers.Main).launch {
        work()
    }
}


fun ioCoroutine(work: suspend (() -> Unit)): Job {
    return CoroutineScope(Dispatchers.IO).launch {
        work()
    }
}


inline fun <reified T> Gson.fromJsonWithType(json: String): T {
    val type: Type = object : TypeToken<T>() {}.type
    return this.fromJson(json, type)
}


inline fun <reified T> List<LinkedTreeMap<String, Any>>.toTypedList(): List<T> {
    val gson = Gson()
    return this.map { mapItem ->
        val json = gson.toJson(mapItem) // Convert map to JSON string
        gson.fromJson(json, T::class.java) // Convert JSON to object
    }
}


fun <T> Gson.fromJsonList(json: String, type: Type): List<T> {
    return this.fromJson(json, type)
}


fun View.setClickWithTimeout(
    timeoutMillis: Long = 100L,
    onClick: (View) -> Unit
) {
    var lastClickTime = 0L

    setOnClickListener { view ->
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime < timeoutMillis) {
            return@setOnClickListener
        }
        lastClickTime = currentTime
        onClick(view)
    }
}




