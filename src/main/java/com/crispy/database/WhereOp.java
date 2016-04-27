package com.crispy.database;

/**
 * Created by harsh on 1/18/16.
 */
public enum WhereOp {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    LIKE(" LIKE "),
    GREATER_THAN_EQUALS(">="),
    LESS_THAN_EQUALS("<="),
    AND("AND"),
    OR("OR"),
    IN("IN"),
    NOT_IN("NOT IN"),
    MATCH("MATCH"),
    IS_NULL("ISNULL"),
    IS_NOT_NULL("!ISNULL");

    String sqlOp;

    private WhereOp(String op) {
        sqlOp = op;
    }

    public String sqlOp() {
        return sqlOp;
    }
}
