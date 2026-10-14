package dev.octoshrimpy.quik.billing

import dev.octoshrimpy.quik.util.StyleEnforcement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FossStyleEnforcement @Inject constructor() : StyleEnforcement {
    override val enabled: Boolean = false
}
