package com.crispy.database;

import ch.qos.logback.classic.Level;
import com.crispy.log.Appender;
import com.crispy.log.Log;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by harsh on 4/2/16.
 */
public class RapidTests {
    @BeforeClass
    public static void initDB() throws SQLException {
        Log.getRoot().appender(Appender.create("console").level(Level.DEBUG).console());

        DB.drop("localhost", "tests_table", "root", "harsh");
        DB.create("localhost", "tests_table", "root", "harsh");
        DB.init("tests_table", "root", "harsh");
    }

    @Test
    public void testMultipleInserts() throws InterruptedException {
        Table.get("boob").columns(Column.bigInteger("id", true),
                Column.integer("boob")).indexes(Index.unique("boob")).create();

        assertEquals(1, Insert.withTable("boob").object("boob", 1).execute());
        assertEquals(0, Insert.withTable("boob").object("boob", 1).ignore().execute());
        assertEquals(0, Insert.withTable("boob").object("boob", 1).ignore().execute());

        AtomicLong counter = new AtomicLong(0);
        Thread t[] = new Thread[100];
        for (int i = 0; i < 100; i++) {
            t[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    int ret = Insert.withTable("boob").object("boob", 2).ignore().execute();
                    if (ret > 0) {
                        counter.incrementAndGet();
                    }
                }
            });
            t[i].start();
        }

        for (int i = 0; i < 100; i++) {
            t[i].join();
        }
        assertEquals(1, counter.intValue());
    }
}
