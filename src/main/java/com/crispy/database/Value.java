package com.crispy.database;

import com.crispy.utils.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;

/**
 * Created by harsh on 1/5/16.
 */
public class Value {
    private Object o;

    private Value(Object o) {
        this.o = o;
    }

    public static Value create(Object o) {
        return new Value(o);
    }

    public long asLong() {
        if (o == null)
            return 0;
        if (o instanceof Number)
            return ((Number) o).longValue();
        if (o instanceof Timestamp)
            return ((Timestamp) o).getTime();
        return Long.parseLong(o.toString());
    }

    public int asInt(int def) {
        if (o == null)
            return def;
        if (o instanceof Number)
            return ((Number) o).intValue();
        return Integer.parseInt(o.toString());
    }

    public String asString() {
        if (o instanceof String)
            return (String) o;
        if (o != null)
            return o.toString();
        return null;
    }

    public File asFile() {
        if (o == null)
            return null;
        return new File(o.toString());
    }

    public URL asURL() throws MalformedURLException {
        if (o == null)
            return null;
        return new URL(o.toString());
    }

    public boolean isNull() {
        return o == null;
    }

    public Object asObject() {
        return o;
    }
}
