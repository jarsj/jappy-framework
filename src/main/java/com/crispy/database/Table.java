package com.crispy.database;

import com.crispy.log.Log;
import com.crispy.server.Params;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

public class Table {
    private static final Log LOG = Log.get("jappy.db");
    private JoinType joinType;
    private ArrayList<Table> joins;
    private boolean random;
    private boolean unique;
    private String name;

    private ArrayList<WhereExp> where;
    private ArrayList<String> columnNames;
    private ArrayList<String> columnFunctions;
    private ArrayList<String> columnAliases;

    private ArrayList<String> overwriteColumns;

    private ArrayList<Column> newColumns;
    private ArrayList<Index> newIndexes;
    private ArrayList<Constraint> newConstraints;
    private ArrayList<UpdateExp> increments;
    private HashMap<String, Predicate<Object>> validators;
    private Index newPrimaryKey;

    private ArrayList<Object> values;
    private boolean deleteOldColumns;
    private int limit;
    private String[] orderBy;
    private JSONObject comment;
    private boolean ignore;
    private int start;
    private boolean ignoreNull;
    private String groupBy;
    private String functionName;
    private String functionColumn;
    private long genId;
    private TreeSet<String> distincts;

    private Table(String name) {
        deleteOldColumns = false;

        random = false;
        unique = false;
        ignore = false;
        joinType = JoinType.NORMAL;
        limit = -1;
        start = -1;
        this.name = name;
        orderBy = new String[0];
        joins = new ArrayList<Table>();
        validators = new HashMap<String, Predicate<Object>>();
        where = new ArrayList<WhereExp>();
        comment = new JSONObject();
    }

    public static Table get(String name) {
        Table t = new Table(name);
        return t;
    }

    private static String safeJoin(List<String> e) {
        List<String> temp = new ArrayList<String>();
        for (String s : e) {
            temp.add("`" + s + "`");
        }
        return StringUtils.join(temp, ',');
    }

    public Table overwrite(String... column) {
        if (this.overwriteColumns == null)
            this.overwriteColumns = new ArrayList<String>();
        this.overwriteColumns.addAll(Arrays.asList(column));
        return this;
    }

    public Table deleteOldColumns() {
        deleteOldColumns = true;
        return this;
    }

    public Table sum(String... name) {
        if (columnNames == null) {
            columnNames = new ArrayList<String>();
            columnFunctions = new ArrayList<String>();
        }
        columnFunctions.addAll(Collections.nCopies(name.length, "SUM"));
        columnNames.addAll(Arrays.asList(name));
        return this;
    }

    public Table max(String... name) {
        if (columnNames == null) {
            columnNames = new ArrayList<String>();
            columnFunctions = new ArrayList<String>();
        }
        columnFunctions.addAll(Collections.nCopies(name.length, "MAX"));
        columnNames.addAll(Arrays.asList(name));
        return this;
    }

    /**
     * Using weird name due to conflict with COUNT function. We need to get rid of that.
     *
     * @param name
     * @return
     */
    public Table cnt(String... name) {
        if (columnNames == null) {
            columnNames = new ArrayList<String>();
            columnFunctions = new ArrayList<String>();
        }
        columnFunctions.addAll(Collections.nCopies(name.length, "COUNT"));
        columnNames.addAll(Arrays.asList(name));
        return this;
    }

    public Table avg(String... name) {
        if (columnNames == null) {
            columnNames = new ArrayList<String>();
            columnFunctions = new ArrayList<String>();
        }
        columnFunctions.addAll(Collections.nCopies(name.length, "AVG"));
        columnNames.addAll(Arrays.asList(name));
        return this;
    }

    public Table distinct(String column) {
        if (distincts == null) {
            distincts = new TreeSet<String>();
        }
        distincts.add(column);
        return this;
    }

    public Table values(Params params) {
        for (String key : params.keys()) {
            value(key, params.get(key));
        }
        return this;
    }

    public Table value(String column, Object value) {
        if (columnNames == null) {
            columnNames = new ArrayList<String>();
            columnFunctions = new ArrayList<String>();
            values = new ArrayList<Object>();
        }
        // Don't add a column twice. Perhaps we should warn here, but I am not
        // sure.
        if (columnNames.contains(column))
            return this;
        columnNames.add(column);
        columnFunctions.add("");
        Metadata m = DB.getMetadata(name);

        Column c = m.getColumn(column);
        if (c == null) {
            throw new IllegalStateException("Missing column " + column + " in table " + name);
        }
        values.add(c.parseObject(value));
        return this;
    }

    public Table columns(Column... c) {
        newColumns = new ArrayList<Column>();
        newColumns.addAll(Arrays.asList(c));
        for (Column cc : newColumns) {
            if (cc.autoIncrement) {
                newPrimaryKey = new Index(null, cc.name);
                newPrimaryKey.isAuto = true;
            }
        }
        return this;
    }

    public Table indexes(Index... i) {
        newIndexes = new ArrayList<Index>();
        newIndexes.addAll(Arrays.asList(i));
        return this;
    }

    public Table constraints(Constraint... c) {
        newConstraints = new ArrayList<Constraint>();
        newConstraints.addAll(Arrays.asList(c));
        for (Constraint nc : newConstraints)
            nc.sourceTable = name;
        return this;
    }

    public Table primary(String... name) {
        if (newPrimaryKey != null)
            throw new IllegalStateException("Already got one primary key");
        newPrimaryKey = new Index(null, name);
        return this;
    }

    public Column columnByName(String name) {
        for (Column c : newColumns) {
            if (c.name.equals(name))
                return c;
        }
        return null;
    }

    public void create() {
        try {
            LOG.debug("create " + name);
            DB.loadMetadata(name);
            // If table does not exist.
            Metadata m = DB.getMetadata(name);
            if (m == null) {
                List<String> defs = new ArrayList<String>();
                for (Column column : newColumns) {
                    defs.addAll(Arrays.asList(column.createDefinitions()));
                }
                DB.updateQuery("CREATE TABLE `" + name + "` (" + StringUtils.join(defs, ',') + ")");
                if (!name.equals("_metadata")) {
                    Table.get("_metadata")
                            .value("table", name)
                            .value("metadata", comment.toString())
                            .overwrite("metadata").add();
                }
            } else {
                for (Column column : newColumns) {
                    Column oldColumn = Column.findByName(m.columns, column.name);
                    if (oldColumn == null) {
                        DB.updateQuery("ALTER TABLE `" + name + "` ADD COLUMN " + column.createDefinitions());
                    } else {
                        if (!oldColumn.equals(column)) {
                            DB.updateQuery("ALTER TABLE `" + name + "` MODIFY COLUMN " + column.createDefinitions());
                        }
                    }
                }

                if (deleteOldColumns) {
                    for (Column oldColumn : m.columns) {
                        Column newColumn = Column.findByName(newColumns, oldColumn.name);
                        if (newColumn == null) {
                            DB.updateQuery("ALTER TABLE `" + name + "` DROP COLUMN `" + oldColumn.name + "`");
                        }
                    }
                }

                if (!m.comment.toString().equals(comment.toString())) {
                    JSONObject merged = new JSONObject(m.comment.toString());
                    Iterator<String> keys = comment.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        merged.put(key, comment.get(key));
                    }
                    Table.get("_metadata")
                            .value("table", name)
                            .value("metadata", merged.toString())
                            .overwrite("metadata")
                            .add();
                }
            }

            if (newIndexes == null)
                newIndexes = new ArrayList<Index>();

            for (Index i : newIndexes) {
                Index oldIndex = (m == null) ? null : m.getIndex(i.name);
                if (oldIndex == null) {
                    DB.updateQuery("ALTER TABLE `" + name + "` ADD " + i.createDefinition());
                } else if (!oldIndex.equals(i)) {
                    DB.updateQuery("ALTER TABLE `" + name + "` DROP INDEX `" + oldIndex.name + "`");
                    DB.updateQuery("ALTER TABLE `" + name + "` ADD " + i.createDefinition());
                    LOG.info("CREATING NEW INDEX " + i.createDefinition());
                }
            }

            if (m != null) {
                for (Index i : m.indexes) {
                    Index newIndex = Index.findByName(newIndexes, i.name);
                    if (newIndex == null) {
                        DB.updateQuery("ALTER TABLE `" + name + "` DROP INDEX `" + i.name + "`");
                    }
                }
            }

            if (newConstraints == null)
                newConstraints = new ArrayList<Constraint>();

            for (Constraint c : newConstraints) {
                Constraint old = (m == null) ? null : m.getConstraint(c.sourceColumn);
                if (old == null) {
                    c.create(name);
                } else if (!old.equals(c)) {
                    old.drop();
                    c.create(name);
                }
            }

            if (m != null) {
                for (Constraint c : m.constraints) {
                    boolean found = false;
                    for (Constraint newC : newConstraints) {
                        if (newC.sourceColumn.equals(c.sourceColumn)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        c.drop();
                    }
                }
            }

            if (newPrimaryKey != null) {
                Index oldPrimary = (m == null) ? null : m.primary;
                if (oldPrimary == null) {
                    if (!newPrimaryKey.isAuto)
                        DB.updateQuery("ALTER TABLE `" + name + "` ADD PRIMARY KEY " + newPrimaryKey.createDefinition
								());
                } else if (!oldPrimary.equals(newPrimaryKey)) {
                    DB.updateQuery("ALTER TABLE `" + name + "` DROP PRIMARY KEY");
                    if (!newPrimaryKey.isAuto)
                        DB.updateQuery("ALTER TABLE `" + name + "` ADD PRIMARY KEY " + newPrimaryKey.createDefinition
								());
                }
            } else {
                if (m != null) {
                    if (m.primary != null) {
                        DB.updateQuery("ALTER TABLE `" + name + "` DROP PRIMARY KEY");
                    }
                }
            }

            // Reload and reorder metadata
            DB.loadMetadata(name).reorderAndRetain(newColumns);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            throw new IllegalStateException(t);
        }

    }

    public Table increment(String... columns) {
        if (this.increments == null) {
            this.increments = new ArrayList<Table.UpdateExp>();
        }
        for (String col : columns) {
            UpdateExp ue = new UpdateExp();
            ue.column = col;
            ue.amount = 1;
            this.increments.add(ue);
        }
        return this;
    }

    public Table add(String column, long value) {
        if (this.increments == null)
            this.increments = new ArrayList<Table.UpdateExp>();
        UpdateExp ue = new UpdateExp();
        ue.column = column;
        ue.amount = value;
        this.increments.add(ue);
        return this;
    }

    public Table decrement(String... columns) {
        if (this.increments == null) {
            this.increments = new ArrayList<Table.UpdateExp>();
        }
        for (String col : columns) {
            UpdateExp ue = new UpdateExp();
            ue.column = col;
            ue.amount = -1;
            this.increments.add(ue);
        }
        return this;
    }



    public Table values(Object... values) {
        this.values = new ArrayList<Object>();
        Metadata m = DB.getMetadata(name);

        for (int i = 0; i < values.length; i++) {
            String columnName = columnNames.get(i);
            Column c = Column.findByName(m.columns, columnName);
            if (c == null) {
                throw new IllegalStateException("Missing column " + columnName + " in table " + name);
            }
            this.values.add(c.parseObject(values[i]));
        }
        return this;
    }

    public Table ignore() {
        ignore = true;
        return this;
    }

    public Table ignoreNull() {
        ignoreNull = true;
        return this;
    }

    public Table random() {
        this.random = true;
        return this;
    }

    public void update() {
        Connection con = DB.getConnection();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE");
            if (ignore) {
                sb.append(" IGNORE");
            }
            sb.append(" `" + name + "` SET ");
            ArrayList<String> updates = new ArrayList<String>();
            if (columnNames != null) {
                for (int c = 0; c < columnNames.size(); c++) {
                    if (!ignoreNull || values.get(c) != null)
                        updates.add("`" + columnNames.get(c) + "`=?");
                }
            }
            if (increments != null) {
                for (UpdateExp ue : increments) {
                    updates.add(String.format("`%s`=`%s`+?", ue.column, ue.column));
                }
            }
            sb.append(StringUtils.join(updates, ','));

            whereStatement(sb);
            LOG.debug("update=" + sb.toString());
            PreparedStatement pstmt = con.prepareStatement(sb.toString());
            int c = 1;

            if (columnNames != null) {
                for (int v = 0; v < values.size(); v++) {
                    if (!ignoreNull || values.get(v) != null)
                        pstmt.setObject(c++, values.get(v));
                }
            }
            if (increments != null) {
                for (UpdateExp ue : increments) {
                    pstmt.setLong(c++, ue.amount);
                }
            }

            whereValues(pstmt, c);

            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    private Object valueForColumn(String column) {
        int index = -1;
        if (columnNames != null) {
            index = columnNames.indexOf(column);
        }
        if (index == -1) {
            throw new IllegalStateException("No Value supplied for the column=" + column);
        }
        return values.get(index);
    }

    @Deprecated
    public Table add() {
        Connection con = DB.getConnection();
        try {
            Metadata myMetadata = DB.getMetadata(name);

            List<String> myColumnNames = columnNames;

            if (overwriteColumns != null) {
                overwriteColumns.retainAll(myColumnNames);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT " + (((overwriteColumns != null) || ignore) ? "IGNORE " : "") + "INTO `" + name + "`(");
            sb.append(safeJoin(myColumnNames));
            sb.append(") VALUES (" + StringUtils.join(Collections.nCopies(myColumnNames.size(), "?"), ',') + ")");

            if (overwriteColumns != null && overwriteColumns.size() > 0) {
                sb.append(" ON DUPLICATE KEY UPDATE ");
                ArrayList<String> updates = new ArrayList<String>();
                for (String column : overwriteColumns) {
                    if (!ignoreNull || valueForColumn(column) != null)
                        updates.add("`" + column + "`=?");
                }
                sb.append(StringUtils.join(updates, ','));
            }

            PreparedStatement pstmt = con.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS);

            int c = 1;
            for (String column : myColumnNames) {
                pstmt.setObject(c++, valueForColumn(column));
            }
            if (overwriteColumns != null && overwriteColumns.size() > 0) {
                for (String column : overwriteColumns) {
                    if (!ignoreNull || valueForColumn(column) != null) {
                        pstmt.setObject(c++, valueForColumn(column));
                    }
                }
            }

            LOG.trace(sb.toString());
            pstmt.executeUpdate();

            genId = -1;

            try {
                ResultSet generated = pstmt.getGeneratedKeys();
                if (generated.next()) {
                    genId = generated.getLong(1);
                }
            } catch (Exception e) {
                LOG.warn("Possibly missing primary key field");
            }

            // Update where, so that call to row after this will reflect what we
            // want
            if (genId == -1) {
                for (String column : myColumnNames) {
                    where(column, valueForColumn(column));
                }
            } else {
                where(myMetadata.getAutoGeneratedColumn(), genId);
            }

            pstmt.close();
            return this;
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            try {
                con.close();
            } catch (SQLException e) {
            }
        }
    }

    public long generatedId() {
        return genId;
    }

    private ArrayList<Table> joinTableList() {
        ArrayList<Table> ret = new ArrayList<Table>();
        ret.add(this);
        for (Table t : joins) {
            ret.addAll(t.joinTableList());
        }
        return ret;
    }

    @Override
    public String toString() {
        return name;
    }

    public void lock() {
        Connection con = DB.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("LOCK TABLE ?");
            pstmt.setString(1, name);
            pstmt.executeQuery();
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        } finally {
            try {
                con.close();
            } catch (Throwable t) {
            }
        }
    }

    public void unlock() {
        Connection con = DB.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("UNLOCK TABLE ?");
            pstmt.setString(1, name);
            pstmt.executeQuery();
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        } finally {
            try {
                con.close();
            } catch (Throwable t) {
            }
        }
    }

    @Deprecated
    private Table where(String column, Object value) {
        return where(column, value, WhereOp.EQUALS);
    }

    @Deprecated
    private Table notIn(String column, Object value[]) throws Exception {
        Metadata m = DB.getMetadata(name);
        Column c = Column.findByName(m.columns, column);
        if (c == null) {
            throw new IllegalStateException("No column exists for " + column + " in table " + name);
        }
        Object[] parsed = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            parsed[i] = c.parseObject(value[i]);
        }
        where.add(WhereExp.notIn(name, column, parsed));
        return this;
    }

    @Deprecated
    private Table in(String column, Object value[]) {
        Metadata m = DB.getMetadata(name);
        Column c = Column.findByName(m.columns, column);
        if (c == null) {
            throw new IllegalStateException("No column exists for " + column + " in table " + name);
        }
        Object[] parsed = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            parsed[i] = c.parseObject(value[i]);
        }
        where.add(WhereExp.in(name, column, parsed));
        return this;
    }

    @Deprecated
    private Table or(String column, Object value[]) {
        Metadata m = DB.getMetadata(name);
        Column c = Column.findByName(m.columns, column);
        if (c == null) {
            throw new IllegalStateException("No column exists for " + column + " in table " + name);
        }
        Object[] parsed = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            parsed[i] = c.parseObject(value[i]);
        }
        where.add(WhereExp.or(name, column, parsed));
        return this;
    }

    @Deprecated
    private Table search(String[] columns, String query, MatchMode mode) {
        Metadata m = DB.getMetadata(name);
        if (columns.length == 0) {
            throw new IllegalStateException("Not a single column available to match");
        }
        for (int i = 0; i < columns.length; i++) {
            Column c = Column.findByName(m.columns, columns[i]);
            if (c == null) {
                throw new IllegalStateException("No column exists for " + c + " in table " + name);
            }
        }
        if (query == null || query.equals("")) {
            throw new IllegalStateException("No keyword available to match against");
        }
        if (mode == null) {
            mode = MatchMode.IN_NATURAL_LANGUAGE_MODE;
        }
        where.add(WhereExp.matchAgainst(name, columns, new String[]{query}, mode));
        return this;
    }

    @Deprecated
    private Table where(String column, Object value, WhereOp op) {
        Metadata m = DB.getMetadata(name);
        if (m == null)
            throw new IllegalStateException("No table exists for " + name);
        Column c = Column.findByName(m.columns, column);
        if (c == null) {
            throw new IllegalStateException("No column exists for " + column + " in table " + name);
        }
        if (c.isCandidateForNullValue(value)) {
            if (op == WhereOp.EQUALS)
                isNull(column);
            else if (op == WhereOp.NOT_EQUALS)
                isNotNull(column);
            else
                throw new IllegalArgumentException("Null value not supported with " + op);
        } else {
            where.add(WhereExp.operator(name, op, column, c.parseObject(value)));
        }
        return this;
    }

    @Deprecated
    private Table limit(int l) {
        limit = l;
        return this;
    }

    /**
     * The column occurs after (o + N units)
     *
     * @param column
     * @param o
     * @return
     */
    @Deprecated
    private Table afterDate(String column, Object o) {
        WhereExp exp = new WhereExp();
        exp.exp = "(DATE(`" + column + "`)>?)";
        exp.values = new Object[]{o};
        where.add(exp);
        return this;
    }

    /**
     * The column occurs after (o + N units)
     *
     * @param column
     * @param o
     * @return
     */
    @Deprecated
    private Table onOrAfterDate(String column, Object o) {
        WhereExp exp = new WhereExp();
        exp.exp = "(DATE(`" + column + "`)>=?)";
        exp.values = new Object[]{o};
        where.add(exp);
        return this;
    }

    @Deprecated
    private Table betweenDates(String column, LocalDate start, LocalDate end) {
        WhereExp exp = new WhereExp();
        exp.exp = "(DATE(`" + column + "`)>=? AND DATE(`" + column + "`)<=?)";
        exp.values = new Object[]{start, end};
        where.add(exp);
        return this;
    }

    @Deprecated
    private Table onDate(String column, LocalDate d) {
        WhereExp exp = new WhereExp();
        exp.exp = "(DATE(`" + column + "`)=?)";
        exp.values = new Object[]{d};
        where.add(exp);
        return this;
    }

    @Deprecated
    private Table beforeDate(String column, Object o) {
        WhereExp exp = new WhereExp();
        exp.exp = "(DATE(`" + column + "`)<?)";
        exp.values = new Object[]{o};
        where.add(exp);
        return this;
    }

    @Deprecated
    private Table onOrBeforeDate(String column, Object o) {
        WhereExp exp = new WhereExp();
        exp.exp = "(DATE(`" + column + "`)<=?)";
        exp.values = new Object[]{o};
        where.add(exp);
        return this;
    }

    public void drop(boolean ignore) {
        try {
            DB.updateQuery("DROP TABLE `" + name + "`");
            DB.loadMetadata(name);
        } catch (Throwable e) {
            if (!ignore)
                throw new IllegalStateException(e);
        }
    }

    public Table ascending(String column) {
        orderBy = (String[]) ArrayUtils.add(orderBy, "`" + column + "` ASC");
        return this;
    }

    public Table descending(String column) {
        orderBy = (String[]) ArrayUtils.add(orderBy, "`" + column + "` DESC");
        return this;
    }

    @Deprecated
    private void delete() {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM `" + name + "`");
        whereStatement(sb);
        Connection con = DB.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement(sb.toString());
            int c = 1;
            whereValues(pstmt, c);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        } finally {
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    private void whereStatement(StringBuilder sb) {
        if (where.size() > 0) {
            sb.append(" WHERE ");
            ArrayList<String> items = new ArrayList<String>();
            for (WhereExp exp : where) {
                items.add(exp.exp);
            }
            sb.append(StringUtils.join(items, " AND "));
        }
    }

    private int whereValues(PreparedStatement pstmt, int ctr) throws SQLException {
        for (WhereExp exp : where) {
            for (Object value : exp.values) {
                pstmt.setObject(ctr++, value);
            }
        }
        return ctr;
    }

    public Table display(String column) throws JSONException {
        comment.put("display", column);
        return this;
    }

    public Table noDataEntry() throws JSONException {
        comment.put("no-data-entry", true);
        return this;
    }

    public Table join(Table t) {
        joins.add(t);
        return this;
    }

    public Table leftJoin(Table t) {
        if (joins.size() != 0)
            throw new IllegalStateException("Left Join only supported with two tables");
        joins.add(t);
        joinType = JoinType.LEFT;
        return this;
    }

    public Table groupBy(String column) {
        this.groupBy = column;
        return this;
    }

    public Table greater(String column, Object value) {
        return where(column, value, WhereOp.GREATER_THAN);
    }

    public Table isNull(String column) {
        WhereExp exp = new WhereExp();
        exp.exp = "ISNULL(`" + column + "`)";
        exp.values = new Object[0];
        where.add(exp);
        return this;
    }

    public Table unique() {
        this.unique = true;
        return this;
    }

    public Table isNotNull(String column) {
        WhereExp exp = new WhereExp();
        exp.exp = "!ISNULL(`" + column + "`)";
        exp.values = new Object[0];
        where.add(exp);
        return this;
    }

    public Table start(int s) {
        start = s;
        if (limit == -1)
            limit = 10;
        return this;
    }

    private void sanityCheck() {
        Metadata m = DB.getMetadata(name);
        if (m == null)
            throw new IllegalStateException("Missing table " + name);
        for (int i = 1; i < joins.size(); i++) {
            Table joinTable = joins.get(i);
            Metadata joinMetadata = DB.getMetadata(joinTable.name);
            if (joinMetadata == null) {
                throw new IllegalStateException("Missing join table " + joinTable.name);
            }
        }
    }

    /**
     * Validate column against the given function before adding the value.
     *
     * @param column
     * @param f
     * @return
     */
    public Table validate(String column, Predicate<Object> f) {
        validators.put(column, f);
        return this;
    }

    public String getName() {
        return name;
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


    static class UpdateExp {
        String column;
        long amount;
    }
}
