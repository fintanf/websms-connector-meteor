package com.fairmichael.fintan.websms.connector.meteor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;
import de.ub0r.android.websms.connector.common.BasicSMSLengthCalculator;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * Connector for sending texts via mymeteor.ie
 * 
 * @author Fintan Fairmichael
 *
 */
public class ConnectorMeteor extends Connector {
	private static final Pattern VALID_ROI_MOBILE = Pattern.compile("08\\d{8}");
	private static final Pattern SUCCESSFUL_SEND = Pattern.compile("switchPane\\(\"fwtSent\"\\);\\s+showEl\\(\"sentTrue\"\\);");
	//private static final Pattern SESSION_CODES = Pattern.compile("var CFID = (\\d+);\\s+var CFTOKEN = (\\d+);");
	//private static final Pattern REMAINING_TEXTS = Pattern.compile("Free web texts left\\s+<input type=\"text\" id=\"numfreesmstext\" value=\"(\\d{1,3})\"");

	/** Tag for output. */
	static final String TAG = "meteor";

	/** Preference identifier for using default number for login. */
	private static final String PREFS_LOGIN_WTIH_DEFAULT = "login_with_default";

	/** Preference identifier for notifying on successful send */
  private static final String SUCCESSFUL_SEND_NOTIFICATION_PREFERENCE_ID = "successful_send_notification_meteor";
	
	/** The URLs we need */
	private static final String LOGIN_URL = "https://www.mymeteor.ie/go/mymeteor-login-manager";
	private static final String FREE_SMS_URL = "https://www.mymeteor.ie/cfusion/meteor/Meteor_REST/service/freeSMS";
	private static final String AJAX_URL = "https://www.mymeteor.ie/mymeteorapi/index.cfm";

	/** Google's ad unit id. */
	private static final String AD_UNITID = "a14e5aefcd1c730";

	/** HTTP Useragent. */
	static final String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 1.1.4322)";

	/** Used encoding. */
	static final String ENCODING = "ISO-8859-15";

	private static final int MAXIMUM_MESSAGE_LENGTH = 480;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_meteor_name);
		final ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(context.getString(R.string.connector_meteor_author));
		c.setAdUnitId(AD_UNITID);
		c.setBalance(null);
		c.setLimitLength(MAXIMUM_MESSAGE_LENGTH);
    c.setSMSLengthCalculator(new BasicSMSLengthCalculator(new int[] { 160, 160, 160 }));
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector("fishtext", c.getName(), SubConnectorSpec.FEATURE_NONE);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec updateSpec(final Context context, final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {
			if (p.getString(Preferences.PREFS_PASSWORD, "").length() > 0) {
				connectorSpec.setReady();
			} else {
				connectorSpec.setStatus(ConnectorSpec.STATUS_ENABLED);
			}
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

	private String convertToValidROINumber(String num) {
	  num = num.trim();
	  if (num.startsWith("+353")) {
	    num = '0' + num.substring(4);
    } else if (num.startsWith("00353")) {
      num = '0' + num.substring(5);
    }
	  if (VALID_ROI_MOBILE.matcher(num).matches()) {
	    Log.d(TAG, "Converted to ROI mobile: " + num);
	    return num;
	  } else {
	    Log.d(TAG, "Not a valid ROI mobile: " + num);
	    return null;
	  }
	}
	
	private LoginStatus doLogin(final Context context, String login) throws IOException {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);

		String processedLogin = convertToValidROINumber(login);
		
		if (processedLogin == null) {
			throw new WebSMSException(context.getString(R.string.invalid_sender_number, login));
		}
		String pass = p.getString(Preferences.PREFS_PASSWORD, "");

		//If we're doing an actual login, clear cookies first
		Utils.clearCookies();

		//Log.d(TAG, "Logging in with: " + processedLogin + ", pass: " + pass);

		//Post to login page
		final ArrayList<BasicNameValuePair> postData = new ArrayList<BasicNameValuePair>();
		postData.add(new BasicNameValuePair("username", processedLogin));
		postData.add(new BasicNameValuePair("userpass", pass));
		final String loginPage = MeteorUtil.http(context, LOGIN_URL, postData);

		//Check we have the cookie we need
		LoginStatus status = LoginStatus.fromCookieMap(MeteorUtil.getClientCookiesAsMap());
		if (!status.loggedIn || !loginPage.contains("MyMeteor Home") || !loginPage.contains("Webtext")) {
		  throw new WebSMSException(context, R.string.error_pw);
		} else {
		  Log.d(TAG, "Successful login. " + MeteorUtil.getClientCookiesAsMap());
		  return status;
		}
	}

	/**
	 * Send text.
	 * 
	 * @param context {@link Context}
	 * @param command {@link ConnectorCommand}
	 * @throws IOException IOException
	 */
	private void sendText(final Context context, final ConnectorCommand command, final ConnectorSpec spec, final LoginStatus status) {
		String[] recipients = command.getRecipients();
		String[] processedRecipients = new String[recipients.length];
		for (int i = 0; i < recipients.length; i++) {
		  String recipient = convertToValidROINumber(Utils.getRecipientsNumber(recipients[i]));
		  if (recipient == null) {
		    throw new WebSMSException(context.getString(R.string.invalid_sender_number, recipients[i]));
		  }
			Log.d(TAG, "RECIPIENT: " + recipient);
			processedRecipients[i] = recipient;
		}
		
		//Add for each recipient?
		final String addNumberUrl = AJAX_URL + "?event=smsAjax&func=addEnteredMsisdns&CFID=" + status.cfid + "&CFTOKEN=" + status.cftoken;
		final ArrayList<BasicNameValuePair> addNumberPostData = PostDataBuilder.start().add("ajaxRequest", "addEnteredMSISDNs").add("remove", "-").add("add", "0|" + processedRecipients[0]).data();
		
		final String sendTextUrl = AJAX_URL + "?event=smsAjax&func=sendSMS&CFID=" + status.cfid + "&CFTOKEN=" + status.cftoken;
		final ArrayList<BasicNameValuePair> sendTextPostData = PostDataBuilder.start().add("ajaxRequest", "sendSMS").add("messageText", command.getText()).data();
		try {
		  MeteorUtil.http(context, addNumberUrl, addNumberPostData);
		  
		  String sendResponse = MeteorUtil.http(context, sendTextUrl, sendTextPostData);
		  
		  if (SUCCESSFUL_SEND.matcher(sendResponse).find()) {
		    Log.d(TAG, "Matched successful send in response. Updating balance");
		    
		    //Update balance
		    checkLoginAndGetBalance(context, spec);
		    
		    final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
	      final boolean notifySend = p.getBoolean(SUCCESSFUL_SEND_NOTIFICATION_PREFERENCE_ID, true);
	      if (notifySend) {
	        final String notification = context.getString(R.string.successful_send_notification_meteor_notification, processedRecipients[0],  spec.getBalance());
	        Log.d(TAG, "Notifying on successful send: " + notification);
	        MeteorUtil.toastNotifyOnMain(context, notification, Toast.LENGTH_SHORT);
	      } else {
	        Log.d(TAG, "Not notifying on successful send");
	      }
		  } else {
		    Log.d(TAG, "No match on send response - did not send?");
		    throw new WebSMSException(context.getString(R.string.unsuccessful_send_meteor, processedRecipients[0]));
		  }
		  
		} catch (IOException ioe) {
		  //TODO clear cookies to force login again?
		  Log.d(TAG, "IOException occurred during send. " + ioe.toString());
      throw new WebSMSException(context, R.string.error_http);
		}
	}
	
	private static String getLogin(final Context context, final ConnectorCommand command) {
    final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
    final String login = p.getBoolean(PREFS_LOGIN_WTIH_DEFAULT, false) ? command.getDefSender() : Utils.getSender(context, command.getDefSender());
    return login;
  }
	
	/**
	 * Extract the balance from the String json response
	 * @param response
	 * @return
	 */
	private static Integer extractBalance(String response) {
	  if (response != null && response.length() > 0) {
	    
	    try {
	      JSONObject json = (JSONObject)new JSONTokener(response).nextValue();
	      JSONObject remainingFreeSMS = (JSONObject)json.get("FreeSMS");
	      return remainingFreeSMS.getInt("remainingFreeSMS");
	    } catch (Exception e) {
	      Log.d(TAG, "Exception thrown during JSON parse: " + e);
	      return null;
	    }
	    
	  } else {
	    return null;
	  }
	}
	
	/**
   * Test whether we're logged in, updating balance if we are
   * 
   * @param context
   * @return
   */
  public static LoginStatus checkLoginAndGetBalance(final Context context, final ConnectorSpec spec) {
    try {
      Log.d(TAG, "Requesting free sms remaining from " + FREE_SMS_URL);
      String response = MeteorUtil.http(context, FREE_SMS_URL);
      Integer remaining = extractBalance(response);
      if (remaining == null) {
        Log.d(TAG, "No balance in response, not logged in.");
        return LoginStatus.NOT_LOGGED_IN;
      } else {
        spec.setBalance(remaining + " webtexts");
        return LoginStatus.fromCookieMap(MeteorUtil.getClientCookiesAsMap());
      }

    } catch (NonOkResponseCodeException noe) {
      Log.d(TAG, "Non-ok response code received when loading " + FREE_SMS_URL + " - " + noe.code);
      return LoginStatus.NOT_LOGGED_IN;
    } catch (IOException ioe) {
      Log.d(TAG, "IOException when loading " + FREE_SMS_URL + " - not logged in");
      return LoginStatus.NOT_LOGGED_IN;
    }
  }

	
	private LoginStatus ensureLoggedIn(final Context context, final ConnectorSpec spec, final String login, final boolean updateBalance) {
	  Log.d(TAG, "Ensuring logged in.");
	  LoginStatus status = checkLoginAndGetBalance(context, spec);
    if (!status.loggedIn) {
      Log.d(TAG, "Not logged in, so doing login.");

      try {
        status = doLogin(context, login);
        //If we reach here without throwing an exception then we're logged in
        Log.d(TAG, "Should now be logged in");
        if (updateBalance) {
          // The aim was to update the balance, so do that now we're logged in
          return checkLoginAndGetBalance(context, spec);
        } else {
          return status;
        }
      } catch (IOException ioe) {
        Log.d(TAG, "An IOException occurred during login. " + ioe);
        throw new WebSMSException(context, R.string.error_http);
      }
    } else {
      return status;
    }
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Context context, final Intent intent) {
	  final ConnectorSpec spec = this.getSpec(context);
    final ConnectorCommand command = new ConnectorCommand(intent);
    ensureLoggedIn(context, spec, getLogin(context, command), true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent) {
	  final ConnectorSpec spec = this.getSpec(context);
    final ConnectorCommand command = new ConnectorCommand(intent);
    // Ensure logged in
    LoginStatus login = ensureLoggedIn(context, spec, getLogin(context, command), false);

    // Do actual send
    this.sendText(context, command, spec, login);
	}
}
