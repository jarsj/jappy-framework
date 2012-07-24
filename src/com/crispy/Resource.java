package com.crispy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

@WebServlet("/resource/*")
public class Resource extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			IOUtils.copy(getClass().getResourceAsStream(req.getPathInfo()),
					resp.getOutputStream());
			resp.getOutputStream().flush();
		} catch (Exception e) {
			resp.sendError(404);
		}
	}
}
