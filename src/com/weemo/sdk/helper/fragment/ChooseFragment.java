package com.weemo.sdk.helper.fragment;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.weemo.sdk.helper.DemoAccounts;
import com.weemo.sdk.helper.R;

/*
 * This is a simple fragment that allows the user of the application to chose from a list
 * or to directly enter a value.
 * It is used  multiple times in this project
 * This is a simple util and does not contain Weemo SDK specific code
 */
public class ChooseFragment extends DialogFragment {

	public static ChooseFragment newInstance(String buttonText, @CheckForNull String removeID) {
		ChooseFragment fragment = new ChooseFragment();
		Bundle args = new Bundle();
		args.putString("buttonText", buttonText);
		args.putString("removeID", removeID);
		fragment.setArguments(args);
		return fragment;
	}

	public static ChooseFragment newInstance(String buttonText) {
		return newInstance(buttonText, null);
	}

	public static interface ChooseListener {
		public void onChoose(String chose);
	}
	
	private @Nullable ListView list;
	private @Nullable EditText input;
	private @Nullable Button go;
	
	private Map<String, String> accounts = new LinkedHashMap<String, String>(DemoAccounts.ACCOUNTS);
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String removeID = getArguments().getString("removeID");
		if (removeID != null)
			accounts.remove(removeID);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		getActivity().finish();
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_choose, container, false);
		
		list = (ListView) root.findViewById(R.id.list);
		input = (EditText) root.findViewById(R.id.input);
		go = (Button) root.findViewById(R.id.go);
		
		go.setText(getArguments().getString("buttonText"));
		
		final String[] ids = accounts.keySet().toArray(new String[0]);
		
		list.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_2, ids) {
			@Override public View getView(int position, View row, ViewGroup parent) {
				if (row == null)
					row = inflater.inflate(android.R.layout.simple_list_item_2, null);
				
				String id = getItem(position);
				
				((TextView) row.findViewById(android.R.id.text1)).setText(accounts.get(id));
				((TextView) row.findViewById(android.R.id.text2)).setText(id);
				
				return row;
			}
		});
		
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override public void onItemClick(AdapterView<?> list, View view, int position, long id) {
				input.setText(ids[position]);
				input.setSelection(ids[position].length());
			}
		});
		
		go.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View arg0) {
				String txt = input.getText().toString();

				if (txt.isEmpty())
					return ;
				
				InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(go.getWindowToken(), 0);

				((ChooseListener) getActivity()).onChoose(txt);
			}
		});
		
		return root;
	}
	
	public void setEnabled(boolean enabled) {
		go.setEnabled(enabled);

		input.setEnabled(enabled);
		input.setFocusable(enabled);
		if (enabled)
			input.setFocusableInTouchMode(enabled);
	}

	public void setButtonText(String text) {
		go.setText(text);
	}
}
