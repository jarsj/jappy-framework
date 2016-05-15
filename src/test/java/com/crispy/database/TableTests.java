package com.crispy.database;

import ch.qos.logback.classic.Level;
import com.crispy.log.Appender;
import com.crispy.log.Log;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by harsh on 1/18/16.
 */
public class TableTests {

    @BeforeClass
    public static void initDB() throws SQLException {
        Log.getRoot().appender(Appender.create("console").level(Level.DEBUG).console());

        DB.drop("localhost", "tests_table", "root", "harsh");
        DB.create("localhost", "tests_table", "root", "harsh");
        DB.init("tests_table", "root", "harsh");
    }

    @Test
    public void testSimple() throws SQLException {
        Table.get("person")
                .columns(Column.bigInteger("id", true),
                        Column.text("name", 100),
                        Column.integer("age")).create();

        Table.get("salary")
                .columns(Column.bigInteger("id", true),
                        Column.bigInteger("person_id"),
                        Column.text("name", 200),
                        Column.integer("salary")).constraints(Constraint.create("person_id", "person", "id")).create();

        Insert.withTable("person").object("name", "Harsh Jain")
                .object("age", 20).execute();
        Insert.withTable("person").object("name", "Ravi Gupta")
                .object("age", 20).execute();
        Insert.withTable("person").object("name", "Monica Belluci")
                .object("age", 35).execute();

        Insert first = Insert.withTable("salary").object("person_id", 1);
        first.object("salary", 100).object("name", "rent").execute();
        first.object("salary", 200).object("name", "monthly").execute();

        Row row = Select.withTable("person", "salary").fetch("person", "name")
                .fetchWithAlias("salary", "name", "salary_type").row();
        System.out.println(row.byFullName("person", "name").asString());
        System.out.println(row.byAlias("salary_type").asString());
        System.out.println(row.toJSON());

        row = Select.withTable("person").count("total").row();
        System.out.println(row.toJSON());

        row = Select.withTable("person").where(Where.equals().column("name").value("Ravi Gupta")).row();
        System.out.println(row.toJSON());
    }

    @Test
    public void testComplex() {
        Table.get("school").columns(Column.bigInteger("id", true),
                Column.text("name")).create();
        long id = Insert.withTable("school").object("name", "Harsh School").executeAndFetch().byIndex(0).asLong();
        System.out.println(id);
        assertTrue(id > 0);
    }

    @Test
    public void testBinary() {
        Table.get("binary_table").columns(Column.bigInteger("id", true),
                Column.binary("boom")).create();
        Insert.withTable("binary_table").object("boom", new byte[]{1, 1, 1, 1, 1, 1}).execute();

        Row r = Select.withTable("binary_table").row();

        byte[] b = r.byName("boom").asBytes();
        assertEquals(6, b.length);
        for (int i = 0; i < 6; i++) {
            assertEquals(1, b[i]);
        }
    }

    @Test
    public void testDates() {
        Table.get("events").columns(Column.bigInteger("id", true),
                Column.date("start"),
                Column.date("end")).create();

        for (int i = 0; i < 10; i++) {
            assertEquals(1, Insert.withTable("events").object("start", LocalDate.of(2015, 06, 01))
                    .object("end", LocalDate.of(2015, 07, 01))
                    .execute());
        }

        assertEquals(10, Select.withTable("events").where(Where.equals().column("end").value(LocalDate.of(2015, 07,
                01))).count
                ("total").row().byAlias("total").def(0).asInt());

    }

    @Test
    public void testWhere() {
        Table.get("boom").columns(Column.bigInteger("id", true),
                Column.text("a"),
                Column.bigInteger("b")).create();

        Insert.withTable("boom").object("b", 10).execute();
        Insert.withTable("boom").object("b", 11).execute();
        Insert.withTable("boom").object("a", "hello").execute();
        Insert.withTable("boom").object("a", "chalo").object("b", 20).execute();

        assertEquals(2, Select.withTable("boom").where(Where.isNull().column("a")).count("total").row().byAlias
                ("total").def(0).asInt());
        assertEquals(1, Select.withTable("boom").where(Where.isNull().column("b")).count("total").row().byAlias
                ("total").def(0).asInt());
        assertEquals(3, Select.withTable("boom").where(Where.isNull().not().column("b")).count("total").row().byAlias
                ("total").def(0).asInt());
    }
}
