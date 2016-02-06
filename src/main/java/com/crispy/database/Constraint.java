package com.crispy.database;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class Constraint {
	String sourceTable;
	String sourceColumn;
	String destTable;
	String destColumn;

	public static Constraint create(String column, String foreignTable,
									String foreignColumn) {
		Constraint c = new Constraint();
		c.sourceColumn = column;
		c.destTable = foreignTable;
		c.destColumn = foreignColumn;
		return c;
	}

	@Override
	public boolean equals(Object obj) {
		Constraint o = (Constraint) obj;
		return Objects.equals(sourceTable, o.sourceTable)
				&& Objects.equals(sourceColumn, o.sourceColumn)
				&& Objects.equals(destTable, o.destTable)
				&& Objects.equals(destColumn, o.destColumn);
	}

	public void create(String table) throws SQLException {
		sourceTable = table;
		DB.updateQuery("ALTER TABLE `" + sourceTable + "` ADD CONSTRAINT `"
				+ sourceTable + "_" + sourceColumn + "` FOREIGN KEY `"
				+ sourceTable + "_" + sourceColumn + "`(`" + sourceColumn
				+ "`) REFERENCES `" + destTable + "`(`" + destColumn + "`)");
	}

	public void drop() throws SQLException {
		DB.updateQuery("ALTER TABLE `" + sourceTable + "` DROP FOREIGN KEY `"
				+ sourceTable + "_" + sourceColumn + "`");
		DB.updateQuery("ALTER TABLE `" + sourceTable + "` DROP INDEX `"
				+ sourceTable + "_" + sourceColumn + "`");
	}

	public static Constraint to(List<Constraint> constraints, String table) {
		for (Constraint c : constraints)
			if (c.destTable.equals(table))
				return c;
		return null;
	}

	public String getDestTable() {
		return destTable;
	}

	public String getDestColumn() {
		return destColumn;
	}
}
