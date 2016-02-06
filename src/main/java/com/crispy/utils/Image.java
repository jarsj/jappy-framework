package com.crispy.utils;

import com.crispy.cloud.Cloud;
import com.crispy.log.Log;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Image {

	private static final Image INSTANCE = new Image();
	private final String CONVERT_PATH;

	private ScheduledExecutorService background;

	private Image() {
        if (new File("/usr/bin/convert").exists()) {
            CONVERT_PATH = "/usr/bin/convert";
        } else if (new File("/usr/local/bin/convert").exists()) {
			CONVERT_PATH = "/usr/local/bin/convert";
		} else {
            CONVERT_PATH = "/usr/bin/convert";
        }

		background = Executors.newSingleThreadScheduledExecutor();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					background.shutdown();
				} catch (Throwable t) {
				}
			}
		}));
	}

	public static Image getInstance() {
		return INSTANCE;
	}

	private final Log LOG = Log.get("resource");

	public String uploadFile(String uploadFolder, InputStream input, String fileName) throws FileNotFoundException, IOException {
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		String nextID = UUID.randomUUID().toString().toLowerCase();
		File folder = new File(uploadFolder);
		folder.mkdirs();
		File f = new File(folder, nextID + "." + ext);
		IOUtils.copy(input, new FileOutputStream(f));
		return f.getAbsolutePath().toString();

	}
	
	public void scale(File raw, File output, int width, int height) throws ServletException {
		try {
			if (raw == null || output == null || !raw.exists())
				throw new ServletException("One of the argument " + raw + " , " + output + " is null");
			String fileExtension = FilenameUtils.getExtension(raw.getName()).toLowerCase();
			ProcessBuilder pb;
			if (fileExtension.equals("mp4") || fileExtension.equals("webm")) {
				pb = new ProcessBuilder("/usr/bin/ffmpeg", "-i", raw.getAbsolutePath(), "-vf", "thumbnail,scale=" +
						width + ":" + height, "-frames:v", "1",
						output.getAbsolutePath());
			} else if (fileExtension.equals("psd")) {
				pb = new ProcessBuilder(CONVERT_PATH, raw.getAbsolutePath() + "[0]", "-auto-orient",
                        "-strip", "-thumbnail", width + "x"
						+ height, output.getAbsolutePath());
			} else {
                String resizeCmd = width + "x" + height + "!";
                if (width == -1) resizeCmd = "x" + height;
                if (height == -1) resizeCmd = width + "";
				pb = new ProcessBuilder(CONVERT_PATH,
						raw.getAbsolutePath(),// 
						"-filter", //
						"Lanczos",// 
						"-sampling-factor",// 
						"1x1",// 
						"-quality",// 
						"100",// 
						"-unsharp",// 
						"1.5x0.7+0.02",// 
						 "-resize",// 
						 resizeCmd,
						output.getAbsolutePath());
			}

            System.out.println(pb.command().toString());

			pb.redirectErrorStream(true);
			pb.redirectOutput(Redirect.to(new File("/dev/null")));
			Process p = pb.start();
			int result = p.waitFor();
			if (result != 0) {
				throw new ServletException();
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	public String uploadS3Async(String s3Bucket, final InputStream input, String fileName, final int width, final int height) throws FileNotFoundException, IOException {
		final String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		final String nextID = UUID.randomUUID().toString().toLowerCase();
		String s3Comps[] = s3Bucket.split("/");

		final String bucket = s3Comps[0];
		s3Comps = (String[]) ArrayUtils.remove(s3Comps, 0);
		final String parent = (s3Comps.length == 0) ? "" : (StringUtils.join(s3Comps, "/") + "/");

		background.execute(new Runnable() {
			@Override
			public void run() {
				try {
					File tmp = File.createTempFile("image", ext);
					IOUtils.copy(input, new FileOutputStream(tmp));
					File output = File.createTempFile("image_output", ext);
                    scale(tmp, output, width, height);
					Cloud.s3(bucket).create().allowRead().neverExpire().upload(parent + nextID + "." + ext, output);
				} catch (Throwable t) {
					t.printStackTrace();
					LOG.error("Error uploading image " + parent + nextID + "." + ext, t);
				}
			}
		});

		return "http://" + bucket + ".s3.amazonaws.com/" + parent + nextID + "." + ext;
	}

	public String uploadS3(String s3Bucket, InputStream input, String fileName) throws FileNotFoundException, IOException {
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		String nextID = UUID.randomUUID().toString().toLowerCase();
		String s3Comps[] = s3Bucket.split("/");
		
		String bucket = s3Comps[0];
		s3Comps = (String[]) ArrayUtils.remove(s3Comps, 0);
		String parent = (s3Comps.length == 0) ? "" : (StringUtils.join(s3Comps, "/") + "/");

		File tmp = File.createTempFile("image", ext);
		IOUtils.copy(input, new FileOutputStream(tmp));
		Cloud.s3(bucket).create().allowRead().neverExpire().upload(parent + nextID + "." + ext, tmp);
		return "http://" + bucket + ".s3.amazonaws.com/" + parent + nextID + "." + ext;
	}
}
