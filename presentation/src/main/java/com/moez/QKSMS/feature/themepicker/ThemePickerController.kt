package dev.octoshrimpy.quik.feature.themepicker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.jakewharton.rxbinding2.view.clicks
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.base.QkController
import dev.octoshrimpy.quik.common.util.Colors
import dev.octoshrimpy.quik.common.util.extensions.setBackgroundTint
import dev.octoshrimpy.quik.common.util.extensions.setTint
import dev.octoshrimpy.quik.databinding.AccentSwatchBinding
import dev.octoshrimpy.quik.databinding.ThemePickerControllerBinding
import dev.octoshrimpy.quik.feature.themepicker.injection.ThemePickerModule
import dev.octoshrimpy.quik.injection.appComponent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class ThemePickerController(
    val threadId: Long = 0L
) : QkController<ThemePickerControllerBinding, ThemePickerView, ThemePickerState, ThemePickerPresenter>(), ThemePickerView {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup): ThemePickerControllerBinding =
        ThemePickerControllerBinding.inflate(inflater, container, false)

    @Inject override lateinit var presenter: ThemePickerPresenter
    @Inject lateinit var colors: Colors

    private val themeSelected: Subject<Int> = PublishSubject.create()

    init {
        appComponent
                .themePickerBuilder()
                .themePickerModule(ThemePickerModule(this))
                .build()
                .inject(this)
    }

    override fun onViewCreated() {
        val inflater = LayoutInflater.from(activity)
        colors.accents.forEach { accent ->
            val swatch = AccentSwatchBinding.inflate(inflater, binding.swatches, false)
            swatch.root.tag = accent.color
            swatch.label.text = activity?.getString(accent.nameRes)
            swatch.swatch.setTint(colors.displayAccent(accent.color))
            swatch.root.setOnClickListener { themeSelected.onNext(accent.color) }
            binding.swatches.addView(swatch.root)
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.title_theme)
        showBackButton(true)
    }

    override fun themeSelected(): Observable<Int> = themeSelected

    override fun styleConversationClicks(): Observable<*> = binding.styleConversation.clicks()

    override fun setCurrentTheme(color: Int) {
        val snapped = colors.snapAccent(color)
        val paint = colors.displayAccent(snapped)
        binding.previewSend.setBackgroundTint(paint)
        binding.previewAvatar.setBackgroundTint(paint)
        binding.styleConversation.setTextColor(paint)
        for (index in 0 until binding.swatches.childCount) {
            val child = binding.swatches.getChildAt(index)
            val selected = child.tag == snapped
            val frame = child.findViewById<View>(R.id.swatchFrame)
            val swatch = colors.displayAccent(child.tag as Int)
            if (selected) frame.setBackgroundTint(swatch) else frame.backgroundTintList = null
            child.findViewById<ImageView>(R.id.swatch).setTint(swatch)
        }
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

}
