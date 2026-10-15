/*
 * Copyright (C) 2026 nico2511
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.common.widget

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.feature.main.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The inbox placeholder is the search pill's hint. After bind it must still
 * carry [HintTypefaceSpan]. The pixels are a pinned text overlay; this checks
 * the CharSequence the framework would otherwise draw.
 */
@RunWith(AndroidJUnit4::class)
class HintSpanAttachedTest {

    @Test
    fun inboxSearchHintKeepsTheTypefaceSpanAfterBind() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent)
        instrumentation.waitForIdleSync()
        val search = activity.findViewById<QkEditText>(R.id.toolbarSearch)
        assertTrue(
            "toolbarSearch hint=${search.hint} class=${search.hint?.javaClass?.name}",
            search.hintHasTypefaceSpan()
        )
    }
}
