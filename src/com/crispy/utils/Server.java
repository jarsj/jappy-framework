package com.crispy.utils;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.webapp.WebAppContext;

import com.crispy.db.DBAdmin;
import com.crispy.log.Log;
import com.crispy.net.Url;

@WebListener
public class Server implements ServletContextListener {
	
	private static ServletContext tomcat_context;
	
	public static String getContextPath() {
		if (tomcat_context != null)
			return tomcat_context.getContextPath();
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
}

	

