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
    private String table;
    private String column;
    private String function;
    private Object value[];
    private ArrayList<Where> children;

    public static Where equals() {
        return operator(WhereOp.EQUALS);
    }

    public static Where gt() {
        return operator(WhereOp.GREATER_THAN);
    }

    public static Where gte() {
        return operator(WhereOp.GREATER_THAN_EQUALS);
    }

    public static Where operator(WhereOp op) {
        Where w = new Where();
        w.op = op;
        return w;
    }

    public static Where or(Where... child) {
        Where w = new Where();
        w.op = WhereOp.OR;
        w.children = new ArrayList<>(Arrays.asList(child));
        return w;
    }

    public static Where and(Where... child) {
        Where w = new Where();
        w.op = WhereOp.AND;
        w.children = new ArrayList<>(Arrays.asList(child));
        return w;
    }

    public Where column(String column) {
        this.column = column;
        return this;
    }

    public Where table(String table) {
        this.table = table;
        return this;
    }

    public Where function(String fn) {
        this.function = fn;
        return this;
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

    public Where value(Object... values) {
        this.value = values;
        return this;
    }

    public Where where(Where w) {
        children.add(w);
        return this;
    }

    public boolean hasChildren() {
        return children.size() > 0;
    }

    String exp() {
        if (children != null) {
            List<String> childExps = new ArrayList<>();
            for (Where child : children) {
                childExps.add(child.exp());
            }
            return "(" + StringUtils.join(childExps, " " + op.sqlOp + " ") + ")";
        }
        switch (op) {
            case EQUALS:
            case NOT_EQUALS:
            case GREATER_THAN:
            case GREATER_THAN_EQUALS:
            case LESS_THAN:
            case LESS_THAN_EQUALS:
                return StringUtils.join(Collections.nCopies(value.length, leftExpr() + op.sqlOp + "?"), " AND ");
            case IN:
            case NOT_IN:
                return leftExpr() + " " + op.sqlOp + " (" + StringUtils.join(Collections.nCopies
                        (value.length,
                        "?"), ",") + ")";
            default:
                return null;
        }
    }

    private String leftExpr() {
        String ret = "`" + column + "`";
        if (table != null) {
            ret = "`" + table + "`." + ret;
        }
        if (function != null) {
            ret = function + "(" + ret + ")";
        }
        return ret;
    }

    Object[] values() {
        if (children != null) {
            ArrayList<Object> ret = new ArrayList<>();
            for (Where child : children) {
                ret.addAll(Arrays.asList(child.values()));
            }
            return ret.toArray();
        }
        Object[] ret = new Object[value.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = value[i];
        }
        return ret;
    }
}
