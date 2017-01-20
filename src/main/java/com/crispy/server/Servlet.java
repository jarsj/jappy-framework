package com.crispy.server;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.crispy.log.Log;
import com.crispy.template.Template;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.json.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic Servlet class that uses reflection to dedicate tasks to respective methods.
 * This might seem performance sensitive, but Java now compiles a method into bytecode
 * after 15 or so invocations.
 * <p>
 * Supports multipart/JSON input.
 */
public class Servlet extends HttpServlet {

    private Log LOG = Log.get("servlet");

    /**
     * TODO: What about delete/put ?
     */
    private MethodSpec[] getMethods;
    private MethodSpec[] postMethods;
    private MethodSpec exceptionMethod;
    private ConcurrentHashMap<String, String> serverAliases;

    private Meter mRequests;

    private static String getFileName(final Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    private MethodSpec[] methodsForAnnotation(Class<? extends Annotation> c) {
        ArrayList<MethodSpec> lMethods = new ArrayList<>();
        Method[] m = getClass().getDeclaredMethods();
        for (int i = 0; i < m.length; i++) {
            if (m[i].isAnnotationPresent(c)) {
                MethodSpec spec = new MethodSpec(m[i]);
                lMethods.add(spec);
            }
        }
        return lMethods.toArray(new MethodSpec[]{});
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        getMethods = methodsForAnnotation(GetMethod.class);
        postMethods = methodsForAnnotation(PostMethod.class);
        MethodSpec temp[] = methodsForAnnotation(ExceptionHandler.class);
        if (temp.length > 1)
            throw new ServletException("Only one exception handler is supported");
        if (temp.length > 0) {
            exceptionMethod = temp[0];
        }
        serverAliases = new ConcurrentHashMap<>();

        MetricRegistry registry = getMetricRegistry();
        mRequests = registry.meter(getClass().getName().toLowerCase() + ".requests");
    }

    protected void addServerAlias(String alias, String serverName) {
        serverAliases.put(serverName, alias);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.info("doPost " + req.getContextPath() + req.getPathInfo());
        doMethod(req, resp, postMethods);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.info("doGet " + req.getServerName() + "/" + req.getContextPath() + req.getPathInfo() + "?" + req
                .getQueryString());
        doMethod(req, resp, getMethods);
    }

    private void doMethod(HttpServletRequest req, HttpServletResponse resp, MethodSpec[] methods) throws
            ServletException,
            IOException {
        mRequests.mark();

        String path = req.getPathInfo();
        String[] pathComponents = StringUtils.split(path, "/");

        MethodSpec matching = null;

        for (int m = 0; m < methods.length; m++) {
            MethodSpec spec = methods[m];
            if (spec.serverName != null) {
                String lServer = req.getServerName().toLowerCase();
                String alias = serverAliases.getOrDefault(lServer, lServer);
                if (!alias.equals(spec.serverName))
                    continue;
            }
            if (spec.matches(pathComponents)) {
                if (matching == null) {
                    matching = spec;
                }
                else if (matching.serverName == null && spec.serverName != null) {
                    matching = spec;
                }
            }
        }

        if (matching == null) {
            resp.sendError(404);
            return;
        }

        if (matching.utf8) {
            req.setCharacterEncoding("UTF-8");
        }

        Params params = Params.withRequest(req);

        String mFormat = params.getString("_format", null);
        String mPretty = params.getString("_pretty", null);
        params.removeKeys("_format", "_pretty");

        LOG.debug("matched method " + matching.method.getName());

        Object[] args = new Object[matching.args.length];
        for (int a = 0; a < matching.args.length; a++) {
            ParamType pType = matching.argTypes[a];
            if (pType == null)
                throw new IllegalStateException("Argument " + a + " " + matching.args[a] + " has a null type");
            switch (pType) {
                case REQUEST: {
                    args[a] = req;
                    break;
                }
                case RESPONSE: {
                    args[a] = resp;
                    break;
                }
                case SESSION: {
                    args[a] = req.getSession(true);
                    break;
                }
                case FILE: {
                    args[a] = params.getFile(matching.args[a]);
                    break;
                }
                case PARAMS: {
                    args[a] = params;
                    break;
                }
                case SERVER: {
                    args[a] = serverAliases.getOrDefault(req.getServerName(), req.getServerName());
                    break;
                }
                default: {
                    int pl = matching.argLocationInPath[a];
                    if (pl != -1) {
                        args[a] = castObject(pathComponents[pl], pType);
                    } else {
                        if (matching.session[a]) {
                            HttpSession session = req.getSession(false);
                            if (session == null) {
                                args[a] = null;
                            } else {
                                args[a] = castObject(session.getAttribute(matching.args[a]), pType);
                            }
                        } else {
                            args[a] = castObject((String) params.getString(matching.args[a]), pType);
                        }
                    }
                }
            }
        }

        try {
            Object out = matching.method.invoke(this, args);
            // It's possible for methods to directly write to response or sendError/Redirects. In
            // which case it's an
            if (!resp.isCommitted()) {
                if (out == null && matching.templateName != null) {
                    throw new ServletException("Can not expand template " + matching.templateName + " as null was " +
                            "returned");
                }
                if (out != null) {
                    if (matching.utf8)
                        resp.setCharacterEncoding("UTF-8");
                    if (out instanceof Message) {
                        Message mOut = (Message) out;
                        if (Objects.equals(mFormat, "json")) {
                            resp.setContentType("application/json");
                            new JsonFormat().print(mOut, resp.getOutputStream());
                            resp.getOutputStream().flush();
                        } else {
                            if (matching.templateName != null) {
                                JSONObject data = new JSONObject(new JsonFormat().printToString(mOut));
                                String content = Template.byName(matching.templateName).expand(data);
                                resp.getWriter().write(content);
                                resp.getWriter().flush();
                            } else {
                                byte data[] = ((Message) out).toByteArray();
                                resp.setContentType("application/x-protobuf");
                                resp.setContentLength(data.length);
                                resp.getOutputStream().write(data);
                                resp.getOutputStream().flush();
                            }
                        }
                    } else if (out instanceof JSONObject) {

                        if (Objects.equals(mFormat, "json") || matching.templateName == null) {
                            if (mPretty == null) {
                                resp.getWriter().write(out.toString());
                            } else {
                                resp.getWriter().write(((JSONObject) out).toString(2));
                            }
                            resp.getWriter().flush();
                        } else {
                            JSONObject data = (JSONObject) out;
                            String content = Template.byName(matching.templateName).expand(data);
                            resp.getWriter().write(content);
                            resp.getWriter().flush();
                        }
                    } else if (out instanceof byte[]) {
                        resp.getOutputStream().write((byte[]) out);
                        resp.getOutputStream().flush();
                    } else {
                        resp.setCharacterEncoding("UTF-8");
                        resp.getWriter().write(out.toString());
                        resp.getWriter().flush();
                    }
                }
            }
        } catch (InvocationTargetException e) {
            LOG.error("domethod name=" + matching.method.getName() + " args=" + ArrayUtils.toString(args));
            if (exceptionMethod != null) {
                try {
                    exceptionMethod.method.invoke(this, e.getTargetException(), req, resp);
                } catch (Throwable t) {
                    throw new ServletException(t);
                }
            } else {
                throw new ServletException(e);
            }
        } catch (Exception e) {
            LOG.error("domethod name=" + matching.method.getName() + " args=" + ArrayUtils.toString(args));

            throw new ServletException(e);
        }
    }

    private Object castObject(Object o, ParamType type) {
        if (o == null)
            return null;
        if (type == ParamType.LONG) {
            if (o instanceof Long) return o;
            return Long.parseLong(o.toString());
        }
        if (type == ParamType.INT) {
            if (o instanceof Integer) return o;
            return Integer.parseInt(o.toString());
        }
        if (type == ParamType.DOUBLE) {
            if (o instanceof Double) return o;
            return Double.parseDouble(o.toString());
        }
        if (type == ParamType.BOOLEAN) {
            if (o == null)
                return false;
            if (o instanceof Boolean)
                return o;
            if (o.toString().equals("on"))
                return true;
            return Boolean.parseBoolean(o.toString());
        }
        return o;
    }

    enum ParamType {
        STRING,
        LONG,
        INT,
        BOOLEAN,
        DOUBLE,
        FILE,
        REQUEST,
        RESPONSE,
        SESSION,
        PARAMS,
        SERVER
    }

    class MethodSpec {
        Method method;

        String[] pathComponents;
        boolean[] session;
        String[] args;
        ParamType[] argTypes;
        int[] argLocationInPath;
        String templateName;

        String serverName;
        boolean utf8;

        MethodSpec(Method m) {
            utf8 = true;
            String path = null;
            serverName = null;
            {
                GetMethod annt = m.getAnnotation(GetMethod.class);
                if (annt != null) {
                    path = annt.path();
                    serverName = annt.server();
                    if (serverName.equals(""))
                        serverName = null;
                    templateName = annt.template();
                    if (templateName.equals(""))
                        templateName = null;
                    utf8 = annt.utf8();
                }
            }
            {
                PostMethod annt = m.getAnnotation(PostMethod.class);
                if (annt != null) {
                    path = annt.path();
                    serverName = annt.server();
                    if (serverName.equals(""))
                        serverName = null;
                    templateName = annt.template();
                    if (templateName.equals(""))
                        templateName = null;
                    utf8 = annt.utf8();
                }
            }

            if (templateName != null &&
                    !(JSONObject.class.isAssignableFrom(m.getReturnType())
                    || Message.class.isAssignableFrom(m.getReturnType()))) {
                throw new IllegalArgumentException("Methods using template must return a JSONObject or a Protocol " +
                        "Buffer");
            }

            pathComponents = StringUtils.split(path, '/');

            int P = m.getParameterCount();

            method = m;
            args = new String[P];
            argTypes = new ParamType[P];
            argLocationInPath = new int[P];
            session = new boolean[P];
            Parameter[] params = m.getParameters();

            for (int i = 0; i < P; i++) {
                session[i] = false;
                if (params[i].isAnnotationPresent(ServerParam.class)) {
                    argLocationInPath[i] = -1;
                    args[i] = null;
                    argTypes[i] = ParamType.SERVER;
                } else{
                    Param annP = params[i].getAnnotation(Param.class);
                    Session annS = params[i].getAnnotation(Session.class);

                    Class type = params[i].getType();
                    argLocationInPath[i] = -1;
                    session[i] = annS != null;

                    if (type.equals(String.class)) {
                        argTypes[i] = ParamType.STRING;
                        args[i] = getValue(annP, annS);
                        argLocationInPath[i] = ArrayUtils.indexOf(pathComponents, ":" + args[i]);
                    } else if (type.equals(Long.TYPE) || type.equals(Integer.TYPE) || type.equals(Short.TYPE)) {
                        if (type.equals(Long.TYPE)) {
                            argTypes[i] = ParamType.LONG;
                        } else {
                            argTypes[i] = ParamType.INT;
                        }
                        args[i] = getValue(annP, annS);
                        argLocationInPath[i] = ArrayUtils.indexOf(pathComponents, ":" + args[i]);
                    } else if (type.equals(Long.class) || type.equals(Integer.class)) {
                        if (type.equals(Long.class)) {
                            argTypes[i] = ParamType.LONG;
                        } else {
                            argTypes[i] = ParamType.INT;
                        }
                        args[i] = getValue(annP, annS);
                        argLocationInPath[i] = ArrayUtils.indexOf(pathComponents, ":" + args[i]);
                    } else if (type.equals(Boolean.TYPE) || type.equals(Boolean.class)) {
                        argTypes[i] = ParamType.BOOLEAN;
                        args[i] = getValue(annP, annS);
                        argLocationInPath[i] = ArrayUtils.indexOf(pathComponents, ":" + args[i]);
                    } else if (type.equals(Double.TYPE) || type.equals(Float.TYPE) || type.equals(Double.class) ||
                            type.equals(Float.class)) {
                        argTypes[i] = ParamType.DOUBLE;
                        args[i] = getValue(annP, annS);
                    } else if (type.equals(HttpServletRequest.class)) {
                        argTypes[i] = ParamType.REQUEST;
                        args[i] = null;
                    } else if (type.equals(HttpServletResponse.class)) {
                        argTypes[i] = ParamType.RESPONSE;
                        args[i] = null;
                    } else if (type.equals(File.class)) {
                        argTypes[i] = ParamType.FILE;
                        args[i] = getValue(annP, annS);
                    } else if (type.equals(Params.class)) {
                        argTypes[i] = ParamType.PARAMS;
                        args[i] = null;
                    } else if (type.equals(HttpSession.class)) {
                        argTypes[i] = ParamType.SESSION;
                        args[i] = null;
                    }
                }
            }
        }

        String getValue(Param appP, Session annS) {
            if (appP != null) return appP.value();
            if (annS != null) return annS.value();
            return null;
        }

        boolean matches(String[] comps) {
            if (pathComponents == null || pathComponents.length == 0) return (comps == null || comps.length == 0);
            if (comps == null)
                return false;
            if (pathComponents.length != comps.length)
                return false;

            for (int p = 0; p < pathComponents.length; p++) {
                if (pathComponents[p].startsWith(":")) {
                    String param = pathComponents[p].substring(1);
                    int paramIndex = ArrayUtils.indexOf(args, param);
                    if (paramIndex == -1)
                        return false;
                    ParamType pType = argTypes[paramIndex];
                    if (pType == ParamType.LONG) {
                        try {
                            Long.parseLong(comps[p]);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }

                    if (pType == ParamType.INT) {
                        try {
                            Integer.parseInt(comps[p]);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }

                    if (pType == ParamType.DOUBLE) {
                        try {
                            Double.parseDouble(comps[p]);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                } else {
                    if (!pathComponents[p].equals(comps[p])) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    protected MetricRegistry getMetricRegistry() {
        return SharedMetricRegistries.getOrCreate("jappy");
    }

    protected void setLogger(Log log) {
        LOG = log;
    }
}
