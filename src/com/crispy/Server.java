package com.crispy;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.servlets.MultiPartFilter;
import org.eclipse.jetty.util.MultiPartInputStream.MultiPart;
import org.eclipse.jetty.webapp.WebAppContext;

@WebListener
public class Server implements ServletContextListener {
	
	private static ServletContext tomcat_context;
	private static WebAppContext jetty_context;
	private static org.eclipse.jetty.server.Server jettyServer;
	
	public static String getContextPath() {
		if (tomcat_context != null)
			return tomcat_context.getContextPath();
		if (jetty_context != null)
			return jetty_context.getContextPath();
		return null;
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		Log.get("core").info("Shutting down server");
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		try {
			Log.get("core").info("Initialized Server");
			Server.tomcat_context = event.getServletContext();
			Dynamic d2 = tomcat_context.addFilter("url-rewrite", Url.class);
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
	public static void startTomcat() {
	}
	
	public static void startJetty(int port, Class ... servlets) throws Exception {
		jettyServer = new org.eclipse.jetty.server.Server(port);
		WebAppContext context = new WebAppContext();
		context.setResourceBase("web/");
		context.setContextPath("/");
		context.addServlet(DBAdmin.class, "/dbadmin/*");
		context.addServlet(Image.class, "/resource/*");
		context.addServlet(Image.class, "/resource");
		
		for (int i = 0; i < servlets.length; i++) {
			WebServlet annotation = (WebServlet) servlets[i].getAnnotation(WebServlet.class);
			for (String path : annotation.urlPatterns()) {
				context.addServlet(servlets[i], path);
			}
		}
		
		context.addFilter(Url.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		jettyServer.setHandler(context);
		jettyServer.start();
		
		Server.jetty_context = context;
	}
	
	
	
	public static void stop() throws Exception {
		if (jettyServer != null) {
			jettyServer.stop();
		}
	}
}

	

