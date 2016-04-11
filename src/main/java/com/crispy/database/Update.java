package com.crispy.database;

import com.crispy.log.Log;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by harsh on 4/10/16.
 */
public class Update {

    private static final Log LOG = Log.get("jappy.database");
    private String table;

    private ArrayList<String> columnNames;
    private ArrayList<Value> values;
    private Where whereRoot;

    public static Update withTable(String table) {
        Update u = new Update();
        u.table = table;
        return u;
    }

    private Update() {
        columnNames = new ArrayList<>();
        values = new ArrayList<>();
        whereRoot = Where.and();
    }


    public Update object(String column, Object o) {
        int index = columnNames.indexOf(column);
        if (index == -1) {
            columnNames.add(column);
            values.add(Value.create(o));
        } else {
            values.set(index, Value.create(o));
        }
        return this;
    }

    public void where(Where w) {
        whereRoot.where(w);
    }

    public int execute() {
        Connection con = DB.getConnection();
        try {
            String q = createQuery();
            PreparedStatement pstmt = con.prepareStatement(q);
            setValues(pstmt);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private Object valueForColumn(String column) {
        int index = -1;
        if (columnNames != null) {
            index = columnNames.indexOf(column);
        }
        if (index == -1) {
            throw new IllegalStateException("No Value supplied for the column=" + column);
        }
        Value v = values.get(index);
        Column c = DB.getMetadata(table).getColumn(column);
        return v.convert(c.internalSimpleType());
    }


    private void setValues(PreparedStatement pstmt) throws SQLException {
        int c = 1;
        for (String column : columnNames) {
            pstmt.setObject(c++, valueForColumn(column));
        }
        if (whereRoot.hasChildren()) {
            Object[] values = whereRoot.values(Collections.singletonList(table));
            for (Object value : values) {
                pstmt.setObject(c++, value);
            }
        }
    }

    private String createQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE `" + table + "` SET ");
        ArrayList<String> setConditions = new ArrayList<>();
        for (String column : columnNames) {
            setConditions.add("`" + column + "`=?");
        }
        sb.append(StringUtils.join(setConditions, ","));
        if (whereRoot.hasChildren()) {
            sb.append(" WHERE ");
            sb.append(whereRoot.exp());
        }
        return sb.toString();
    }
}
