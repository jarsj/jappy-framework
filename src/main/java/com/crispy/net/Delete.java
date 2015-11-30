package com.crispy.net;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by harsh on 11/15/15.
 */
public class Delete {
    private HashMap<String, String> headers;
    private String url;

    private Delete() {
        headers = new HashMap<>();
    }

    private Delete(Delete p) {
        url = p.url;
        headers = new HashMap<>(p.headers);
    }

    public static Delete create() {
        Delete p = new Delete();
        return p;
    }

    public Delete withUrl(String url) {
        Delete p = new Delete(this);
        p.url = url;
        return p;
    }

    public Delete withHeader(String key, String value) {
        Delete p = new Delete(this);
        p.headers.put(key, value);
        return p;
    }

    public String response() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpDelete del = new HttpDelete(url);
            headers.forEach((k, v) -> {
               del.addHeader(k, v);
            });
            try (CloseableHttpResponse response = client.execute(del)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity);
                }
                return null;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
