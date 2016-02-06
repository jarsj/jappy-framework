package com.crispy.database;

import com.crispy.log.Log;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by harsh on 1/18/16.
 */
public class Select {
    private static final Log LOG = Log.get("jappy.database");

    /**
     * Type of join. Immaterial if it's
     * just a single table.
     */
    private JoinType joinType;
    private ArrayList<String> tables;

    // Are we getting random elements.
    private boolean random;

    private Where rootWhere;
    private ArrayList<String> columnExprs;
    private ArrayList<String> columnAliases;

    private int start;
    private int limit;

    private ArrayList<String> orderBy;
    private String groupBy;

    private Select() {
        this.tables = new ArrayList<>();
        this.columnExprs = new ArrayList<>();
        this.columnAliases = new ArrayList<>();
        this.random = false;

        this.rootWhere = Where.and();
        this.start = 0;
        this.limit = -1;
        this.orderBy = new ArrayList<>();
        this.groupBy = null;
    }

    public static Select withTable(String... table) {
        Select s = new Select();
        s.tables.addAll(Arrays.asList(table));
        return s;
    }

    public Select where(Where w) {
        rootWhere.where(w);
        return this;
    }

    public Select function(String fnName, String column, String alias) {
        checkColumn(column, false);
        columnExprs.add(fnName + "(`" + column + "`)");
        columnAliases.add(alias);
        return this;
    }

    public Select count(String column, String alias) {
        return function("COUNT", column, alias);
    }

    public Select count(String alias) {
        columnExprs.add("COUNT(*)");
        columnAliases.add(alias);
        return this;
    }

    public Select function(String fnName, String table, String column, String alias) {
        checkColumn(column, false);
        columnExprs.add(fnName + "(`" + table + "`.`" + column + "`)");
        columnAliases.add(alias);
        return this;
    }

    public Select groupBy(String column) {
        groupBy = "`" + column + "`";
        return this;
    }

    public Select groupBy(String table, String column) {
        groupBy = "`" + table + "`.`" + column + "`";
        return this;
    }

    public Select fetchWithAlias(String column, String alias) {
        checkColumn(column, false);
        columnExprs.add("`" + column + "`");
        columnAliases.add(alias);
        return this;
    }

    public Select fetchWithAlias(String table, String column, String alias) {
        Metadata m = DB.getMetadata(table);
        if (!m.containsColumn(column))
            throw new IllegalArgumentException("Column " + column + " is missing");

        columnExprs.add("`" + table + "`.`" + column + "`");
        columnAliases.add(alias);
        return this;
    }



    public Select fetch(String table, String column) {
        Metadata m = DB.getMetadata(table);
        if (!m.containsColumn(column))
            throw new IllegalArgumentException("Column " + column + " is missing");

        columnExprs.add("`" + table + "`.`" + column + "`");
        columnAliases.add("");
        return this;
    }

    public Select fetch(String column) {
        checkColumn(column, true);

        columnExprs.add("`" + column + "`");
        columnAliases.add("");

        return this;
    }

    public Select fetchEverything(String... ignore) {
        for (String t : tables) {
            fetchFromTable(t, ignore);
        }
        return this;
    }

    public Select fetchFromTable(String table, String... ignore) {
        for (String col : DB.getMetadata(table).columnNames()) {
            if (!ArrayUtils.contains(ignore, col)) {
                columnExprs.add("`" + table + "`.`" + col + "`");
                columnAliases.add("");
            }
        }
        return this;
    }

    private void checkColumn(String column, boolean checkUnique) {
        int found = 0;
        for (String t : tables) {
            Metadata m = DB.getMetadata(t);
            if (m.containsColumn(column)) {
                found++;
            }
        }
        if (found == 0)
            throw new IllegalArgumentException("Column " + column + " doesn't exist in any tables");
        if (checkUnique && found > 1)
            throw new IllegalArgumentException("Column " + column + " exist in more than one tables");

    }

    private PreparedStatement createSelectStatement(Connection con) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");

        if (columnExprs.size() > 0) {
            for (int i = 0; i < columnExprs.size(); i++) {
                String expr = columnExprs.get(i);
                String alias = columnAliases.get(i);
                if (alias.length() == 0) {
                    sb.append(expr);
                } else {
                    sb.append(expr + " AS " + alias);
                }
                if (i < columnExprs.size() - 1) {
                    sb.append(", ");
                }
            }
        } else {
            sb.append("*");
        }

        sb.append(" FROM " + StringUtils.join(tables.stream().map((s) -> {
            return "`" + s + "`";
        }).toArray(), " JOIN "));
        if (tables.size() > 1) {
            sb.append(" ON ");

            ArrayList<String> joinCondition = new ArrayList<String>();
            for (int t = 0; t < tables.size() - 1; t++) {
                String t1 = tables.get(t);
                String t2 = tables.get(t + 1);
                Metadata m1 = DB.getMetadata(t1);
                Metadata m2 = DB.getMetadata(t2);

                Constraint c = Constraint.to(m1.constraints, t2);
                if (c == null)
                    c = Constraint.to(m2.constraints, t1);

                joinCondition.add("`" + c.sourceTable + "`.`" + c.sourceColumn + "`=`" + c.destTable + "`.`" + c
                        .destColumn + "`");
            }
            sb.append("(" + StringUtils.join(joinCondition, " AND ") + ")");
        }

        whereStatement(sb);
        if (groupBy != null) {
            sb.append(" GROUP BY `" + groupBy + "`");
        }

        if (orderBy.size() > 0) {
            sb.append(" ORDER BY " + StringUtils.join(orderBy, ","));
        }
        if (orderBy.size() == 0 && random) {
            sb.append(" ORDER BY RAND()");
        }
        if (limit != -1) {
            if (start != -1) {
                sb.append(" LIMIT " + start + "," + limit);
            } else {
                sb.append(" LIMIT " + limit);
            }
        }

        LOG.debug(sb.toString());
        PreparedStatement pstmt = con.prepareStatement(sb.toString());
        int c = 1;
        whereValues(pstmt, c);
        return pstmt;
    }

    private void whereStatement(StringBuilder sb) {
        if (rootWhere.hasChildren()) {
            rootWhere.assignTables(tables);
            sb.append(" WHERE " + rootWhere.exp());
        }
    }

    private int whereValues(PreparedStatement pstmt, int ctr) throws SQLException {
        if (rootWhere.hasChildren()) {
            Object[] values = rootWhere.values();
            for (Object value : values) {
                pstmt.setObject(ctr++, value);
            }
        }
        return ctr;
    }

    public Row row() {
        Connection con = DB.getConnection();
        try {
            PreparedStatement pstmt = createSelectStatement(con);
            Row ret = null;
            ResultSet results = pstmt.executeQuery();
            if (results.next())
                ret = new Row(results);

            pstmt.close();

            return ret;
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            throw new IllegalStateException(t);
        } finally {
            try {
                con.close();
            } catch (Throwable t) {
            }
        }
    }

    private enum JoinType {
        LEFT, RIGHT, NORMAL;

        public String sqlString() {
            switch (this) {
                case LEFT:
                    return "LEFT JOIN";
                case RIGHT:
                    return "RIGHT JOIN";
                case NORMAL:
                    return "JOIN";
            }
            return null;
        }
    }

    private String tableForColumn(String column) {
        for (String table : tables) {
            Metadata m = DB.getMetadata(table);
            if (m.containsColumn(column))
                return table;
        }
        return null;
    }
}
