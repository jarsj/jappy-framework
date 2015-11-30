package com.crispy;

import com.crispy.db.*;
import com.crispy.db.Index.IndexType;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TableTests {
	
	@BeforeClass
	public static void initDB() throws SQLException {
		DB.drop("localhost", "tests_table", "root", "harsh");
		DB.create("localhost", "tests_table", "root", "harsh");
		DB.init("tests_table", "root", "harsh");
	}
	
	@Test
	public void testUpdate() {
		Table.get("test").columns(Column.bigInteger("x"),
				Column.bigInteger("y"),
				Column.bigInteger("total")).indexes(Index.create("u_xy", IndexType.UNIQUE, "x", "y")).create();
		
		Table.get("test").columns("x", "y", "total").values(1, 1, 0).ignore().add();
		Table.get("test").columns("x", "y", "total").values(2, 1, 0).add("total", 100).ignore().add();
	}

    @After
    public void tearDown() throws SQLException {
        DB.updateQuery("DROP TABLE test");
    }

    @Test
    public void testDate() {
        Table.get("test").columns(Column.bigInteger("id", true),
                Column.date("start")).create();

		// LocalDAte
        Table.get("test").columns("start").values(LocalDate.now()).add();
        Table.get("test").columns("start").values(LocalDate.now().minus(1, ChronoUnit.DAYS)).add();

        assertEquals(1, Table.get("test").where("start", LocalDate.now()).rows().size());

		Table.get("test").delete();

		assertEquals(0, Table.get("test").count());

		Table.get("test").columns("start").values(System.currentTimeMillis()).add();
		assertEquals(1, Table.get("test").where("start", LocalDate.now()).rows().size());
	}

	@Test
	public void testDateRange() {
		Table.get("test").columns(Column.bigInteger("id", true),
				Column.timestamp("ts")).create();

		LocalDateTime t = LocalDateTime.now();
		for (int i = 0; i < 100; i++) {
			Table.get("test").columns("ts").values(t.minus(i, ChronoUnit.DAYS)).add();
		}
		for (Row r : Table.get("test").rows()) {
			System.out.println(r.columnAsString("ts"));
		}
		assertEquals(100, Table.get("test").count());
		assertEquals(50, Table.get("test").afterDate("ts", LocalDate.now().minus(50, ChronoUnit.DAYS)).count());
		assertEquals(10, Table.get("test").onOrBeforeDate("ts", LocalDate.now().minus(90, ChronoUnit.DAYS)).count());

		assertEquals(50, Table.get("test").greater("ts", LocalDateTime.now().minus(50, ChronoUnit.DAYS)).count());
		assertEquals(10, Table.get("test").where("ts", LocalDateTime.now().minus(90, ChronoUnit.DAYS), Table.WhereOp
				.LESS_THAN_EQUALS).count
				());
	}
}
