/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.common.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import dev.octoshrimpy.quik.common.util.TextViewStyler
import dev.octoshrimpy.quik.injection.appComponent
import java.lang.reflect.Field
import javax.inject.Inject

open class QkTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    @Inject lateinit var textViewStyler: TextViewStyler

    /**
     * Thread titles and avatar initials pin Doto bold. AppCompat's
     * setTypeface(face, style) and the framework bold-text adjustment both
     * rebuild the face with Typeface.create. On Android 14 that drops a custom
     * face whose family name collided with another weight and draws the system
     * sans instead. A pinned face is put back.
     */
    private var pinnedTypeface: Typeface? = null

    /**
     * Collapse a multiline list of strings into a single line
     *
     * Ex.
     *
     * Toronto, New York, Los Angeles,
     * Seattle, Portland
     *
     * Will be converted to
     *
     * Toronto, New York, Los Angeles, +2
     */
    var collapseEnabled: Boolean = false

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
            textViewStyler.applyAttributes(this, attrs)
        } else {
            TextViewStyler.applyEditModeAttributes(this, attrs)
        }
    }

    fun pinTypeface(typeface: Typeface?) {
        pinnedTypeface = typeface
        if (typeface != null) applyFace(typeface) else super.setTypeface(null)
    }

    fun unpinTypeface() {
        pinnedTypeface = null
    }

    override fun setTypeface(tf: Typeface?) {
        val pin = pinnedTypeface
        if (pin != null) {
            applyFace(pin)
            return
        }
        super.setTypeface(tf)
    }

    override fun setTypeface(tf: Typeface?, style: Int) {
        val pin = pinnedTypeface
        if (pin != null) {
            applyFace(pin)
            return
        }
        super.setTypeface(tf, style)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pinnedTypeface?.let { applyFace(it) }
    }

    private fun applyFace(typeface: Typeface) {
        val field = fontWeightAdjustmentField
        val saved = field?.let {
            try {
                it.getInt(this)
            } catch (e: Exception) {
                null
            }
        }
        if (field != null && saved != null && saved != 0) {
            try {
                field.setInt(this, 0)
            } catch (e: Exception) {
                // Hidden-API block: the family still has the bold file.
            }
        }
        try {
            super.setTypeface(typeface)
        } finally {
            if (field != null && saved != null && saved != 0) {
                try {
                    field.setInt(this, saved)
                } catch (e: Exception) {
                    // Restoring the adjustment is best-effort.
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (collapseEnabled) {
            layout
                    ?.takeIf { layout -> layout.lineCount > 0 }
                    ?.let { layout -> layout.getEllipsisCount(layout.lineCount - 1) }
                    ?.takeIf { ellipsisCount -> ellipsisCount > 0 }
                    ?.let { ellipsisCount -> text.dropLast(ellipsisCount).lastIndexOf(',') }
                    ?.takeIf { lastComma -> lastComma >= 0 }
                    ?.let { lastComma ->
                        val remainingNames = text.drop(lastComma).count { c -> c == ',' }
                        text = "${text.take(lastComma)}, +$remainingNames"
                    }
        }
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)
        setLinkTextColor(color)
    }

    private companion object {
        val fontWeightAdjustmentField: Field? = try {
            TextView::class.java.getDeclaredField("mFontWeightAdjustment").apply {
                isAccessible = true
            }
        } catch (e: Exception) {
            null
        }
    }

}