package dev.octoshrimpy.quik.util

/**
 * Whether a free account is limited to full style on one conversation.
 * The Play flavor turns this on. The FOSS flavor leaves it off.
 */
interface StyleEnforcement {
    val enabled: Boolean
}
