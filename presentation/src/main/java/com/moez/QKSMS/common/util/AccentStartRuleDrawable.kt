/*
 * Copyright (C) 2026 nico2511
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.octoshrimpy.quik.common.util

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Full-height accent bar on the start edge (message selection, quotes, scheduled). */
class AccentStartRuleDrawable(
    private val accent: Int,
    private val widthPx: Int,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }

    override fun draw(canvas: Canvas) {
        if (widthPx <= 0) return
        canvas.drawRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.left + widthPx.toFloat(),
            bounds.bottom.toFloat(),
            paint,
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
