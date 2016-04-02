package com.crispy.database;

import com.crispy.log.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by harsh on 4/1/16.
 */
public class Delete {
    private static final Log LOG = Log.get("jappy.database");
    private String table;
    private Where rootWhere;

    private Delete() {
        this.rootWhere = Where.and();
    }

    public static Delete withTable(String table) {
        Delete d = new Delete();
        d.table = table;
        return d;
    }

    public Delete where(Where w) {
        rootWhere.where(w);
        return this;
    }

    public int execute() {
        Connection con = DB.getConnection();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE FROM `" + table + "`");
            if (rootWhere.hasChildren()) {
                sb.append(" WHERE " + rootWhere.exp());
            }
            PreparedStatement pstmt = con.prepareStatement(sb.toString());
            if (rootWhere.hasChildren()) {
                Object[] values = rootWhere.values();
                for (int i = 0; i < values.length; i++) {
                    pstmt.setObject(i + 1, values[i]);
                }
            }
            int ret = pstmt.executeUpdate();
            pstmt.close();
            return ret;
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        } finally {
            try {
                con.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

}
