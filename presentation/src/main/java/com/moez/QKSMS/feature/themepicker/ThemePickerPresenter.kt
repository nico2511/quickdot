package dev.octoshrimpy.quik.feature.themepicker

import com.f2prateek.rx.preferences2.Preference
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import dev.octoshrimpy.quik.common.base.QkPresenter
import dev.octoshrimpy.quik.common.util.Colors
import dev.octoshrimpy.quik.manager.WidgetManager
import dev.octoshrimpy.quik.util.Preferences
import dev.octoshrimpy.quik.util.ProBillingHook
import dev.octoshrimpy.quik.util.accentContrastTooLow
import dev.octoshrimpy.quik.util.isHexAccentLocked
import dev.octoshrimpy.quik.util.isPresetAccentLocked
import dev.octoshrimpy.quik.util.parseAccentHex
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class ThemePickerPresenter @Inject constructor(
    private val prefs: Preferences,
    @Named("recipientId") private val threadId: Long,
    private val colors: Colors,
    private val widgetManager: WidgetManager,
    private val proBillingHook: ProBillingHook
) : QkPresenter<ThemePickerView, ThemePickerState>(ThemePickerState(threadId = threadId)) {

    private val theme: Preference<Int> = prefs.theme()

    override fun bindIntents(view: ThemePickerView) {
        super.bindIntents(view)

        theme.asObservable()
                .autoDisposable(view.scope())
                .subscribe { color -> view.setCurrentTheme(color) }

        prefs.styledThreadId.asObservable()
                .autoDisposable(view.scope())
                .subscribe { styledId ->
                    val styled = threadId != 0L && styledId == threadId
                    newState { copy(styled = styled) }
                    view.showStyled(styled)
                }

        prefs.quickdotPro.asObservable()
                .autoDisposable(view.scope())
                .subscribe {
                    view.showFreeStyleChoice(freeChoice())
                    view.setAccentLocks(showAccentLocks())
                }

        view.themeSelected()
                .autoDisposable(view.scope())
                .subscribe { color -> applyPreset(color) }

        view.lockedAccentClicks()
                .map { }
                .mergeWith(
                    view.hexPickerClicks()
                        .filter { isHexAccentLocked(proBillingHook.enforcementEnabled, proBillingHook.isUnlocked()) }
                        .map { }
                )
                .autoDisposable(view.scope())
                .subscribe { view.requestProPurchase() }

        view.hexInputChanges()
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(view.scope())
                .subscribe { input -> onHexInput(view, input) }

        view.styleConversationClicks()
                .autoDisposable(view.scope())
                .subscribe { markStyledConversation() }

        view.setAccentLocks(showAccentLocks())
    }

    private fun applyPreset(color: Int) {
        if (isPresetAccentLocked(color, proBillingHook.enforcementEnabled, proBillingHook.isUnlocked())) {
            return
        }
        apply(colors.snapAccent(color))
    }

    private fun onHexInput(view: ThemePickerView, input: String) {
        if (isHexAccentLocked(proBillingHook.enforcementEnabled, proBillingHook.isUnlocked())) {
            return
        }
        val parsed = parseAccentHex(input)
        if (parsed == null) {
            if (input.isEmpty()) {
                view.setHexPreview(colors.displayAccent(theme.get()), false)
            }
            return
        }
        val snapped = colors.snapAccent(parsed)
        val lowContrast = accentContrastTooLow(
            colors.displayAccent(snapped),
            colors.themeSurfaceColor()
        )
        view.setHexPreview(snapped, lowContrast)
        if (input.length == 6) {
            apply(snapped)
        }
    }

    private fun apply(color: Int) {
        theme.set(color)
        markStyledConversation()
        widgetManager.updateTheme()
    }

    private fun markStyledConversation() {
        if (threadId != 0L) prefs.styledThreadId.set(threadId)
    }

    private fun freeChoice(): Boolean {
        return proBillingHook.enforcementEnabled && !proBillingHook.isUnlocked()
    }

    private fun showAccentLocks(): Boolean {
        return proBillingHook.enforcementEnabled && !proBillingHook.isUnlocked()
    }

}
