package com.crispy;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

@WebFilter("/*")
public class Filter implements javax.servlet.Filter {

	private Logger LOG;

	@Override
	public void destroy() {

	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		try {
			chain.doFilter(req, resp);
			if (response.getStatus() != 200) {
				if ((response.getStatus() % 100) == 5) {
					LOG.error(request.getRequestURI() + "?"
							+ request.getQueryString() + " gave status="
							+ response.getStatus());
				} else {
					LOG.debug(request.getRequestURI() + "?"
							+ request.getQueryString() + " gave status="
							+ response.getStatus());
				}
			}
		} catch (Throwable t) {
			LOG.error(
					request.getRequestURI() + "?" + request.getQueryString()
							+ " threw exception. \nRequest was made from "
							+ request.getRemoteAddr() + " using user-agent "
							+ request.getHeader("User-Agent"), t);
			if (t instanceof ServletException)
				throw (ServletException) t;
			if (t instanceof IOException)
				throw (IOException) t;
			throw new ServletException(t);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		LOG = Log.getInstance().getLogger("core");
	}
}
