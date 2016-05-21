package com.crispy.server;

import com.google.protobuf.Message;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic Servlet class that uses reflection to dedicate tasks to respective methods.
 * This might seem performance sensitive, but Java now compiles a method into bytecode
 * after 15 or so invocations.
 * <p>
 * Supports multipart/JSON input.
 */
public class Servlet extends HttpServlet {

    /**
     * TODO: What about delete/put ?
     */
    private MethodSpec[] getMethods;
    private MethodSpec[] postMethods;
    private ConcurrentHashMap<String, String> serverAliases;

    /**
     * Convert a string to param
     *
     * @param s
     * @param p
     * @return
     */
    static Object paramAsObject(String s, ParamType p, String defValue) {
        if (s == null)
            s = defValue;
        if (p == ParamType.LONG) {
            return Long.parseLong(s);
        }
        if (p == ParamType.INT) {
            return Integer.parseInt(s);
        }
        if (p == ParamType.DOUBLE) {
            return Double.parseDouble(s);
        }
        if (p == ParamType.BOOLEAN) {
            if (s == null)
                return false;
            if (s.equals("on"))
                return true;
            return Boolean.parseBoolean(s);
        }
        return s;
    }

    private static String getFileName(final Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    private static Object getRequestParameter(HttpServletRequest req, JSONObject jsonInput, boolean isMultipart, String
            name) throws IOException, ServletException {
        if (isMultipart) {
            Part part = req.getPart(name);
            if (part == null)
                return null;
            String fileName = getFileName(part);
            if (fileName == null) {
                return IOUtils.toString(part.getInputStream());
            } else {
                File temp = File.createTempFile("tmp", "." + FilenameUtils.getExtension(fileName));
                part.write(temp.getAbsolutePath());
                return temp;
            }
        } else if (jsonInput != null) {
            return jsonInput.optString(name, null);
        } else {
            return req.getParameter(name);
        }
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
        serverAliases = new ConcurrentHashMap<>();
    }

    protected void addServerAlias(String alias, String serverName) {
        serverAliases.put(serverName, alias);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doMethod(req, resp, postMethods);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doMethod(req, resp, getMethods);
    }

    private void doMethod(HttpServletRequest req, HttpServletResponse resp, MethodSpec[] methods) throws
            ServletException,
            IOException {
        String path = req.getPathInfo();
        String[] pathComponents = StringUtils.split(path, "/");

        Params params = Params.withRequest(req);

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

        Object[] args = new Object[matching.args.length];
        for (int a = 0; a < matching.args.length; a++) {
            ParamType pType = matching.argTypes[a];
            switch (pType) {
                case REQUEST: {
                    args[a] = req;
                    break;
                }
                case RESPONSE: {
                    args[a] = resp;
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
                        args[a] = paramAsObject(pathComponents[pl], pType, matching.defValues[a]);
                    } else {
                        args[a] = paramAsObject((String) params.getString(matching.args[a]), pType, matching
                                .defValues[a]);
                    }
                }
            }
        }

        try {
            Object out = matching.method.invoke(this, args);
            // It's possible for methods to directly write to response or sendError/Redirects. In
            // which case it's an
            if (!resp.isCommitted()) {
                if (out != null) {
                    if (out instanceof Message) {
                        byte data[] = ((Message) out).toByteArray();
                        resp.setContentType("application/x-protobuf");
                        resp.setContentLength(data.length);
                        resp.getOutputStream().write(data);
                        resp.getOutputStream().flush();
                    } else if (out instanceof byte[]) {
                        resp.getOutputStream().write((byte[]) out);
                        resp.getOutputStream().flush();
                    } else {
                        resp.getWriter().write(out.toString());
                        resp.getWriter().flush();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
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
        PARAMS,
        SERVER
    }

    class MethodSpec {
        Method method;

        String[] pathComponents;
        String[] args;
        ParamType[] argTypes;
        String[] defValues;
        int[] argLocationInPath;
        String serverName;

        MethodSpec(Method m) {
            String path = null;
            serverName = null;
            {
                GetMethod annt = m.getAnnotation(GetMethod.class);
                if (annt != null) {
                    path = annt.path();
                    serverName = annt.server();
                    if (serverName.equals(""))
                        serverName = null;
                }

            }
            {
                PostMethod annt = m.getAnnotation(PostMethod.class);
                if (annt != null) {
                    path = annt.path();
                    serverName = annt.server();
                    if (serverName.equals(""))
                        serverName = null;
                }
            }


            pathComponents = StringUtils.split(path, '/');

            int P = m.getParameterCount();

            method = m;
            args = new String[P];
            defValues = new String[P];
            argTypes = new ParamType[P];
            argLocationInPath = new int[P];
            Parameter[] params = m.getParameters();

            for (int i = 0; i < P; i++) {
                if (params[i].isAnnotationPresent(ServerParam.class)) {
                    argLocationInPath[i] = -1;
                    args[i] = null;
                    argTypes[i] = ParamType.SERVER;
                } else{
                    Param annotation = params[i].getAnnotation(Param.class);
                    Class type = params[i].getType();
                    argLocationInPath[i] = -1;

                    if (type.equals(String.class)) {
                        argTypes[i] = ParamType.STRING;
                        args[i] = annotation.value();
                        defValues[i] = annotation.def();
                        argLocationInPath[i] = ArrayUtils.indexOf(pathComponents, ":" + args[i]);
                    } else if (type.equals(Long.TYPE) || type.equals(Integer.TYPE) || type.equals(Short.TYPE)) {
                        if (type.equals(Long.TYPE)) {
                            argTypes[i] = ParamType.LONG;
                        } else {
                            argTypes[i] = ParamType.INT;
                        }
                        args[i] = annotation.value();
                        defValues[i] = annotation.def();
                        argLocationInPath[i] = ArrayUtils.indexOf(pathComponents, ":" + args[i]);
                    } else if (type.equals(Boolean.TYPE)) {
                        argTypes[i] = ParamType.BOOLEAN;
                        args[i] = annotation.value();
                        defValues[i] = annotation.def();
                        argLocationInPath[i] = ArrayUtils.indexOf(pathComponents, ":" + args[i]);
                    } else if (type.equals(Double.TYPE) || type.equals(Float.TYPE)) {
                        argTypes[i] = ParamType.DOUBLE;
                        args[i] = annotation.value();
                        defValues[i] = annotation.def();
                    } else if (type.equals(HttpServletRequest.class)) {
                        argTypes[i] = ParamType.REQUEST;
                        args[i] = null;
                    } else if (type.equals(HttpServletResponse.class)) {
                        argTypes[i] = ParamType.RESPONSE;
                        args[i] = null;
                    } else if (type.equals(File.class)) {
                        argTypes[i] = ParamType.FILE;
                        args[i] = annotation.value();
                    } else if (type.equals(Params.class)) {
                        argTypes[i] = ParamType.PARAMS;
                        args[i] = null;
                    }
                }
            }
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
}
