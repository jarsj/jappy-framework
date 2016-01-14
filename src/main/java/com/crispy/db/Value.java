package com.crispy.db;

import com.crispy.utils.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;

/**
 * Created by harsh on 1/5/16.
 */
public class Value {
    private Object o;
    private Column c;

    private Value(Object o, Column c) {
        this.o = o;
        this.c = c;
    }

    public static Value create(Object o, Column c) {
        return new Value(o, c);
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
        if (c != null)
            return c.def;
        return null;
    }

    public File asFile() {
        if (o == null)
            return null;
        switch (c.simpleType(null)) {
            case FILE: {
                return new File(o.toString());
            }
            case S3: {
                try {
                    // Temporary hack. This needs to be optimized later.
                    URL u = asURL();
                    String fileName = StringUtils.strip(u.getPath(), "/#");
                    File temp = File.createTempFile(FilenameUtils.getBaseName(fileName), "." + FilenameUtils
                            .getExtension(fileName));
                    InputStream is = u.openStream();
                    FileOutputStream fout = new FileOutputStream(temp);
                    IOUtils.copy(is, fout);
                    return temp;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            case BASE64: {

            }
        }
        return null;
    }

    public URL asURL() {
        if (o == null)
            return null;
        switch (c.simpleType(null)) {
            case FILE: {
                try {
                    return new URL("file://" + o.toString());
                } catch (MalformedURLException e) {
                    throw new IllegalStateException(e);
                }
            }
            case S3: {
                try {
                    URL ret = null;
                    if (c.comment_cloudfront != null) {
                        URIBuilder builder = new URIBuilder(o.toString());
                        builder.setHost(c.comment_cloudfront[Utils.random(c.comment_cloudfront.length)]);
                        ret = builder.build().toURL();
                    } else {
                        ret = new URL(o.toString());
                    }
                    return ret;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return null;

    }

    public boolean isNull() {
        return o == null;
    }

    public Object asObject() {
        return o;
    }
}
