package com.crispy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/facebook/*")
public class Facebook extends HttpServlet {

	private static String appId;
	private static String secret;

	public static void init(String appId, String secret) {
		Facebook.appId = appId;
		Facebook.secret = secret;
		Table.get("facebook").columns(Column.bigInteger("fbuid"), Column.text("access_token")).primary("fbuid")
				.create();
	}

	public static String connect(String... permissions) throws Exception {
		return connect(null, null, permissions);
	}

	public static String connect(HttpSession session, String next, String... permissions) throws Exception {
		if (next != null)
			session.setAttribute("fb_next_url", next);
		return String
				.format("https://www.facebook.com/dialog/oauth?client_id=%s&redirect_uri=http://%s%s/facebook/auth_done&state=%d",
						appId, Server.getHost(), Server.getContext().getContextPath(), System.currentTimeMillis());
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		if (path.equals("/auth_done")) {
			HttpSession session = req.getSession();
			if (session.getAttribute("fb_next_url") != null) {
				resp.sendRedirect((String) session.getAttribute("fb_next_url"));
			} else {
				resp.sendRedirect(req.getContextPath());
			}
		}
	}
}
