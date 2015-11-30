package com.crispy.net;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by harsh on 11/15/15.
 */
public class Post {
    private HashMap<String, String> headers;
    private HashMap<String, String> data;
    private HashMap<String, String> params;

    private File file;
    private String url;
    private ContentType contentType;

    private Post() {
        headers = new HashMap<>();
        data = new HashMap<>();
        params = new HashMap<>();
    }

    private Post(Post p) {
        url = p.url;
        data = new HashMap<>(p.data);
        headers = new HashMap<>(p.headers);
        params = new HashMap<>(p.params);
        file = p.file;
        contentType = p.contentType;
    }

    public static Post create() {
        Post p = new Post();
        return p;
    }

    public Post withUrl(String url) {
        Post p = new Post(this);
        p.url = url;
        return p;
    }

    public Post withHeader(String key, String value) {
        Post p = new Post(this);
        p.headers.put(key, value);
        return p;
    }

    public Post withParams(String key, String value) {
        Post p = new Post(this);
        p.params.put(key, value);
        return p;
    }


    public Post withData(String ... args) {
        Post p = new Post(this);
        for (int i = 0; i < args.length; i += 2) {
            p.data.put(args[i], args[i + 1]);
        }
        return p;
    }

    public Post withData(Map<String, String> args) {
        Post p = new Post(this);
        p.data.putAll(args);
        return p;
    }

    public Post withFile(File f, String mimeType) {
        Post p = new Post(this);
        p.file = f;
        p.contentType = ContentType.create(mimeType);
        return p;
    }

    public JSONObject json() {
        return new JSONObject(response());
    }

    public String response() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            List<NameValuePair> queryParams = new ArrayList<>();
            params.forEach((k, v) -> {
                queryParams.add(new BasicNameValuePair(k, v));
            });
            HttpPost post = new HttpPost(url + "?" + URLEncodedUtils.format(queryParams, "UTF-8"));
            if (file == null) {
                List<NameValuePair> params = new ArrayList<>();
                data.forEach((k, v) -> {
                    params.add(new BasicNameValuePair(k, v));
                });
                post.setEntity(new UrlEncodedFormEntity(params));
            } else {
                post.setEntity(new FileEntity(file, contentType));
            }

            headers.forEach((k, v) -> {
               post.addHeader(k, v);
            });

            try (CloseableHttpResponse response = client.execute(post)) {
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
