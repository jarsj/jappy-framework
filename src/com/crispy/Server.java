package com.crispy;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class Server implements ServletContextListener {
	
	private static ServletContext context;
	
	public static ServletContext getContext() {
		return context;
	}
	
	public Server() {
		mID = new AtomicLong(System.currentTimeMillis());
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		Log.info("core", "Shutting down server");
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		try {
			Log.info("core", "Initialized Server");
			Server.context = event.getServletContext();
			Dynamic d = context.addFilter("url-rewrite",
					Url.getInstance());
			d.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST),
					false, "/*");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	
	
	public static Server getInstance() {
		return INSTANCE;
	}
	
	private static Server INSTANCE = new Server();
	
	private AtomicLong mID;
	
	public long nextID() {
		return mID.getAndIncrement();
	}
}
	

