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
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.widget.TextViewCompat
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.Navigator
import dev.octoshrimpy.quik.common.util.Colors
import dev.octoshrimpy.quik.common.util.FontProvider
import dev.octoshrimpy.quik.common.util.extensions.dpToPx
import dev.octoshrimpy.quik.common.util.extensions.getColorCompat
import dev.octoshrimpy.quik.common.util.extensions.setBackgroundTint
import dev.octoshrimpy.quik.common.util.extensions.setTint
import dev.octoshrimpy.quik.databinding.AvatarViewBinding
import dev.octoshrimpy.quik.injection.appComponent
import dev.octoshrimpy.quik.model.Recipient
import dev.octoshrimpy.quik.util.GlideApp
import dev.octoshrimpy.quik.util.Preferences
import javax.inject.Inject

class AvatarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    @Inject lateinit var colors: Colors
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var fontProvider: FontProvider
    @Inject lateinit var prefs: Preferences

    private var lookupKey: String? = null
    private var fullName: String? = null
    private var photoUri: String? = null
    private var lastUpdated: Long? = null
    private var address: String? = null
    private var theme: Colors.Theme
    private var layout: AvatarViewBinding

    private var inflated = false

    var monochrome: Boolean = false
        set(value) {
            field = value
            if (inflated && this::colors.isInitialized) {
                theme = avatarTheme(null)
                updateView()
            }
        }

    /** Thread avatars use the speaker ink for this address instead of a neutral fill. */
    var speakerInk: Boolean = false
        set(value) {
            field = value
            if (inflated && this::colors.isInitialized) {
                theme = avatarTheme(null)
                updateView()
            }
        }

    /**
     * Toolbar chip only. A smaller dark fill and one Doto initial,
     * so the title stays first. Thread rows keep speaker ink.
     */
    var calmHeader: Boolean = false
        set(value) {
            field = value
            if (inflated && this::colors.isInitialized) {
                theme = avatarTheme(null)
                updateView()
            }
        }

    /** Compose and contact rows use a square stamp instead of the inbox circle. */
    var square: Boolean = false
        set(value) {
            field = value
            if (inflated) applyShape()
        }

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
        }

        theme = colors.theme()

        layout = AvatarViewBinding.inflate(LayoutInflater.from(context), this)
        inflated = true
        applyShape()
    }

    private fun applyShape() {
        setBackgroundResource(if (square) R.drawable.rounded_rectangle_4dp else R.drawable.circle)
        clipToOutline = true
    }

    /**
     * Use the contact information to display the avatar.
     */
    fun setRecipient(recipient: Recipient?) {
        lookupKey = recipient?.contact?.lookupKey
        fullName = recipient?.contact?.name
        photoUri = recipient?.contact?.photoUri
        lastUpdated = recipient?.contact?.lastUpdate
        address = recipient?.address ?: address
        theme = avatarTheme(recipient)
        updateView()
    }

    private fun avatarTheme(recipient: Recipient?): Colors.Theme {
        if (calmHeader) {
            return Colors.Theme(context.getColorCompat(R.color.headerAvatar), colors)
        }
        val key = recipient?.address ?: address
        if (speakerInk && !key.isNullOrBlank()) {
            return Colors.Theme(colors.speakerInk(key), colors)
        }
        return if (monochrome) colors.neutral() else colors.theme(recipient)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (!isInEditMode) {
            updateView()
        }
    }

    private fun updateView() {
        // Bold Doto keeps the dots readable inside the circle. Regular weight
        // anti-aliases into a plain sans at these sizes. Pin it so a later
        // setTypeface from bold text or an OEM font hook cannot swap the sans back in.
        if (this::prefs.isInitialized && this::fontProvider.isInitialized && !prefs.systemFont.get()) {
            layout.initial.pinTypeface(fontProvider.displayBold)
        } else if (this::prefs.isInitialized && prefs.systemFont.get()) {
            layout.initial.unpinTypeface()
        }

        if (calmHeader) {
            TextViewCompat.setAutoSizeTextTypeWithDefaults(
                layout.initial,
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
            )
            // One letter in the 24dp chip. 16sp keeps the dots inside the square.
            layout.initial.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            layout.initial.setPadding(0, 0, 0, 0)
            val icon = 18.dpToPx(context)
            layout.icon.layoutParams = layout.icon.layoutParams.apply {
                width = icon
                height = icon
            }
        } else {
            layout.initial.setPadding(0, 0, 0, 0)
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                layout.initial,
                12,
                24,
                1,
                TypedValue.COMPLEX_UNIT_SP
            )
            val icon = 24.dpToPx(context)
            layout.icon.layoutParams = layout.icon.layoutParams.apply {
                width = icon
                height = icon
            }
        }

        // Apply theme
        setBackgroundTint(theme.theme)
        layout.initial.setTextColor(theme.textPrimary)
        layout.icon.setTint(theme.textPrimary)

        val initials = fullName
                ?.substringBefore(',')
                ?.split(" ").orEmpty()
                .filter { name -> name.isNotEmpty() }
                .map { name -> name[0] }
                .filter { initial -> initial.isLetterOrDigit() }
                .map { initial -> initial.toString() }

        if (initials.isNotEmpty()) {
            // The 24dp header fits one letter. Two letters collide.
            layout.initial.text = if (!calmHeader && initials.size > 1) {
                initials.first() + initials.last()
            } else {
                initials.first()
            }
            layout.icon.visibility = GONE
        } else {
            layout.initial.text = null
            layout.icon.visibility = VISIBLE
        }

        layout.photo.setImageDrawable(null)
        photoUri?.let { photoUri ->
            GlideApp.with(layout.photo)
                    .load(photoUri)
                    .into(layout.photo)
        }
    }
}
