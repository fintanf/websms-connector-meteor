package com.fairmichael.fintan.websms.connector.meteor;

import android.content.Context;
import de.ub0r.android.websms.connector.common.WebSMSException;

public class NonOkResponseCodeException extends WebSMSException {
  private static final long serialVersionUID = -3292343187123739090L;

  public final int code;
  
  public NonOkResponseCodeException(Context c, int rid, String s, int code) {
    super(c, rid, s);
    this.code = code;
  }

}
