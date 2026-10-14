package dev.octoshrimpy.quik.billing

import dev.octoshrimpy.quik.util.StyleEnforcement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayStyleEnforcement @Inject constructor() : StyleEnforcement {
    override val enabled: Boolean = true
}
