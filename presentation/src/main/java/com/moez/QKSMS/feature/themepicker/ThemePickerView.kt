package dev.octoshrimpy.quik.feature.themepicker

import dev.octoshrimpy.quik.common.base.QkViewContract
import io.reactivex.Observable

interface ThemePickerView : QkViewContract<ThemePickerState> {

    fun themeSelected(): Observable<Int>

    fun styleConversationClicks(): Observable<*>

    fun hexInputChanges(): Observable<String>

    fun hexPickerClicks(): Observable<*>

    fun lockedAccentClicks(): Observable<Int>

    fun setCurrentTheme(color: Int)

    fun setHexField(hexBody: String?, selected: Boolean)

    fun setHexPreview(color: Int, showContrastWarning: Boolean)

    fun setAccentLocks(showLocks: Boolean)

    fun showStyled(styled: Boolean)

    fun showFreeStyleChoice(show: Boolean)

    fun requestProPurchase()

}
