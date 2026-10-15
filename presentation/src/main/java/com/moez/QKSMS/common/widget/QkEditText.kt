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
import android.os.Build
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.google.android.mms.ContentType
import dev.octoshrimpy.quik.common.util.TextViewStyler
import dev.octoshrimpy.quik.common.util.fittedHintSizePx
import dev.octoshrimpy.quik.injection.appComponent
import dev.octoshrimpy.quik.util.tryOrNull
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.lang.reflect.Field
import javax.inject.Inject
import kotlin.math.abs


/**
 * Custom implementation of EditText to allow for dynamic text colors
 *
 * Beware of updating to extend AppCompatTextView, as this inexplicably breaks the view in
 * the contacts chip view
 */
class QkEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : AppCompatEditText(context, attrs) {

    @Inject lateinit var textViewStyler: TextViewStyler

    val backspaces: Subject<Unit> = PublishSubject.create()
    val inputContentSelected: Subject<InputContentInfoCompat> = PublishSubject.create()
    var supportsInputContent: Boolean = false

    /**
     * Same pin as [QkTextView], used only when the typed text itself is Doto.
     * The hint does not use this. Android 14's [Typeface.create] drops a custom
     * face, and the hint is a separate layout built from the text paint.
     */
    private var pinnedTypeface: Typeface? = null
    private var hintFace: Typeface? = null
    private var filledFace: Typeface? = null
    private var filledStyle: Int = Typeface.NORMAL
    private var pinFilled: Boolean = false
    private var restingTextSizePx: Float = 0f
    private var facesBound = false
    private var ensuringHint = false

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
            textViewStyler.applyAttributes(this, attrs)
        } else {
            TextViewStyler.applyEditModeAttributes(this, attrs)
        }
    }

    /**
     * [emptyFace] is drawn only on the placeholder, via [HintTypefaceSpan].
     * [filledFace] stays on the view, so typed text keeps Inter (or Doto, when
     * the field was already Doto). [TextView.setHint](android.widget.TextView)
     * is final, so a later plain hint is wrapped again from [onMeasure].
     */
    fun bindFaces(emptyFace: Typeface, filledFace: Typeface, filledStyle: Int, pinFilled: Boolean) {
        hintFace = emptyFace
        this.filledFace = filledFace
        this.filledStyle = filledStyle
        this.pinFilled = pinFilled
        if (!facesBound) {
            facesBound = true
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (text.isNullOrEmpty()) {
                        if (width > 0) fitHint()
                    } else {
                        restoreRestingSize()
                    }
                }
            })
        }
        ensureHintSpan()
        applyTypedFace()
    }

    /** Preference size, captured after [TextViewStyler] sets it. Typed text uses this. */
    fun noteRestingTextSize() {
        restingTextSizePx = textSize
        if (text.isNullOrEmpty() && width > 0) fitHint()
    }

    private fun applyTypedFace() {
        if (pinFilled) {
            pinTypeface(filledFace)
        } else {
            unpinTypeface()
            setTypeface(filledFace, filledStyle)
        }
    }

    /**
     * BoringLayout skips a hint that carries a [MetricAffectingSpan][android.text.style.MetricAffectingSpan]
     * and the framework builds a StaticLayout instead, which applies the span
     * while measuring and drawing. That is the layout the placeholder uses.
     */
    private fun ensureHintSpan() {
        if (ensuringHint) return
        val face = hintFace ?: return
        val current = hint ?: return
        val existing = (current as? Spanned)?.getSpans(0, current.length, HintTypefaceSpan::class.java)
        if (existing != null && existing.any { it.typeface == face }) return
        val raw = current.toString()
        if (raw.isEmpty()) return
        val spanned = SpannableString(raw)
        spanned.setSpan(HintTypefaceSpan(face), 0, raw.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        ensuringHint = true
        try {
            setHint(spanned)
        } finally {
            ensuringHint = false
        }
    }

    /**
     * Single-line hints ellipsize. Doto bold is wider than the Inter hint it
     * replaces, so a long French placeholder is drawn smaller until it fits.
     * The floor is 12sp, the app's small secondary size. Typed text is restored
     * to the preference size.
     */
    private fun fitHint() {
        if (maxLines != 1) return
        val hintText = hint?.toString()?.takeIf { it.isNotEmpty() } ?: return
        val safety = 2f * resources.displayMetrics.density
        val available = hintAvailablePx() - safety
        if (available <= 0f) return
        val resting = if (restingTextSizePx > 0f) restingTextSizePx else textSize
        // The view paint is the typed-text face. Measure the placeholder with Doto
        // or a long French hint is treated as if it already fits.
        val measure = TextPaint(paint)
        measure.typeface = hintFace ?: paint.typeface
        measure.isFakeBoldText = false
        measure.textSize = resting
        val hintWidth = measure.measureText(hintText)
        val floor = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            12f,
            resources.displayMetrics
        )
        val fitted = fittedHintSizePx(resting, hintWidth, available, floor)
        if (abs(textSize - fitted) > 0.5f) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, fitted)
        }
    }

    private fun restoreRestingSize() {
        if (restingTextSizePx > 0f && abs(textSize - restingTextSizePx) > 0.5f) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, restingTextSizePx)
        }
    }

    private fun hintAvailablePx(): Float {
        val drawables = compoundDrawablesRelative
        val start = drawables[0]
        val end = drawables[2]
        val startW = start?.let { drawable ->
            drawable.bounds.width().takeIf { it > 0 } ?: drawable.intrinsicWidth
        } ?: 0
        val endW = end?.let { drawable ->
            drawable.bounds.width().takeIf { it > 0 } ?: drawable.intrinsicWidth
        } ?: 0
        val startGap = if (startW > 0) compoundDrawablePadding else 0
        val endGap = if (endW > 0) compoundDrawablePadding else 0
        return (width - paddingLeft - paddingRight - startW - endW - startGap - endGap).toFloat()
    }

    fun pinTypeface(typeface: Typeface?) {
        pinnedTypeface = typeface
        if (typeface != null) applyPinned(typeface) else super.setTypeface(null)
    }

    fun unpinTypeface() {
        pinnedTypeface = null
    }

    override fun setTypeface(tf: Typeface?) {
        val pin = pinnedTypeface
        if (pin != null) {
            applyPinned(pin)
            return
        }
        super.setTypeface(tf)
    }

    override fun setTypeface(tf: Typeface?, style: Int) {
        val pin = pinnedTypeface
        if (pin != null) {
            applyPinned(pin)
            return
        }
        super.setTypeface(tf, style)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // XML and later setHint() calls store a plain string. Wrap it before the
        // hint layout is built, or the first measure draws the system sans.
        ensureHintSpan()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (hintFace != null) {
            ensureHintSpan()
            applyTypedFace()
        } else {
            pinnedTypeface?.let { applyPinned(it) }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (hintFace != null && text.isNullOrEmpty()) {
            ensureHintSpan()
            fitHint()
        }
    }

    private fun applyPinned(typeface: Typeface) {
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

    private companion object {
        val fontWeightAdjustmentField: Field? = try {
            TextView::class.java.getDeclaredField("mFontWeightAdjustment").apply {
                isAccessible = true
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {

        val inputConnection = object : InputConnectionWrapper(super.onCreateInputConnection(editorInfo), true) {
            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                    backspaces.onNext(Unit)
                }
                return super.sendKeyEvent(event)
            }


            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength == 1 && afterLength == 0) {
                    backspaces.onNext(Unit)
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }
        }

        if (supportsInputContent) {
            EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf(
                    ContentType.IMAGE_JPEG,
                    ContentType.IMAGE_JPG,
                    ContentType.IMAGE_PNG,
                    ContentType.IMAGE_GIF))
        }

        val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
            val grantReadPermission = flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && grantReadPermission) {
                return@OnCommitContentListener tryOrNull {
                    inputContentInfo.requestPermission()
                    inputContentSelected.onNext(inputContentInfo)
                    true
                } ?: false

            }

            true
        }

        return InputConnectionCompat.createWrapper(inputConnection, editorInfo, callback)
    }

}