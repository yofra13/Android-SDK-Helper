package com.weemo.sdk.helper.util;

import java.lang.reflect.Field;

import android.app.Activity;
import android.view.ViewConfiguration;

public abstract class UIUtils {

	// This is a horrible hack whose purpose is to force the display of the overflow...
	// http://stackoverflow.com/a/11438245/1269640
	// https://code.google.com/p/android/issues/detail?id=38013
	public static void forceOverflowMenu(Activity activity) {
		try {
			ViewConfiguration config = ViewConfiguration.get(activity);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		}
		catch (Exception ex) {
			// Ignore
		}
	}
	
}
