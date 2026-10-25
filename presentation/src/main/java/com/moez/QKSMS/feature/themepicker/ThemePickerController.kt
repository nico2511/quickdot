package dev.octoshrimpy.quik.feature.themepicker

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.gridlayout.widget.GridLayout
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.billing.ProBillingClient
import dev.octoshrimpy.quik.common.base.QkController
import dev.octoshrimpy.quik.common.util.Colors
import dev.octoshrimpy.quik.common.util.extensions.setBackgroundTint
import dev.octoshrimpy.quik.common.util.extensions.setTint
import dev.octoshrimpy.quik.databinding.AccentSwatchBinding
import dev.octoshrimpy.quik.databinding.ThemePickerControllerBinding
import dev.octoshrimpy.quik.feature.themepicker.injection.ThemePickerModule
import dev.octoshrimpy.quik.injection.appComponent
import dev.octoshrimpy.quik.util.Preferences
import dev.octoshrimpy.quik.util.accentContrastTooLow
import dev.octoshrimpy.quik.util.formatAccentHex
import dev.octoshrimpy.quik.util.parseAccentHex
import dev.octoshrimpy.quik.util.textColorOnAccent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlin.math.roundToInt
import javax.inject.Inject

class ThemePickerController(
    val threadId: Long = 0L
) : QkController<ThemePickerControllerBinding, ThemePickerView, ThemePickerState, ThemePickerPresenter>(), ThemePickerView {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup): ThemePickerControllerBinding =
        ThemePickerControllerBinding.inflate(inflater, container, false)

    @Inject override lateinit var presenter: ThemePickerPresenter
    @Inject lateinit var colors: Colors
    @Inject lateinit var proBillingClient: ProBillingClient

    private val themeSelected: Subject<Int> = PublishSubject.create()
    private val lockedAccentClicksSubject: Subject<Int> = PublishSubject.create()
    private var accentLocksVisible = false
    private var suppressHexBinding = false

    init {
        appComponent
                .themePickerBuilder()
                .themePickerModule(ThemePickerModule(this))
                .build()
                .inject(this)
    }

    override fun onViewCreated() {
        val inflater = LayoutInflater.from(activity)
        val grid = binding.swatches
        val columnCount = 3
        colors.accents.forEachIndexed { index, accent ->
            val swatch = AccentSwatchBinding.inflate(inflater, grid, false)
            swatch.root.tag = accent.color
            swatch.label.text = activity?.getString(accent.nameRes)
            fitAccentLabel(swatch.label)
            swatch.swatch.setTint(colors.displayAccent(accent.color))
            swatch.lock.isVisible = false
            swatch.root.setOnClickListener {
                if (accentLocksVisible && accent.color != Preferences.DEFAULT_THEME_COLOR) {
                    lockedAccentClicksSubject.onNext(accent.color)
                } else {
                    themeSelected.onNext(accent.color)
                }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % columnCount, 1f)
                rowSpec = GridLayout.spec(index / columnCount)
            }
            grid.addView(swatch.root, params)
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.title_theme)
        showBackButton(true)
    }

    override fun themeSelected(): Observable<Int> = themeSelected

    override fun lockedAccentClicks(): Observable<Int> = lockedAccentClicksSubject

    override fun hexInputChanges(): Observable<String> =
        binding.hexPicker.hexInput.textChanges()
            .filter { !suppressHexBinding }
            .map { it.toString() }

    override fun hexPickerClicks(): Observable<*> = Observable.merge(
        binding.hexPicker.hexPreviewFrame.clicks(),
        binding.hexPicker.hexInput.clicks()
    )

    override fun styleConversationClicks(): Observable<*> = binding.styleConversation.clicks()

    override fun requestProPurchase() {
        activity?.let { proBillingClient.purchase(it) }
    }

    override fun setAccentLocks(showLocks: Boolean) {
        accentLocksVisible = showLocks
        for (index in 0 until binding.swatches.childCount) {
            val child = binding.swatches.getChildAt(index)
            val color = child.tag as Int
            val lock = child.findViewById<ImageView>(R.id.lock)
            lock.isVisible = showLocks && color != Preferences.DEFAULT_THEME_COLOR
        }
        binding.hexPicker.hexLock.isVisible = showLocks
        binding.hexPicker.hexInput.isEnabled = !showLocks
        binding.hexPicker.hexInput.alpha = if (showLocks) 0.5f else 1f
    }

    override fun setCurrentTheme(color: Int) {
        val snapped = colors.snapAccent(color)
        val paint = colors.displayAccent(snapped)
        binding.previewSend.setBackgroundTint(paint)
        binding.previewSend.setTint(textColorOnAccent(paint))
        binding.previewAvatar.setBackgroundTint(paint)
        binding.styleConversation.setTextColor(paint)

        val preset = colors.isPresetAccent(snapped)
        for (index in 0 until binding.swatches.childCount) {
            val child = binding.swatches.getChildAt(index)
            val selected = preset && child.tag == snapped
            val frame = child.findViewById<View>(R.id.swatchFrame)
            val swatch = colors.displayAccent(child.tag as Int)
            if (selected) frame.setBackgroundTint(swatch) else frame.backgroundTintList = null
            child.findViewById<ImageView>(R.id.swatch).setTint(swatch)
        }

        val lowContrast = accentContrastTooLow(paint, colors.themeSurfaceColor())
        if (preset) {
            setHexField(null, selected = false)
            setHexPreview(snapped, lowContrast)
        } else {
            setHexField(formatAccentHex(snapped), selected = true)
            setHexPreview(snapped, lowContrast)
        }
    }

    override fun setHexField(hexBody: String?, selected: Boolean) {
        suppressHexBinding = true
        binding.hexPicker.hexInput.setText(hexBody ?: "")
        suppressHexBinding = false
        if (selected) {
            val parsed = hexBody?.let { parseAccentHex(it) } ?: return
            binding.hexPicker.hexPreviewFrame.setBackgroundTint(colors.displayAccent(parsed))
        } else {
            binding.hexPicker.hexPreviewFrame.backgroundTintList = null
        }
    }

    override fun setHexPreview(color: Int, showContrastWarning: Boolean) {
        val paint = colors.displayAccent(color)
        binding.hexPicker.hexPreview.setBackgroundTint(paint)
        binding.hexPicker.hexContrastWarning.isVisible = showContrastWarning
    }

    override fun showStyled(styled: Boolean) {
        binding.styleConversation.setText(
            if (styled) R.string.theme_style_conversation_done
            else R.string.theme_style_conversation
        )
    }

    override fun showFreeStyleChoice(show: Boolean) {
        binding.styleConversation.isVisible = show && threadId != 0L
        binding.proNote.isVisible = show
    }

    override fun render(state: ThemePickerState) = Unit

    /**
     * TextStamp sets a fixed size, which turns autosize off. GRAPHITE is wider
     * than the swatch at that size once letter spacing is added, and a large
     * text preference makes every name wider. Shrink to fit one line. Do not
     * ellipsize: a cut name is the bug this avoids.
     */
    private fun fitAccentLabel(label: TextView) {
        val maxSp = (label.textSize / label.resources.displayMetrics.scaledDensity)
            .roundToInt()
            .coerceAtLeast(8)
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            label,
            8,
            maxSp,
            1,
            TypedValue.COMPLEX_UNIT_SP
        )
    }

}
