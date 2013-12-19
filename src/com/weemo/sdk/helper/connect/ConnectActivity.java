package com.weemo.sdk.helper.connect;

import org.acra.ACRA;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;

import com.weemo.sdk.Weemo;
import com.weemo.sdk.WeemoEngine;
import com.weemo.sdk.event.WeemoEventListener;
import com.weemo.sdk.event.global.AuthenticatedEvent;
import com.weemo.sdk.event.global.ConnectedEvent;
import com.weemo.sdk.helper.R;
import com.weemo.sdk.helper.contacts.ContactsActivity;
import com.weemo.sdk.helper.fragment.ChooseFragment;
import com.weemo.sdk.helper.fragment.ErrorFragment;
import com.weemo.sdk.helper.fragment.InputFragment;
import com.weemo.sdk.helper.fragment.LoadingDialogFragment;
import com.weemo.sdk.helper.fragment.ChooseFragment.ChooseListener;
import com.weemo.sdk.helper.fragment.InputFragment.InputListener;
import com.weemo.sdk.helper.util.ReportException;
import com.weemo.sdk.helper.util.UIUtils;

/*
 * This is the first activity being launched.
 * Its role is to handle connection and authentication of the user.
 */
public class ConnectActivity extends Activity implements InputListener, ChooseListener {

	private boolean hasLoggedIn = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		UIUtils.forceOverflowMenu(this);
		
		String appId = getString(R.string.weemo_appId);
		
		// Checks if Weemo is already initialized and authenticated.
		// If it is, it is probably because the user clicked on the service notification.
		// In which case, the user is redirected to the second screen
		WeemoEngine weemo = Weemo.instance();
		if (weemo != null && weemo.isAuthenticated()) {
			hasLoggedIn = true;
			startActivity(
				new Intent(this, ContactsActivity.class)
					.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
				);
			finish();
			return ;
		}
		
		// This activity starts with a LoadingFragment (which makes the user wait while Weemo is connecting)
		// The connection is started in onStart, after registering the listener
		// (so that the ConnectedEvent can't be launched while we are not yet listening).
		if (savedInstanceState == null) {
			InputFragment.newInstance(appId).show(getFragmentManager(), "dialog");
		}
		
		// Register the activity as event listener
		Weemo.eventBus().register(this);

		// Initialize Weemo, can be called multiple times
	}
	
	@Override
	protected void onDestroy() {
		// Unregister as event listener
		Weemo.eventBus().unregister(this);

		// If this activity is destroyed with hasLoggedIn is true,
		// this means it is destroyed after CallActivity being displayed.
		// However, if hasLoggedIn is false, it means that the activity is destroyed
		// because the user has got out of the application without logging in.
		// In this case we need to stop the Weemo engine.
		if (!hasLoggedIn)
			Weemo.disconnect();
		
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
	 * This is called by the InputFragment when user clicks on "OK" button
	 */
	@Override
	public void onInput(String appId) {
		if (appId.isEmpty() || appId.contains(" "))
			return ;

		DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag("dialog");
		if (dialog != null)
			dialog.dismiss();

		LoadingDialogFragment loadingFragment = LoadingDialogFragment.newFragmentInstance(getString(R.string.connection_title), getString(R.string.connection_text));
		loadingFragment.setCancelable(false);
		loadingFragment.show(getFragmentManager(), "dialog");

		Weemo.initialize(appId, this);
	}
	
	/*
	 * This is called by the ChoseFragment when user clicks on "login" button
	 */
	@Override
	public void onChoose(String userId) {
		WeemoEngine weemo = Weemo.instance();
		// Weemo must be instanciated at this point
		if (weemo == null)
			throw new NullPointerException("onChoose called while while Weemo is not initialized");

		LoadingDialogFragment dialog = LoadingDialogFragment.newFragmentInstance(userId, getString(R.string.authentication_title));
		dialog.setCancelable(false);
		dialog.show(getFragmentManager(), "dialog");

		// Start authentication with the userId chosen by the user.
		weemo.authenticate(userId, WeemoEngine.UserType.INTERNAL);
	}

	/*
	 * This listener catches ConnectedEvent
	 * 1. It is annotated with @WeemoEventListener
	 * 2. It takes one argument which type is ConnectedEvent
	 * 3. It's activity object has been registered with Weemo.getEventBus().register(this) in onStart()
	 * */
	@WeemoEventListener
	public void onConnected(ConnectedEvent e) {
		ConnectedEvent.Error error = e.getError();

		// Stop the loading dialog
		DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag("dialog");
		if (dialog != null)
			dialog.dismiss();

		// If there is an error, this means that connection failed
		// So we display the English description of the error
		if (error != null) {
			getFragmentManager().beginTransaction().replace(android.R.id.content, ErrorFragment.newInstance(error.description())).commit();
			return ;
		}
		
		// If there is no error, everything went normal, connection succeeded.
		
		ChooseFragment chooseFragment = ChooseFragment.newInstance(getString(R.string.log_in));
		if (getResources().getBoolean(R.bool.isTablet))
			chooseFragment.show(getFragmentManager(), "dialog");
		else
			getFragmentManager().beginTransaction().replace(android.R.id.content, ChooseFragment.newInstance(getString(R.string.log_in))).commit();
	}

	/*
	 * This listener catches AuthenticatedEvent
	 * 1. It is annotated with @WeemoEventListener
	 * 2. It takes one argument which type is AuthenticatedEvent
	 * 3. It's activity object has been registered with Weemo.getEventBus().register(this) in onStart()
	 */
	@WeemoEventListener
	public void onAuthenticated(AuthenticatedEvent e) {
		AuthenticatedEvent.Error error = e.getError();
		
		// If there is an error, this means that authentication failed
		// So we display the English description of the error
		// We then go back to the login fragment so that authentication can be tried again
		if (error != null) {
			DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag("dialog");
			if (dialog != null)
				dialog.dismiss();

			if (error == AuthenticatedEvent.Error.BAD_APIKEY) {
				getFragmentManager().beginTransaction().replace(android.R.id.content, ErrorFragment.newInstance(error.description())).commit();
				return ;
			}

			Toast.makeText(this, error.description(), Toast.LENGTH_LONG).show();

			return ;
		}
		
		// If there is no error, everything went normal, go to call activity
		hasLoggedIn = true;
		startActivity(new Intent(this, ContactsActivity.class));
		finish();
	}

}
