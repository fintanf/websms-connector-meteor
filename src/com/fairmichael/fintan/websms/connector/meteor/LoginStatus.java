package com.fairmichael.fintan.websms.connector.meteor;

import java.util.Map;

/**
 * A simple datastructure for keeping track of our login status.
 * @author Fintan Fairmichael
 *
 */
public class LoginStatus {
	public static final LoginStatus NOT_LOGGED_IN = new LoginStatus(false, "", "");
	
	public final String cfid;
	public final String cftoken;
	public final boolean loggedIn;
	
	public LoginStatus(boolean loggedIn, String cfid, String cftoken) {
		this.loggedIn = loggedIn;
		this.cfid = cfid;
		this.cftoken = cftoken;
	}
	
	public static LoginStatus fromCookieMap(Map<String, String> map) {
	  final String cfid = map.get("CFID");
    final String cftoken = map.get("CFTOKEN");
    if (cfid == null || cftoken == null) {
      return NOT_LOGGED_IN;
    } else {
      return new LoginStatus(true, cfid, cftoken);
    }
	}
}