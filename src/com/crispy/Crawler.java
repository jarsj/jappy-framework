package com.crispy;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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

	private Crawler() {
		handlers = new ConcurrentHashMap<String, CrawlHandler>();
	}

	public void setHandler(String tag, CrawlHandler handler) {
		handlers.put(tag, handler);
	}

	public void start() {
		if (!Cache.getInstance().isRunning()) {
			Log.error("core", "Cache is not configured");
			throw new IllegalStateException(
					"Crawling won't start unless cache is configured");
		}
		Table.get("crawl_queue").columns(Column.bigInteger("id", true),//
				Column.text("url", 512),//
				Column.integer("priority"),//
				Column.longtext("metadata")).indexes(Index.create("priority"))
				.create();

		httpClient = new DefaultHttpClient();
		background = Executors.newSingleThreadScheduledExecutor();
		stats = new ConcurrentHashMap<String, CrawlStats>();
		future = background
				.scheduleWithFixedDelay(this, 0, 1, TimeUnit.SECONDS);
		Log.info("core", "CrawlerConstrucor called again");
	}

	public static Crawler getInstance() {
		return INSTANCE;
	}

	public void schedule(Job j) throws Exception {
		if (future == null)
			throw new IllegalStateException(
					"Scheduling job when there is no crawler running");
		Log.info("crawler", "Scheduling job url=" + j.getUrl());
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
		Log.info("crawler", "POST:" + url);
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
		final Logger LOG = Log.getInstance().getLogger("crawler");
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
					LOG.info("begin-crawl url=" + j.getUrl() + " tag=" + j.getTag() + " handler=" + handlers.get(j.getTag()));
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
			Log.error("crawler", e.getMessage(), e);
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