package dev.octoshrimpy.quik.feature.settings.pro

import android.content.Context
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.billing.ProBillingClient
import dev.octoshrimpy.quik.billing.ProOfferState
import dev.octoshrimpy.quik.billing.ProRecapCopy
import dev.octoshrimpy.quik.billing.ProRecapScreenState
import dev.octoshrimpy.quik.billing.resolveProRecapScreenState
import dev.octoshrimpy.quik.common.base.QkPresenter
import dev.octoshrimpy.quik.extensions.asObservable
import dev.octoshrimpy.quik.util.Preferences
import dev.octoshrimpy.quik.util.ProBillingHook
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

class ProRecapPresenter @Inject constructor(
    private val context: Context,
    private val prefs: Preferences,
    private val proBillingHook: ProBillingHook,
    private val proBillingClient: ProBillingClient,
) : QkPresenter<ProRecapView, ProRecapScreenState>(
    resolveProRecapScreenState(
        proBillingHook.enforcementEnabled,
        proBillingHook.isUnlocked(),
        ProOfferState(
            price = "",
            summary = "",
            purchaseLabel = "",
            purchaseEnabled = false,
            actionsVisible = false,
        ),
        recapCopy(context),
    )
) {

    private val copy = recapCopy(context)

    init {
        val offer = proBillingClient.states
            .startWith(
                ProOfferState(
                    price = context.getString(R.string.settings_pro_price),
                    summary = context.getString(R.string.settings_pro_summary),
                    purchaseLabel = context.getString(R.string.settings_pro_purchase),
                    purchaseEnabled = true,
                    actionsVisible = proBillingHook.enforcementEnabled,
                )
            )

        disposables += Observable.combineLatest(
            prefs.quickdotPro.asObservable().startWith(prefs.quickdotPro.get()),
            offer,
        ) { unlocked, state -> unlocked to state }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { (unlocked, offerState) ->
                newState {
                    resolveProRecapScreenState(
                        proBillingHook.enforcementEnabled,
                        unlocked,
                        offerState,
                        copy,
                    )
                }
            }
    }

    override fun bindIntents(view: ProRecapView) {
        super.bindIntents(view)

        view.buyClicks()
            .autoDisposable(view.scope())
            .subscribe { activity -> proBillingClient.purchase(activity) }

        view.restoreClicks()
            .autoDisposable(view.scope())
            .subscribe { proBillingClient.restore() }
    }

    companion object {
        private fun recapCopy(context: Context): ProRecapCopy = ProRecapCopy(
            fossIncludedTitle = context.getString(R.string.pro_recap_foss_title),
            fossIncludedSummary = context.getString(R.string.pro_recap_foss_summary),
            unlockedTitle = context.getString(R.string.settings_pro_owned),
            unlockedSummary = context.getString(R.string.settings_pro_summary_owned),
            purchaseVerb = context.getString(R.string.settings_pro_purchase),
            restore = context.getString(R.string.settings_pro_restore),
        )
    }
}
