package com.fairmichael.fintan.websms.connector.meteor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

public class MeteorUtil {
  public static String appendWithSeparator(final Collection<?> items, final String sep) {
    return appendWithSeparator(items, sep, false);
  }

  public static String appendWithSeparator(final Collection<?> items, final String sep, final boolean separatorAtEnd) {
    StringBuilder sb = new StringBuilder();
    for (Object o : items) {
      sb.append(o.toString());
      sb.append(sep);
    }
    if (sb.length() > 0 && !separatorAtEnd) {
      sb.delete(sb.length() - sep.length(), sb.length());
    }
    return sb.toString();
  }

  public static <T> String appendWithSeparator(final T[] items, final String sep, final boolean separatorAtEnd) {
    return appendWithSeparator(Arrays.asList(items), sep, separatorAtEnd);
  }

  public static <T> String appendWithSeparator(final T[] items, final String sep) {
    return appendWithSeparator(Arrays.asList(items), sep);
  }

  public static String http(final Context context, final String url, final ArrayList<BasicNameValuePair> postData, final String referrer)
      throws IOException {

    final HttpResponse response = Utils.getHttpClient(url, null, postData, ConnectorMeteor.USER_AGENT, referrer, ConnectorMeteor.ENCODING, false);
    final int responseCode = response.getStatusLine().getStatusCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      Log.d(ConnectorMeteor.TAG, "Received non-ok status code when attempting to load " + url);
      throw new NonOkResponseCodeException(context, R.string.error_http, "" + responseCode, responseCode);
    }

    final String pageHtml = Utils.stream2str(response.getEntity().getContent());
    Log.d(ConnectorMeteor.TAG, "----START HTTP RESPONSE for " + url + "---");
    Log.d(ConnectorMeteor.TAG, pageHtml);
    Log.d(ConnectorMeteor.TAG, "----END HTTP RESPONSE---");

    return pageHtml;
  }

  public static String http(final Context context, final String url, final ArrayList<BasicNameValuePair> postData) throws IOException {
    return http(context, url, postData, null);
  }

  public static String http(final Context context, final String url) throws IOException {
    return http(context, url, (ArrayList<BasicNameValuePair>) null);
  }

  public static String http(final Context context, final String url, final String referrer) throws IOException {
    return http(context, url, null, referrer);
  }

  /**
   * Fix html currency symbols to the unicode equivalent
   * 
   * @param string
   * @return
   */
  public static String currencyFix(final String string) {
    return string.replaceAll("&pound;", "\u00A3").replaceAll("&euro;", "\u20AC");
  }

  /**
   * Slightly complicated method of showing a toast notification after the
   * current thread has died
   * 
   * @param context
   * @param message
   * @param length
   */
  public static void toastNotifyOnMain(final Context context, final String message, final int length) {
    final Handler handler = new Handler(Looper.getMainLooper());
    final Runnable showNotification = new Runnable() {
      public void run() {
        Log.d(ConnectorMeteor.TAG, "Actually notifying on successful send: " + message);
        Toast.makeText(context.getApplicationContext(), message, length).show();
      }
    };

    new Thread() {
      @Override
      public void run() {
        handler.post(showNotification);
      }
    }.start();
  }

  /**
   * Helper for building parameter arrays.
   * 
   * @param map
   * @return
   */
  public static ArrayList<BasicNameValuePair> parametersMapToParametersArray(final Map<String, String> map) {
    final ArrayList<BasicNameValuePair> paramArray = new ArrayList<BasicNameValuePair>(map.size());
    for (Map.Entry<String, String> entry : map.entrySet()) {
      paramArray.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
    }
    return paramArray;
  }
  
  public static Map<String,String> getClientCookiesAsMap() {
    return cookiesAsMap(Utils.getCookies());
  }
  
  public static Map<String,String> cookiesAsMap(Collection<Cookie> cookies) {
    final Map<String,String> cookiesMap = new HashMap<String,String>(cookies.size());
    for (final Cookie cookie : cookies) {
      cookiesMap.put(cookie.getName(), cookie.getValue());
    }
    return cookiesMap;
  }
}
