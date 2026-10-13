/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
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
package dev.octoshrimpy.quik.feature.compose.editing

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.RelativeLayout
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.util.extensions.resolveThemeColor
import dev.octoshrimpy.quik.common.util.extensions.setBackgroundTint
import dev.octoshrimpy.quik.common.util.extensions.setTint
import dev.octoshrimpy.quik.databinding.ContactChipDetailedBinding
import dev.octoshrimpy.quik.model.Recipient

class DetailedChipView(context: Context) : RelativeLayout(context) {

    private val binding: ContactChipDetailedBinding

    init {
        binding = ContactChipDetailedBinding.inflate(LayoutInflater.from(context), this, true)

        setOnClickListener { hide() }

        visibility = View.GONE

        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setRecipient(recipient: Recipient) {
        binding.avatar.square = true
        binding.avatar.speakerInk = true
        binding.avatar.setRecipient(recipient)
        binding.name.text = recipient.contact?.name?.takeIf { it.isNotBlank() } ?: recipient.address
        binding.info.text = recipient.address

        binding.card.setBackgroundTint(context.resolveThemeColor(R.attr.bubbleColor))
        binding.name.setTextColor(context.resolveThemeColor(android.R.attr.textColorPrimary))
        binding.info.setTextColor(context.resolveThemeColor(android.R.attr.textColorTertiary))
        binding.delete.setTint(context.resolveThemeColor(android.R.attr.textColorSecondary))
    }

    fun show() {
        startAnimation(AlphaAnimation(0f, 1f).apply { duration = 200 })

        visibility = View.VISIBLE
        requestFocus()
        isClickable = true
    }

    fun hide() {
        startAnimation(AlphaAnimation(1f, 0f).apply { duration = 200 })

        visibility = View.GONE
        clearFocus()
        isClickable = false
    }

    fun setOnDeleteListener(listener: (View) -> Unit) {
        binding.delete.setOnClickListener(listener)
    }

}
