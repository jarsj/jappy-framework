package com.crispy;

import com.crispy.log.Log;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class ImageProcessor {

    private final Log LOG = Log.get("imageprocessor");

    public static ImageProcessor newInstance() {
        ImageProcessor processor = new ImageProcessor();
        return processor;
    }

	private final String CONVERT_PATH;

	private ScheduledExecutorService background;

	private ImageProcessor() {
        if (new File("/usr/bin/convert").exists()) {
            CONVERT_PATH = "/usr/bin/convert";
        } else if (new File("/usr/local/bin/convert").exists()) {
			CONVERT_PATH = "/usr/local/bin/convert";
		} else {
            throw new IllegalStateException("Missing ImageMagick.");
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

	public void scale(File input, String extension, int width, int height, Consumer<File> callback) {

        if (input == null || !input.exists())
            throw new IllegalArgumentException("Input is null or the file doesn't exist");
        String fileExtension = extension == null ? FilenameUtils.getExtension(input.getName()).toLowerCase() :
                extension.toLowerCase();

        background.execute(() -> {
            try {
                File output = File.createTempFile("scale", "." + fileExtension);
                ProcessBuilder pb;
                if (fileExtension.equals("mp4") || fileExtension.equals("webm")) {
                    pb = new ProcessBuilder("/usr/bin/ffmpeg", "-i", input.getAbsolutePath(), "-vf", "thumbnail," +
                            "scale=" + width + ":" + height, "-frames:v", "1",
                            output.getAbsolutePath());
                } else if (fileExtension.equals("psd")) {
                    pb = new ProcessBuilder(CONVERT_PATH, input.getAbsolutePath() + "[0]", "-auto-orient",
                            "-strip", "-thumbnail", width + "x"
                            + height, output.getAbsolutePath());
                } else {
                    String resizeCmd = width + "x" + height + "!";
                    if (width == -1) resizeCmd = "x" + height;
                    if (height == -1) resizeCmd = width + "";
                    pb = new ProcessBuilder(CONVERT_PATH,
                            input.getAbsolutePath(),//
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
                    LOG.error("scale input=" + input.getAbsolutePath());
                    callback.accept(null);
                } else {
                    callback.accept(output);
                }
            } catch (Exception e) {
                LOG.error("scale input=" + input.getAbsolutePath(), e);
                callback.accept(null);
            }
        });
	}
}
