package dev.octoshrimpy.quik.feature.themepicker

import dev.octoshrimpy.quik.common.base.QkViewContract
import io.reactivex.Observable

interface ThemePickerView : QkViewContract<ThemePickerState> {

    fun themeSelected(): Observable<Int>

    fun styleConversationClicks(): Observable<*>

    fun setCurrentTheme(color: Int)

    fun showStyled(styled: Boolean)

}
