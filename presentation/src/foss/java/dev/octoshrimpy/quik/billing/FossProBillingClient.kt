package dev.octoshrimpy.quik.billing

import android.app.Activity
import android.content.Context
import dev.octoshrimpy.quik.R
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FOSS builds ship with style unlocked and no Play Billing classes.
 */
@Singleton
class FossProBillingClient @Inject constructor(
    context: Context
) : ProBillingClient {

    private val state = BehaviorSubject.createDefault(
        ProOfferState(
            price = context.getString(R.string.settings_pro_price),
            summary = context.getString(R.string.settings_pro_summary),
            purchaseLabel = "",
            purchaseEnabled = false,
            actionsVisible = false
        )
    )

    override val states: Observable<ProOfferState> = state.hide()

    override fun start() = Unit

    override fun purchase(activity: Activity) = Unit

    override fun restore() = Unit
}
