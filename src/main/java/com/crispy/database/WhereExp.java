package com.crispy.database;

import org.apache.commons.lang.StringUtils;

import java.util.Collections;

/**
 * Created by harsh on 1/18/16.
 */
public class WhereExp {
    String exp;
    Object values[];

    static WhereExp operator(String table, WhereOp op, String column, Object value) {
        WhereExp where = new WhereExp();
        where.exp = "`" + table + "`.`" + column + "`" + op.sqlOp() + "?";
        where.values = new Object[1];
        where.values[0] = value;
        return where;
    }

    static WhereExp or(String table, String column, Object value[]) {
        WhereExp where = new WhereExp();
        where.exp = "(" + StringUtils.join(Collections.nCopies(value.length, table + ".`" + column + "`=?"), " OR" +
                " ") + ")";
        where.values = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            where.values[i] = value[i];
        }
        return where;
    }

    static WhereExp in(String table, String column, Object value[]) {
        WhereExp where = new WhereExp();
        where.exp = table + ".`" + column + "` IN (" + StringUtils.join(Collections.nCopies(value.length, "?"),
                ",") + ")";
        where.values = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            where.values[i] = value[i];
        }
        return where;
    }

    static WhereExp notIn(String table, String column, Object value[]) {
        WhereExp where = new WhereExp();
        where.exp = table + ".`" + column + "` NOT IN (" + StringUtils.join(Collections.nCopies(value.length,
                "?"), ",") + ")";
        where.values = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            where.values[i] = value[i];
        }
        return where;
    }

    static WhereExp matchAgainst(String table, String[] columns, String[] value, MatchMode mode) {
        WhereExp where = new WhereExp();
        StringBuilder match = new StringBuilder(table + ".`" + columns[0] + "`");
        for (int i = 1; i < columns.length; i++) {
            match.append("," + table + ".`" + columns[i] + "`");
        }
        where.exp = "MATCH (" + match.toString() + ") AGAINST (" + StringUtils.join(Collections.nCopies(value
                .length, "?"), " ") + " "
                + mode.sqlMatchMode() + ")";
        where.values = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            where.values[i] = value[i];
        }
        return where;
    }
}
