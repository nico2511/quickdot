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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.Gravity
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.TextView
import dev.octoshrimpy.quik.BuildConfig
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
     * The placeholder is a separate [QkTextView], not this paint.
     */
    private var pinnedTypeface: Typeface? = null
    private var hintFace: Typeface? = null
    private var filledFace: Typeface? = null
    private var filledStyle: Int = Typeface.NORMAL
    private var pinFilled: Boolean = false
    private var restingTextSizePx: Float = 0f
    private var ensuringHint = false
    private var frameworkHintHidden = false
    private var loggedHint = false
    private var loggedDraw = false
    private var hintInk: Int = Color.GRAY

    /**
     * Drawn while the field is empty. Its paint is the pinned Doto face, the
     * same path as screen titles. The EditText hint layout is not used for
     * pixels: Nothing OS still paints that layout with [getPaint], which stays
     * Geist so typed text stays readable.
     */
    private var hintOverlay: QkTextView? = null

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
            textViewStyler.applyAttributes(this, attrs)
        } else {
            TextViewStyler.applyEditModeAttributes(this, attrs)
        }
    }

    /**
     * [emptyFace] is the placeholder. [filledFace] stays on this view, so typed
     * text keeps Geist (or Doto, when the field was already Doto).
     * [TextView.setHint](android.widget.TextView) is final, so a later plain
     * hint is wrapped again from [onMeasure]. The wrap keeps [HintTypefaceSpan]
     * on the CharSequence; the pixels come from [hintOverlay].
     */
    fun bindFaces(emptyFace: Typeface, filledFace: Typeface, filledStyle: Int, pinFilled: Boolean) {
        hintFace = emptyFace
        this.filledFace = filledFace
        this.filledStyle = filledStyle
        this.pinFilled = pinFilled
        hintOverlay?.pinTypeface(emptyFace)
        ensureHintSpan()
        applyTypedFace()
        logHintSpan("bind")
    }

    /** True when the hint CharSequence still carries [HintTypefaceSpan] after bind. */
    fun hintHasTypefaceSpan(): Boolean {
        val current = hint ?: return false
        if (current.isEmpty() || current !is Spanned) return false
        return current.getSpans(0, current.length, HintTypefaceSpan::class.java).isNotEmpty()
    }

    /** Preference size, captured after [TextViewStyler] sets it. Typed text uses this. */
    fun noteRestingTextSize() {
        restingTextSizePx = textSize
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
     * Stock Android skips [BoringLayout] when the hint has a
     * [MetricAffectingSpan][android.text.style.MetricAffectingSpan] and builds a
     * StaticLayout that applies the span. BoringLayout paints [getPaint] and
     * ignores the span. The inbox field's paint is Geist. Nothing OS still
     * takes that boring path for this hint, so the span stays on the
     * CharSequence and a pinned [QkTextView] draws the placeholder.
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
     * Single-line placeholders ellipsize. Doto bold is wider than Geist, so a
     * long French placeholder is drawn smaller until it fits. The floor is
     * 12sp. Typed text keeps [restingTextSizePx] on this view.
     */
    private fun overlayTextSizePx(label: String): Float {
        val resting = if (restingTextSizePx > 0f) restingTextSizePx else textSize
        if (maxLines != 1) return resting
        val safety = 2f * resources.displayMetrics.density
        val available = (width - compoundPaddingLeft - compoundPaddingRight).toFloat() - safety
        if (available <= 0f) return resting
        val measure = TextPaint()
        measure.typeface = hintFace
        measure.isFakeBoldText = false
        measure.textSize = resting
        val hintWidth = measure.measureText(label)
        val floor = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            12f,
            resources.displayMetrics
        )
        return fittedHintSizePx(resting, hintWidth, available, floor)
    }

    private fun hideFrameworkHint() {
        if (frameworkHintHidden) return
        val ink = currentHintTextColor
        if (Color.alpha(ink) != 0) hintInk = ink
        frameworkHintHidden = true
        setHintTextColor(Color.TRANSPARENT)
    }

    private fun ensureHintOverlay(): QkTextView? {
        val face = hintFace ?: return null
        hintOverlay?.let { return it }
        if (isInEditMode) return null
        val created = QkTextView(context).apply {
            // Never attached. setText() still calls checkForRelayout(), which
            // reads LayoutParams.width. A null params object crashes the draw.
            // WRAP_CONTENT takes the dynamic-width branch (null the layout and
            // return) instead of the fixed-width branch that needs a parent.
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            background = null
            includeFontPadding = this@QkEditText.includeFontPadding
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            isFocusable = false
            isClickable = false
            pinTypeface(face)
        }
        hintOverlay = created
        return created
    }

    /**
     * Pixels come from a [StaticLayout] whose paint holds [hintFace].
     * [TextView.setTypeface] runs [Typeface.create] when font weight adjustment
     * is non-zero and can swap that face for the system sans. Assigning the
     * loaded instance skips that. The pinned [QkTextView] is still measured so
     * the same face titles use is the one drawn here.
     *
     * BoringLayout, which this field's hint uses, paints [getPaint] and ignores
     * [HintTypefaceSpan]. [getPaint] stays the typed-text face.
     */
    private fun drawPinnedHint(canvas: Canvas) {
        val label = hint?.toString()?.takeIf { it.isNotEmpty() } ?: return
        if (width <= 0 || height <= 0) return
        val overlay = ensureHintOverlay() ?: return
        val face = hintFace ?: return
        if (overlay.typeface != face) overlay.pinTypeface(face)
        val pinned = overlay.typeface ?: face
        val drawFace = if (pinned == face) pinned else face
        val sizePx = overlayTextSizePx(label)
        val padLeft = compoundPaddingLeft
        val padTop = compoundPaddingTop
        val padRight = compoundPaddingRight
        val padBottom = compoundPaddingBottom
        val textWidth = (width - padLeft - padRight).coerceAtLeast(0)
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
        paint.typeface = drawFace
        paint.isFakeBoldText = false
        paint.color = hintInk
        paint.textSize = sizePx
        val lines = if (maxLines > 0) maxLines else Integer.MAX_VALUE
        val layout = StaticLayout.Builder.obtain(label, 0, label.length, paint, textWidth)
            .setAlignment(hintAlignment())
            .setIncludePad(includeFontPadding)
            .setMaxLines(lines)
            .setEllipsize(if (maxLines == 1) TextUtils.TruncateAt.END else null)
            .build()
        val vspace = height - padTop - padBottom
        var top = padTop.toFloat()
        val vertical = gravity and Gravity.VERTICAL_GRAVITY_MASK
        if (vertical == Gravity.CENTER_VERTICAL) {
            top += ((vspace - layout.height) / 2f).coerceAtLeast(0f)
        } else if (vertical == Gravity.BOTTOM) {
            top += (vspace - layout.height).toFloat().coerceAtLeast(0f)
        }
        canvas.save()
        canvas.translate(padLeft.toFloat(), top)
        layout.draw(canvas)
        canvas.restore()
        if (!loggedDraw) {
            loggedDraw = true
            val name = try {
                resources.getResourceEntryName(id)
            } catch (e: Exception) {
                id.toString()
            }
            Log.i(
                HINT_LOG,
                "stage=draw id=$name paintPinned=${drawFace == face} " +
                    "viewPinned=${overlay.typeface == face} layoutW=${layout.width} " +
                    "lineW=${layout.getLineWidth(0).toInt()} labelLen=${label.length}"
            )
        }
    }

    private fun hintAlignment(): Layout.Alignment {
        val horizontal = Gravity.getAbsoluteGravity(gravity, layoutDirection) and
            Gravity.HORIZONTAL_GRAVITY_MASK
        return when (horizontal) {
            Gravity.CENTER_HORIZONTAL -> Layout.Alignment.ALIGN_CENTER
            Gravity.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
    }

    private fun logHintSpan(stage: String) {
        if (loggedHint && stage != "bind") return
        if (stage != "bind") loggedHint = true
        val current = hint
        val count = if (current is Spanned) {
            current.getSpans(0, current.length, HintTypefaceSpan::class.java).size
        } else {
            0
        }
        val layoutName = try {
            val field = TextView::class.java.getDeclaredField("mHintLayout")
            field.isAccessible = true
            field.get(this)?.javaClass?.name ?: "null"
        } catch (e: Exception) {
            "unavailable:${e.javaClass.simpleName}"
        }
        val name = try {
            resources.getResourceEntryName(id)
        } catch (e: Exception) {
            id.toString()
        }
        Log.i(
            HINT_LOG,
            "stage=$stage id=$name spans=$count hintClass=${current?.javaClass?.simpleName} " +
                "hintLayout=$layoutName overlay=${hintOverlay?.typeface == hintFace}"
        )
        if (BuildConfig.DEBUG && stage == "bind" && !current.isNullOrEmpty() && count == 0) {
            error("QkEditText $name hint is missing HintTypefaceSpan after bind")
        }
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

    override fun onDraw(canvas: Canvas) {
        val showOverlay = hintFace != null && !isInEditMode && text.isNullOrEmpty()
        if (showOverlay) {
            ensureHintSpan()
            hideFrameworkHint()
        }
        super.onDraw(canvas)
        // After the framework hint. BoringLayout paints the typed-text face on
        // top of anything drawn earlier, so the pinned placeholder goes last.
        if (showOverlay) drawPinnedHint(canvas)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (hintFace != null) {
            ensureHintSpan()
            logHintSpan("layout")
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
        private const val HINT_LOG = "QkHint"

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