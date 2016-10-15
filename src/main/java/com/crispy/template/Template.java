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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
            CONFIGURATION.setNumberFormat("computer");
        }
        return CONFIGURATION;
    }

    private HashMap<String, TemplateMethodModelEx> methods;
    private freemarker.template.Template tpl;

    private Template(freemarker.template.Template tpl) {
        this.tpl = tpl;
        methods = new HashMap<>();
    }

    private Template(String content) throws IOException {
        tpl = new freemarker.template.Template(UUID.randomUUID().toString(), content, getConfiguration());
        methods = new HashMap<>();
    }

    private Template(File f) throws IOException {
        tpl = new freemarker.template.Template(UUID.randomUUID().toString(), FileUtils.readFileToString(f),
                getConfiguration());
        methods = new HashMap<>();
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

    public void installIntFn(String name, Function<Integer, Object> fn) {
        methods.put(name, new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                if (arguments.size() != 1)
                    throw new TemplateModelException("Need only 1 argument for " + name);
                SimpleNumber number = (SimpleNumber) arguments.get(0);
                return fn.apply(number.getAsNumber().intValue());
            }
        });
    }

    public void installStringFn(String name, Function<String, Object> fn) {
        methods.put(name, new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                if (arguments.size() != 1)
                    throw new TemplateModelException("Need only 1 argument for " + name);
                SimpleScalar arg = (SimpleScalar) arguments.get(0);
                return fn.apply(arg.getAsString());
            }
        });
    }

    public void installJSONObjectFn(String name, Function<JSONObject, Object> fn) {
        methods.put(name, new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                if (arguments.size() != 1)
                    throw new TemplateModelException("Need only 1 argument for " + name);
                TemplateHashModelEx arg = (TemplateHashModelEx) arguments.get(0);
                if (arg.size() == 0)
                    return fn.apply(new JSONObject());
                JSONObjectModel jarg = (JSONObjectModel) arg;
                return fn.apply(jarg.getAsJSONObject());
            }
        });
    }

    public void installJSONArrayFn(String name, Function<JSONArray, Object> fn) {
        methods.put(name, new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                if (arguments.size() != 1)
                    throw new TemplateModelException("Need only 1 argument for " + name);
                TemplateSequenceModel arg = (TemplateSequenceModel) arguments.get(0);
                if (arg.size() == 0)
                    return fn.apply(new JSONArray());
                JSONArrayModel jarg = (JSONArrayModel) arg;
                return fn.apply(jarg.getAsJSONArray());
            }
        });
    }

    public String expand(JSONObject data) {
        try {
            methods.forEach((name, fn) -> {
                data.put(name, fn);
            });
            StringWriter sw = new StringWriter();
            tpl.process(data, sw, JSONWrapper.INSTANCE);
            return sw.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Error expanding template", e);
        }
    }
}
