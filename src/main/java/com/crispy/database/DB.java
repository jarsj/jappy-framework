package com.crispy.database;

import com.crispy.log.Log;
import org.apache.commons.dbcp.BasicDataSource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DB {
    private static DB INSTANCE = new DB();
    private static Log LOG = Log.get("jappy.db");
    private BasicDataSource mDS;
    private String database;
    private ConcurrentHashMap<String, Metadata> tables;

    private DB() {
        tables = new ConcurrentHashMap<String, Metadata>();
    }

    public static void drop(String host, String database, String user, String password) throws SQLException {
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName("com.mysql.jdbc.Driver");
        bds.setUrl("jdbc:mysql://" + host + "/?zeroDateTimeBehavior=convertToNull");
        bds.setUsername(user);
        bds.setPassword(password);
        bds.setTestOnBorrow(true);
        bds.setValidationQuery("SELECT 1");

        Connection conn = bds.getConnection();
        try {
            Statement s = conn.createStatement();
            s.executeUpdate("DROP DATABASE IF EXISTS " + database);
            s.close();
        } finally {
            conn.close();
        }
        bds.close();
    }

    public static void create(String host, String database, String user, String password) throws SQLException {
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName("com.mysql.jdbc.Driver");
        bds.setUrl("jdbc:mysql://" + host + "/?zeroDateTimeBehavior=convertToNull");
        bds.setUsername(user);
        bds.setPassword(password);
        bds.setTestOnBorrow(true);
        bds.setValidationQuery("SELECT 1");

        Connection conn = bds.getConnection();
        try {
            Statement s = conn.createStatement();
            s.executeUpdate("CREATE DATABASE " + database);
            s.close();
        } finally {
            conn.close();
        }
        bds.close();
    }


    /**
     * Initialize DB. If database doesn't exist it's created
     *
     * @param host
     * @param database
     * @param user
     * @param password
     */
    public static void init(String host, String database, String user, String password) {
        if (INSTANCE.mDS != null) {
            try {
                INSTANCE.mDS.close();
            } catch (Throwable t) {
                LOG.warn("Connection might not have been closed. Potential leak");
            }
        }
        INSTANCE.database = database;
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName("com.mysql.jdbc.Driver");
        bds.setUrl("jdbc:mysql://" + host + "/" + database + "?zeroDateTimeBehavior=convertToNull");
        bds.setUsername(user);
        bds.setPassword(password);
        bds.setTestOnBorrow(true);
        bds.setValidationQuery("SELECT 1");
        INSTANCE.mDS = bds;

        Table.get("_metadata")
                .columns(Column.text("table", 100),
                        Column.mediumtext("metadata")).primary("table")
                .create();
    }

    public static void init(String database, String user, String password) {
        init("localhost", database, user, password);
    }

    public static void shutdown() {
        if (INSTANCE.mDS != null) {
            try {
                INSTANCE.mDS.close();
                Driver d = DriverManager.getDriver("jdbc:mysql://localhost/"
                        + INSTANCE.database);
                DriverManager.deregisterDriver(d);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Load metadata from given table into memory.
     *
     * @param table
     * @return
     * @throws Exception
     */
    static Metadata loadMetadata(String table) throws Exception {
        Connection con = getConnection();
        try {
            Metadata m = new Metadata(table);

            boolean tableExists = false;

            DatabaseMetaData meta = con.getMetaData();
            ResultSet results = meta.getTables(null, null, table, null);
            if (results.next()) {
                tableExists = true;
                if (!table.equals("_metadata")) {
                    Row commentRow = Select.withTable("_metadata")
                            .where(Where.equals("table", table))
                            .row();
                    String comment = null;
                    if (commentRow != null) {
                        comment = commentRow.byName("metadata").asString();
                    }
                    if (comment != null) {
                        try {
                            m.comment = new JSONObject(comment);
                        } catch (JSONException e) {
                            m.comment = new JSONObject();
                        }
                    } else
                        m.comment = new JSONObject();
                }

            }

            if (tableExists) {
                // First let's load columns.
                results = meta.getColumns(null, null, table, null);
                while (results.next()) {
                    m.columns.add(Column.parseResultSet(results));
                }

                results = meta.getImportedKeys(null, null, table);
                while (results.next()) {
                    Constraint c = new Constraint();
                    c.sourceTable = m.name;
                    c.sourceColumn = results.getString("FKCOLUMN_NAME");
                    c.destTable = results.getString("PKTABLE_NAME");
                    c.destColumn = results.getString("PKCOLUMN_NAME");
                    m.constraints.add(c);
                }

                results = meta.getIndexInfo(null, null, table, false, false);
                while (results.next()) {
                    String name = results.getString("INDEX_NAME");
                    if (name.startsWith(m.name + "_"))
                        continue;
                    if (name.equals("PRIMARY")) {
                        if (m.primary == null)
                            m.primary = new Index(null);
                        m.primary.process(results);
                    } else {
                        Index index = Index.findByName(m.indexes, name);
                        if (index == null) {
                            index = new Index(name);
                            m.indexes.add(index);
                        }
                        index.process(results);
                    }
                }

            }

            if (tableExists) {
                INSTANCE.tables.put(table, m);
            } else {
                INSTANCE.tables.remove(table);
            }
            return m;
        } finally {
            con.close();
        }
    }

    public static Metadata getMetadata(String table) {
        return INSTANCE.tables.get(table);
    }

    public static Connection getConnection() {
        try {
            return INSTANCE.mDS.getConnection();
        } catch (Throwable t) {
            LOG.error("Couldn't retrieve connection from datastore", t);
            return null;
        }
    }

    public static Object singleItemQuery(String sql, Object... args)
            throws SQLException {
        Connection con = getConnection();
        try {
            return singleItemQuery(con, sql, args);
        } finally {
            con.close();
        }
    }

    public static Object singleItemQuery(Connection con, String sql,
                                         Object... args) throws SQLException {
        PreparedStatement pstmt = con.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            pstmt.setObject(i + 1, args[i]);
        }
        ResultSet results = pstmt.executeQuery();
        if (!results.next())
            return null;
        Object ret = results.getObject(1);
        pstmt.close();
        return ret;
    }

    public static void updateQuery(Connection con, String sql, Object... args)
            throws SQLException {
        LOG.trace(sql);
        PreparedStatement pstmt = con.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            pstmt.setObject(i + 1, args[i]);
        }
        pstmt.executeUpdate();
        pstmt.close();
    }

    public static void updateQuery(String sql, Object... args)
            throws SQLException {
        System.out.println(sql);
        Connection con = getConnection();
        try {
            updateQuery(con, sql, args);
        } finally {
            con.close();
        }
    }

    public static String date(Date d) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(d);
    }

    public static List<Object> listQuery(String sql, Object... args)
            throws SQLException {
        Connection con = getConnection();
        try {
            ArrayList<Object> ret = new ArrayList<Object>();
            PreparedStatement pstmt = con.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                pstmt.setObject(i + 1, args[i]);
            }
            ResultSet results = pstmt.executeQuery();
            while (results.next()) {
                ret.add(results.getObject(1));
            }
            return ret;
        } finally {
            con.close();
        }
    }

    public static String formatAsDateTime(Date d) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:MM:SS");
        String ret = format.format(d);
        if (ret.equals("0000-00-00 00:00:00"))
            return null;
        return ret;
    }

    public static String formatAsDateTime(java.sql.Date d) {
        return formatAsDateTime(new Date(d.getTime()));
    }

    public static String formatAsDate(Calendar c) {
        return formatAsDate(c.getTime());
    }

    public static String formatAsDate(Timestamp t) {
        return formatAsDate(new Date(t.getTime()));
    }

    public static String formatAsDate(java.sql.Date day) {
        return formatAsDate(new Date(day.getTime()));
    }

    public static String formatAsDate(Date day) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String ret = format.format(day);
        if (ret.equals("0000-00-00"))
            return null;
        return ret;
    }

    public static String formatAsTime(Date d) {
        SimpleDateFormat format = new SimpleDateFormat("HH:MM:SS");
        String ret = format.format(d);
        return ret;
    }

    public static List<Metadata> getTables() {
        return new ArrayList<Metadata>(INSTANCE.tables.values());
    }

    public static JSONArray query(String sql, Function<ResultSet, JSONObject> fn, Object ... args) {
        JSONArray ret = new JSONArray();
        Connection con = DB.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                pstmt.setObject(i + 1, args[i]);
            }
            ResultSet results = pstmt.executeQuery();
            while (results.next()) {
                ret.put(fn.apply(results));
            }
            return ret;

        } catch (SQLException e) {
            throw new IllegalArgumentException("Check your query", e);
        } finally {
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public boolean tableExists(String table) throws SQLException {
        Connection con = getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("SHOW TABLES");
            ResultSet results = pstmt.executeQuery();
            while (results.next()) {
                String tableName = results.getString(1);
                if (tableName.equals(table))
                    return true;
            }
            return false;
        } finally {
            con.close();
        }
    }
}
