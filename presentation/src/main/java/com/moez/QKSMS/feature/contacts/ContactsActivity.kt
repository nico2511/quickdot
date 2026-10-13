/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
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
package dev.octoshrimpy.quik.feature.contacts

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import com.jakewharton.rxbinding2.widget.editorActions
import com.jakewharton.rxbinding2.widget.textChanges
import dagger.android.AndroidInjection
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.ExternalNavigator
import dev.octoshrimpy.quik.common.Navigator
import dev.octoshrimpy.quik.common.ViewModelFactory
import dev.octoshrimpy.quik.common.base.QkThemedActivity
import dev.octoshrimpy.quik.common.util.extensions.hideKeyboard
import dev.octoshrimpy.quik.common.util.extensions.setBackgroundTint
import dev.octoshrimpy.quik.common.util.extensions.showKeyboard
import dev.octoshrimpy.quik.common.widget.QkDialog
import dev.octoshrimpy.quik.databinding.ContactsActivityBinding
import dev.octoshrimpy.quik.extensions.Optional
import dev.octoshrimpy.quik.feature.compose.editing.ChipsAdapter
import dev.octoshrimpy.quik.feature.compose.editing.ComposeItem
import dev.octoshrimpy.quik.feature.compose.editing.ComposeItemAdapter
import dev.octoshrimpy.quik.feature.compose.editing.PhoneNumberAction
import dev.octoshrimpy.quik.feature.compose.editing.PhoneNumberPickerAdapter
import dev.octoshrimpy.quik.manager.PermissionManager
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class ContactsActivity : QkThemedActivity(), ContactsContract {

    companion object {
        const val SHARING_KEY = "sharing"
        const val CHIPS_KEY = "chips"
    }

    private lateinit var binding: ContactsActivityBinding

    @Inject lateinit var contactsAdapter: ComposeItemAdapter
    @Inject lateinit var chipsAdapter: ChipsAdapter
    @Inject lateinit var phoneNumberAdapter: PhoneNumberPickerAdapter
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var externalNavigator: ExternalNavigator
    @Inject lateinit var permissionManager: PermissionManager

    override val queryChangedIntent: Observable<CharSequence> by lazy { binding.search.textChanges() }
    override val queryClearedIntent: Observable<*> by lazy { binding.cancel.clicks() }
    override val queryEditorActionIntent: Observable<Int> by lazy { binding.search.editorActions() }
    override val composeItemPressedIntent: Subject<ComposeItem> by lazy { contactsAdapter.clicks }
    override val composeItemLongPressedIntent: Subject<ComposeItem> by lazy { contactsAdapter.longClicks }
    override val phoneNumberSelectedIntent: Subject<Optional<Long>> by lazy { phoneNumberAdapter.selectedItemChanges }
    override val phoneNumberActionIntent: Subject<PhoneNumberAction> = PublishSubject.create()
    override val contactsPermissionChangedIntent: Subject<Boolean> = PublishSubject.create()
    override val requestContactsPermissionIntent: Observable<*> by lazy { binding.permissionButton.clicks() }

    private val contactsPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactsPermissionChangedIntent.onNext(true)
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
            externalNavigator.showPermissionsInSettings()
        }
    }

    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory)[ContactsViewModel::class.java] }

    private val phoneNumberDialog by lazy {
        QkDialog(this).apply {
            titleRes = R.string.compose_number_picker_title
            adapter = phoneNumberAdapter
            positiveButton = R.string.compose_number_picker_always
            positiveButtonListener = { phoneNumberActionIntent.onNext(PhoneNumberAction.ALWAYS) }
            negativeButton = R.string.compose_number_picker_once
            negativeButtonListener = { phoneNumberActionIntent.onNext(PhoneNumberAction.JUST_ONCE) }
            cancelListener = { phoneNumberActionIntent.onNext(PhoneNumberAction.CANCEL) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        binding = ContactsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showBackButton(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        viewModel.bindView(this)

        theme.autoDisposable(scope()).subscribe {
            binding.permissionButton.setBackgroundTint(it.theme)
        }

        chipsAdapter.interactive = false
        binding.chips.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.chips.adapter = chipsAdapter
        binding.contacts.adapter = contactsAdapter

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigator.showMainActivity()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onResume() {
        super.onResume()
        contactsPermissionChangedIntent.onNext(permissionManager.hasContacts())
    }

    override fun render(state: ContactsState) {
        binding.cancel.isVisible = state.query.length > 1

        contactsAdapter.data = state.composeItems
        chipsAdapter.data = state.selectedRecipients
        binding.chips.isVisible = state.selectedRecipients.isNotEmpty()

        val contactRows = state.composeItems.any { item ->
            item is ComposeItem.Person || item is ComposeItem.Starred || item is ComposeItem.Group
        }
        val showPermission = state.contactsLoaded && !state.hasContactsPermission && !contactRows
        val showEmpty = state.contactsLoaded && state.hasContactsPermission && state.composeItems.isEmpty()
        binding.permission.isVisible = showPermission
        binding.empty.isVisible = showEmpty
        binding.emptyTitle.setText(
                if (state.query.isBlank()) R.string.contacts_empty_title else R.string.contacts_search_empty_title
        )
        binding.emptyMessage.setText(
                if (state.query.isBlank()) R.string.contacts_empty_message else R.string.contacts_search_empty_message
        )
        binding.contacts.isVisible = state.composeItems.isNotEmpty()

        if (state.selectedContact != null && !phoneNumberDialog.isShowing) {
            phoneNumberAdapter.data = state.selectedContact.numbers
            phoneNumberDialog.subtitle = state.selectedContact.name
            phoneNumberDialog.show()
        } else if (state.selectedContact == null && phoneNumberDialog.isShowing) {
            phoneNumberDialog.dismiss()
        }
    }

    override fun clearQuery() {
        binding.search.text = null
    }

    override fun requestContactsPermission() {
        if (permissionManager.hasContacts()) {
            contactsPermissionChangedIntent.onNext(true)
            return
        }
        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    override fun openKeyboard() {
        binding.search.postDelayed({
            binding.search.showKeyboard()
        }, 200)
    }

    override fun finish(result: HashMap<String, String?>) {
        binding.search.hideKeyboard()
        val intent = Intent().putExtra(CHIPS_KEY, result)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

}
