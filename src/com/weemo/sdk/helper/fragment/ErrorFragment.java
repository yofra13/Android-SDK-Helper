package com.weemo.sdk.helper.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.weemo.sdk.helper.R;

public class ErrorFragment extends Fragment implements android.view.View.OnClickListener {

	public static ErrorFragment newInstance(String text) {
		ErrorFragment fragment = new ErrorFragment();
		Bundle args = new Bundle();
		args.putString("text", text);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_error, container, false);
		
		TextView errorText = (TextView) root.findViewById(R.id.errorText);
		errorText.setText(getArguments().getString("text"));
		
		Button close = (Button) root.findViewById(R.id.close);
		close.setOnClickListener(this);
		
		return root;
	}

	@Override
	public void onClick(View v) {
		getActivity().finish();
	}
}
