package com.crispy;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.catalina.filters.RemoteIpFilter;
import org.apache.catalina.startup.Tomcat;

@WebListener
public class Server implements ServletContextListener {
	
	private static ServletContext context;
	private static String host;
	
	public static ServletContext getContext() {
		return context;
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
			
			Dynamic d1 = context.addFilter("remoteip", new RemoteIpFilter());
			d1.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
			
			Dynamic d2 = context.addFilter("url-rewrite",
					Url.getInstance());
			d2.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST),
					false, "/*");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public static void init(String host) {
		Server.host = host;
	}
	
	public static String getHost() {
		return host;
	}
}
	

