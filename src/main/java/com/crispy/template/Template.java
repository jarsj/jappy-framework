package com.crispy.template;

import com.crispy.log.Log;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.*;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private Template(freemarker.template.Template tpl) {
        this.tpl = tpl;
    }

    private Template(String content) throws IOException {
        tpl = new freemarker.template.Template(UUID.randomUUID().toString(), content, getConfiguration());
    }

    private Template(File f) throws IOException {
        tpl = new freemarker.template.Template(UUID.randomUUID().toString(), FileUtils.readFileToString(f),
                getConfiguration());
    }

    public static void setMacroDirectory(File f) throws IOException {
        getConfiguration().setTemplateLoader(new FileTemplateLoader(f));
    }

    public static Template byName(String name) throws IOException {
        Template ret = new Template(getConfiguration().getTemplate(name));
        return ret;
    }

    public static Template fromString(String content) throws IOException {
        Template ret = new Template(content);
        return ret;
    }

    public static Template fromFile(File f) throws IOException {
        Template ret = new Template(f);
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
