package com.crispy;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

public class Job {
	private String url;
	private HashMap<String, String> headers;
	private int priority;
	private long expiry;
	private String category;
	private String ua;
	private String cacheKey;
	private String tag;
	private JSONObject extra;

	public Job(String url, int p) {
		this(url, p, null);
	}

	public Job(String url, int p, String tag) {
		this(url, p, 0, tag);
	}

	public Job(String url, int p, long expiry, String tag) {
		this.url = url;
		this.priority = p;
		this.expiry = expiry;
		this.headers = new HashMap<String, String>();
		this.cacheKey = url;
		this.ua = null;
		this.tag = tag;
		this.extra = new JSONObject();
	}

	public void setMetadata(JSONObject o) throws JSONException {
		this.cacheKey = o.optString("cache-key", null);
		this.category = o.optString("category", null);
		this.expiry = o.getLong("expiry");
		this.tag = o.optString("tag", null);
		this.ua = o.optString("ua", null);
		this.headers = new HashMap<String, String>();
		JSONObject ho = o.getJSONObject("headers");
		String[] keys = JSONObject.getNames(ho);
		if (keys != null) {
			for (String key : JSONObject.getNames(ho)) {
				this.headers.put(key, ho.getString(key));
			}
		}
		this.extra = o.getJSONObject("extra");
	}

	public JSONObject getMetadata() throws JSONException {
		JSONObject ret = new JSONObject();
		if (this.cacheKey != null)
			ret.put("cache-key", this.cacheKey);
		if (this.category != null)
			ret.put("category", this.category);
		if (this.tag != null)
			ret.put("tag", tag);
		ret.put("expiry", this.expiry);
		if (this.ua != null)
			ret.put("ua", this.ua);
		ret.put("headers", new JSONObject(headers));
		ret.put("extra", extra);
		return ret;
	}

	public void setCacheKey(String cacheKey) {
		this.cacheKey = cacheKey;
	}

	public String getCacheKey() {
		return cacheKey;
	}

	public void setHeader(String key, String value) {
		this.headers.put(key, value);
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCategory() {
		return (category == null ? "default" : category);
	}

	public String getUrl() {
		return url;
	}

	public int getPriority() {
		return priority;
	}

	public long getExpiry() {
		return expiry;
	}

	public HashMap<String, String> getHeaders() {
		return headers;
	}

	public static void setITunes9Headers(Job j, String storeId) {
		j.setHeader("Host", "itunes.apple.com");
		j.setHeader("Accept-Language", "en-us, en;q=0.50");
		j.setHeader("X-Apple-Store-Front", storeId);
		j.setHeader("X-Apple-Tz", "3600");
		j.setUserAgent("iTunes/9.2.1 (Macintosh; Intel Mac OS X 10.5.8) AppleWebKit/533.16");
		j.setCacheKey(j.getUrl() + "#" + storeId);

	}

	public static void setITunes11Headers(Job j, String storeId) {
		j.setHeader("Host", "client-api.itunes.apple.com");
	}
	
	public void setUserAgent(String string) {
		this.ua = string;
	}

	public String getUserAgent() {
		return ua;
	}

	public String getTag() {
		return tag;
	}

	public JSONObject getExtra() {
		return extra;
	}

	public static void main(String[] args) throws JSONException {
		JSONObject o = new JSONObject("{'headers':{}}");
		System.out.println(JSONObject.getNames(o.getJSONObject("headers")));
	}
}
