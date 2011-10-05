package com.fairmichael.fintan.websms.connector.meteor;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Meteor preferences.
 * 
 * @author Fintan Fairmichael
 *
 */
public class Preferences extends PreferenceActivity {
	public static final String PREFS_ENABLED = "enable_meteor";
	public static final String PREFS_PASSWORD = "password_meteor";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.connector_meteor_prefs);
	}
}
