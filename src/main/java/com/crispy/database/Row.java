package com.crispy.database;

import com.crispy.log.Log;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

public class Row {
    private static final Log LOG = Log.get("jappy.db");

    private ArrayList<Value> values;
    private ArrayList<String> columnNames;
    private ArrayList<String> tableNames;
    private ArrayList<String> columnAlias;

    protected Row(ResultSet results) throws SQLException {
        values = new ArrayList<>();
        columnNames = new ArrayList<>();
        tableNames = new ArrayList<>();
        columnAlias = new ArrayList<>();

        ResultSetMetaData meta = results.getMetaData();
        for (int c = 0; c < meta.getColumnCount(); c++) {
            String column = meta.getColumnName(c + 1);
            String table = meta.getTableName(c + 1);
            String alias = meta.getColumnLabel(c + 1);
            columnNames.add(column);
            tableNames.add(table);
            columnAlias.add(alias);
            values.add(Value.create(results.getObject(c + 1)));
        }
    }

    public Value byName(String name) {
        int index = columnNames.indexOf(name);
        if (index == -1)
            throw new IllegalArgumentException("No such column exist : " + name);
        return byIndex(columnNames.indexOf(name));
    }

    public Value byAlias(String alias) {
        return byIndex(columnAlias.indexOf(alias));
    }

    public Value byFullName(String table, String name) {
        for (int i = 0; i < columnNames.size(); i++) {
            if (table.equals(tableNames.get(i)) && name.equals(columnNames.get(i))) {
                return byIndex(i);
            }
        }
        return null;
    }

    public Value byIndex(int i) {
        return values.get(i);
    }

    public JSONObject toJSON() {
        JSONObject ret = new JSONObject();
        for (int i = 0; i < values.size(); i++) {
            String key = columnAlias.get(i);
            if (key == null || key.length() == 0)
                key = columnNames.get(i);
            put(ret, key, values.get(i));
        }
        return ret;
    }

    private void put(JSONObject o, String key, Value value) {
        int dot = key.indexOf('.');
        if (dot == -1) {
            o.put(key, value.asObject());
        } else {
            String first = key.substring(0, dot);
            String second = key.substring(dot + 1);
            if (!(o.has(first) && (o.get(first) instanceof JSONObject))) {
                o.put(first, new JSONObject());
            }
            put(o.getJSONObject(first), second, value);
        }
    }
}
