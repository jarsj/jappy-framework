package com.crispy;

import java.net.URLEncoder;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import sun.misc.BASE64Decoder;

@WebServlet("/facebook/*")
public class Facebook extends HttpServlet {

	public static JSONObject parseSignedRequest(String signedRequest)
			throws Exception {
		String payload = signedRequest.split(".")[1];
		BASE64Decoder decoder = new BASE64Decoder();
		payload = payload.replace("-", "+").replace("_", "/").trim();
		byte[] decoded = decoder.decodeBuffer(payload);
		payload = new String(decoded, "UTF8");
		return new JSONObject(payload);
	}

	public static void publishAction(String accessToken, String action,
			String object_type, String object_url, HttpSession session)
			throws Exception {
		Crawler.getInstance()
				.post(String
						.format("https://graph.facebook.com/me/%s?%s=%s&access_token=%s",
								action, object_type,
								URLEncoder.encode(object_url), accessToken));
	}

	public static JSONArray getFriends(String accessToken, String... fields)
			throws Exception {
		try {
			String url = String
					.format("https://graph.facebook.com/me/friends?access_token=%s&fields=%s",
							accessToken, StringUtils.join(fields, ","));
			System.out.println(url);
			String response = Crawler
					.getInstance()
					.get(url);
			JSONObject ret = new JSONObject(response);
			return ret.getJSONArray("data");
		} catch (Exception e) {
			return null;
		}
	}
}
