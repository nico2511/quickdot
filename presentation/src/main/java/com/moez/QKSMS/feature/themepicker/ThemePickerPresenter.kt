package dev.octoshrimpy.quik.feature.themepicker

import com.f2prateek.rx.preferences2.Preference
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import dev.octoshrimpy.quik.common.base.QkPresenter
import dev.octoshrimpy.quik.common.util.Colors
import dev.octoshrimpy.quik.manager.WidgetManager
import dev.octoshrimpy.quik.util.Preferences
import dev.octoshrimpy.quik.util.ProBillingHook
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
                .subscribe { color -> view.setCurrentTheme(colors.snapAccent(color)) }

        prefs.styledThreadId.asObservable()
                .autoDisposable(view.scope())
                .subscribe { styledId ->
                    val styled = threadId != 0L && styledId == threadId
                    newState { copy(styled = styled) }
                    view.showStyled(styled)
                }

        prefs.quickdotPro.asObservable()
                .autoDisposable(view.scope())
                .subscribe { view.showFreeStyleChoice(freeChoice()) }

        view.themeSelected()
                .autoDisposable(view.scope())
                .subscribe { color -> apply(colors.snapAccent(color)) }

        view.styleConversationClicks()
                .autoDisposable(view.scope())
                .subscribe { markStyledConversation() }
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

}
