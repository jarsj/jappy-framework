package com.crispy.db;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.crispy.utils.Image;

public class Column {
	String name;
	String type;
	String def;
	boolean autoIncrement;

	String comment_folder;
	String comment_s3;
	String[] comment_cloudfront;

	public Column(String name, String type) {
		this.name = name;
		this.type = type.toUpperCase();
		this.def = null;
		this.autoIncrement = false;
	}

	public String getType() {
		return type;
	}

	public static Column file(String name, String folder) {
		Column c = new Column(name, "VARCHAR(512)");
		c.comment_folder = folder;
		return c;
	}

	public static Column s3(String name, String bucket) {
		Column c = new Column(name, "VARCHAR(512)");
		c.comment_s3 = bucket;
		return c;
	}

	public Column cloudfront(String... host) {
		comment_cloudfront = host;
		return this;
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

	private JSONObject commentJSON() {
		JSONObject ret = new JSONObject();
		if (comment_folder != null) {
			ret.put("folder", comment_folder);
		}
		if (comment_s3 != null) {
			ret.put("s3", comment_s3);
		}
		if (comment_cloudfront != null) {
			ret.put("cloudfront", new JSONArray(Arrays.asList(comment_cloudfront)));
		}
		return ret;
	}

	public String createDefinitions() {
		StringBuilder sb = new StringBuilder();
		sb.append("`" + name + "` ");
		sb.append(type);
		if (def != null) {
			sb.append(" DEFAULT " + def);
		} else if (autoIncrement)
			sb.append(" PRIMARY KEY AUTO_INCREMENT");
		JSONObject commentJSON = commentJSON();
		if (commentJSON.length() > 0)
			sb.append(" COMMENT '" + StringEscapeUtils.escapeSql(commentJSON.toString()) + "'");
		return sb.toString();
	}

	private static JSONObject parseComment(String remarks) {
		try {
			JSONObject ret = new JSONObject(remarks);
			return ret;
		} catch (JSONException e) {
			return new JSONObject();
		}
	}

	public static Column parseResultSet(ResultSet results) throws SQLException {
		String columnName = results.getString("COLUMN_NAME").toLowerCase();
		String type = results.getString("TYPE_NAME").toUpperCase();
		Column c = new Column(columnName, type);
		c.def = results.getString("COLUMN_DEF");
		c.autoIncrement = results.getString("IS_AUTOINCREMENT").equals("YES");
		
		JSONObject commentJSON = parseComment(results.getString("REMARKS"));
		if (commentJSON.has("folder")) {
			c.comment_folder = commentJSON.getString("folder");
		}
		if (commentJSON.has("s3")) {
			c.comment_s3 = commentJSON.getString("s3");
		}
		if (commentJSON.has("cloudfront")) {
			JSONArray cfArray = commentJSON.getJSONArray("cloudfront");
			c.comment_cloudfront = new String[cfArray.length()];
			for (int i = 0; i < cfArray.length(); i++) {
				c.comment_cloudfront[i] = cfArray.getString(i);
			}
		}
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

	public Object parseObject(Object value) {
		if (value == null)
			return null;
		if (type.endsWith("TEXT") || type.startsWith("VARCHAR")) {
			if (comment_folder != null) {
				String uploadFolder = comment_folder;
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
			} else if (comment_s3 != null) {
				String s3Bucket = comment_s3;
				if (value instanceof File) {
					try {
						return Image.uploadS3(s3Bucket, new FileInputStream((File) value), ((File) value).getName());
					} catch (Exception e) {
						throw new IllegalStateException("Can not upload " + value, e);
					}
				} else if (value instanceof URL) {
					try {
						return Image.uploadS3(s3Bucket, ((URL) value).openStream(), ((URL) value).getPath());
					} catch (Exception e) {
						throw new IllegalStateException("Can not upload " + value.toString(), e);
					}
				} else {
					return value.toString();
				}
			} else {
				return value.toString();
			}
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
			throw new IllegalArgumentException("Value should be of type Timestamp");
		}
		if (type.equals("INT")) {
			if (value.toString().trim().length() == 0)
				return null;
			return Integer.parseInt(value.toString().trim());
		}
		if (type.equals("FLOAT")) {
			if (value.toString().trim().length() == 0)
				return null;
			return Float.parseFloat(value.toString().trim());
		}
		if (type.equals("BOOL")) {
			if (value.toString().trim().length() == 0)
				return null;
			return Boolean.parseBoolean(value.toString().trim());
		}

		return value;
	}

	public String getName() {
		return name;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}
	
	private SimpleType internalSimpleType() {
		if (type.startsWith("VARCHAR")) {
			if (comment_folder != null) {
				return SimpleType.FILE;
			} else if (comment_s3 != null) {
				return SimpleType.S3;
			} else {
				String temp = type.substring(type.indexOf('(') + 1, type.indexOf(')'));
				int length = Integer.parseInt(temp);
				if (length < 200) {
					return SimpleType.TEXT;
				} else {
					return SimpleType.LONGTEXT;
				}
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
		
		return SimpleType.TEXT;
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
		
		return internalSimpleType();
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

	public boolean isCandidateForNullValue(Object value) {
		if (value == null)
			return true;
		if (!(value instanceof String))
			return false;
		String sValue = (String) value;
		if (sValue.trim().length() != 0)
			return false;
		return type.equals("BIGINT") || type.equals("INT") || type.equals("FLOAT");
	}

	public boolean isDisplayAsURL() {
		if (comment_s3 != null) return true;
		return false;
	}

}
