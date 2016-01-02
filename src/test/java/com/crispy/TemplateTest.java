package com.crispy;

import com.crispy.template.Template;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;

/**
 * Created by harsh on 1/1/16.
 */
public class TemplateTest {

    @Test
    public void testSimple() throws IOException {
        Template tpl = Template.fromString("Hello ${user}");
        assertEquals("Hello Harsh", tpl.expand(new JSONObject().put("user", "Harsh")));
    }
}
