package com.crispy;

import java.util.concurrent.atomic.AtomicInteger;

public class CrawlStats {
	AtomicInteger total;
	AtomicInteger errors;
	AtomicInteger cache;
	AtomicInteger crawled;
	
	public CrawlStats() {
		total = new AtomicInteger(0);
		errors = new AtomicInteger(0);
		cache = new AtomicInteger(0);
		crawled = new AtomicInteger(0);
	}
}
