package com.crispy;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

@WebServlet("/facebook/*")
public class Facebook extends HttpServlet {
	
	private static String appId;
	
	public static void init(String appId) {
		Facebook.appId = appId;
		Table.get("facebook").columns(Column.bigInteger("fbuid"), Column.text("access_token")).primary("fbuid")
				.create();
	}
	
	public static void connect(String fbuid, String accessToken) throws Exception {
		Table.get("facebook").columns("fbuid", "access_token").values(fbuid, accessToken).add();
	}
}
