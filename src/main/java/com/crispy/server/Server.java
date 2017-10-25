package com.crispy.server;

import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.websocket.server.Constants;
import org.apache.tomcat.websocket.server.WsContextListener;

import javax.servlet.Filter;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author harsh
 */
public class Server {

    public List<String> mDefaultPatterns;
    private Tomcat tomcat;
    private String baseDir;
    private List<Class<? extends HttpServlet>> mServlets;
    private List<ServletInfo> mServletObjects;
    private boolean mHttps;
    private String welcomeFile;
    private HashMap<Class<? extends HttpServlet>, HashMap<String, String>> mInitParams;
    private static List<Class> websocketEndpoints = new ArrayList<>();
    private AtomicBoolean started;
    private List<FilterDef> filterDefs;
    private List<FilterMap> filterMaps;

    private String errorPage;
    private String notFound;

    public Server(String ip, int port) {
        tomcat = new Tomcat();
        mHttps = false;
        tomcat.setPort(port);
        started = new AtomicBoolean(false);
        baseDir = "";
        if (ip != null) {
            tomcat.getConnector().setProperty("address", ip);
        }
        mServlets = new ArrayList<Class<? extends HttpServlet>>();
        mServletObjects = new ArrayList<>();
        mInitParams = new HashMap<Class<? extends HttpServlet>, HashMap<String, String>>();
        websocketEndpoints = new ArrayList<>();
        filterDefs = new ArrayList<>();
        filterMaps = new ArrayList<>();
    }
    public Server(int port) {
        this(null, port);
    }

    public void enableHttps(int port, String keystoreFile, String keystorePass, String keyAlias) {
        if (mHttps)
            return;
        mHttps = true;
        Connector httpsConnector = new Connector();
        httpsConnector.setPort(port);
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");
        httpsConnector.setAttribute("keyAlias", keyAlias);
        httpsConnector.setAttribute("keystorePass", keystorePass);
        httpsConnector.setAttribute("keystoreFile", keystoreFile);
        httpsConnector.setAttribute("clientAuth", "false");
        httpsConnector.setAttribute("sslProtocol", "TLS");
        httpsConnector.setAttribute("SSLEnabled", true);

        tomcat.getService().addConnector(httpsConnector);
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

    public void addFilter(String filterName, Class<? extends Filter> filterClass, String ... patterns) {
        FilterDef fdef = new FilterDef();
        fdef.setFilterName(filterName);
        fdef.setFilterClass(filterClass.getName());
        filterDefs.add(fdef);

        FilterMap map = new FilterMap();
        map.setFilterName(filterName);
        for (String pattern : patterns) {
            map.addURLPattern(pattern);
        }
        filterMaps.add(map);
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

    public void setErrorPage(String errorPage) {
        this.errorPage = errorPage;
    }

    public void setNotFound(String notFound) {
        this.notFound = notFound;
    }

    public boolean isStarted() {
        return started.get();
    }

    public void startAsync() throws InterruptedException {
        new Thread(() -> {
            try {
                start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        while (!isStarted()) {
            Thread.sleep(1000);
        }
    }

    public void start() throws Exception {
        File docBase = new File(".");
        File ctxtDir = new File(docBase, baseDir);
        Context ctx = tomcat.addContext("", ctxtDir.getAbsolutePath());
        ctx.addApplicationListener(Config.class.getName());
        ctx.addMimeMapping("svg", "image/svg+xml");
        ctx.addMimeMapping("css", "text/css");
        ctx.addMimeMapping("js", "text/javascript");

        if (this.errorPage != null) {
            ErrorPage error = new ErrorPage();
            error.setExceptionType("java.lang.Exception");
            error.setLocation(this.errorPage);
            ctx.addErrorPage(error);
        }
        if (this.notFound != null) {
            ErrorPage notFound = new ErrorPage();
            notFound.setErrorCode(404);
            notFound.setLocation(this.notFound);
            ctx.addErrorPage(notFound);
        }

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

        for (FilterDef filter : filterDefs) {
            ctx.addFilterDef(filter);
        }

        for (FilterMap filter : filterMaps) {
            ctx.addFilterMap(filter);
        }

        for (Class<? extends HttpServlet> servlet : mServlets) {
            WebServlet annotation = servlet.getAnnotation(WebServlet.class);
            String name = annotation.name();
            if (name.length() == 0)
                name = servlet.getSimpleName();
            Wrapper wrapper = Tomcat.addServlet(ctx, name, servlet.newInstance());
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
                ctx.addServletMapping(url, name);
            }
        }
        for (ServletInfo info : mServletObjects) {
            WebServlet annotation = info.servlet.getClass().getAnnotation(WebServlet.class);
            String name = "";
            if (annotation != null)
                name = annotation.name();
            if (name.length() == 0)
                name = info.servlet.getClass().getSimpleName();

            Wrapper wrapper = Tomcat.addServlet(ctx, name, info.servlet);
            if (info.isMultipart) {
                MultipartConfigElement config = new MultipartConfigElement("/tmp");
                wrapper.setMultipartConfigElement(config);
            }
            wrapper.setLoadOnStartup(info.loadOrder);
            for (String url : info.urls) {
                ctx.addServletMapping(url, name);
            }
        }

        Http11NioProtocol protocol = (Http11NioProtocol) tomcat.getConnector().getProtocolHandler();
        tomcat.getServer().addLifecycleListener(new LifecycleListener() {
            @Override
            public void lifecycleEvent(LifecycleEvent event) {
                if (event.getType().equals("after_start")) {
                    started.set(true);
                }
            }
        });
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
