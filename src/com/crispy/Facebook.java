package com.crispy;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

@WebServlet("/facebook/*")
public class Facebook extends HttpServlet {

	private static String appId;
	private static String secret;

	public static void init(String appId, String secret) {
		Facebook.appId = appId;
		Facebook.secret = secret;
		Table.get("facebook")
				.columns(Column.bigInteger("uid"), Column.text("access_token"), Column.text("username"),
						Column.text("name"), Column.timestamp("expires")).primary("uid").create();
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
			String code = req.getParameter("code");
			try {
				String body = Crawler
						.getInstance()
						.get(String
								.format("https://graph.facebook.com/oauth/access_token?client_id=%s&redirect_uri=http://%s%s/facebook/auth_done&client_secret=%s&code=%s",
										appId, Server.getHost(), Server.getContext().getContextPath(), secret, code));
				String sp[] = body.split("&");
				String accessToken = sp[0].split("=")[1];
				long expiresIn = Integer.parseInt(sp[1].split("=")[1]);
				
				expiresIn = System.currentTimeMillis() + (expiresIn * 1000);
				
				body = Crawler.getInstance().get(
						String.format("https://graph.facebook.com/me?access_token=%s", accessToken));

				JSONObject me = new JSONObject(body);
				String uid = me.getString("id");
				String username = me.getString("username");
				String name = me.getString("name");

				Table.get("facebook").columns("uid", "access_token", "username", "name", "expires")
						.values(uid, accessToken, username, name, expiresIn).ignore()
						.overwrite("access_token", "username", "name", "expires").add();
				
				HttpSession session = req.getSession();
				session.setAttribute("uid", uid);
				
				if (session.getAttribute("fb_next_url") != null) {
					resp.sendRedirect((String) session.getAttribute("fb_next_url"));
				} else {
					resp.sendRedirect(Url.complete("/"));
				}
			} catch (Exception e) {
			}
		}
	}
}
