package com.crispy;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.enums.EnumUtils;
import org.json.JSONObject;

import ch.qos.logback.classic.Level;

import com.crispy.log.Appender;
import com.crispy.log.Log;

public class Analytics {

    private static String S3_PATH = null;
    private static String CACHE_PATH = null;
    private static String UNIQUE_ID = null;
    private static String LOG_FILE_SIZE = "64mb";
    private static Log mLogger;

    /**
     * Initialize Analytics with a s3Path and a local filesystem path.
     *
     * @param s3Path
     */
    public static void init(String s3Path, String cachePath, String uniqueId, boolean hourly) {
        if (S3_PATH != null)
            throw new IllegalStateException("Analytics has already been initialized");
        S3_PATH = s3Path;
        if (S3_PATH.endsWith("/")) {
            S3_PATH = S3_PATH.substring(0, S3_PATH.length() - 1);
        }
        CACHE_PATH = FilenameUtils.normalizeNoEndSeparator(cachePath);
        UNIQUE_ID = uniqueId;

        File lockFile = new File(cachePath + "/.lock");
        if (lockFile.exists())
            throw new IllegalArgumentException(cachePath + " is already used by another analytics vm");
        try {
            FileUtils.touch(lockFile);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create lock file. Make sure " + cachePath + " is writable.", e);
        }

        mLogger = Log.get("analytics");

        Appender app = Appender.create().s3(S3_PATH, UNIQUE_ID)
                .folder(CACHE_PATH)
                .level(Level.INFO)
                .size(LOG_FILE_SIZE)
                .pattern("%m%n");
        if (hourly)
            app.hourly();

        mLogger.appender(app);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                FileUtils.deleteQuietly(new File(CACHE_PATH + "/.lock"));
            }
        }));
    }

    public static void setLogFileSize(String size) {
        LOG_FILE_SIZE = size;
    }


    public static void eventWithRequest(String event, HttpServletRequest request, Object... args) {
        List<Object> argList = new ArrayList<Object>(Arrays.asList(args));
        Enumeration<String> e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            argList.add(key);
            argList.add(request.getParameter(key));
        }

        eventWithTime(event, Instant.now().getEpochSecond(), argList.toArray());
    }

    /**
     * Log the event using the logger. The logger will in-turn upload it to S3
     * in due time.
     *
     * @param event
     * @param args
     */
    public static void event(String event, Object... args) {
        eventWithTime(event, Instant.now().getEpochSecond(), args);
    }

    public static void event(JSONObject o) {
        if (mLogger != null)
            mLogger.info(o.toString());
    }

    public static void eventWithTime(String event, long ts, Object... args) {
        JSONObject o = new JSONObject();
        o.put("event", event);
        o.put("ts", ts);
        for (int i = 0; i < args.length; i += 2) {
            if ((i + 1) < args.length) {
                o.put(args[i].toString(), args[i + 1]);
            }
        }
        if (mLogger != null)
            mLogger.info(o.toString());
    }
}
