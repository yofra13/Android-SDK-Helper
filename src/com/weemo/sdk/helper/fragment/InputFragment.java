package com.weemo.sdk.helper.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.weemo.sdk.helper.R;

public class InputFragment extends DialogFragment {

	public static InputFragment newInstance(String text) {
		InputFragment fragment = new InputFragment();
		Bundle args = new Bundle();
		args.putString("text", text);
		fragment.setArguments(args);
		return fragment;
	}

	public static interface InputListener {
		public void onInput(String text);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.weemo_appid);
		final View root = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_input, null);
		final EditText input = (EditText) root.findViewById(R.id.input);
		input.setText(getArguments().getString("text"));
		input.selectAll();
		builder.setView(root);

		builder.setPositiveButton(android.R.string.ok, null);

		final AlertDialog dialog = builder.create();

		// We use this instead of setPositiveButtonlistener to be able to prevent dismissal
		dialog.setOnShowListener(new OnShowListener() {
			@Override public void onShow(final DialogInterface dialogInterface) {
				final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
				button.setOnClickListener(new OnClickListener() {
					@Override public void onClick(View view) {
						String txt = input.getText().toString();
						((InputListener) getActivity()).onInput(txt);
					}
				});
			}
		});
		
//		setCancelable(false);
//		dialog.setCancelable(false);

		dialog.setCanceledOnTouchOutside(false);

		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		return dialog;
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		getActivity().onBackPressed();
	}
}
