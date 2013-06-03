package com.crispy;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Row implements IJSONConvertible {
	private HashMap<String, Object> columns;
	private HashMap<String, LinkedList<String>> columnToTableIndex;
	private TreeSet<String> tables;

	protected Row(ResultSet results) throws SQLException {
		columns = new HashMap<String, Object>();
		columnToTableIndex = new HashMap<String, LinkedList<String>>();
		tables = new TreeSet<String>();
		ResultSetMetaData meta = results.getMetaData();
		for (int c = 0; c < meta.getColumnCount(); c++) {
			String table = meta.getTableName(c + 1);
			String column = meta.getColumnName(c + 1);
			columns.put(table + "." + column, results.getObject(c + 1));
			tables.add(table);
			LinkedList<String> myTables = columnToTableIndex.get(column);
			if (myTables == null) {
				myTables = new LinkedList<String>();
				columnToTableIndex.put(column, myTables);
			}
			myTables.add(table);
		}
	}

	public String display() {
		Metadata m = DB.getMetadata(tables.first());
		if (m.getDisplay() == null)
			return string(m.columns.get(0).getName());
		return string(m.getDisplay());
	}

	private String getTable(String name) {
		LinkedList<String> tables = columnToTableIndex.get(name);
		if (tables == null)
			throw new IllegalArgumentException("Column does not exist " + name);
		if (tables.size() > 1)
			throw new IllegalArgumentException("Multiple tables exist for column " + name);
		return tables.getFirst();
	}

	public Object column(String name) {
		return column(getTable(name), name);
	}

	public Object column(String table, String name) {
		return columns.get(table + "." + name);
	}

	public String string(String name) {
		if (name == null)
			throw new IllegalArgumentException(
					"Passed null argument to string argument=name");
		return string(getTable(name), name);
	}

	public String string(String table, String name) {
		if (table == null)
			throw new IllegalArgumentException(
					"Passed null argument to string argument=table");
		Column c = DB.getMetadata(table).getColumn(name);
		Object o = column(table, name);
		if (o instanceof String)
			return (String) o;
		return (o != null) ? o.toString() : c.def;
	}

	public String url(String name) {
		return url(getTable(name), name);
	}

	public String url(String table, String name) {
		Column c = DB.getMetadata(table).getColumn(name);
		Object value = column(name);
		if (value == null)
			return null;
		switch (c.simpleType(DB.getMetadata(table))) {
		case FILE:
			return "/resource/local" + value.toString();
		case S3:
			return value.toString();
		}
		return null;
	}

	public JSONObject jsonObject(String name) throws JSONException {
		return new JSONObject(string(name));
	}

	public String moneyAsString(String name, String currency) {
		return moneyAsString(getTable(name), name, currency);
	}

	public String moneyAsString(String table, String name, String currency) {
		long money = biginteger(table, name);
		if (currency.equals("USD")) {
			if (money == 0)
				return "";
			if (money < 1000)
				return String.format("$%d", money);
			if (money < 1000000)
				return String.format("$%.1fK", (money * 1.0f) / 1000);
			return String.format("$%.1fM", (money * 1.0f) / 1000000);
		} else if (currency.equals("INR")) {
			if (money == 0)
				return "";
			if (money < 1000)
				return String.format("%d", money);
			if (money < 100000)
				return String.format("%dK", (money) / 1000);
			if (money < 10000000)
				return String.format("%dL", (money) / 100000);
			return String.format("%dCr", (money) / 10000000);
		}
		throw new IllegalStateException("Currency not supported");
	}

	public String dateAsString(String table, String name, String format) {
		Object o = column(table, name);
		if (o instanceof java.sql.Date) {
			java.sql.Date d = (java.sql.Date) o;
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			return sdf.format(new Date(d.getTime()));
		}
		if (o == null)
			return "";
		return o.toString();
	}

	public long biginteger(String table, String name) {
		Object o = column(table, name);
		if (o == null)
			return 0;
		if (o instanceof Number)
			return ((Number) o).longValue();
		return Long.parseLong(o.toString());
	}

	public int integer(String name, int def) {
		return integer(getTable(name), name, def);
	}

	public boolean bool(String table, String name) {
		Object o = column(table, name);
		if (o instanceof Boolean)
			return (Boolean) o;
		if (o instanceof Number) {
			return ((Integer) o) > 0;
		}
		return o != null;
	}

	public boolean bool(String name) {
		return bool(getTable(name), name);
	}

	public int integer(String table, String name, int def) {
		Object o = column(table, name);
		if (o == null)
			return def;
		if (o instanceof Number)
			return ((Number) o).intValue();
		return Integer.parseInt(o.toString());
	}

	public Date date(String table, String name) {
		java.sql.Date sqlDate = (java.sql.Date) column(table, name);
		return new Date(sqlDate.getTime());
	}

	public Date date(String name) {
		return date(getTable(name), name);
	}

	@Override
	public JSONObject toJSONObject() {
		return new JSONObject(columns);
	}

	public static JSONObject rowToJSON(Row r) throws JSONException {
		JSONObject o = new JSONObject();
		for (Map.Entry<String, Object> entry : r.columns.entrySet()) {
			String cname = entry.getKey();
			cname = cname.substring(cname.indexOf('.') + 1);
			o.put(cname, entry.getValue());
		}
		return o;
	}

	public static JSONArray rowsToJSON(List<Row> rows) throws JSONException {
		JSONArray ret = new JSONArray();
		for (Row r : rows) {
			ret.put(rowToJSON(r));
		}
		return ret;
	}

	public int integer(String name) {
		return integer(getTable(name), name, 0);
	}

	public long biginteger(String name) {
		return biginteger(getTable(name), name);
	}

	public String dateAsString(String name, String format) {
		return dateAsString(getTable(name), name, format);
	}
}
