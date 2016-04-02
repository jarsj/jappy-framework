package com.crispy.database;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by harsh on 2/7/16.
 */
public class Rows {
    private ArrayList<Row> rows;

    public Rows() {
        this.rows = new ArrayList<>();
    }

    public void addRow(Row r) {
        rows.add(r);
    }

    public JSONArray toJSON() {
        JSONArray ret = new JSONArray();
        for (Row r : rows) {
            ret.put(r.toJSON());
        }
        return ret;
    }

    public ArrayList<Row> getRows() {
        return rows;
    }

    public Set<Value> unique(int index) {
        Set<Value> ret = new TreeSet<>();
        for (Row row : rows) {
            ret.add(row.byIndex(index));
        }
        return ret;
    }
}
