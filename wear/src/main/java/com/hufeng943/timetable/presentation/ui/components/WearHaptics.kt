package com.hufeng943.timetable.presentation.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import java.lang.reflect.Method
import java.lang.reflect.Modifier as JavaModifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Haptic adapter for Wear OS, including China-ROM Xiaomi watches.
 *
 * Xiaomi Watch 5 exposes scroll constants from com.xiaomi.miwear.input.WearHapticFeedbackConstants,
 * while global Wear OS builds may expose the Google wear-sdk class. We resolve either API lazily
 * instead of linking it directly, so devices that do not ship either shared library cannot crash
 * class loading. Normal Android haptic constants remain the final fallback.
 */
@Composable
fun rememberWearHaptics(): WearHaptics {
    val view = LocalView.current
    return remember(view) { WearHaptics(view) }
}

class WearHaptics internal constructor(private val view: View) {
    fun toggle() = performCompat("getScrollItemFocus", HapticFeedbackConstants.CONTEXT_CLICK)
    fun tick() = performCompat("getScrollTick", HapticFeedbackConstants.CLOCK_TICK)
    fun confirm() { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }

    private fun performCompat(methodName: String, fallback: Int) {
        val constant = WearHapticCompat.get(methodName) ?: fallback
        view.performHapticFeedback(constant)
    }
}

private object WearHapticCompat {
    private const val GOOGLE = "com.google.wear.input.WearHapticFeedbackConstants"
    private const val XIAOMI = "com.xiaomi.miwear.input.WearHapticFeedbackConstants"
    private const val UNAVAILABLE = Int.MIN_VALUE
    private val cache = ConcurrentHashMap<String, Int>()

    fun get(methodName: String): Int? {
        val cached = cache[methodName]
        if (cached != null) return cached.takeUnless { it == UNAVAILABLE }
        val resolved = resolve(GOOGLE, methodName) ?: resolve(XIAOMI, methodName)
        cache[methodName] = resolved ?: UNAVAILABLE
        return resolved
    }

    private fun resolve(className: String, methodName: String): Int? = runCatching {
        val clazz = Class.forName(className, true, WearHapticCompat::class.java.classLoader)
        val method: Method = clazz.getMethod(methodName)
        if (!JavaModifier.isStatic(method.modifiers) || method.parameterTypes.isNotEmpty() || method.returnType != Int::class.javaPrimitiveType) {
            return@runCatching null
        }
        method.invoke(null) as Int
    }.getOrNull()
}
