package com.crispy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;

import javax.mail.Folder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONObject;

import com.google.common.base.Objects;

public class Column {
	String name;
	String type;
	String def;
	boolean autoIncrement;
	String comment;

	public Column(String name, String type) {
		this.name = name;
		this.type = type.toUpperCase();
		this.def = null;
		this.autoIncrement = false;
		this.comment = "";
	}

	public String getType() {
		return type;
	}

	public static Column file(String name, String folder) {
		Column c = new Column(name, "VARCHAR(512)");
		c.comment = "folder:" + folder;
		return c;
	}

	public static Column s3(String name, String bucket) {
		Column c = new Column(name, "VARCHAR(512)");
		c.comment = "s3:" + bucket;
		return c;
	}

	public static Column text(String name, int length) {
		return new Column(name, "VARCHAR(" + length + ")");
	}

	public static Column text(String name, int length, String def) {
		Column c = Column.text(name, length);
		c.def = "'" + def + "'";
		return c;
	}

	public static Column text(String name) {
		return new Column(name, "TEXT");
	}

	public static Column mediumtext(String name) {
		return new Column(name, "MEDIUMTEXT");
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

	public static Column timestamp(String name, boolean def) {
		Column c = new Column(name, "TIMESTAMP");
		if (def)
			c.def = "CURRENT_TIMESTAMP";
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
			sb.append(" DEFAULT " + def);
		} else if (autoIncrement)
			sb.append(" PRIMARY KEY AUTO_INCREMENT");
		if (comment.length() > 0)
			sb.append(" COMMENT '" + StringEscapeUtils.escapeSql(comment) + "'");
		return sb.toString();
	}

	public static Column parseResultSet(ResultSet results) throws SQLException {
		String columnName = results.getString("COLUMN_NAME").toLowerCase();
		String type = results.getString("TYPE_NAME").toUpperCase();
		Column c = new Column(columnName, type);
		c.def = results.getString("COLUMN_DEF");
		c.autoIncrement = results.getString("IS_AUTOINCREMENT").equals("YES");
		c.comment = results.getString("REMARKS");
		if (type.equals("VARCHAR"))
			c.type = "VARCHAR(" + results.getInt("COLUMN_SIZE") + ")";
		if (type.equals("BIT"))
			c.type = "BOOL";
		return c;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Column))
			return false;
		Column other = (Column) o;
		return Objects.equal(name, other.name)
				&& Objects.equal(type, other.type)
				&& Objects.equal(comment, other.comment);
	}

	public static Column findByName(Collection<Column> columns, String name) {
		for (Column c : columns) {
			if (c.name.equals(name))
				return c;
		}
		return null;
	}

	public Object parseObject(Object value) {
		if (value == null)
			return null;
		if (type.endsWith("TEXT") || type.startsWith("VARCHAR")) {
			if (comment.length() > 0) {
				if (comment.startsWith("folder:")) {
					String uploadFolder = comment.substring(comment.indexOf(':') + 1);
					if (value instanceof File) {
						try {
							return Image.uploadFile(uploadFolder, new FileInputStream((File) value), ((File) value).getName());
						} catch (Exception e) {
							return null;
						}
					} else if (value instanceof URL) { 
						try {
							return Image.uploadFile(uploadFolder, ((URL) value).openStream(), ((URL) value).getPath());
						} catch (Exception e) {
							return null;
						}
					} else {
						return value.toString();
					}
				} else {
					String s3Bucket = comment.substring(comment.indexOf(':') + 1);
					if (value instanceof File) {
						try {
							return Image.uploadS3(s3Bucket, new FileInputStream((File) value), ((File) value).getName());
						} catch (Exception e) {
							return null;
						}
					} else if (value instanceof URL) {
						try {
							return Image.uploadS3(s3Bucket, ((URL) value).openStream(), ((URL) value).getPath());
						} catch (Exception e) {
							return null;
						}
					} else {
						return value.toString();
					}
				}
			} else
				return value.toString();
		}
		if (type.equals("BIGINT")) {
			if (value.toString().trim().length() == 0)
				return null;
			return Long.parseLong(value.toString());
		}
		if (type.equals("TIME")) {
			if (value instanceof String) {
				return parseTime((String) value);
			}
			if (value instanceof Long) {
				return DB.formatAsTime(new Date((Long) value));
			}
			if (value instanceof Date) {
				return value;
			}
			if (value instanceof java.util.Date)
				return DB.formatAsTime((java.util.Date) value);
			throw new IllegalArgumentException("Value should be of type time");
		}

		if (type.equals("DATE")) {
			if (value instanceof String) {
				return parseDate((String) value);
			}
			if (value instanceof Long)
				return DB.formatAsDate(new Date((Long) value));
			if (value instanceof Calendar)
				return DB.formatAsDate((Calendar) value);
			if (value instanceof Date)
				return value;
			if (value instanceof java.util.Date)
				return DB.formatAsDate((java.util.Date) value);
			throw new IllegalArgumentException("Value should be of type date");
		}
		if (type.equals("DATETIME")) {
			if (value instanceof String) {
				return parseDateTime((String) value);
			}
			if (value instanceof Date)
				return value;
			if (value instanceof java.util.Date)
				return DB.formatAsDateTime((java.util.Date) value);
			throw new IllegalArgumentException("Value should be of type date");
		}
		if (type.equals("TIMESTAMP")) {
			if (value instanceof String) {
				if (((String) value).trim().length() == 0)
					return null;
				return new Timestamp(Long.parseLong((String) value));
			}
			if (value instanceof java.util.Date)
				return new Timestamp(((java.util.Date) value).getTime());
			if (value instanceof Date)
				return new Timestamp(((Date) value).getTime());
			if (value instanceof Calendar)
				return new Timestamp(((Calendar) value).getTimeInMillis());
			if (value instanceof Timestamp)
				return value;
			if (value instanceof Long)
				return new Timestamp((Long) value);
			throw new IllegalArgumentException(
					"Value should be of type Timestamp");
		}
		if (type.equals("INT")) {
			if (value.toString().trim().length() == 0)
				return null;
			return Integer.parseInt(value.toString().trim());
		}
		return value;
	}

	public String getComment() {
		return comment;
	}

	public String getName() {
		return name;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public SimpleType simpleType(Metadata m) {
		if (m.getConstraint(name) != null) {
			Constraint c = m.getConstraint(name);
			Metadata dest = DB.getMetadata(c.destTable);
			if (dest.getDisplay() != null) {
				return SimpleType.REFERENCE;
			} else {
				return SimpleType.TEXT;
			}
		}

		if (type.startsWith("VARCHAR") && comment.length() > 0) {
			if (comment.startsWith("folder:")) {
				return SimpleType.FILE;
			} else {
				return SimpleType.S3;
			}
		}
		if (type.startsWith("VARCHAR")) {
			String temp = type.substring(type.indexOf('(') + 1,
					type.indexOf(')'));
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

	private static java.util.Date parseDate(String value) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return format.parse(value);
		} catch (ParseException e) {
			return null;
		}
	}

	private static java.util.Date parseTime(String value) {
		SimpleDateFormat format = new SimpleDateFormat("HH:MM:SS");
		try {
			return format.parse(value);
		} catch (ParseException e) {
			return null;
		}
	}

	public static java.util.Date parseDateTime(String value) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:MM:SS");
		try {
			return format.parse(value);
		} catch (ParseException e) {
			return null;
		}
	}

}
