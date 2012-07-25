package com.crispy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Url implements Filter {

	private ConcurrentHashMap<String, String> levelOne;
	private ConcurrentHashMap<String, String> levelTwo;
	private ConcurrentHashMap<String, String> levelTwoId;
	private ConcurrentHashMap<String, String[]> secured;

	private static Url INSTANCE = new Url();

	public static Url getInstance() {
		return INSTANCE;
	}

	private Url() {
		levelOne = new ConcurrentHashMap<String, String>();
		levelTwo = new ConcurrentHashMap<String, String>();
		levelTwoId = new ConcurrentHashMap<String, String>();
		secured = new ConcurrentHashMap<String, String[]>();
	}

	public void addRule(String parent, String redirect) {
		this.levelOne.put(parent, redirect);
	}

	public void addRule(String parent, String child, boolean id, String redirect) {
		if (id) {
			this.levelTwoId.put(parent + "_" + child, redirect);
		} else {
			this.levelTwo.put(parent + "_" + child, redirect);
		}
	}
	
	public void secure(String path, String attribute, String redirect) {
		secured.put(path, new String[]{attribute, redirect});
	}

	@Override
	public void destroy() {
	}
	
	private static ArrayList<String> removeEmpty(String[] comps) {
		ArrayList<String> ret = new ArrayList<String>();
		for (String s : comps) {
			s = s.trim().toLowerCase();
			if (s.length() > 0)
				ret.add(s);
		}
		return ret;
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		String path = request.getServletPath();
		List<String> components = removeEmpty(path.split("/"));
		
		if (components.size() < 1) {
			components.add("index.jsp");
		}

		// Now let's check for security stuff.
		if (secured.containsKey(components.get(0))) {
			String[] auth = secured.get(components.get(0));
			HttpSession session = request.getSession();
			boolean authenticated = false;
			if (session != null) {
				if (session.getAttribute(auth[0]) != null) {
					authenticated = true;
				}
			}
			
			if (!authenticated) {
				request.setAttribute("redirect", request.getRequestURI());
				response.sendRedirect(auth[1]);
				return;
			}
		}
		
		if (components.size() == 1) {
			String value = levelOne.get(components.get(0));
			if (value != null) {
				request.getRequestDispatcher(value).forward(req, resp);
				return;
			}
		} else if (components.size() == 2) {
			String joined = components.get(0) + "_" + components.get(1);
			String value = levelTwo.get(joined);
			if (value != null) {
				request.getRequestDispatcher(value).forward(req, resp);
				return;
			}
			
			value = levelOne.get(components.get(0));
			if (value != null) {
				String id = components.get(1);
				if (id.contains("-")) {
					id = id.substring(id.lastIndexOf('-') + 1);
					request.setAttribute("id", id);
				}
				request.getRequestDispatcher(value).forward(req, resp);
				return;
			}
		} else if (components.size() == 3) {
			String value = levelTwoId.get(components.get(0) + "_" + components.get(2));
			if (value != null) {
				String id = components.get(1);
				if (id.contains("-")) {
					id = id.substring(id.lastIndexOf('-') + 1);
					request.setAttribute("id", id);
				}
				request.getRequestDispatcher(value).forward(req, resp);
				return;
			}
		}
		
		chain.doFilter(req, resp);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
	
	public static String friendly(String name) {
		name = name.toLowerCase();
		StringBuilder sb = new StringBuilder();
		boolean ignoreWhitespace = false;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isWhitespace(c) && !ignoreWhitespace) {
				sb.append('-');
				ignoreWhitespace = true;
			} else if (Character.isLetterOrDigit(c)) {
				sb.append(c);
				ignoreWhitespace = false;
			}
		}
		return sb.toString();
	}

	public void setDefault(String def) {
		if (def.equals("/index.jsp"))
			throw new IllegalArgumentException("Index.JSP is already default");
		levelOne.put("index.jsp", def);
	}

}
