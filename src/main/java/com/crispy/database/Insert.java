package com.crispy.database;

import com.crispy.log.Log;
import org.apache.commons.lang.StringUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by harsh on 2/9/16.
 */
public class Insert {
    private static final Log LOG = Log.get("jappy.database");
    private String table;

    private ArrayList<String> columnNames;
    private ArrayList<Value> values;
    private boolean ignore;
    private ArrayList<String> overwriteColumns;

    public static Insert withTable(String table) {
        Insert i = new Insert();
        i.table = table;
        return i;
    }

    private Insert() {
        columnNames = new ArrayList<>();
        values = new ArrayList<>();
        ignore = false;
        overwriteColumns = new ArrayList<>();
    }

    public Insert object(String column, Object o) {
        int index = columnNames.indexOf(column);
        if (index == -1) {
            columnNames.add(column);
            values.add(Value.create(o));
        } else {
            values.set(index, Value.create(o));
        }
        return this;
    }

    public Insert remove(String column) {
        int index = columnNames.indexOf(column);
        if (index != -1) {
            columnNames.remove(index);
            values.remove((int) index);
        }
        overwriteColumns.remove(column);
        return this;
    }

    public Insert overwrite(String ... column) {
        overwriteColumns.addAll(Arrays.asList(column));
        return this;
    }

    public Insert ignore() {
        this.ignore = true;
        return this;
    }

    private static String safeJoin(List<String> e) {
        List<String> temp = new ArrayList<String>();
        for (String s : e) {
            temp.add("`" + s + "`");
        }
        return StringUtils.join(temp, ',');
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

    private void validate() {
        if (!columnNames.containsAll(overwriteColumns)) {
            throw new IllegalStateException("You are overwriting atleast one column that doesn't exist");
        }
    }

    private String createQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT " + (((overwriteColumns.size() > 0) || ignore) ? "IGNORE " : "") + "INTO `" + table + "`(");
        sb.append(safeJoin(columnNames));
        sb.append(") VALUES (" + StringUtils.join(Collections.nCopies(columnNames.size(), "?"), ',') + ")");

        if (overwriteColumns != null && overwriteColumns.size() > 0) {
            sb.append(" ON DUPLICATE KEY UPDATE ");
            ArrayList<String> updates = new ArrayList<String>();
            for (String column : overwriteColumns) {
                updates.add("`" + column + "`=?");
            }
            sb.append(StringUtils.join(updates, ','));
        }
        return sb.toString();
    }

    private void setValues(PreparedStatement pstmt) throws SQLException {
        int c = 1;
        for (String column : columnNames) {
            pstmt.setObject(c++, valueForColumn(column));
        }
        if (overwriteColumns != null && overwriteColumns.size() > 0) {
            for (String column : overwriteColumns) {
                pstmt.setObject(c++, valueForColumn(column));
            }
        }
    }

    public int execute() {
        validate();
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

    public Row executeAndFetch() {
        validate();
        Connection con = DB.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement(createQuery(), Statement.RETURN_GENERATED_KEYS);
            setValues(pstmt);
            pstmt.executeUpdate();
            Row ret = null;
            try {
                ResultSet generated = pstmt.getGeneratedKeys();
                if (generated.next()) {
                    ret = new Row(generated);
                }
                generated.close();
            } catch (Exception e) {
                LOG.warn("Possibly missing primary key field");
            }

            return ret;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                con.close();
            } catch (SQLException e) {
            }
        }
    }


}
