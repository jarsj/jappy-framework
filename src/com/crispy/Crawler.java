package com.crispy;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.*;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * 
 * @author harsh
 * 
 */
@WebServlet("/crawler/*")
public class Crawler extends HttpServlet implements Runnable {
	private ConcurrentHashMap<String, CrawlHandler> handlers;
	private ConcurrentHashMap<String, CrawlStats> stats;
	private static final Crawler INSTANCE = new Crawler();
	private DefaultHttpClient httpClient;
	private ScheduledExecutorService background;
	private ScheduledFuture<?> future;
	private Log LOG;

	private Crawler() {
		LOG = Log.get("crawler");
		handlers = new ConcurrentHashMap<String, CrawlHandler>();
	}

	public void setHandler(String tag, CrawlHandler handler) {
		handlers.put(tag, handler);
	}

	public void start() {
		if (!Cache.getInstance().isRunning()) {
			LOG.error("Cache is not configured");
			throw new IllegalStateException(
					"Crawling won't start unless cache is configured");
		}
		Table.get("crawl_queue").columns(Column.bigInteger("id", true),//
				Column.text("url", 512),//
				Column.integer("priority"),//
				Column.longtext("metadata")).indexes(Index.create("priority"))
				.create();

		httpClient = (DefaultHttpClient) wrapClient(new DefaultHttpClient());
		background = Executors.newSingleThreadScheduledExecutor();
		stats = new ConcurrentHashMap<String, CrawlStats>();
		future = background
				.scheduleWithFixedDelay(this, 0, 1, TimeUnit.SECONDS);
		LOG.info("CrawlerConstrucor called again");
	}

	private HttpClient wrapClient(HttpClient base) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void checkServerTrusted(
						java.security.cert.X509Certificate[] chain,
						String authType)
						throws java.security.cert.CertificateException {
					// TODO Auto-generated method stub

				}

				@Override
				public void checkClientTrusted(
						java.security.cert.X509Certificate[] chain,
						String authType)
						throws java.security.cert.CertificateException {
					// TODO Auto-generated method stub

				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			org.apache.http.conn.ssl.SSLSocketFactory ssf = new org.apache.http.conn.ssl.SSLSocketFactory(
					ctx);
			ssf.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, 443));
			return new DefaultHttpClient(ccm, base.getParams());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static Crawler getInstance() {
		return INSTANCE;
	}

	public void schedule(Job j) throws Exception {
		if (future == null)
			throw new IllegalStateException(
					"Scheduling job when there is no crawler running");
		LOG.info("Scheduling job url=" + j.getUrl());
		Table.get("crawl_queue")
				.columns("url", "priority", "metadata")
				.values(j.getUrl(), j.getPriority(), j.getMetadata().toString())
				.add();
	}

	public String get(String url) throws Exception {
		Job j = new Job(url, 0);
		return internalFetchAndCache(j);
	}

	public void post(String url) throws Exception {
		LOG.info("POST:" + url);
		HttpPost post = new HttpPost(url);
		HttpResponse response = httpClient.execute(post);
		if (response.getStatusLine().getStatusCode() == 200) {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				EntityUtils.consume(entity);
			}
		} else {
			EntityUtils.consume(response.getEntity());
		}
		System.out.println(response.getStatusLine().getStatusCode());
	}

	public void fetch(Job j) throws Exception {
		String data = lookupCache(j);
		if (data == null) {
			data = internalFetchAndCache(j);
		}
		CrawlHandler handler = handlers.get(j.getTag());
		if (handler != null)
			handler.ready(j, data);
	}

	private String lookupCache(Job j) throws Exception {
		if (j.getCacheKey() == null)
			return null;
		return Cache.getInstance().fetch(j.getCacheKey(), null);
	}

	private String internalFetchAndCache(Job j) throws Exception {
		String data = null;
		HttpGet get = new HttpGet(j.getUrl());
		for (Map.Entry<String, String> entry : j.getHeaders().entrySet()) {
			get.setHeader(entry.getKey(), entry.getValue());
		}
		if (j.getUserAgent() != null) {
			get.getParams().setParameter("http.useragent", j.getUserAgent());
		}
		HttpResponse response = httpClient.execute(get);
		if (response.getStatusLine().getStatusCode() == 200) {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				data = EntityUtils.toString(entity);
				EntityUtils.consume(entity);
			}
		} else {
			EntityUtils.consume(response.getEntity());
		}

		if (data != null && j.getCacheKey() != null) {
			Cache.getInstance().store(j.getCacheKey(), data, j.getExpiry());
		}

		return data;
	}

	@Override
	public void run() {
		try {
			LOG.trace("new-crawl-run");
			Row r = Table.get("crawl_queue").limit(1).ascending("priority")
					.row();
			if (r == null)
				return;
			Job j = new Job((String) r.column("url"),
					r.columnAsInt("priority"), null);
			JSONObject o = new JSONObject(r.columnAsString("metadata"));
			j.setMetadata(o);

			while (j != null) {
				LOG.info("new-crawl-job url=" + j.getUrl());
				CrawlStats cstats = stats.get(j.getCategory());
				if (cstats == null) {
					cstats = new CrawlStats();
					stats.put(j.getCacheKey(), cstats);
				}
				cstats.total.incrementAndGet();
				boolean cacheHit = false;
				try {
					LOG.info("begin-crawl url=" + j.getUrl() + " tag="
							+ j.getTag() + " handler="
							+ handlers.get(j.getTag()));
					String data = lookupCache(j);
					if (data == null) {
						LOG.info("cache-miss url=" + j.getUrl());
						data = internalFetchAndCache(j);
						cstats.crawled.incrementAndGet();
					} else {
						cacheHit = true;
						cstats.cache.incrementAndGet();
					}

					if (handlers.containsKey(j.getTag()))
						try {
							handlers.get(j.getTag()).ready(j, data);
						} catch (Throwable t) {
							LOG.error("Error in parsing", t);
						}
					else
						LOG.warn("Missing Handler for url=" + j.getUrl()
								+ " tag=" + j.getTag());
				} catch (Throwable t) {
					if (handlers.containsKey(j.getTag()))
						handlers.get(j.getTag()).ready(j, null);
					else
						LOG.warn("Missing Handler for url=" + j.getUrl()
								+ " tag=" + j.getTag());
					LOG.error("crawl-failed url=" + j.getUrl(), t);
					cstats.errors.incrementAndGet();
				}

				Table.get("crawl_queue").where("id", r.columnAsLong("id"))
						.delete();

				// If we got a cache hit let's try crawling one more time.
				if (cacheHit) {
					r = Table.get("crawl_queue").limit(1).ascending("priority")
							.row();
					if (r != null) {
						j = new Job((String) r.column("url"),
								r.columnAsInt("priority"), null);
						j.setMetadata(new JSONObject(r
								.columnAsString("metadata")));
					} else {
						break;
					}
				} else {
					break;
				}
			}
		} catch (Exception e) {
			future = null;
			LOG.error(e.getMessage(), e);
			throw new IllegalStateException(e);
		}
	}

	public void shutdown() {
		if (background != null)
			background.shutdownNow();
		if (httpClient != null)
			httpClient.getConnectionManager().shutdown();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
	}

}