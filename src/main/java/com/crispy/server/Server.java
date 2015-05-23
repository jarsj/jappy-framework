package com.crispy.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.websocket.server.WsContextListener;

/**
 * 
 * 
 * 
 * @author harsh
 */
public class Server {
	
	public static class Config extends WsContextListener {
		@Override
		public void contextInitialized(ServletContextEvent sce) {
			super.contextInitialized(sce);
		}
	}
	
	private int port;
	private Tomcat tomcat;
	private String baseDir;
	private List<Class<? extends HttpServlet>> mServlets;
		
	public Server(int port) {
		this.port = port;
		tomcat = new Tomcat();
		tomcat.setPort(port);
		mServlets = new ArrayList<Class<? extends HttpServlet>>();
	}
		
	public void addServlet(Class<? extends HttpServlet> servletClass) {
		mServlets.add(servletClass);
	}
	
	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}
	
	public void start() throws Exception {
		File docBase = new File(".");
		
		Context ctx = tomcat.addContext("", new File(docBase, baseDir).getAbsolutePath());
		ctx.addApplicationListener(Config.class.getName());
		
        Tomcat.addServlet(ctx, "default", new DefaultServlet());
        
        ctx.addServletMapping("/", "default");
        
        for (Class<? extends HttpServlet> servlet : mServlets) {
        	WebServlet annotation = servlet.getAnnotation(WebServlet.class);
        	Wrapper wrapper = Tomcat.addServlet(ctx, servlet.getSimpleName(), servlet.newInstance());
        	if (servlet.getAnnotation(MultipartConfig.class) != null) {
        		MultipartConfigElement config = new MultipartConfigElement("/tmp");
        		wrapper.setMultipartConfigElement(config);
        	}
        	wrapper.setLoadOnStartup(annotation.loadOnStartup());
        	for (String url : annotation.urlPatterns()) {
        		ctx.addServletMapping(url, servlet.getSimpleName());
        		
        	}
        }
        Http11NioProtocol protocol = (Http11NioProtocol) tomcat.getConnector().getProtocolHandler();
		tomcat.start();
		
		tomcat.getServer().await();
	}
	
	public void shutdown() throws LifecycleException {
		tomcat.stop();
	}
}
