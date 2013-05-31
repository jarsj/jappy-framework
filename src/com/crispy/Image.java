package com.crispy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.imgscalr.Scalr;
import org.json.JSONObject;

@WebServlet(urlPatterns = { "/resource", "/resource/*" })
public class Image extends HttpServlet {

	private static final Log LOG = Log.get("resource");
	private static AtomicLong mID = new AtomicLong(System.currentTimeMillis());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String fileName = req.getPathInfo();
		LOG.info(fileName);
		if (fileName.startsWith("/class")) {
			doClass(fileName.substring(fileName.indexOf('/', 1)), resp);
		} else if (fileName.startsWith("/local")) {
			doLocal(fileName.substring(fileName.indexOf('/', 1)), resp);
		}
	}

	private void doClass(String path, HttpServletResponse resp)
			throws IOException {
		LOG.info(path);
		IOUtils.copy(getClass().getResourceAsStream(path),
				resp.getOutputStream());
		resp.getOutputStream().flush();
	}

	private void doLocal(String path, HttpServletResponse resp)
			throws IOException {
		String extension = path.substring(path.lastIndexOf('.') + 1);
		if (extension.equals("jpg")) {
			extension = "jpeg";
		}
		File realFile = new File(path);
		if (!realFile.exists()) {
			resp.setStatus(402);
			return;
		}
		resp.setContentType("image/" + extension);
		IOUtils.copy(new FileInputStream(realFile), resp.getOutputStream());
		resp.getOutputStream().flush();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			String sourceFileName = req.getHeader("X-File-Name");
			String uploadFolder = req.getParameter("folder");
			String s3Bucket = req.getParameter("bucket");

			if (uploadFolder != null) {
				resp.getWriter().write(
						new JSONObject()
								.put("success", true)
								.put("value",
										uploadFile(uploadFolder,
												req.getInputStream(),
												sourceFileName)).toString());
				resp.getWriter().flush();
			} else if (s3Bucket != null) {
				resp.getWriter().write(
						new JSONObject()
								.put("success", true)
								.put("value",
										uploadS3(s3Bucket,
												req.getInputStream(),
												sourceFileName)).toString());
				resp.getWriter().flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
			resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().print("{success: false}");
			resp.getWriter().flush();
		}
	}

	static String uploadFile(String uploadFolder, InputStream input,
			String fileName) throws FileNotFoundException, IOException {
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		long nextID = mID.incrementAndGet();
		File folder = new File(uploadFolder);
		folder.mkdirs();
		File f = new File(folder, nextID + "." + ext);
		IOUtils.copy(input, new FileOutputStream(f));
		return f.getAbsolutePath().toString();

	}

	static String uploadS3(String s3Bucket, InputStream input, String fileName)
			throws FileNotFoundException, IOException {
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		long nextID = mID.incrementAndGet();
		String s3Comps[] = s3Bucket.split("/");

		String bucket = s3Comps[0];
		s3Comps = (String[]) ArrayUtils.remove(s3Comps, 0);
		String parent = (s3Comps.length == 0) ? "" : (StringUtils.join(s3Comps,
				"/") + "/");

		File tmp = File.createTempFile("image", ext);
		IOUtils.copy(input, new FileOutputStream(tmp));
		Cloud.s3(bucket).create().allowRead().neverExpire()
				.upload(parent + nextID + "." + ext, tmp);
		return "http://" + bucket + ".s3.amazonaws.com/" + parent + nextID
				+ "." + ext;
	}

	public static File scale(File source, int width, int height)
			throws IOException {
		BufferedImage image = JAI.create("fileload", source.getAbsolutePath())
				.getAsBufferedImage();
		String extension = source.getName().substring(
				source.getName().lastIndexOf(".") + 1);
		return internalScale(image, extension, width, height);
	}
	
	public static int[] dimensions(URL source) throws IOException {
		BufferedImage image = ImageIO.read(Cache.getInstance().fetchUrl(source));
		if (image == null) {
			return new int[]{-1,-1};
		}
		return new int[]{image.getWidth(), image.getHeight()};
	}

	public static File scale(File source, int size) throws IOException {
		BufferedImage image = JAI.create("fileload", source.getAbsolutePath())
				.getAsBufferedImage();
		String extension = source.getName().substring(
				source.getName().lastIndexOf(".") + 1);
		return internalScale(image, extension, size);
	}

	public static File scale(URL source, int width, int height)
			throws IOException {
		String extension = source.getPath().substring(
				source.getPath().lastIndexOf(".") + 1);
		return internalScale(ImageIO.read(Cache.getInstance().fetchUrl(source)), extension, width, height);
	}

	public static File scale(URL source, int size) throws IOException {
		String extension = source.getPath().substring(
				source.getPath().lastIndexOf(".") + 1);
		return internalScale(ImageIO.read(Cache.getInstance().fetchUrl(source)), extension, size);
	}

	private static File internalScale(BufferedImage image, String extension,
			int width, int height) throws IOException {
		BufferedImage scaled = Scalr.resize(image, width, height);
		File tempFile = File.createTempFile("tempScaled_", "." + extension);
		ImageIO.write(scaled, extension, tempFile);
		return tempFile;
	}

	private static File internalScale(BufferedImage image, String extension,
			int size) throws IOException {
		BufferedImage scaled = Scalr.resize(image, size);
		File tempFile = File.createTempFile("tempScaled_", "." + extension);
		ImageIO.write(scaled, extension, tempFile);
		return tempFile;
	}
	
	

}
