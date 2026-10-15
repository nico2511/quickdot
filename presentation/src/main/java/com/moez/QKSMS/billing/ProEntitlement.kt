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

/**
 * Delay before the next Play connection or purchase query.
 * Attempt 0 waits [baseMs], then doubles, and stops growing at [capMs].
 */
fun proRetryDelayMs(attempt: Int, baseMs: Long = 1_000L, capMs: Long = 30_000L): Long {
    if (baseMs <= 0L) return 0L
    val capped = capMs.coerceAtLeast(baseMs)
    var delay = baseMs
    val steps = attempt.coerceAtLeast(0).coerceAtMost(16)
    repeat(steps) {
        if (delay >= capped / 2) return capped
        delay *= 2
    }
    return delay.coerceAtMost(capped)
}
