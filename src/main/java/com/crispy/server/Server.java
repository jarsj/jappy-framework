package com.crispy.server;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.websocket.server.WsContextListener;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	private Tomcat tomcat;
	private String baseDir;
	private List<Class<? extends HttpServlet>> mServlets;
	private String welcomeFile;
	private HashMap<Class<? extends HttpServlet>, HashMap<String, String>> mInitParams;
	
	public Server(String ip, int port) {
		tomcat = new Tomcat();
		tomcat.setPort(port);
		if (ip != null) {
			tomcat.getConnector().setProperty("address", ip);
		}
		mServlets = new ArrayList<Class<? extends HttpServlet>>();
		mInitParams = new HashMap<Class<? extends HttpServlet>, HashMap<String, String>>();
	}
	
	public void setConnectorProperty(String key, String value) {
		tomcat.getConnector().setProperty(key, value);
	}
	
	public Server(int port) {
		this(null, port);
	}
		
	public void addServlet(Class<? extends HttpServlet> servletClass) {
		mServlets.add(servletClass);
	}

	public void addServlet(Class<? extends HttpServlet> servletClass, Map<String, String> initParams) {
		mServlets.add(servletClass);
		mInitParams.put(servletClass, new HashMap<String, String>(initParams));
	}
		
	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}
	
	public void setWelcomeFile(String wf) {
		this.welcomeFile = wf;
	}
	
	public void start() throws Exception {
		File docBase = new File(".");
		
		Context ctx = tomcat.addContext("", new File(docBase, baseDir).getAbsolutePath());
		ctx.addApplicationListener(Config.class.getName());
		if (this.welcomeFile != null)
        	ctx.addWelcomeFile(welcomeFile);
		
        Tomcat.addServlet(ctx, "default", new DefaultServlet());
                
        ctx.addServletMapping("/", "default");
                        
        for (Class<? extends HttpServlet> servlet : mServlets) {
        	WebServlet annotation = servlet.getAnnotation(WebServlet.class);
        	Wrapper wrapper = Tomcat.addServlet(ctx, servlet.getSimpleName(), servlet.newInstance());
            if (mInitParams.containsKey(servlet)) {
                mInitParams.get(servlet).forEach((k, v) -> {
                    wrapper.addInitParameter(k, v);
                });
            }

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
