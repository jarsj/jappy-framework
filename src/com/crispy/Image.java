package com.crispy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

@WebServlet(urlPatterns = { "/images", "/images/*" })
public class Image extends HttpServlet {

	private AtomicLong mID;
	private String uploadFolder;
	private static Image INSTANCE = new Image();
	
	public static Image getInstance() {
		return INSTANCE;
	}
	
	public Image() {
		mID = new AtomicLong(System.currentTimeMillis());
	}
	
	public void setUploadFolder(String uploadFolder) {
		this.uploadFolder = uploadFolder;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String fileName = req.getPathInfo().substring(1);
		String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
		if (extension.equals("jpg")) {
			extension = "jpeg";
		}
		resp.setContentType("image/" + extension);
		IOUtils.copy(new FileInputStream(new File(Image.getInstance()
				.uploadFolder, fileName)), resp.getOutputStream());
		resp.getOutputStream().flush();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			long nextID = mID.incrementAndGet();
			String sourceFileName = req.getHeader("X-File-Name");
			String ext = sourceFileName.substring(sourceFileName
					.lastIndexOf('.') + 1);
			File f = new File(Image.getInstance().uploadFolder, nextID
					+ "." + ext);
			IOUtils.copy(req.getInputStream(), new FileOutputStream(f));

			resp.getWriter().write(
					new JSONObject().put("success", true)
							.put("value", nextID + "." + ext).toString());
			resp.getWriter().flush();
		} catch (Exception e) {
			e.printStackTrace();
			resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().print("{success: false}");
			resp.getWriter().flush();
		}
	}

	public String getURL(String reference, Size size) {
		return "/images/" + reference;
	}
}
