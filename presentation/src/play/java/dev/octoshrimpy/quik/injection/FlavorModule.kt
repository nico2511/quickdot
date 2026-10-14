package dev.octoshrimpy.quik.injection

import dagger.Binds
import dagger.Module
import dev.octoshrimpy.quik.billing.PlayProBillingClient
import dev.octoshrimpy.quik.billing.PlayStyleEnforcement
import dev.octoshrimpy.quik.billing.ProBillingClient
import dev.octoshrimpy.quik.util.StyleEnforcement
import javax.inject.Singleton

@Module
abstract class FlavorModule {
    @Binds
    @Singleton
    abstract fun styleEnforcement(impl: PlayStyleEnforcement): StyleEnforcement

    @Binds
    @Singleton
    abstract fun proBillingClient(impl: PlayProBillingClient): ProBillingClient
}
