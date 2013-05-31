package com.crispy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import sun.util.LocaleServiceProviderPool.LocalizedObjectGetter;


public class Cache {
	
	static Log LOG = Log.get("cache");

	public enum Expire {
		HOUR, TONIGHT, WEEKEND, MONTH;

		public long expires() {
			switch (this) {
			case HOUR:
				return Utils.endOfHour().getTimeInMillis();
			case TONIGHT:
				return Utils.midnight().getTimeInMillis();
			case WEEKEND:
				return Utils.weekend().getTimeInMillis();
			case MONTH:
				return Utils.monthLater().getTimeInMillis();
			}
			return 0;
		}
	}

	private static final Cache INSTANCE = new Cache();
	private boolean isRunning;
	private File cacheFolder;

	public Cache() {
		isRunning = false;
	}

	public void start(File folder) {
		Table.get("cache")
				.columns(Column.text("key", 512), Column.longtext("value"),
						Column.timestamp("expires")).primary("key").create();
		this.cacheFolder = folder;
		isRunning = true;
	}
	

	public String fetch(String key, String def) throws Exception {
		String data = (String) DB

				.singleItemQuery(
						"SELECT `value` FROM `cache` WHERE `key`=? AND `expires`>CURRENT_TIMESTAMP()",
						key);
		if (data == null)
			return def;
		return data;
	}

	public void store(String key, String value, long expire) throws Exception {
		if (expire == 0)
			return;
		Table.get("cache").columns("key", "value", "expires").values(key, value, expire).overwrite("value", "expires").add();
	}
	
	public void remove(String key) throws Exception {
		Table.get("cache").where("key", key).delete();
	}

	public void store(String key, String value, Expire e) throws Exception {
		store(key, value, e.expires());
	}

	public File fetchUrl(String url) throws IOException {
		URL u = new URL(url); 
		return fetchUrl(u);
	}
	
	public void shutdown() {

	}

	public static Cache getInstance() {
		return INSTANCE;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public File fetchUrl(URL u) throws IOException {
		File f = new File(cacheFolder, u.getHost() + "/" + u.getPath());
		if (f.exists()) {
			LOG.debug("cache-hit u=" + u.toString());
			return f;
		}
		LOG.debug("cache-miss u=" + u.toString());
		FileUtils.copyURLToFile(u, f);
		return f;

	}
}
