package com.crispy.database;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by harsh on 2/6/16.
 */
public class Where {
    private WhereOp op;
    private String column;
    private String table;
    private Object value[];
    private ArrayList<Where> children;

    public static Where equals(String column, Object value) {
        Where w = new Where();
        w.op = WhereOp.EQUALS;
        w.column = column;
        w.value = new Object[]{value};
        return w;
    }

    public Where not() {
        switch (op) {
            case EQUALS:
                op = WhereOp.NOT_EQUALS;
                break;
            case GREATER_THAN:
                op = WhereOp.LESS_THAN;
                break;
            case GREATER_THAN_EQUALS:
                op = WhereOp.LESS_THAN_EQUALS;
                break;
            case IN:
                op = WhereOp.NOT_IN;
                break;
            case LESS_THAN:
                op = WhereOp.GREATER_THAN;
                break;
            case LESS_THAN_EQUALS:
                op = WhereOp.GREATER_THAN_EQUALS;
                break;
            case NOT_EQUALS:
                op = WhereOp.EQUALS;
                break;
            case NOT_IN:
                op = WhereOp.IN;
                break;
            default:
                break;
        }
        return this;
    }

    public static Where or(Where ... child) {
        Where w = new Where();
        w.op = WhereOp.OR;
        w.children = new ArrayList<>(Arrays.asList(child));
        return w;
    }

    public static Where in(String column, Object ... values) {
        Where w = new Where();
        w.op = WhereOp.IN;
        w.value = values;
        return w;
    }

    public static Where and(Where ... child) {
        Where w = new Where();
        w.op = WhereOp.AND;
        w.children = new ArrayList<>(Arrays.asList(child));
        return w;
    }

    public Where where(Where w) {
        children.add(w);
        return this;
    }

    public boolean hasChildren() {
        return children.size() > 0;
    }

    void assignTables(ArrayList<String> tables) {
        if (table == null) {
            for (String t : tables) {
                Metadata m = DB.getMetadata(t);
                if (m.containsColumn(column)) {
                    if (table != null) {
                        throw new IllegalArgumentException("Column " + column + " is ambiguous in where expression");
                    }
                    table = t;
                }
            }
        }
        if (children != null) {
            for (Where child : children) {
                child.assignTables(tables);
            }
        }
    }

    String exp() {
        if (children != null) {
            List<String> childExps = new ArrayList<>();
            for (Where child : children) {
                childExps.add(child.exp());
            }
            return "(" + StringUtils.join(childExps, " " + op.sqlOp + " ") + ")";
        }
        if (table == null) {
            throw new IllegalStateException("Table can't be null. Forgot to call assignTables ?");
        }
        switch (op) {
            case EQUALS:
            case NOT_EQUALS:
            case GREATER_THAN:
            case GREATER_THAN_EQUALS:
            case LESS_THAN:
            case LESS_THAN_EQUALS:
                return "`" + table + "`.`" + column + "`" + op.sqlOp + "?";
            case IN:
            case NOT_IN:
                return "`" + table + "`.`" + column + "` " + op.sqlOp + " (" + StringUtils.join(Collections.nCopies(value.length,
                        "?"), ",") + ")";
            default:
                return null;
        }
    }

    Object[] values() {
        if (children != null) {
            ArrayList<Object> ret = new ArrayList<>();
            for (Where child : children) {
                ret.addAll(Arrays.asList(child.values()));
            }
            return ret.toArray();
        }
        Column c = DB.getMetadata(table).getColumn(column);
        Object[] ret = new Object[value.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = c.parseObject(value[i]);
        }
        return ret;
    }
}
