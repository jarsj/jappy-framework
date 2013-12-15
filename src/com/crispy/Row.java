package com.crispy;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Row implements IJSONConvertible {
	private HashMap<String, Object> columns;
	private HashMap<String, LinkedList<String>> columnToTableIndex;
	private TreeSet<String> tables;
	private static final Log LOG = Log.get("db");

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
			return columnAsString(m.columns.get(0).getName());
		return columnAsString(m.getDisplay());
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

	public String columnAsString(String name) {
		if (name == null)
			throw new IllegalArgumentException("Passed null argument to columnAsString argument=name");
		return columnAsString(getTable(name), name);
	}

	public String columnAsString(String table, String name) {
		if (table == null)
			throw new IllegalArgumentException("Passed null argument to columnAsString argument=table");
		Column c = DB.getMetadata(table).getColumn(name);
		Object o = column(table, name);
		if (o instanceof String)
			return (String) o;
		return (o != null) ? o.toString() : c.def;
	}

	public URL columnAsUrl(String name) throws MalformedURLException {
		// Hello World
		return columnAsUrl(getTable(name), name);
	}

	public URL columnAsUrl(String table, String name) {
		Column c = DB.getMetadata(table).getColumn(name);
		Object value = column(name);
		if (value == null)
			return null;
		switch (c.simpleType(DB.getMetadata(table))) {
		case FILE: {
			try {
				return new URL("file://" + value.toString());
			} catch (MalformedURLException e) {
				LOG.error("Malformed url table=" + table + " column=" + name);
				return null;
			}
		}
		case S3: {
			try {
				return new URL(value.toString());
			} catch (MalformedURLException e) {
				LOG.error("Malformed url table=" + table + " column=" + name);
				return null;
			}
		}
		}
		return null;
	}

	public JSONObject columnAsJSONObject(String name) throws JSONException {
		return new JSONObject(columnAsString(name));
	}

	public String moneyAsString(String name, String currency) {
		return moneyAsString(getTable(name), name, currency);
	}

	public String moneyAsString(String table, String name, String currency) {
		long money = columnAsLong(table, name);
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

	public long columnAsLong(String table, String name) {
		Object o = column(table, name);
		if (o == null)
			return 0;
		if (o instanceof Number)
			return ((Number) o).longValue();
		return Long.parseLong(o.toString());
	}

	public int columnAsInt(String name, int def) {
		return columnAsInt(getTable(name), name, def);
	}
	
	public float columnAsFloat(String name, float def) {
		return columnAsFloat(getTable(name), name, def);
	}

	public float columnAsFloat(String table, String name, float def) {
		Object o = column(table, name);
		if (o == null)
			return def;
		if (o instanceof Number)
			return ((Number) o).floatValue();
		return Float.parseFloat(o.toString());
	}

	public boolean columnAsBool(String table, String name) {
		Object o = column(table, name);
		if (o instanceof Boolean)
			return (Boolean) o;
		if (o instanceof Number) {
			return ((Integer) o) > 0;
		}
		return o != null;
	}

	public boolean columnAsBool(String name) {
		return columnAsBool(getTable(name), name);
	}

	public int columnAsInt(String table, String name, int def) {
		Object o = column(table, name);
		if (o == null)
			return def;
		if (o instanceof Number)
			return ((Number) o).intValue();
		return Integer.parseInt(o.toString());
	}

	public Date columnAsDate(String table, String name) {
		java.sql.Date sqlDate = (java.sql.Date) column(table, name);
		return new Date(sqlDate.getTime());
	}

	public Date columnAsDate(String name) {
		return columnAsDate(getTable(name), name);
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

	public int columnAsInt(String name) {
		return columnAsInt(getTable(name), name, 0);
	}

	public long columnAsLong(String name) {
		return columnAsLong(getTable(name), name);
	}

	public String dateAsString(String name, String format) {
		return dateAsString(getTable(name), name, format);
	}
	
	public void update(String column, Object value) {
		String table = getTable(column);
		Metadata meta = DB.getMetadata(table);
		Index primary = meta.getPrimary();
		Table t = Table.get(table);
		for (String col : primary.columns) {
			t.where(col, column(col));
		}
		t.columns(column).values(value).update();
	}
}
