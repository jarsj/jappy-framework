package com.crispy;

import com.crispy.database.Value;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by harsh on 1/31/17.
 */
public class JSON {

    public static JSONArray merge(JSONArray ... a) {
        JSONArray ret = new JSONArray();
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length(); j++) {
                ret.put(a[i].get(j));
            }
        }
        return ret;
    }

    public static JSONObject merge(JSONObject ... o) {
        JSONObject ret = new JSONObject();
        for (int i = 0; i < o.length; i++) {
            Iterator keys = o[i].keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                ret.put(key, o[i].get(key));
            }
        }
        return ret;
    }

    public static JSONObject accumulate(JSONObject ... o) {
        JSONObject ret = new JSONObject();
        for (int i = 0; i < o.length; i++) {
            Iterator keys = o[i].keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                ret.accumulate(key, o[i].get(key));
            }
        }
        return ret;
    }

    /**
     *
     * @param o
     * @param key
     * @param value
     */
    public static void put(JSONObject o, String key, Object value) {
        int dot = key.indexOf('.');
        if (dot == -1) {
            o.put(key, value);
        } else {
            String first = key.substring(0, dot);
            String second = key.substring(dot + 1);
            if (!(o.has(first) && (o.get(first) instanceof JSONObject))) {
                o.put(first, new JSONObject());
            }
            put(o.getJSONObject(first), second, value);
        }
    }
}
