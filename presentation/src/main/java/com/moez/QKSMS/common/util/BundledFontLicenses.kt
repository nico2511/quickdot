/*
 * Copyright (C) 2026 nico2511
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.octoshrimpy.quik.common.util

/**
 * OFL typefaces bundled under [res/font][dev.octoshrimpy.quik.R.font] with full
 * license text in assets/licenses/.
 */
object BundledFontLicenses {

    data class Entry(
        val title: String,
        val summary: String,
        val licenseAssetPath: String,
    )

    val entries: List<Entry> = listOf(
        Entry(
            title = "Geist",
            summary = "Vercel — SIL Open Font License 1.1",
            licenseAssetPath = "licenses/geist-OFL.txt",
        ),
        Entry(
            title = "Doto",
            summary = "Oliver Lalan — SIL Open Font License 1.1",
            licenseAssetPath = "licenses/doto-OFL.txt",
        ),
        Entry(
            title = "Space Mono",
            summary = "Colophon Foundry — SIL Open Font License 1.1",
            licenseAssetPath = "licenses/space-mono-OFL.txt",
        ),
    )
}
