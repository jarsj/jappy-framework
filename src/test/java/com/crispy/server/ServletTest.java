package com.crispy.server;

import com.crispy.net.Get;
import com.crispy.net.Http;
import com.crispy.net.Post;
import com.crispy.net.Response;
import com.crispy.server.Servlet;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;

/**
 * Created by harsh on 6/30/16.
 */
public class ServletTest {



    @BeforeClass
    public static void setup() throws InterruptedException {
        Server s = new Server(8087);
        s.addServlet(BasicUnitServlet.class);
        s.startAsync();
    }

    @Test
    public void testArgs() throws IOException, InterruptedException {
        Http http = Http.builder().enableCookies().build();

        http.execute(Get.builder("http://localhost:8087/basic/args?int=10&string=harsh&integer=20").build(), (r) -> {
            JSONObject o = r.toJSONObject();
            assertEquals(10, o.getInt("int"));
            assertEquals("harsh", o.getString("string"));
            assertEquals(20, o.getInt("integer"));
        });

        http.get("http://localhost:8087/basic/args?int=10&integer=20", (r) -> {
            JSONObject o = r.toJSONObject();
            assertEquals(10, o.getInt("int"));
            assertFalse(o.has("string"));
            assertEquals(20, o.getInt("integer"));
        });

        http.get("http://localhost:8087/basic/args?int=10", (r) -> {
            JSONObject o = r.toJSONObject();
            assertEquals(10, o.getInt("int"));
            assertFalse(o.has("string"));
            assertFalse(o.has("integer"));
        });

        http.get("http://localhost:8087/basic/args", (r) -> {
            assertNull(r.toJSONObject());
        });

        http.close();
    }


    @Test
    public void testSession() throws UnsupportedEncodingException {
        Http http = Http.builder().enableCookies().build();

        http.get("http://localhost:8087/basic/session", (r) -> {
            assertEquals("", r.toString());
        });

        http.execute(Post.builder("http://localhost:8087/basic/session")
                .addData("key", "userid")
                .addData("value", "10").build(), (r) -> {

                });

        http.get("http://localhost:8087/basic/session", (r) -> {
            assertEquals("10", r.toString());
        });
    }

}
