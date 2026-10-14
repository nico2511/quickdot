package dev.octoshrimpy.quik.billing

/**
 * What a Play purchases query means for the offline cache.
 * A query that did not succeed must not clear a saved unlock.
 */
enum class ProEntitlement {
    GRANT,
    PENDING,
    REVOKE,
    KEEP
}

fun decideProEntitlement(
    querySucceeded: Boolean,
    purchased: Boolean,
    pending: Boolean
): ProEntitlement {
    if (!querySucceeded) return ProEntitlement.KEEP
    if (purchased) return ProEntitlement.GRANT
    if (pending) return ProEntitlement.PENDING
    return ProEntitlement.REVOKE
}
