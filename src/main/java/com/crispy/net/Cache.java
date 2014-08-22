package com.crispy.net;

import com.crispy.db.Column;
import com.crispy.db.DB;
import com.crispy.db.Table;
import com.crispy.log.Log;
import com.crispy.utils.Utils;

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

	public Cache() {
		isRunning = false;
	}

	public void start() {
		Table.get("cache").columns(Column.text("key", 512), Column.longtext("value"), Column.timestamp("expires")).primary("key").create();
		isRunning = true;
	}

	public String fetch(String key, String def) throws Exception {
		String data = (String) DB

		.singleItemQuery("SELECT `value` FROM `cache` WHERE `key`=? AND `expires`>CURRENT_TIMESTAMP()", key);
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

	public void shutdown() {

	}

	public static Cache getInstance() {
		return INSTANCE;
	}

	public boolean isRunning() {
		return isRunning;
	}

}
