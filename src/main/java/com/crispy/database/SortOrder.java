package com.crispy.database;

/**
 * Created by harsh on 5/16/16.
 */
public enum SortOrder {
    ASCENDING,
    DESCENDING;

    public String sqlString() {
        switch (this) {
            case ASCENDING: return "ASC";
            case DESCENDING: return "DESC";
        }
        return "DESC";
    }
}
