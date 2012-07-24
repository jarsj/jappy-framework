package com.crispy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Objects;

public class Metadata {
	String name;
	CopyOnWriteArrayList<Column> columns;
	CopyOnWriteArrayList<Index> indexes;
	CopyOnWriteArrayList<Constraint> constraints;
	Index primary;
	JSONObject comment;
	
	public Metadata(String table) {
		this.name = table;
		columns = new CopyOnWriteArrayList<Column>();
		indexes = new CopyOnWriteArrayList<Index>();
		constraints = new CopyOnWriteArrayList<Constraint>();
		comment = new JSONObject();
	}
	
	public Column getColumn(String name) {
		return Column.findByName(columns, name);
	}
	
	public Index getIndex(String name) {
		return Index.findByName(indexes, name);
	}
	
	public Constraint getConstraint(String column) {
		for (Constraint c : constraints) {
			if (c.sourceColumn.equals(column))
				return c;
		}
		return null;
	}
	
	public Index getPrimary() {
		return primary;
	}

	public JSONObject toJSONObject() throws JSONException {
		JSONObject o = new JSONObject();
		o.put("name", name);
		JSONArray columns = new JSONArray();
		for (Column c : this.columns) {
			columns.put(new JSONObject().put("name", c.name).put("type",
					c.type));
		}
		o.put("columns", columns);
		o.put("comment", comment);
		return o;
	}
	
	public String getTableName() {
		return name;
	}
	
	public String[] columnNames() {
		List<String> ret = new ArrayList<String>();
		for (Column c : columns) {
			ret.add(c.name);
		}
		return ret.toArray(new String[]{});
	}
	public CopyOnWriteArrayList<Column> getColumns() {
		return columns;
	}
	
	public void reorderAndRetain(final List<Column> refs) {
		ArrayList<Column> temp = new ArrayList<Column>(columns);
		
		// First get rid of all columns that we do not need
		Iterator<Column> iter = temp.iterator();
		while (iter.hasNext()) {
			Column col = iter.next();
			if (!refs.contains(col)) 
				iter.remove();
		}
		
		Collections.sort(temp, new Comparator<Column>() {
			@Override
			public int compare(Column o1, Column o2) {
				Integer o1i = refs.indexOf(o1);
				Integer o2i = refs.indexOf(o2);
				return o1i.compareTo(o2i);
			}
		});
		columns = new CopyOnWriteArrayList<Column>(temp);
	}
	
	public String getDisplay() {
		return comment.optString("display", columns.get(0).getName());
	}
	
	public boolean dataEntry() {
		if (name.startsWith("_"))
			return false;
		return !(comment.optBoolean("no-data-entry", false));
	}
		
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(" columns=" + columns.size());
		sb.append(" indexes=" + indexes.size());
		sb.append(" constraints=" + constraints.size());
		sb.append(" comment=" + comment.toString());
		return sb.toString();
	}

	public boolean isSystem() {
		return name.startsWith("_");
	}
}
