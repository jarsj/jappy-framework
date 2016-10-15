package com.crispy.net;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by harsh on 11/15/15.
 */
public class Get {
    private GetBuilder mGet;

    private Get(GetBuilder get) {
        this.mGet = get;
    }

    HttpGet createHttpGet() {
        return mGet.create();
    }

    public GetBuilder builder() {
        return new GetBuilder(mGet);
    }

    public static GetBuilder builder(String url) {
        return new GetBuilder(url);
    }

    public static class GetBuilder {
        String url;
        List<NameValuePair> params;
        HashMap<String, String> headers;

        GetBuilder(String url) {
            this.url = url;
            params = new ArrayList<>();
            headers = new HashMap<>();
        }

        GetBuilder(GetBuilder old) {
            this.url = old.url;
            this.params = new ArrayList<>(old.params);
            this.headers = new HashMap<>(old.headers);
        }

        public GetBuilder addParam(String key, String value) {
            params.add(new BasicNameValuePair(key, value));
            return this;
        }

        public GetBuilder clearParams() {
            params.clear();
            return this;
        }

        public Get build() {
            return new Get(this);
        }

        HttpGet create() {
            String separator = "?";
            if (url.contains("?")) {
                if (url.endsWith("?")) separator = "";
                separator = "&";
            }
            HttpGet get = new HttpGet(url + separator + URLEncodedUtils.format(params, "UTF-8"));
            headers.forEach((k, v) -> {
                get.addHeader(k, v);
            });
            return get;
        }
    }
}
