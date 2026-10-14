package dev.octoshrimpy.quik.injection

import dagger.Binds
import dagger.Module
import dev.octoshrimpy.quik.billing.FossProBillingClient
import dev.octoshrimpy.quik.billing.FossStyleEnforcement
import dev.octoshrimpy.quik.billing.ProBillingClient
import dev.octoshrimpy.quik.util.StyleEnforcement
import javax.inject.Singleton

@Module
abstract class FlavorModule {
    @Binds
    @Singleton
    abstract fun styleEnforcement(impl: FossStyleEnforcement): StyleEnforcement

    @Binds
    @Singleton
    abstract fun proBillingClient(impl: FossProBillingClient): ProBillingClient
}
