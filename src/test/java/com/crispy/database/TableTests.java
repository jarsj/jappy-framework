package com.crispy.database;

import ch.qos.logback.classic.Level;
import com.crispy.log.Appender;
import com.crispy.log.Log;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

/**
 * Created by harsh on 1/18/16.
 */
public class TableTests {

    @BeforeClass
    public static void initDB() throws SQLException {
        Log.getRoot().appender(Appender.create().level(Level.DEBUG).console());

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

        DB.updateQuery("INSERT INTO person(name, age) VALUES (?, ?)", "Harsh Jain", 20);
        DB.updateQuery("INSERT INTO person(name, age) VALUES (?, ?)", "Ravi Gupta", 20);
        DB.updateQuery("INSERT INTO person(name, age) VALUES (?, ?)", "Monica Belluci", 35);
        DB.updateQuery("INSERT INTO salary(person_id, salary, name) VALUES (?,?,?)", 1, 100, "rent");
        DB.updateQuery("INSERT INTO salary(person_id, salary, name) VALUES (?,?,?)", 1, 200, "monthly");

        Row row = Select.withTable("person", "salary").fetch("person", "name")
                .fetchWithAlias("salary", "name", "salary_type").row();
        System.out.println(row.byFullName("person", "name").asString());
        System.out.println(row.byAlias("salary_type").asString());
        System.out.println(row.toJSON());

        row = Select.withTable("person").count("total").row();
        System.out.println(row.toJSON());

        row = Select.withTable("person").where(Where.equals("name", "Ravi Gupta")).row();
        System.out.println(row.toJSON());

    }
}
