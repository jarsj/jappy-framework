package com.crispy.database;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Blob;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Created by harsh on 1/5/16.
 */
public class Value {
    private Object o;
    private Value d;

    private Value(Object o) {
        this.o = o;
        this.d = null;
    }

    public static Value create(Object o) {
        return new Value(o);
    }

    public Value def(Object o) {
        this.d = Value.create(o);
        return this;
    }

    public LocalDate asDate() {
        if (o == null)
            return (d != null) ? d.asDate() : null;
        if (o instanceof Date)
            return ((Date) o).toLocalDate();
        if (o instanceof LocalDate)
            return (LocalDate) o;
        if (o instanceof LocalDateTime)
            return ((LocalDateTime) o).toLocalDate();
        return null;
    }

    public Boolean asBool() {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return Boolean.parseBoolean(asString());
    }

    public long asLong() {
        if (o == null)
            return (d != null) ? d.asLong() : 0;
        if (o instanceof Number)
            return ((Number) o).longValue();
        if (o instanceof Timestamp)
            return ((Timestamp) o).getTime();
        return Long.parseLong(o.toString());
    }

    public int asInt() {
        if (o == null)
            return (d != null) ? d.asInt() : 0;
        if (o instanceof Number)
            return ((Number) o).intValue();
        return Integer.parseInt(o.toString());
    }

    public String asString() {
        if (o == null)
            return (d != null) ? d.asString() : null;
        if (o instanceof String)
            return (String) o;
        if (o instanceof Blob) {
            return new String(asBytes());
        }
        return null;
    }

    public byte[] asBytes() {
        if (o == null)
            return (d != null) ? d.asBytes() : null;
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
            return (d != null) ? d.asFile() : null;
        return new File(o.toString());
    }

    public URL asURL() throws MalformedURLException {
        if (o == null)
            return (d != null) ? d.asURL() : null;
        return new URL(asString());
    }

    public Instant asInstant() {
        if (o == null)
            return (d != null) ? d.asInstant() : null;
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
        if (o == null)
            return (d != null) ? d.asObject() : null;
        return o;
    }
}
