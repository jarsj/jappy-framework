package com.crispy.database;

import ch.qos.logback.classic.Level;
import com.crispy.log.Appender;
import com.crispy.log.Log;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
    @Test
    public void testJoins() {
        Table.get("user").columns(Column.bigInteger("id", true),
                Column.text("name", 32)).create();
        Table.get("transaction").columns(Column.bigInteger("id", true),
                Column.bigInteger("userid"),
                Column.bigInteger("amount")).constraints(Constraint.create("userid", "user", "id"))
                .create();

        Insert insert = Insert.withTable("user");
        long idA = insert.object("name", "a").executeAndFetch().byIndex(0).asLong();
        long idB = insert.object("name", "b").executeAndFetch().byIndex(0).asLong();
        long idC = insert.object("name", "c").executeAndFetch().byIndex(0).asLong();

        Insert txnA = Insert.withTable("transaction").object("userid", idA);
        Insert txnB = Insert.withTable("transaction").object("userid", idB);

        txnA.object("amount", 100).execute();
        txnA.object("amount", 200).execute();

        txnB.object("amount", 100).execute();

        Select select = Select.withTable("user", "transaction").groupBy("user", "id").function("SUM", "amount",
                "balance")
                .fetchEverything();
        assertEquals(2, select.rows().getRows().size());
        assertEquals(3, select.leftJoin().rows().getRows().size());

        List<Row> rows = select.orderBy("balance", SortOrder.DESCENDING).rows().getRows();

        assertEquals("a", rows.get(0).byName("name").asString());
        assertEquals(300, rows.get(0).byName("balance").asLong());

        assertEquals("b", rows.get(1).byName("name").asString());
        assertEquals(100, rows.get(1).byName("balance").asLong());

        assertEquals("c", rows.get(2).byName("name").asString());
        assertEquals(0, rows.get(2).byName("balance").asLong());
    }

    @Test
    public void testUpdates() {
        Table.get("bots").columns(Column.text("name", 50), Column.text("avatar", 256), Column.bool("used")).primary("name").create();

        for (int i = 0; i < 10; i++) {
            Insert.withTable("bots").object("name", "guest" + (i + 1)).object("avatar", "photo" + (i + 1))
                    .object("used", false).execute();
        }

        Set<String> holder = new TreeSet<>();
        for (int i = 0; i < 10; i++) {
            Row bot = Select.withTable("bots").where(Where.equals().column("used").value(false))
                    .orderByRandom()
                    .row();
            assertEquals(1, Update.withTable("bots").where(Where.equals().column("name").value(bot.byName("name")))
                    .object("used", true).execute());
            holder.add(bot.byName("name").asString());
        }

        assertEquals(10, holder.size());
    }

    @Test
    public void testDuplicateInserts() {
        Table.get("transactions").columns(//
                Column.bigInteger("id", true),
                Column.bigInteger("userid"),
                Column.bigInteger("coins"),
                Column.integer("diamonds"),
                Column.integer("tickets"),
                Column.bigInteger("cash"),
                Column.timestamp("ts"),
                Column.text("code", 64)
        ).indexes(Index.create("u_entry", Index.IndexType.UNIQUE, "userid", "ts", "code")).create();

        Insert insert = Insert.withTable("transactions");
        // Let's try adding
        long ts = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            insert.object("userid", 1).object("coins", 100).object("diamonds", 0).object("tickets", 0)
                    .object("cash", 0).object("code", "random").object("ts", ts).ignore().execute();
        }

        // Test IGNORE
        assertEquals(1, Select.withTable("transactions").count("total").row().byAlias("total").asLong());

        // Test Overwrite
        for (int i = 0; i < 10; i++) {
            insert.object("userid", 1).object("coins", 100 * i).object("diamonds", 0).object("tickets", 0)
                    .object("cash", 0).object("code", "random").object("ts", ts).overwrite("coins").execute();
        }
        assertEquals(1, Select.withTable("transactions").count("total").row().byAlias("total").asLong());
        assertEquals(900, Select.withTable("transactions").where(Where.equals().column("userid").value(1)).row()
                .byName("coins").asLong());

        // Test generated ID
        long ID = insert.object("userid", 2).object("coins", 200).executeAndFetch().byIndex(0).asLong();
        assertEquals(21, ID);

        Update.withTable("transactions").where(Where.equals().column("id").value(ID))
                .object("coins", 1000)
                .object("cash", 200)
                .execute();
        Row r = Select.withTable("transactions").where(Where.equals().column("id").value(ID))
                .row();
        assertEquals(1000, r.byName("coins").asLong());
        assertEquals(200, r.byName("cash").asLong());
    }

    @Test
    public void testEmptyRowBug() {
        Table.get("booboo").columns(Column.bigInteger("id", true),
                Column.text("name", 20))
                .create();

        Insert.withTable("booboo").object("name", "hello").execute();

        Row r = Select.withTable("booboo").row();
        assertEquals("hello", r.byName("name").asString());
    }

    @Test
    public void testDouble() {
        Table.get("booboo").columns(Column.bigInteger("id", true),
                Column.dbl("money")).create();

        Insert.withTable("booboo").object("money", 10.24f).execute();
        Insert.withTable("booboo").object("money", 10.25).execute();
        Insert.withTable("booboo").object("money", "10.26").execute();
        Insert.withTable("booboo").object("money", 11).execute();

        List<Row> rows = Select.withTable("booboo").rows().getRows();
        assertEquals(10.24, rows.get(0).byName("money").asDouble(), 0.0001);
        assertEquals(10.25f, rows.get(1).byName("money").asFloat(), 0.0001f);
        assertEquals(10.26, rows.get(2).byName("money").asDouble(), 0.0001);
        assertEquals(11, rows.get(3).byName("money").asInt());
    }

    @After
    public void tearDown() {
        Table.get("booboo").drop(true);
    }
}
