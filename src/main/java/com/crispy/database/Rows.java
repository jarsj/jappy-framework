package com.crispy.database;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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

    public List<Long> longs(String name) {
        return rows.stream().map(r -> r.byName(name).asLong()).collect(Collectors.toList());
    }

    public List<Integer> ints(String name) {
        return rows.stream().map(r -> r.byName(name).asInt()).collect(Collectors.toList());
    }

    public Set<Value> unique(int index) {
        Set<Value> ret = new TreeSet<>();
        for (Row row : rows) {
            ret.add(row.byIndex(index));
        }
        return ret;
    }
}
