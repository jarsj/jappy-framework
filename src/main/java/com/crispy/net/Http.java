package com.crispy.net;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by harsh on 7/1/16.
 */
public class Http implements Closeable {

    private CloseableHttpClient client;
    private ScheduledExecutorService background;

    private Http(HttpClientBuilder builder, int numThreads) {
        client = builder.build();
        if (numThreads > 0) {
            background = Executors.newScheduledThreadPool(numThreads);
        }
    }

    public static HttpBuilder builder() {
        return new HttpBuilder();
    }

    public void execute(Get get, Consumer<Response> callback) {
        asyncRequest(get.createHttpGet(), callback);
    }

    public void execute(Post post, Consumer<Response> callback) {
        asyncRequest(post.createHttpPost(), callback);
    }

    public Response execute(Get get) {
        return request(get.createHttpGet());
    }

    public Response execute(Post post) {
        return request(post.createHttpPost());
    }

    public Response get(String url) {
        return request(new HttpGet(url));
    }

    public void get(String url, Consumer<Response> callback) {
        asyncRequest(new HttpGet(url), callback);
    }

    public void delete(String url, Consumer<Response> callback) {
        asyncRequest(new HttpDelete(url), callback);
    }

    private void asyncRequest(HttpUriRequest request, Consumer<Response> callback) {
        Runnable r = () -> {
            try {
                Response resp = client.execute(request, new ResponseHandler<Response>() {
                    @Override
                    public Response handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                        return new Response(response);
                    }
                });
                callback.accept(resp);
            } catch (Throwable t) {
                callback.accept(null);
            }
        };
        if (background == null) {
            r.run();
        } else {
            background.execute(r);
        }

    }

    private Response request(HttpUriRequest request) {
        try {
            Response resp = client.execute(request, new ResponseHandler<Response>() {
                @Override
                public Response handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    return new Response(response);
                }
            });
            return resp;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        if (background != null) {
            try {
                background.shutdown();
                background.awaitTermination(10, TimeUnit.SECONDS);
            } catch (Exception e) {
            }
        }
        client.close();
    }

    public static class HttpBuilder {
        private HttpClientBuilder builder;
        private int numThreads;

        HttpBuilder() {
            this.builder = HttpClientBuilder.create();
            this.numThreads = -1;
        }

        public HttpBuilder enableCookies() {
            builder.setDefaultCookieStore(new BasicCookieStore());
            return this;
        }

        public HttpBuilder setAsync(int threads) {
            this.numThreads = threads;
            return this;
        }

        public HttpBuilder setMaxConnections(int m) {
            builder.setMaxConnTotal(m);
            return this;
        }

        public HttpBuilder setMaxConnectionsPerRoute(int m) {
            builder.setMaxConnPerRoute(m);
            return this;
        }

        public HttpBuilder withTimeoutInSeconds(int connect, int socket) {
            RequestConfig.Builder config = RequestConfig.custom();
            if (connect > 0) {
                config.setConnectTimeout(connect * 1000);
            }
            if (socket > 0) {
                config.setSocketTimeout(socket * 1000);
            }
            builder.setDefaultRequestConfig(config.build());
            return this;
        }

        public Http build() {
            return new Http(builder, numThreads);
        }
    }
}
