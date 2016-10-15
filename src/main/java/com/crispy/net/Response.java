package com.crispy.net;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by harsh on 7/1/16.
 */
public class Response {

    private HashMap<String, String> headers;
    private byte[] data;
    private int status;

    Response(HttpResponse response) throws IOException {
        status = response.getStatusLine().getStatusCode();
        if (status == 200) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                data = EntityUtils.toByteArray(entity);
            }
        }
        headers = new HashMap<>();
        for (Header h : response.getAllHeaders()) {
            headers.put(h.getName(), h.getValue());
        }
    }

    public int status() {
        return status;
    }

    public String toString() {
        if (data == null)
            return null;
        return new String(data);
    }

    public byte[] toBytes() {
        return data;
    }

    public JSONObject toJSONObject() {
        if (data == null)
            return null;
        return new JSONObject(toString());
    }

    public JSONArray toJSONArray() {
        if (data == null)
            return null;
        return new JSONArray(toString());
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String header(String key) {
        return headers.get(key);
    }
}
