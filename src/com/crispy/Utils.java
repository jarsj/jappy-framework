package com.crispy;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;

import com.crispy.DB;

public class Utils {
	public static DateFormat getMySQLDateformat() {
		return new SimpleDateFormat("yyyy-MM-dd");
	}

	public static Calendar midnight() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 23);
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		return c;
	}

	public static Calendar weekend() {
		Calendar c = midnight();
		c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		return c;
	}

	public static Calendar endOfHour() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.HOUR_OF_DAY, 1);
		return c;
	}

	public static Calendar monthLater() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, 30);
		return c;
	}

	public static Calendar never() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.YEAR, 2);
		return c;
	}

	public static String sqlFormat(Calendar c) {
		return getMySQLDateformat().format(c.getTime());
	}

	public static Calendar beginning() {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(0);
		return c;
	}

	public static void addToMap(HashMap<String, Long> map, String key,
			long value) {
		if (map.containsKey(key)) {
			map.put(key, map.get(key) + value);
		} else {
			map.put(key, value);
		}
	}

	public static Calendar yesterday() {
		Calendar c = midnight();
		c.add(Calendar.DATE, -1);
		return c;
	}

	public static List<Calendar> days(Calendar from, Calendar to) {
		List<Calendar> ret = new ArrayList<Calendar>();
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(from.getTimeInMillis());
		while (!DB.formatAsDate(c).equals(DB.formatAsDate(to))) {
			ret.add((Calendar) c.clone());
			c.add(Calendar.DATE, 1);
		}

		ret.add((Calendar) to.clone());
		return ret;
	}

	public static String trim(String s, int length) {
		if (s.length() > length)
			return s.substring(0, length) + "...";
		return s;
	}

	public static String trimLines(String s, int N) {
		String sp[] = s.split("\\.", N + 1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Math.min(sp.length, N); i++) {
			sb.append(sp[i].trim() + ". ");
		}
		return sb.toString();
	}

	public static JSONArray arrayToJSON(List players) {
		JSONArray ret = new JSONArray();
		for (Object o : players) {
			if (o instanceof IJSONConvertible)
				ret.put(((IJSONConvertible) o).toJSONObject());
			else
				ret.put(o);
		}
		return ret;
	}

	public static int random(int M) {
		int R = (int) Math.floor((Math.random() * M));
		return R % M;
	}

	public static String getHost(String url) throws MalformedURLException {
		URL u = new URL(url);
		return u.getHost();
	}

	public static String clean(String trim) {
		trim = trim.replaceAll("\\s+", " ");
		trim = trim.trim();
		return trim;
	}
}
