package com.crispy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

@WebServlet(urlPatterns = { "/cache", "/cache/*" })
public class Cache extends HttpServlet {

	static Log LOG = Log.get("cache");

	public enum Expire {
		HOUR, TONIGHT, WEEKEND, MONTH;

		public long expires() {
			switch (this) {
			case HOUR:
				return Utils.endOfHour().getTimeInMillis();
			case TONIGHT:
				return Utils.midnight().getTimeInMillis();
			case WEEKEND:
				return Utils.weekend().getTimeInMillis();
			case MONTH:
				return Utils.monthLater().getTimeInMillis();
			}
			return 0;
		}
	}

	private static final Cache INSTANCE = new Cache();
	private boolean isRunning;
	private File cacheFolder;

	public Cache() {
		isRunning = false;
	}

	public void start(File folder) {
		Table.get("cache").columns(Column.text("key", 512), Column.longtext("value"), Column.timestamp("expires")).primary("key").create();
		this.cacheFolder = folder;
		isRunning = true;
	}

	public String fetch(String key, String def) throws Exception {
		String data = (String) DB

		.singleItemQuery("SELECT `value` FROM `cache` WHERE `key`=? AND `expires`>CURRENT_TIMESTAMP()", key);
		if (data == null)
			return def;
		return data;
	}

	public void store(String key, String value, long expire) throws Exception {
		if (expire == 0)
			return;
		Table.get("cache").columns("key", "value", "expires").values(key, value, expire).overwrite("value", "expires").add();
	}

	public void remove(String key) throws Exception {
		Table.get("cache").where("key", key).delete();
	}

	public void store(String key, String value, Expire e) throws Exception {
		store(key, value, e.expires());
	}

	public void shutdown() {

	}

	public static Cache getInstance() {
		return INSTANCE;
	}

	public boolean isRunning() {
		return isRunning;
	}
	
	public boolean existsInCache(URL u) {
		File f = new File(cacheFolder, u.getHost() + "/" + u.getPath());
		return f.exists();
	}

	public static String wrap(URL u) throws IOException {
		if (u == null)
			return "null";
		if (Cloud.localMode) {
			if (Cloud.connected) {
				Cache.getInstance().fetchUrl(u);
			}
			return "/cache/" + u.getHost() + (u.getPath().startsWith("/") ? "" : "/") + u.getPath();
		}
		return u.toString();
	}
	
	public static String wrap(URL u, int width, int height) throws IOException {
		if (u == null)
			return "null";
		if (Cloud.localMode) {
			if (Cloud.connected) {
				Cache.getInstance().fetchUrl(u, width, height);
			}
			return "/cache/" + u.getHost() + "/" + u.getPath() + "?w=" + width + "&h=" + height;
		}
		return u.toString();
	}
	
	public File fetchUrl(URL u) throws IOException {
		File f = new File(cacheFolder, u.getHost() + "/" + u.getPath());
		if (f.exists()) {
			LOG.info("cache-hit u=" + u.toString());
			return f;
		}
		LOG.info("cache-miss u=" + u.toString());
		FileUtils.copyURLToFile(u, f);
		return f;
	}
	
	public void storeUrl(URL u, File content) throws IOException {
		File f = new File(cacheFolder, u.getHost() + "/" + u.getPath());
		f.getParentFile().mkdirs();
		FileUtils.copyFile(content, f);
	}

	public File fetchUrl(URL u, int width, int height) throws IOException {
		File f = new File(cacheFolder, u.getHost() + "/" + u.getPath() + "#" + width + "," + height);
		if (f.exists()) {
			return f;
		}
		File orig = fetchUrl(u);
		File scaled = Image.scale(orig, width, height);
		FileUtils.copyFile(scaled, f);
		return f;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		File f = new File(INSTANCE.cacheFolder, path);
		resp.setContentType("image/" + FilenameUtils.getExtension(f.getName()));
		if (f.exists()) {
			FileInputStream fin = new FileInputStream(f);
			IOUtils.copy(fin, resp.getOutputStream());
			resp.getOutputStream().flush();
		} else if (Cloud.connected) { 
			if (!f.getParentFile().isDirectory()) {
				FileUtils.forceMkdir(f.getParentFile());
			}
			String url = "http:/" + path;
			URL u = new URL(url);
			InputStream in = u.openStream();
			FileOutputStream fout = new FileOutputStream(f);
			IOUtils.copy(in, fout);
			fout.flush();
			fout.close();
			
			IOUtils.copy(new FileInputStream(f), resp.getOutputStream());
			resp.getOutputStream().flush();
		} else if (req.getParameter("w") != null){
			int w = Integer.parseInt(req.getParameter("w"));
			int h = Integer.parseInt(req.getParameter("h"));
			
			BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
			ImageIO.write(bi, "png", resp.getOutputStream());
			resp.getOutputStream().flush();
		}
	}
}
