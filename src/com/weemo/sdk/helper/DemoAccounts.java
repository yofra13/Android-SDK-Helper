package com.weemo.sdk.helper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class DemoAccounts extends LinkedHashMap<String, String> {

	/*
	 * Change this to set your own list of demo accounts
	 * These are the names the main characters of the manga "Monster" by Naoki Urasawa
	 * Every UID must comply to the Weemo naming rules:
	 * https://github.com/weemo/Release-4.x/wiki/WeemoDriver-Naming#token
	 */
	private DemoAccounts() {
		put("k.tenma", "Kenzo Tenma");
		put("fortner-n", "Nina Fortner");
		put("runge_h@bka.de", "Heinrich Runge");
		put("ev@heinman", "Eva Heinman");
		put("l j", "Johan Liebert"); // This UID does NOT comply to the Weemo naming rules.
		                                     // You can never find Johan, he finds you.
	}
	
	public static final Map<String, String> ACCOUNTS = Collections.unmodifiableMap(new DemoAccounts());
}
