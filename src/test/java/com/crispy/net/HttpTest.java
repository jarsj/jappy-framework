package com.crispy.net;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by harsh on 7/5/16.
 */
public class HttpTest {

    @Test
    public void simpleTest() throws IOException {
        Get g = Get.builder("http://www.google.com").build();
        Http http = Http.builder().setAsync(10).setMaxConnectionsPerRoute(10).build();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            http.execute(g, (r) -> {
                assertEquals(r.status(), 200);
            });
        }
        http.close();
        long end = System.currentTimeMillis();
        System.out.println("Time Taken " + (end - start) + "ms");
    }
}
