package com.fairmichael.fintan.websms.connector.meteor;

import java.util.ArrayList;

import org.apache.http.message.BasicNameValuePair;

public class PostDataBuilder {

  private final ArrayList<BasicNameValuePair> data;

  public PostDataBuilder() {
    this.data = new ArrayList<BasicNameValuePair>();
  }

  public static PostDataBuilder start() {
    return new PostDataBuilder();
  }

  public PostDataBuilder add(final String name, final String value) {
    this.data.add(new BasicNameValuePair(name, value));
    return this;
  }

  public ArrayList<BasicNameValuePair> data() {
    return this.data;
  }

}
