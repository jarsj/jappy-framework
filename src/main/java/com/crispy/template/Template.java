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
    private static WatchService WATCH_SERVICE = null;
    private static ScheduledExecutorService background;

    private static Configuration getConfiguration() {
        if (CONFIGURATION == null) {
            CONFIGURATION = new Configuration(Configuration.VERSION_2_3_23);
            CONFIGURATION.setDefaultEncoding("UTF-8");
            CONFIGURATION.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

            try {
                WATCH_SERVICE = FileSystems.getDefault().newWatchService();
                background = Executors.newSingleThreadScheduledExecutor();
                background.scheduleWithFixedDelay(checkWatchService, 0, 10, TimeUnit.SECONDS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return CONFIGURATION;
    }

    private static final Runnable checkWatchService = new Runnable() {
        @Override
        public void run() {
            try {
                WatchKey key = WATCH_SERVICE.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW)
                        continue;

                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filename = ev.context();
                    System.out.println(filename.toAbsolutePath().toString() + " has changed");
                }
            } catch (Exception e) {
            }
        }
    };

    private freemarker.template.Template tpl;

    private Template(String content) throws IOException {
        tpl = new freemarker.template.Template(UUID.randomUUID().toString(), content, getConfiguration());
    }

    private Template(File f) throws IOException {
        tpl = new freemarker.template.Template(UUID.randomUUID().toString(), FileUtils.readFileToString(f),
                getConfiguration());

        if (WATCH_SERVICE != null) {
            Path dir = f.toPath();
            dir.register(WATCH_SERVICE, StandardWatchEventKinds.ENTRY_MODIFY);
        }
    }

    public static void setMacroDirectory(File f) throws IOException {
        getConfiguration().setTemplateLoader(new FileTemplateLoader(f));
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
