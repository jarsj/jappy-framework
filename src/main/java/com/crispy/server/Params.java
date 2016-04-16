package com.crispy.server;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by harsh on 12/29/15.
 */
public class Params {
    private HashMap<String, Object> values;

    private Params() {
        values = new HashMap<>();
    }
    private Params(Params o) {
        values = new HashMap<>(o.values);
    }

    public static Params create(Map values) {
        Params p = new Params();
        p.values.putAll(values);
        return p;
    }

    public Params withoutKeys(String ...keys) {
        Params ret = new Params(this);
        for (String key : keys)
            ret.values.remove(key);
        return ret;
    }

    public static Params withRequest(HttpServletRequest request) {
        Params p = new Params();
        try {
            p.loadNormal(request);
            if ((request.getContentType() != null) && (request.getContentType().toLowerCase().indexOf("application/json") > -1)) {
                p.loadJSON(new JSONObject(IOUtils.toString(request.getReader())));
            }
            else if ((request.getContentType() != null) && (request.getContentType().toLowerCase().indexOf
                    ("multipart/form-data") > -1)) {
                p.loadMultipart(request.getParts());
            }
            return p;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Object get(String name) {
        return values.get(name);
    }

    public String getString(String name) {
        Object o = values.get(name);
        if (o == null) return null;
        if (o instanceof File) {
            try {
                return FileUtils.readFileToString((File) o);
            } catch (IOException e) {
                throw new IllegalStateException("Can't read file " + ((File) o).getAbsolutePath(), e);
            }
        }
        return o.toString();
    }

    public File getFile(String name) {
        Object o = values.get(name);
        if (o instanceof File)
            return (File) o;
        return null;
    }

    public boolean isFile(String key) {
        return values.get(key) instanceof File;
    }

    public List<String> keys() {
        return new ArrayList<>(values.keySet());
    }

    private void loadJSON(JSONObject data) {
        Iterator iter = data.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            values.put(key, data.get(key));
        }
    }

    private void loadMultipart(Collection<Part> parts) throws IOException {
        for (Part part : parts) {
            String key = part.getName();
            String fileName = getFileName(part);
            if (fileName == null) {
                values.put(key, IOUtils.toString(part.getInputStream()));
            } else {
                File temp = File.createTempFile("tmp", "." + FilenameUtils.getExtension(fileName));
                part.write(temp.getAbsolutePath());
                values.put(key, temp);
            }
        }
    }

    private void loadNormal(HttpServletRequest request) {
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String key = names.nextElement();
            values.put(key, request.getParameter(key));
        }
    }

    private static String getFileName(final Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }



}
