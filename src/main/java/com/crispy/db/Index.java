package com.crispy.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;


public class Index {
	String name;
	boolean isAuto;
	public static enum IndexType{
		UNIQUE("UNIQUE"), FULLTEXT("FULLTEXT");
		
		String index;

		private IndexType(String indx) {
			this.index = indx;
		}

		public String getIndex() {
			return index;
		}
	}
	private IndexType indexType;
	CopyOnWriteArrayList<String> columns;
	
	public static Index create(String column) {
		return new Index(column, column);
	}

	public static Index unique(String column) {
		return Index.create("u_" + column, IndexType.UNIQUE, column);
	}
	
	public static Index create(String name, IndexType index, String... column) {
		Index i = new Index(name, column);
		i.indexType = index;
		return i;
	}

	public Index(String name, String... column) {
		this.name = name;
		this.columns = new CopyOnWriteArrayList<String>(Arrays.asList(column));
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Index))
			return false;
		Index oi = (Index) o;
		if (indexType != null && indexType != oi.indexType)
			return false;
		if (!Objects.equals(name, oi.name))
			return false;
		if (!columns.equals(oi.columns))
			return false;
		return true;
	}

	public static Index findByName(Collection<Index> indexes, String name) {
		for (Index i : indexes) {
			if (i.name.equals(name) || i.name.equals("fk_" + name))
				return i;
		}
		return null;
	}

	public void process(ResultSet results) throws SQLException {
		String column = results.getString("COLUMN_NAME");
		int ordinal = results.getShort("ORDINAL_POSITION");

		while (this.columns.size() < ordinal) {
			this.columns.add(null);
		}

		this.columns.set(ordinal - 1, column);
	}

	public String createDefinition() {
		StringBuilder sb = new StringBuilder();
		if(indexType != null)
			sb.append(indexType.getIndex()+" ");
		if (name != null) {
			sb.append("INDEX `" + name + "` ");
		}
		sb.append("(`");
		sb.append(StringUtils.join(columns, "`,`"));
		sb.append("`)");
		return sb.toString();
	}

	
	public String getColumn(int i) {
		return columns.get(i);
	}
	
	public boolean hasColumn(String column) {
		return columns.contains(column);
	}
}
