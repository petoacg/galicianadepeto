package gal.galiciana.app

import android.app.Instrumentation
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.graphics.Bitmap

/** Explicit, device-only gesture check. Leaves the foreground PWA and its data intact. */
internal object ViewerPinchVerification {
    fun run(instrumentation: Instrumentation, args: Bundle, result: Bundle) {
        val cx = args.getString("cx", "600").toFloat()
        val cy = args.getString("cy", "1000").toFloat()
        val start = args.getString("start", "70").toFloat()
        val end = args.getString("end", "210").toFloat()
        val down = SystemClock.uptimeMillis()
        fun event(action: Int, distance: Float, count: Int) {
            val properties = Array(count) { i -> MotionEvent.PointerProperties().apply {
                id = i; toolType = MotionEvent.TOOL_TYPE_FINGER
            } }
            val coords = Array(count) { i -> MotionEvent.PointerCoords().apply {
                x = cx + if (i == 0) -distance else distance; y = cy; pressure = 1f; size = .1f
            } }
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, count,
                properties, coords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
            try { check(instrumentation.uiAutomation.injectInputEvent(event, true)) }
            finally { event.recycle() }
        }
        fun screenshot(name: String): Bitmap {
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            java.io.File(instrumentation.targetContext.cacheDir, "peto-pinch-$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            return bitmap
        }
        screenshot("before").recycle()
        event(MotionEvent.ACTION_DOWN, start, 1)
        event(MotionEvent.ACTION_POINTER_DOWN or (1 shl 8), start, 2)
        for (i in 1..35) {
            event(MotionEvent.ACTION_MOVE, start + (end - start) * i / 35f, 2)
            SystemClock.sleep(18)
        }
        SystemClock.sleep(150)
        val held = screenshot("held")
        event(MotionEvent.ACTION_POINTER_UP or (1 shl 8), end, 2)
        SystemClock.sleep(120)
        event(MotionEvent.ACTION_UP, end, 1)
        SystemClock.sleep(450)
        val released = screenshot("released")
        var changed = 0; var sampled = 0
        for (y in 600 until minOf(held.height, 2050) step 3) for (x in 10 until held.width - 10 step 3) {
            sampled++; if (held.getPixel(x,y) != released.getPixel(x,y)) changed++
        }
        held.recycle(); released.recycle()
        result.putString("stream", "Pinch injected: centre=$cx,$cy ratio=${end/start}; changed pixels after release=$changed/$sampled\n")
    }
}
