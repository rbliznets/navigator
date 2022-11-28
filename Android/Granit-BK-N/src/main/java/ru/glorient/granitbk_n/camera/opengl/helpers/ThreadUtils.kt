package ru.glorient.granitbk_n.camera.opengl.helpers

import android.os.Handler
import android.os.Looper

object ThreadUtils {
    private val handler = Handler(Looper.getMainLooper())

    fun runOnUI(delay: Long = 0L, action: () -> Unit) {
        if (delay == 0L && Looper.myLooper() === Looper.getMainLooper()) action.invoke()
        else handler.postDelayed(action, delay)
    }
}