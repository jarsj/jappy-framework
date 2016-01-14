package com.crispy.net;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by harsh on 11/15/15.
 */
public class Get {
    private HashMap<String, String> headers;
    private HashMap<String, String> data;
    private String url;
    private int timeout;

    private Get() {
        headers = new HashMap<>();
        data = new HashMap<>();
        timeout = -1;
    }

    private Get(Get p) {
        url = p.url;
        data = new HashMap<>(p.data);
        headers = new HashMap<>(p.headers);
        timeout = p.timeout;
    }

    public static Get create() {
        Get p = new Get();
        return p;
    }

    public Get withUrl(String url) {
        Get p = new Get(this);
        p.url = url;
        return p;
    }

    public Get withTimeout(int timeout) {
        Get g = new Get(this);
        g.timeout = timeout;
        return g;
    }

    public Get withHeader(String key, String value) {
        Get p = new Get(this);
        p.headers.put(key, value);
        return p;
    }

    public Get withData(String ... args) {
        Get p = new Get(this);
        for (int i = 0; i < args.length; i += 2) {
            p.data.put(args[i], args[i + 1]);
        }
        return p;
    }

    public Get withData(Map<String, String> args) {
        Get p = new Get(this);
        p.data.putAll(args);
        return p;
    }

    public int status() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = createGet();
            try (CloseableHttpResponse response = client.execute(get)) {
                return response.getStatusLine().getStatusCode();
            }
        } catch (IOException e) {
            return 500;
        }
    }

    public String response() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = createGet();
            try (CloseableHttpResponse response = client.execute(get)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    EntityUtils.consume(response.getEntity());
                    return null;
                }
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity);
                }
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public byte[] bytes() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = createGet();
            try (CloseableHttpResponse response = client.execute(get)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    EntityUtils.consume(response.getEntity());
                    return null;
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) return null;
                return EntityUtils.toByteArray(entity);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private HttpGet createGet() {
        List<NameValuePair> params = new ArrayList<>();
        data.forEach((k, v) -> {
            params.add(new BasicNameValuePair(k, v));
        });
        String separator = "?";
        if (url.contains("?")) {
            if (url.endsWith("?")) separator = "";
            separator = "&";
        }
        HttpGet get = new HttpGet(url + separator + URLEncodedUtils.format(params, "UTF-8"));
        if (timeout != -1) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(timeout * 1000)
                    .setConnectTimeout(timeout * 1000)
                    .build();
            get.setConfig(requestConfig);
        }
        headers.forEach((k, v) -> {
            get.addHeader(k, v);
        });
        return get;
    }

    public JSONObject json() {
        String str = response();
        if (str == null) return null;
        return new JSONObject(str);
    }
}
