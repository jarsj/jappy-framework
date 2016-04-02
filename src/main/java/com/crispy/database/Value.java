package com.crispy.database;

import com.crispy.utils.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Blob;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;

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

    public LocalDate asDate() {
        if (o == null)
            return null;
        if (o instanceof Date)
            return ((Date) o).toLocalDate();
        if (o instanceof LocalDate)
            return (LocalDate) o;
        if (o instanceof LocalDateTime)
            return ((LocalDateTime) o).toLocalDate();
        return null;
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

    public String asString(String def) {
        if (o == null)
            return def;
        return asString();
    }

    public String asString() {
        if (o instanceof String)
            return (String) o;
        if (o instanceof Blob) {
            return new String(asBytes());
        }
        if (o != null)
            return o.toString();
        return null;
    }

    public byte[] asBytes() {
        if (o instanceof byte[])
            return (byte[]) o;
        if (o instanceof Blob) {
            try {
                return IOUtils.toByteArray(((Blob) o).getBinaryStream());
            } catch (Exception e) {
                return null;
            }
        }
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

    public Instant asInstant() {
        if (o == null)
            return null;
        if (o instanceof Instant)
            return (Instant) o;
        if (o instanceof Date)
            return ((Date) o).toInstant();
        if (o instanceof Timestamp)
            return ((Timestamp) o).toInstant();
        return null;
    }

    public boolean isNull() {
        return o == null;
    }

    public Object asObject() {
        return o;
    }
}
