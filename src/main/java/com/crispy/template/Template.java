package com.crispy.template;

import freemarker.template.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

/**
 * Created by harsh on 1/1/16.
 */
public class Template {

    private static Configuration CONFIGURATION = null;

    private static Configuration getConfiguration() {
        if (CONFIGURATION == null) {
            CONFIGURATION = new Configuration(Configuration.VERSION_2_3_23);
            CONFIGURATION.setDefaultEncoding("UTF-8");
            CONFIGURATION.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        }
        return CONFIGURATION;
    }

    private freemarker.template.Template tpl;

    private Template(String content) throws IOException {
        tpl = new freemarker.template.Template(UUID.randomUUID().toString(), content, getConfiguration());
    }

    public static Template fromString(String content) throws IOException {
        Template ret = new Template(content);
        return ret;
    }

    public String expand(JSONObject data) {
        try {
            StringWriter sw = new StringWriter();
            tpl.process(data, sw, JSONWrapper.INSTANCE);
            return sw.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Error expanding template", e);
        }
    }
}
