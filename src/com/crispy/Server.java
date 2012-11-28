package com.crispy;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.catalina.filters.RemoteIpFilter;
import org.eclipse.jetty.webapp.WebAppContext;

@WebListener
public class Server implements ServletContextListener {
	
	private static ServletContext context;
	private static String host;
	private static org.eclipse.jetty.server.Server jettyServer;
	
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
			
			Dynamic d2 = context.addFilter("url-rewrite", Url.class);
					
			d2.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST),
					false, "/*");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Nothing to do while starting tomcat as context will get initialized automatically. 
	 * @param host
	 */
	public static void startTomcat(String host) {
		Server.host = host;
	}
	
	public static void startJetty(String host, int port) throws Exception {
		Server.host = host;
		
		jettyServer = new org.eclipse.jetty.server.Server(port);
		WebAppContext context = new WebAppContext();
		context.setResourceBase("web/");
		context.setContextPath("/");
		context.addServlet(DBAdmin.class, "/dbadmin/*");
		context.addServlet(Resource.class, "/resource/*");
		context.addFilter(Url.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		jettyServer.setHandler(context);
		jettyServer.start();
	}
	
	public static void stop() throws Exception {
		if (jettyServer != null) {
			jettyServer.stop();
		}
	}
	
	public static String getHost() {
		return host;
	}
}
	

