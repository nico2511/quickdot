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

    private val globalTheme: Preference<Int> = prefs.theme()

    private val editingTheme: Preference<Int>
        get() = if (threadId != 0L) prefs.threadTheme(threadId) else globalTheme

    override fun bindIntents(view: ThemePickerView) {
        super.bindIntents(view)

        val themeChanges = if (threadId != 0L) {
            prefs.keyChanges.filter { key ->
                key == "theme" || key == "quickdotPro" || key == "thread_theme_$threadId"
            }.startWith("")
        } else {
            globalTheme.asObservable().map { }
        }

        themeChanges
                .autoDisposable(view.scope())
                .subscribe { refreshThemePreview(view) }

        prefs.quickdotPro.asObservable()
                .autoDisposable(view.scope())
                .subscribe {
                    view.showProNote(showProNote())
                    view.setAccentLocks(showAccentLocks())
                    refreshThemePreview(view)
                }

        view.themeSelected()
                .autoDisposable(view.scope())
                .subscribe { color -> applyPreset(view, color) }

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

        view.resetAccentClicks()
                .autoDisposable(view.scope())
                .subscribe { resetConversationAccent(view) }

        view.setAccentLocks(showAccentLocks())
        view.showProNote(showProNote())
        refreshThemePreview(view)
    }

    private fun refreshThemePreview(view: ThemePickerView) {
        view.setCurrentTheme(currentStoredAccent())
        view.showResetConversationAccent(threadId != 0L && prefs.hasThreadTheme(threadId))
    }

    private fun currentStoredAccent(): Int {
        return when {
            threadId != 0L && prefs.hasThreadTheme(threadId) -> prefs.threadTheme(threadId).get()
            threadId != 0L -> prefs.theme().get()
            else -> globalTheme.get()
        }
    }

    private fun applyPreset(view: ThemePickerView, color: Int) {
        if (isPresetAccentLocked(color, proBillingHook.enforcementEnabled, proBillingHook.isUnlocked())) {
            return
        }
        apply(view, colors.snapAccent(color))
    }

    private fun onHexInput(view: ThemePickerView, input: String) {
        if (isHexAccentLocked(proBillingHook.enforcementEnabled, proBillingHook.isUnlocked())) {
            return
        }
        val parsed = parseAccentHex(input)
        if (parsed == null) {
            if (input.isEmpty()) {
                view.setHexPreview(colors.displayAccent(editingTheme.get()), false)
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
            apply(view, snapped)
        }
    }

    private fun apply(view: ThemePickerView, color: Int) {
        editingTheme.set(color)
        widgetManager.updateTheme()
        refreshThemePreview(view)
    }

    private fun resetConversationAccent(view: ThemePickerView) {
        if (threadId == 0L) return
        prefs.clearThreadTheme(threadId)
        widgetManager.updateTheme()
        refreshThemePreview(view)
    }

    private fun showAccentLocks(): Boolean {
        return proBillingHook.enforcementEnabled && !proBillingHook.isUnlocked()
    }

    private fun showProNote(): Boolean {
        return showAccentLocks()
    }

}
