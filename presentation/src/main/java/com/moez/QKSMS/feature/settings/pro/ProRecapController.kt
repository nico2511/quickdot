package dev.octoshrimpy.quik.feature.settings.pro

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.gridlayout.widget.GridLayout
import com.jakewharton.rxbinding2.view.clicks
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.billing.ProRecapScreenState
import dev.octoshrimpy.quik.common.base.QkController
import dev.octoshrimpy.quik.common.util.Colors
import dev.octoshrimpy.quik.common.util.extensions.dpToPx
import dev.octoshrimpy.quik.common.util.extensions.setBackgroundTint
import dev.octoshrimpy.quik.databinding.ProRecapControllerBinding
import dev.octoshrimpy.quik.databinding.ProRecapFeatureRowBinding
import dev.octoshrimpy.quik.databinding.ProRecapPreviewAccentBinding
import dev.octoshrimpy.quik.databinding.ProRecapPreviewConversationBinding
import dev.octoshrimpy.quik.databinding.ProRecapPreviewHexBinding
import dev.octoshrimpy.quik.databinding.ProRecapPreviewQuoteBinding
import dev.octoshrimpy.quik.injection.appComponent
import dev.octoshrimpy.quik.util.textColorOnAccent
import io.reactivex.Observable
import javax.inject.Inject

class ProRecapController : QkController<ProRecapControllerBinding, ProRecapView, ProRecapScreenState, ProRecapPresenter>(),
    ProRecapView {

    @Inject override lateinit var presenter: ProRecapPresenter
    @Inject lateinit var colors: Colors

    init {
        appComponent.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup): ProRecapControllerBinding =
        ProRecapControllerBinding.inflate(inflater, container, false)

    override fun onViewCreated() {
        bindFeatureRow(
            binding.featureAccent,
            R.string.pro_recap_feature_accent_title,
            R.string.pro_recap_feature_accent_summary,
        ) { parent ->
            ProRecapPreviewAccentBinding.inflate(LayoutInflater.from(parent.context), parent, true)
            parent
        }
        bindFeatureRow(
            binding.featurePalette,
            R.string.pro_recap_feature_palette_title,
            R.string.pro_recap_feature_palette_summary,
        ) { parent ->
            LayoutInflater.from(parent.context).inflate(R.layout.pro_recap_preview_palette, parent, true)
            parent
        }
        bindFeatureRow(
            binding.featureHex,
            R.string.pro_recap_feature_hex_title,
            R.string.pro_recap_feature_hex_summary,
        ) { parent ->
            ProRecapPreviewHexBinding.inflate(LayoutInflater.from(parent.context), parent, true)
            parent
        }
        bindFeatureRow(
            binding.featureConversation,
            R.string.pro_recap_feature_conversation_title,
            R.string.pro_recap_feature_conversation_summary,
        ) { parent ->
            ProRecapPreviewConversationBinding.inflate(LayoutInflater.from(parent.context), parent, true)
            parent
        }
        bindFeatureRow(
            binding.featureQuote,
            R.string.pro_recap_feature_quote_title,
            R.string.pro_recap_feature_quote_summary,
        ) { parent ->
            ProRecapPreviewQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, true)
            parent
        }
        paintPreviews()
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.pro_recap_title)
        showBackButton(true)
    }

    override fun buyClicks(): Observable<Activity> = binding.proPurchase.clicks().map { activity!! }

    override fun restoreClicks(): Observable<*> = binding.proRestore.clicks()

    override fun render(state: ProRecapScreenState) {
        val accent = colors.accentColor()
        binding.statusTitle.text = state.statusTitle
        binding.statusSummary.text = state.statusSummary
        binding.unlockedBadge.isVisible = state.showUnlockedBadge
        if (state.showUnlockedBadge) {
            binding.unlockedBadge.setBackgroundTint(accent)
        }
        binding.proActions.isVisible = state.actionsVisible
        binding.proPurchase.isVisible = state.actionsVisible
        binding.proRestore.isVisible = state.actionsVisible && state.restoreVisible
        if (state.buyLabel.isNotEmpty()) binding.proPurchase.text = state.buyLabel
        binding.proPurchase.isEnabled = state.buyEnabled
        binding.proPurchase.alpha = if (state.buyEnabled) 1f else 0.45f
        binding.proPurchase.setBackgroundTint(accent)
        binding.proPurchase.setTextColor(textColorOnAccent(accent))
    }

    private fun bindFeatureRow(
        row: ProRecapFeatureRowBinding,
        titleId: Int,
        summaryId: Int,
        previewInflater: (ViewGroup) -> ViewGroup,
    ) {
        row.featureTitle.setText(titleId)
        row.featureSummary.setText(summaryId)
        previewInflater(row.featurePreview)
    }

    private fun paintPreviews() {
        val accent = colors.accentColor()
        val cobalt = colors.displayAccent(colors.accents[1].color)
        val pine = colors.displayAccent(colors.accents[2].color)

        binding.featureAccent.featurePreview.findViewById<View>(R.id.previewBubble)?.setBackgroundTint(accent)
        binding.featureAccent.featurePreview.findViewById<View>(R.id.previewSend)?.setBackgroundTint(accent)

        val grid = binding.featurePalette.featurePreview.findViewById<GridLayout>(R.id.paletteGrid)
        if (grid != null && grid.childCount == 0) {
            val size = 20.dpToPx(grid.context)
            val margin = 4.dpToPx(grid.context)
            colors.accents.forEachIndexed { index, item ->
                val dot = View(grid.context).apply {
                    setBackgroundResource(R.drawable.rounded_rectangle_4dp)
                    setBackgroundTint(colors.displayAccent(item.color))
                }
                val params = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    columnSpec = GridLayout.spec(index % 6)
                    rowSpec = GridLayout.spec(index / 6)
                    setMargins(margin, margin, margin, margin)
                }
                grid.addView(dot, params)
            }
        }

        binding.featureHex.featurePreview.findViewById<View>(R.id.hexSwatch)?.setBackgroundTint(cobalt)
        binding.featureConversation.featurePreview.findViewById<View>(R.id.avatarA)?.setBackgroundTint(accent)
        binding.featureConversation.featurePreview.findViewById<View>(R.id.avatarB)?.setBackgroundTint(pine)
        binding.featureConversation.featurePreview.findViewById<View>(R.id.bubbleB)?.setBackgroundTint(pine)
    }
}
