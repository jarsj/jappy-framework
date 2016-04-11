package com.crispy.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class Column {
	String name;
	String type;
	String def;
	boolean autoIncrement;

	public Column(String name, String type) {
		this.name = name;
		this.type = type.toUpperCase();
		this.def = null;
		this.autoIncrement = false;
	}

	public String getType() {
		return type;
	}

	public static Column text(String name, int length) {
		return new Column(name, "VARCHAR(" + length + ")");
	}

	public static Column text(String name, int length, String def) {
		Column c = Column.text(name, length);
		c.def = def;
		return c;
	}

	public static Column text(String name) {
		return new Column(name, "TEXT");
	}

	public static Column mediumtext(String name) {
		return new Column(name, "MEDIUMTEXT");
	}

	public static Column floating(String name) {
		return new Column(name, "FLOAT");
	}

	public static Column longtext(String name) {
		return new Column(name, "LONGTEXT");
	}

	public static Column date(String name) {
		return new Column(name, "DATE");
	}

	public static Column timestamp(String name) {
		return timestamp(name, true);
	}

	public static Column binary(String name) {
		return new Column(name, "BLOB");
	}


	public static Column timestamp(String name, boolean def) {
		Column c = new Column(name, "TIMESTAMP");
		if (def)
			c.def = "CURRENT_TIMESTAMP";
		else
			c.def = "0000-00-00 00:00:00";
		return c;
	}

	public static Column datetime(String name) {
		Column c = new Column(name, "DATETIME");
		return c;
	}

	public static Column time(String name) {
		Column c = new Column(name, "TIME");
		return c;
	}

	public static Column integer(String name) {
		return new Column(name, "INT");
	}

	public static Column integer(String name, int def) {
		Column c = new Column(name, "INT");
		c.def = Integer.toString(def);
		return c;
	}

	public static Column bigInteger(String name) {
		return new Column(name, "BIGINT");
	}

	public static Column bigInteger(String name, boolean autoIncrement) {
		Column c = bigInteger(name);
		c.autoIncrement = true;
		return c;
	}

	public String createDefinitions() {
		StringBuilder sb = new StringBuilder();
		sb.append("`" + name + "` ");
		sb.append(type);
		if (def != null) {
			sb.append(" DEFAULT ");
			if (type.startsWith("VARCHAR") || type.contains("TEXT")) {
				sb.append("'" + def + "'");
			} else if (type.equals("TIMESTAMP")) {
				if (!def.equals("CURRENT_TIMESTAMP")) {
					sb.append("'" + def + "'");
				} else {
					sb.append(def);
				}
			} else {
				sb.append(def);
			}
		} else if (autoIncrement)
			sb.append(" PRIMARY KEY AUTO_INCREMENT");
		return sb.toString();
	}

	public static Column parseResultSet(ResultSet results) throws SQLException {
		String columnName = results.getString("COLUMN_NAME").toLowerCase();
		String type = results.getString("TYPE_NAME").toUpperCase();
		Column c = new Column(columnName, type);
		c.def = results.getString("COLUMN_DEF");
		c.autoIncrement = results.getString("IS_AUTOINCREMENT").equals("YES");

		if (type.equals("VARCHAR")) {
			c.type = "VARCHAR(" + results.getInt("COLUMN_SIZE") + ")";
		}

		if (!results.getBoolean("IS_NULLABLE")) {
			if (c.internalSimpleType() == SimpleType.TEXT) {
				if (c.def != null && c.def.length() == 0) {
					c.def = null;
				}
			} else if (c.internalSimpleType() == SimpleType.INTEGER) {
				if (c.def != null && c.def.equals("0")) {
					c.def = null;
				}
			}
		}

		if (type.equals("BIT"))
			c.type = "BOOL";
		return c;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Column))
			return false;
		Column other = (Column) o;
		return createDefinitions().equals(other.createDefinitions());
	}

	public static Column findByName(Collection<Column> columns, String name) {
		for (Column c : columns) {
			if (c.name.equals(name))
				return c;
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	SimpleType internalSimpleType() {
		if (type.startsWith("VARCHAR")) {
			String temp = type.substring(type.indexOf('(') + 1, type.indexOf(')'));
			int length = Integer.parseInt(temp);
			if (length < 200) {
				return SimpleType.TEXT;
			} else {
				return SimpleType.LONGTEXT;
			}
		}
		if (type.endsWith("TEXT"))
			return SimpleType.LONGTEXT;
		if (type.equals("DATE"))
			return SimpleType.DATE;
		if (type.equals("TIME"))
			return SimpleType.TIME;
		if (type.equals("DATETIME"))
			return SimpleType.DATETIME;
		if (type.equals("TIMESTAMP"))
			return SimpleType.TIMESTAMP;
		if (type.endsWith("INT"))
			return SimpleType.INTEGER;
		if (type.equals("BOOL"))
			return SimpleType.BOOL;
        if (type.equals("BLOB"))
            return SimpleType.BINARY;

		return SimpleType.TEXT;
	}

	public static Column bool(String name) {
		return new Column(name, "BOOL");
	}

	public static Column bool(String name, boolean def) {
		Column c = new Column(name, "BOOL");
		c.def = def ? "1" : "0";
		return c;
	}

	public String getDefault() {
		return def;
	}
}
