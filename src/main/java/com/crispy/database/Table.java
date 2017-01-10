package com.crispy.database;

import com.crispy.log.Log;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Table {
    private static final Log LOG = Log.get("jappy.db");

    private String name;

    private ArrayList<Column> newColumns;
    private ArrayList<Index> newIndexes;
    private ArrayList<Constraint> newConstraints;

    private Index newPrimaryKey;

    private ArrayList<Object> values;
    private boolean deleteOldColumns;


    private JSONObject comment;

    private Table(String name) {
        deleteOldColumns = false;
        this.name = name;
        comment = new JSONObject();
    }

    public static Table get(String name) {
        Table t = new Table(name);
        return t;
    }

    public Table deleteOldColumns() {
        deleteOldColumns = true;
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
                    DB.updateQuery("INSERT INTO `_metadata`(`table`, `metadata`) VALUES (?,?) ON DUPLICATE KEY UPDATE" +
                            " `metadata`=?", name, comment.toString(), comment.toString());
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
                    DB.updateQuery("INSERT INTO `_metadata`(`table`, `metadata`) VALUES (?,?) ON DUPLICATE KEY UPDATE" +
                            " `metadata`=?", name, merged.toString(), merged.toString());
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


    public void drop(boolean ignore) {
        try {
            DB.updateQuery("DROP TABLE `" + name + "`");
            DB.loadMetadata(name);
        } catch (Throwable e) {
            if (!ignore)
                throw new IllegalStateException(e);
        }
    }

    public String getName() {
        return name;
    }
}
