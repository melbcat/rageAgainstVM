package com.example.Net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.annotation.SuppressLint;
import android.util.Base64;


public class HttpUtils { 

	private static  List<Cookie> cookies;
  public static String urlContent(String address) throws IOException, ClientProtocolException {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet httpGet = new HttpGet(address);
    ResponseHandler<String> handler = new BasicResponseHandler();
   // client.getParams().setParameter(CookieSpecPNames.DATE_PATTERNS, Arrays.asList("EEE, d MMM yyyy HH:mm:ss z"));
   // client.getParams().setParameter(
    //        ClientPNames.COOKIE_POLICY, CookiePolicy);
    
    CookieStore cookieStore = new BasicCookieStore();


    return(client.execute(httpGet, handler));
  }
  
  public static List<Cookie> getCookieList()
  {
	  return cookies;
  }

  public static String urlContentPost(String address, String ... paramNamesAndValues) 
      throws IOException, ClientProtocolException {
	DefaultHttpClient client = new DefaultHttpClient();
    HttpPost httpPost = new HttpPost(address);
    httpPost.setHeader("Authorization", getB64Auth("jvoyatz", "antisandbox") );
    List<NameValuePair> params = new ArrayList<NameValuePair>();
    for(int i=0; i<paramNamesAndValues.length-1; i=i+2) {
      String paramName = paramNamesAndValues[i];
      String paramValue = paramNamesAndValues[i+1];  // NOT URL-Encoded
      params.add(new BasicNameValuePair(paramName, paramValue));
    }
    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
    httpPost.setEntity(entity);
    ResponseHandler<String> handler = new BasicResponseHandler();
    //client.getParams().setParameter(CookieSpecPNames.DATE_PATTERNS, Arrays.asList("EEE, d MMM yyyy HH:mm:ss z"));
    cookies = client.getCookieStore().getCookies();
    return(client.execute(httpPost, handler));
    
    
  }
  @SuppressLint("NewApi")
private static String getB64Auth (String login, String pass) {
	   String source=login+":"+pass;
	   String ret="Basic "+Base64.encodeToString(source.getBytes(),Base64.URL_SAFE|Base64.NO_WRAP);
	   return ret;
	 }
	public static  Cookie getCookieList(String which)
	{
		List<Cookie> sessionCookie = HttpUtils.getCookieList();
		for(Cookie c : sessionCookie)
		{
			System.out.println(c.getName() + " | " + c.getValue());
			if(c.getName().equals(which) == true)
				return c;
		}
		return null;
	}
}

