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
package dev.octoshrimpy.quik.feature.settings.about

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bluelinelabs.conductor.archlifecycle.LifecycleController
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.util.BundledFontLicenses
import dev.octoshrimpy.quik.common.widget.PreferenceView
import dev.octoshrimpy.quik.databinding.OpenSourceLicensesControllerBinding

class OpenSourceLicensesController : LifecycleController() {

    private var binding: OpenSourceLicensesControllerBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val vb = OpenSourceLicensesControllerBinding.inflate(inflater, container, false)
        binding = vb
        val inflaterLocal = LayoutInflater.from(activity)
        BundledFontLicenses.entries.forEach { entry ->
            val row = inflaterLocal.inflate(R.layout.preference_view, vb.licenses, false) as PreferenceView
            row.title = entry.title
            row.summary = entry.summary
            row.setOnClickListener { showLicense(entry.licenseAssetPath) }
            vb.licenses.addView(row)
        }
        return vb.root
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        activity?.title = activity?.getString(R.string.about_open_source_licenses_title)
        view.findViewById<TextView>(R.id.toolbarTitle)?.text =
            activity?.getString(R.string.about_open_source_licenses_title)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onDestroyView(view: View) {
        binding = null
        super.onDestroyView(view)
    }

    private fun showLicense(assetPath: String) {
        val context = activity ?: return
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        AlertDialog.Builder(context, R.style.QuickdotAlertDialog)
            .setMessage(text)
            .setPositiveButton(R.string.button_continue, null)
            .show()
    }
}
