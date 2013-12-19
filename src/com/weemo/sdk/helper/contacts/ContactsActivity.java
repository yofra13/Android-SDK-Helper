package com.weemo.sdk.helper.contacts;

import org.acra.ACRA;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.Toast;

import com.weemo.sdk.Weemo;
import com.weemo.sdk.WeemoCall;
import com.weemo.sdk.WeemoCall.CallStatus;
import com.weemo.sdk.WeemoEngine;
import com.weemo.sdk.event.WeemoEventListener;
import com.weemo.sdk.event.call.CallStatusChangedEvent;
import com.weemo.sdk.event.global.CanCreateCallChangedEvent;
import com.weemo.sdk.helper.DemoAccounts;
import com.weemo.sdk.helper.R;
import com.weemo.sdk.helper.call.CallActivity;
import com.weemo.sdk.helper.call.CallFragment;
import com.weemo.sdk.helper.connect.ConnectedService;
import com.weemo.sdk.helper.fragment.ChooseFragment;
import com.weemo.sdk.helper.fragment.ChooseFragment.ChooseListener;
import com.weemo.sdk.helper.util.ReportException;
import com.weemo.sdk.helper.util.UIUtils;

/*
 * This is the main activity when the user is connected.
 * From this activity, the user can:
 *  - Poll for the status of a remote contact
 *  - Call a remote contact.
 *
 * When a call starts, this activity will :
 *  - Start the CallActivity if we are using a phone or a small tablet
 *  - Display the CallFragment and handle the call itself if we are using a 10 inch or bigger tablet
 *
 * It is also that activity that starts and stops the ConnectedService.
 * This service will run as long as the user is connected. 
 */
public class ContactsActivity extends Activity implements ChooseListener {

	private static final String LOGTAG = "ContactsActivity";
	
	private boolean checkedMode = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_contacts);
		
		UIUtils.forceOverflowMenu(this);

		if (savedInstanceState == null) {
			// Ensure that Weemo is initialized
			WeemoEngine weemo = Weemo.instance();
			if (weemo == null) {
				Log.e(LOGTAG, "ContactsActivity was started while Weemo is not initialized");
				stopService(new Intent(this, ConnectedService.class));
				return ;
			}
			
			// Ensure that there is a connected user
			String uid = weemo.getUserId();
			if (uid == null) {
				Log.e(LOGTAG, "ContactsActivity was started while Weemo is not authenticated");
				finish();
				return ;
			}

			// If a display name is not set, that getDisplayName will return null
			// In which case we need to ask the user for its display name
			String displayName = weemo.getDisplayName();
			boolean askForDisplayName = displayName.isEmpty();
			
			// If display name was not set, we display a temporary one that is the user ID
			if (askForDisplayName)
				displayName = uid;
			
			// Display the contact fragment
			getFragmentManager()
				.beginTransaction()
				.add(R.id.contact_list, ChooseFragment.newInstance(getResources().getString(checkedMode ? R.string.check : R.string.call), displayName))
				.commit();
		
			// If we need to ask for the display name, shows the apropriate popup
			if (askForDisplayName) {
				String defaultName = "";
				if (DemoAccounts.ACCOUNTS.containsKey(uid))
					defaultName = DemoAccounts.ACCOUNTS.get(uid);
	            AskDisplayNameDialogFragment.newInstance(defaultName).show(getFragmentManager(), null);
			}
			
			// If there is a call currently going on,
			// it's probably because the user has clicked in the notification after going on its device home.
			// In which case we redirect him to the CallActivity
			WeemoCall call = weemo.getCurrentCall();
			if (call != null)
				startCallWindow(call.getCallId());
			else if (getIntent().getBooleanExtra("pickup", false)) {
				int callId = getIntent().getIntExtra("callId", -1);
				if (callId != -1)
					startCallWindow(callId);
			}
			else
				// This activity can only be launched if the user is connected
				// Therefore, we launch the ConnectedService
				startService(new Intent(this, ConnectedService.class));
		}
		
		setTitleFromDisplayName();

		// Register as event listener
		Weemo.eventBus().register(this);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		WeemoEngine weemo = Weemo.instance();
		if (weemo == null) {
			finish();
			return ;
		}

		// If there is a call currently going on,
		// it's probably because the user has clicked in the notification after going on its device home.
		// In which case we redirect him to the CallActivity
		WeemoCall call = weemo.getCurrentCall();
		if (call != null)
			startCallWindow(call.getCallId());
		if (intent.getBooleanExtra("pickup", false)) {
			int callId = intent.getIntExtra("callId", -1);
			if (callId != -1)
				startCallWindow(callId);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();

		WeemoEngine weemo = Weemo.instance();
		assert weemo != null;

		if (weemo.getCurrentCall() == null)
			// Enables or disable the call button according to weemo.canCreateCall()
			((ChooseFragment) getFragmentManager().findFragmentById(R.id.contact_list)).setEnabled(weemo.canCreateCall());

		// If we were in background mode, we got back to foreground
		if (weemo.isInBackground())
			weemo.goToForeground();
	}

	@Override
	protected void onStop() {
		WeemoEngine weemo = Weemo.instance();

		// If there is no call going on, then we go to background when this activity stops,
		// which allows the Weemo engine to save battery
		if (weemo != null && weemo.getCurrentCall() == null)
			weemo.goToBackground();

		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		// Unregister as event listener
		Weemo.eventBus().unregister(this);

		super.onDestroy();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("checkedMode", checkedMode);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		checkedMode = savedInstanceState.getBoolean("checkedMode", false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(R.string.disconnect)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override public boolean onMenuItemClick(MenuItem item) {
					Weemo.disconnect();
					return true;
				}
			})
		;

		menu.add(R.string.checked_mode)
			.setCheckable(true)
			.setChecked(checkedMode)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override public boolean onMenuItemClick(MenuItem item) {
					checkedMode = !item.isChecked();
					item.setChecked(checkedMode);
					String text = getResources().getString(item.isChecked() ? R.string.check : R.string.call);
					((ChooseFragment) getFragmentManager().findFragmentById(R.id.contact_list)).setButtonText(text);
					return true;
				}
			})
		;
		
		menu.add(R.string.core_dump)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override public boolean onMenuItemClick(MenuItem item) {
					ACRA.getErrorReporter().handleSilentException(new ReportException());
					return true;
				}
			})
		;
		
		return super.onCreateOptionsMenu(menu);
	}
	
	/*
	 * When the user has chosen someone to check, starts the dialog fragment that will make the check
	 */
	@Override
	public void onChoose(String contactId) {
		if (checkedMode)
			ContactCheckDialogFragment.newInstance(contactId).show(getFragmentManager(), null);
		else {
			WeemoEngine weemo = Weemo.instance();
			assert weemo != null;
			weemo.createCall(contactId);
		}
	}

	/*
	 * Set the activity title according to the display name
	 * If there is no display name, then we use the user ID
	 */
	private void setTitleFromDisplayName() {
		WeemoEngine weemo = Weemo.instance();
		assert weemo != null;
		String displayName = weemo.getDisplayName();
		if (!displayName.isEmpty())
			setTitle(displayName);
		else {
			String uid = weemo.getUserId();
			assert uid != null;
			setTitle("{" + uid + "}");
		}
	}

	/*
	 * Displays the call window.
	 * If we are using a 10 inch or bigger tablet, shows the Call fragment in this activity
	 * Otherwise, shows the activity that displays the call window fullscreen.
	 */
	void startCallWindow(int callId) {
		View display = findViewById(R.id.contact_display);
		((ChooseFragment)(getFragmentManager().findFragmentById(R.id.contact_list))).setEnabled(false);
		if (display != null) {
			getFragmentManager()
				.beginTransaction()
				.replace(R.id.contact_display, CallFragment.newInstance(callId, false))
				.commit();
		}
		else
			startActivity(
				new Intent(this, CallActivity.class)
					.putExtra("callId", callId)
			);
	}
	
	/*
	 * Set the display name
	 */
	public void setDisplayName(String displayName) {
		WeemoEngine weemo = Weemo.instance();
		assert weemo != null;
		weemo.setDisplayName(displayName);

		// Tell the service the user has changed display name, so the service can update the notification
		startService(new Intent(this, ConnectedService.class));
		setTitleFromDisplayName();
	}

	/*
	 * This listener method catches CanCreateCallChangedEvent
	 * 1. It is annotated with @WeemoEventListener
	 * 2. It takes one argument which type is CanCreateCallChangedEvent
	 * 3. It's activity object has been registered with Weemo.getEventBus().register(this) in onStart()
	 */
	@WeemoEventListener
	public void onCanCreateCallChanged(CanCreateCallChangedEvent e) {
		CanCreateCallChangedEvent.Error error = e.getError();

		// If there is an error, the action we'll take depends on the error
		if (error != null) {
			switch (error) {
			// This is a loss of network. We cannot create call, but the network should be back anytime soon.
			// We disable the call button.
			case NETWORK_LOST:
				((ChooseFragment) getFragmentManager().findFragmentById(R.id.contact_list)).setEnabled(false);
				break;
				
			// This is either a system error or the Weemo engine was destroyed (by the user when he clicks disconnect)
			// We print the error message and finish the activity, the service (and the application along)
			case SIP_NOK:
			case CLOSED:
				Toast.makeText(this, error.description(), Toast.LENGTH_LONG).show();
				stopService(new Intent(ContactsActivity.this, ConnectedService.class));
				finish();
				break;
			}
			return ;
		}

		WeemoEngine weemo = Weemo.instance();
		assert weemo != null;
		if (weemo.getCurrentCall() == null)
			// If there is no error, we can create call
			// We enable the call button.
			((ChooseFragment) getFragmentManager().findFragmentById(R.id.contact_list)).setEnabled(true);
	}
	
	/*
	 * This listener method catches CallStatusChangedEvent
	 * 1. It is annotated with @WeemoEventListener
	 * 2. It takes one argument which type is CallStatusChangedEvent
	 * 3. It's activity object has been registered with Weemo.getEventBus().register(this) in onStart()
	 */
	@WeemoEventListener
	public void onCallStatusChanged(CallStatusChangedEvent e) {
		// If there's a call whose status is newly to PROCEEDING, this means the user has initiated an outgoing call
		// and that this call is currently ringing on the remote user's device.
		// In which case, we show the ContactCallingDialogFragment that will monitor the babysteps of this newborn call
		if (e.getCallStatus() == CallStatus.PROCEEDING) {
			ContactCallingDialogFragment dialog = ContactCallingDialogFragment.newInstance(e.getCall().getCallId());
			dialog.setCancelable(false);
			dialog.show(getFragmentManager(), null);
		}
		
		// If a call has ended and we are in tablet mode, we need to remove the call window fragment.
		if (e.getCallStatus() == CallStatus.ENDED) {
			((ChooseFragment)(getFragmentManager().findFragmentById(R.id.contact_list))).setEnabled(true);
			View display = findViewById(R.id.contact_display);
			if (display != null) {
				Fragment fragment = getFragmentManager().findFragmentById(R.id.contact_display);
				if (fragment != null)
				getFragmentManager().beginTransaction().remove(fragment)
				.commit();
			}
		}
	}
	
	/*
	 * We only allow going back if there are no calls going on.
	 * That way, if we are in a tablet, the call fragment will never be destroyed during a call.
	 * Note that it could, but destroying the call fragment would stop the video out.
	 */
	@Override
	public void onBackPressed() {
		WeemoEngine weemo = Weemo.instance();
		assert weemo != null;
		if (weemo.getCurrentCall() == null)
			super.onBackPressed();
	}
}
