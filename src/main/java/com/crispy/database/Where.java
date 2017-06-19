package com.crispy.database;

import org.apache.commons.lang3.StringUtils;

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
    private Value value[];
    private ArrayList<Where> children;

    public static Where in() {
        return operator(WhereOp.IN);
    }

    public static Where equals() {
        return operator(WhereOp.EQUALS);
    }

    public static Where gt() {
        return operator(WhereOp.GREATER_THAN);
    }

    public static Where gte() {
        return operator(WhereOp.GREATER_THAN_EQUALS);
    }

    public static Where lt() {
        return operator(WhereOp.LESS_THAN);
    }

    public static Where lte() {
        return operator(WhereOp.LESS_THAN_EQUALS);
    }

    public static Where noop() {
        return operator(WhereOp.TRUE);
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

    public static Where isNull() {
        Where w = new Where();
        w.op = WhereOp.IS_NULL;
        w.function = "ISNULL";
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
            case IS_NULL:
                op = WhereOp.IS_NOT_NULL;
                function = "!ISNULL";
                break;
            case IS_NOT_NULL:
                op = WhereOp.IS_NULL;
                function = "ISNULL";
                break;
            default:
                break;
        }
        return this;
    }

    public Where value(Object... values) {
        this.value = new Value[values.length];
        for (int i = 0; i < values.length; i++) {
            this.value[i] = Value.create(values[i]);
        }
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
                String exp = child.exp();
                if (exp != null)
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
                if (value.length == 0)
                    return null;
                return leftExpr() + " " + op.sqlOp + " (" + StringUtils.join(Collections.nCopies
                        (value.length,
                        "?"), ",") + ")";
            case IS_NULL:
            case IS_NOT_NULL:
                return leftExpr();
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

    Object[] values(List<String> tables) {
        if (children != null) {
            ArrayList<Object> ret = new ArrayList<>();
            for (Where child : children) {
                ret.addAll(Arrays.asList(child.values(tables)));
            }
            return ret.toArray();
        }
        if (value == null) {
            return new Object[0];
        }
        SimpleType type = detectType(tables);
        Object[] ret = new Object[value.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = value[i].convert(type);
        }
        return ret;
    }

    private SimpleType detectType(List<String> tables) {
        SimpleType type = columnType(tables);
        if (function == null) {
            return type;
        }
        if (function.equals("SUM"))
            return type;
        if (function.equals("AVG"))
            return type;
        if (function.equals("COUNT"))
            return SimpleType.INTEGER;
        if (function.equals("DATE"))
            return SimpleType.DATE;
        if (function.equals("DATETIME"))
            return SimpleType.DATETIME;
        throw new UnsupportedOperationException("Function " + function + " is not supported");
    }

    private SimpleType columnType(List<String> tables) {
        if (table != null) {
            Column c = DB.getMetadata(table).getColumn(column);
            return c.internalSimpleType();
        }
        Column c = null;
        int count = 0;
        for (String table : tables) {
            Column temp = DB.getMetadata(table).getColumn(column);
            if (temp != null) {
                count++;
                c = temp;
            }
        }
        if (count == 0) {
            throw new IllegalStateException("Column " + column + " doesn't exist in any table");
        } else if (count == 1) {
            return c.internalSimpleType();
        } else {
            throw new IllegalStateException("Column " + column + " is ambigiuous");
        }
    }
}
