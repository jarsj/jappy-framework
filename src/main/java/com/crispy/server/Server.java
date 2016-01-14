package com.crispy.server;

import com.crispy.utils.Pair;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.websocket.server.Constants;
import org.apache.tomcat.websocket.server.WsContextListener;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.util.*;

/**
 * @author harsh
 */
public class Server {

    public List<String> mDefaultPatterns;
    private Tomcat tomcat;
    private String baseDir;
    private List<Class<? extends HttpServlet>> mServlets;
    private List<ServletInfo> mServletObjects;
    private String welcomeFile;
    private HashMap<Class<? extends HttpServlet>, HashMap<String, String>> mInitParams;
    private static List<Class> websocketEndpoints = new ArrayList<>();

    public Server(String ip, int port) {
        tomcat = new Tomcat();
        tomcat.setPort(port);
        if (ip != null) {
            tomcat.getConnector().setProperty("address", ip);
        }
        mServlets = new ArrayList<Class<? extends HttpServlet>>();
        mServletObjects = new ArrayList<>();
        mInitParams = new HashMap<Class<? extends HttpServlet>, HashMap<String, String>>();
        websocketEndpoints = new ArrayList<>();
    }
    public Server(int port) {
        this(null, port);
    }

    public void setConnectorProperty(String key, String value) {
        tomcat.getConnector().setProperty(key, value);
    }

    public void addDefaultServletPatterns(String... patterns) {
        mDefaultPatterns = Arrays.asList(patterns);
    }

    public void addServlet(Class<? extends HttpServlet> servletClass) {
        mServlets.add(servletClass);
    }

    public void addEndpoint(Class c) {
        if (c.getAnnotation(ServerEndpoint.class) == null)
            throw new IllegalArgumentException("Missing annotation from class");
        websocketEndpoints.add(c);
    }

    public void addServlet(HttpServlet servlet, String[] urlPatterns, boolean multipart, int loadOrder) {
        ServletInfo info = new ServletInfo();
        info.servlet = servlet;
        info.urls = urlPatterns;
        info.isMultipart = multipart;
        info.loadOrder = loadOrder;
        mServletObjects.add(info);
    }

    public void addServlet(HttpServlet servlet, String[] urlPatterns) {
        addServlet(servlet, urlPatterns, false, -1);
    }

    public void addServlet(HttpServlet servlet) {
        WebServlet annotation = servlet.getClass().getAnnotation(WebServlet.class);
        if (annotation == null)
            throw new IllegalArgumentException("Needs annotation on the Servlet");
        addServlet(servlet, annotation.urlPatterns(), servlet.getClass().getAnnotation(MultipartConfig.class) != null,
                annotation.loadOnStartup());
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

        if (mDefaultPatterns == null || mDefaultPatterns.size() > 0) {
            Tomcat.addServlet(ctx, "default", new DefaultServlet());
        }
        if (mDefaultPatterns == null) {
            ctx.addServletMapping("/", "default");
        } else if (mDefaultPatterns.size() > 0) {
            for (String pattern : mDefaultPatterns) {
                ctx.addServletMapping(pattern, "default");
            }
        }

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
        for (ServletInfo info : mServletObjects) {
            // WebServlet annotation = servlet.getClass().getAnnotation(WebServlet.class);
            Wrapper wrapper = Tomcat.addServlet(ctx, info.servlet.getClass().getSimpleName(), info.servlet);
            if (info.isMultipart) {
                MultipartConfigElement config = new MultipartConfigElement("/tmp");
                wrapper.setMultipartConfigElement(config);
            }
            wrapper.setLoadOnStartup(info.loadOrder);
            for (String url : info.urls) {
                ctx.addServletMapping(url, info.servlet.getClass().getSimpleName());
            }
        }

        Http11NioProtocol protocol = (Http11NioProtocol) tomcat.getConnector().getProtocolHandler();
        tomcat.start();
        tomcat.getServer().await();
    }

    public void shutdown() throws LifecycleException {
        tomcat.stop();
    }

    public static class Config extends WsContextListener {
        @Override
        public void contextInitialized(ServletContextEvent sce) {
            super.contextInitialized(sce);
            ServerContainer sc = (ServerContainer) sce.getServletContext().getAttribute(Constants.SERVER_CONTAINER_SERVLET_CONTEXT_ATTRIBUTE);
            for (Class c : websocketEndpoints) {
                try {
                    sc.addEndpoint(c);
                } catch (DeploymentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ServletInfo {
        HttpServlet servlet;
        String[] urls;
        boolean isMultipart;
        Map<String, String> initParams;
        int loadOrder;
    }
}
