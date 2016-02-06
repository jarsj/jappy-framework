package com.crispy.database;

/**
 * Created by harsh on 1/18/16.
 */
public enum MatchMode {
    IN_BOOLEAN_MODE("IN BOOLEAN MODE"), IN_NATURAL_LANGUAGE_MODE("IN NATURAL LANGUAGE MODE"),
    WITH_QUERY_EXPANSION("WITH QUERY EXPANSION");

    String mode;

    private MatchMode(String mode) {
        this.mode = mode;
    }

    public String sqlMatchMode() {
        return mode;
    }
}
