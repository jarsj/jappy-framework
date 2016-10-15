package com.crispy.net;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Created by harsh on 11/15/15.
 */
public class Post {
    private PostBuilder mPost;

    private Post(PostBuilder post) {
        mPost = post;
    }

    HttpPost createHttpPost() {
        return mPost.create();
    }

    public static PostBuilder builder(String url) {
        return new PostBuilder(url);
    }

    public PostBuilder builder() {
        return new PostBuilder(mPost);
    }

    public static class PostBuilder {
        private HashMap<String, String> headers;
        private HashMap<String, Object> files;
        private HashMap<String, ContentType> contentTypes;

        URIBuilder uriBuilder;

        HashMap<String, String> data;

        private boolean isMultipart;

        PostBuilder(PostBuilder old) {
            try {
                uriBuilder = new URIBuilder(old.uriBuilder.build());
                headers = new HashMap<>(old.headers);
                data = new HashMap<>(old.data);
                files = new HashMap<>(old.files);
                contentTypes = new HashMap<>(old.contentTypes);
                isMultipart = old.isMultipart;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Can't parse");
            }
        }

        PostBuilder(String url) {
            try {
                uriBuilder = new URIBuilder(url);
                headers = new HashMap<>();
                data = new HashMap<>();
                files = new HashMap<>();
                contentTypes = new HashMap<>();
                isMultipart = false;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Can't parse url " + url);
            }
        }

        public PostBuilder enableMultipart() {
            isMultipart = true;
            return this;
        }

        public PostBuilder addHeader(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public PostBuilder addParam(String key, String value) {
            uriBuilder.addParameter(key, value);
            return this;
        }

        public PostBuilder addData(String key, String value) {
            data.put(key, value);
            return this;
        }

        public PostBuilder addData(HashMap<String, String> data) {
            data.forEach((k, v) -> {
                addData(k, v);
            });
            return this;
        }

        public PostBuilder setBody(JSONObject o) {
            files.put("default", o);
            contentTypes.put("default", ContentType.create("text/json"));
            return this;
        }

        public PostBuilder setFile(String key, File f) {
            files.put(key, f);
            return this;
        }

        public PostBuilder setFile(File f, String mimeType) {
            files.put("default", f);
            contentTypes.put("default", ContentType.create(mimeType));
            return this;
        }

        HttpPost create() {
            try {
                HttpPost post = new HttpPost(uriBuilder.build());
                if (isMultipart) {
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    data.forEach((key, value) -> {
                        builder.addTextBody(key, value);
                    });
                    files.forEach((k, v) -> {
                        if (v instanceof File) {
                            builder.addBinaryBody(k, (File) v);
                        } else {
                            builder.addTextBody(k, ((JSONObject) v).toString());
                        }
                    });
                    post.setEntity(builder.build());
                } else {
                    if (files.size() == 0) {
                        post.setEntity(new UrlEncodedFormEntity(data.entrySet().stream().map(entry -> {
                            return new BasicNameValuePair(entry.getKey(), entry.getValue());
                        }).collect(Collectors.toList())));
                    } else {
                        String key = files.keySet().toArray(new String[]{})[0];
                        Object value = files.get(key);
                        if (value instanceof File) {
                            if (contentTypes.get(key) == null) {
                                post.setEntity(new FileEntity((File) value));
                            } else {
                                post.setEntity(new FileEntity((File) value, contentTypes.get(key)));
                            }
                        } else {
                            post.setEntity(new StringEntity(((JSONObject) value).toString()));
                        }
                    }
                }

                headers.forEach((k, v) -> {
                    post.addHeader(k, v);
                });

                return post;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        public Post build() {
            return new Post(this);
        }
    }
}
