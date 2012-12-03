package com.crispy;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import sun.misc.BASE64Decoder;

@WebServlet("/facebook/*")
public class Facebook extends HttpServlet {

	private static String appId;
	private static String secret;
	private static String host;
	
	public static void init(String appId, String secret, String host) {
		Facebook.appId = appId;
		Facebook.secret = secret;
		Facebook.host = host;
		Table.get("facebook")
				.columns(Column.bigInteger("uid"), Column.text("access_token"), Column.text("username"),
						Column.text("name"), Column.timestamp("expires")).primary("uid").create();
	}
	
	public static JSONObject parseSignedRequest(String signedRequest) throws Exception {
		String payload = signedRequest.split(".")[1];
		BASE64Decoder decoder = new BASE64Decoder();
		payload = payload.replace("-", "+").replace("_", "/").trim();
		byte[] decoded = decoder.decodeBuffer(payload);
		payload = new String(decoded, "UTF8");
		return new JSONObject(payload);
	}

	public static String connect(String... permissions) throws Exception {
		return connect(null, null, permissions);
	}
	
	public static Row item(long uid) {
		return Table.get("facebook").columns("uid", "username", "name").where("uid", uid).row();
	}

	public static String connect(HttpSession session, String next, String... permissions) throws Exception {
		if (next != null)
			session.setAttribute("fb_next_url", next);
		return String
				.format("https://www.facebook.com/dialog/oauth?client_id=%s&redirect_uri=http://%s%s/facebook/auth_done&state=%d&scope=%s",
						appId, host, Server.getContextPath(), System.currentTimeMillis(), StringUtils.join(permissions, ","));
	}

	public static void publishAction(String action, String object_type, String object_url, HttpSession session) throws Exception {
		String accessToken = getAccessToken(session);
		if (accessToken == null)
			throw new IllegalStateException("Can't publish action without accessToken");
		Crawler.getInstance().post(
				String.format("https://graph.facebook.com/me/%s?%s=%s&access_token=%s", action, object_type, URLEncoder.encode(object_url), accessToken));
	}

	private static String getAccessToken(HttpSession session) {
		String uid = (String) session.getAttribute("uid");
		if (uid == null)
			return null;
		return Table.get("facebook").columns("access_token").where("uid", uid).row().columnAsString("access_token");
	}
	
	public static boolean isConnected(HttpSession session) {
		return session.getAttribute("uid") != null;
	}

	public static String username(HttpSession session) {
		String uid = (String) session.getAttribute("uid");
		if (uid == null)
			return null;
		String username = Table.get("facebook").columns("username").where("uid", uid).row().columnAsString("username");
		return username;
	}
	
	public static String profilePic(HttpSession session) {
		String username = username(session);
		if (username == null) {
			return "http://www.crabplace.com/images/facebook-profile-default.gif";
		}
		return String.format("http://graph.facebook.com/%s/picture?type=square", username);
	}

	public static void updateAccessToken(String token, long expiresIn, HttpSession session) throws Exception {
		String body = Crawler.getInstance().get(
				String.format("https://graph.facebook.com/me?access_token=%s", token));

		JSONObject me = new JSONObject(body);
		String uid = me.getString("id");
		String username = me.getString("username");
		String name = me.getString("name");

		Table.get("facebook").columns("uid", "access_token", "username", "name", "expires")
				.values(uid, token, username, name, expiresIn).ignore()
				.overwrite("access_token", "username", "name", "expires").add();
		session.setAttribute("uid", uid);
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
										appId, host, Server.getContextPath(), secret, code));
				String sp[] = body.split("&");
				String accessToken = sp[0].split("=")[1];
				long expiresIn = Integer.parseInt(sp[1].split("=")[1]);

				expiresIn = System.currentTimeMillis() + (expiresIn * 1000);

				HttpSession session = req.getSession();
				
				updateAccessToken(accessToken, expiresIn, session);
				
				if (session.getAttribute("fb_next_url") != null) {
					resp.sendRedirect((String) session.getAttribute("fb_next_url"));
				} else {
					resp.sendRedirect(Url.complete("/"));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
