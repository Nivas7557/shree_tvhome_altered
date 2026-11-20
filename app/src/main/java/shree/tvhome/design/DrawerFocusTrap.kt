package shree.tvhome.design

import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import shree.tvhome.TopNavBar

object DrawerFocusTrap {

    fun handleKeyEvent(
        event: KeyEvent,
        isDrawerOpen: Boolean,
        drawerLayout: ViewGroup,
        topNavBar: TopNavBar,
        parentNavTag: String
    ): Boolean {
        if (!isDrawerOpen) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val currentFocus = drawerLayout.findFocus()
        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT
            else -> return false
        }

        if (currentFocus is SeekBar && (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT)) {
            return false
        }

        if (currentFocus != null) {
            val next = FocusFinder.getInstance().findNextFocus(drawerLayout, currentFocus, direction)
            if (next != null) {
                next.requestFocus()
                return true
            }
        }

        if (direction == View.FOCUS_LEFT) {
            val navBtn = topNavBar.findViewWithTag<View>(parentNavTag)
            if (navBtn != null) {
                navBtn.requestFocus()
                return true
            }
        }

        val fallback = FocusFinder.getInstance().findNextFocus(drawerLayout, null, View.FOCUS_DOWN)
            ?: FocusFinder.getInstance().findNextFocus(drawerLayout, null, View.FOCUS_FORWARD)
        fallback?.requestFocus()
        return true
    }
}
