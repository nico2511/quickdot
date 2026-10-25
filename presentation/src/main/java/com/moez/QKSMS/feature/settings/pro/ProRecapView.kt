package dev.octoshrimpy.quik.feature.settings.pro

import android.app.Activity
import dev.octoshrimpy.quik.billing.ProRecapScreenState
import dev.octoshrimpy.quik.common.base.QkViewContract
import io.reactivex.Observable

interface ProRecapView : QkViewContract<ProRecapScreenState> {
    fun buyClicks(): Observable<Activity>
    fun restoreClicks(): Observable<*>
}
