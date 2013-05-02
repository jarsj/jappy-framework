package com.crispy.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * This class syncs a folder on local file system with an S3 bucket in multiple
 * configurations. Typical use case is to backup log files to S3.
 * 
 * @author harsh
 */
public class S3Sync {

	private static AWSCredentials credentials;
	private static File logFolder;
	private static File tmpFolder;
	private static String bucketName;
	private static String bucketPath;
	private static int fileSize;
	private static boolean current;
	private static AmazonS3Client s3;
	private static File bufferFile;
	private static FileOutputStream bufferStream;
	private static byte[] NEW_LINE = "\n".getBytes();

	private static long run;
	private static int bytesWritten;

	public static void main(String[] args) throws ParseException, IOException {
		Options options = new Options();
		options.addOption("accessKey", true, "Your AWS AccessKey");
		options.addOption("secretKey", true, "Your AWS SecretKey");
		options.addOption("logFolder", true, "Local folder to sync");
		options.addOption("bucket", true, "Remote S3 bucket");
		options.addOption("path", true, "Bucket Path");
		options.addOption("current", false, "Whether to include current file");
		options.addOption("size", true, "File size in MB. default=64");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);

		if (!(cmd.hasOption("accessKey") && cmd.hasOption("secretKey")
				&& cmd.hasOption("logFolder") && cmd.hasOption("bucket"))) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("s3sync", options);
			System.exit(1);
		}

		credentials = new BasicAWSCredentials(cmd.getOptionValue("accessKey"),
				cmd.getOptionValue("secretKey"));
		logFolder = new File(cmd.getOptionValue("logFolder"));
		bucketName = cmd.getOptionValue("bucket");
		bucketPath = cmd.getOptionValue("path", "");
		if (bucketPath.length() > 0 && !bucketPath.endsWith("/"))
			bucketPath = bucketPath + "/";
		run = System.currentTimeMillis();
		tmpFolder = new File(System.getProperty("java.io.tmpdir"), "tmp-" + run);
		tmpFolder.mkdir();
		fileSize = Integer.parseInt(cmd.getOptionValue("size", "64"));
		fileSize *= 1000000;
		current = cmd.hasOption("current");
		s3 = new AmazonS3Client(credentials);

		bufferFile = null;
		bufferStream = null;

		for (String prefix : cmd.getArgs()) {
			doPrefix(prefix);
		}
	}

	private static void doPrefix(final String prefix) throws IOException {
		System.out.println("Doing prefix = " + prefix);
		bufferFile = File.createTempFile(prefix, "buffer");
		bufferStream = new FileOutputStream(bufferFile);
		bytesWritten = 0;
		File fs[] = logFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.equals(prefix) && current)
					return true;
				if (name.startsWith(prefix)
						&& name.matches("^" + prefix
								+ "\\d{4}-\\d{1,2}-\\d{1,2}")) {
					return true;
				}
				if (name.startsWith(prefix)
						&& name.matches("^" + prefix + "\\.\\d"))
					return true;
				return false;
			}
		});

		Arrays.sort(fs, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				Long l1 = o1.lastModified();
				Long l2 = o2.lastModified();
				return l1.compareTo(l2);
			}
		});

		if (fs.length == 0)
			return;

		String yearMonthPrefix = yearMonth(fs[0].lastModified());

		for (File f : fs) {
			String myYM = yearMonth(f.lastModified());
			String name = f.getName();
			System.out.println("Processing file = " + name);
			FileUtils.moveFileToDirectory(f, tmpFolder, false);
			File fileToCopy = new File(tmpFolder, f.getName());
			// We split the month boundary.
			if (!myYM.equals(yearMonthPrefix)) {
				uploadBuffer(prefix, yearMonthPrefix);
				yearMonthPrefix = myYM;
			}

			// Read this entire file line by line
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileToCopy)));
			String s = null;
			while ((s = br.readLine()) != null) {
				byte[] b = s.getBytes();

				if ((bytesWritten + b.length + 2) > fileSize) {
					uploadBuffer(prefix, yearMonthPrefix);
				}

				bufferStream.write(b);
				bufferStream.write(NEW_LINE);
				bytesWritten += b.length;
				bytesWritten += NEW_LINE.length;
			}
			br.close();
		}
		uploadBuffer(prefix, yearMonthPrefix);
	}

	private static void uploadBuffer(String prefix, String ym)
			throws IOException {
		bufferStream.close();

		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentType("text/plain");
		meta.setContentLength(bufferFile.length());
		PutObjectRequest put = new PutObjectRequest(bucketName, bucketPath
				+ prefix + "/" + prefix + "-" + ym + "-" + (++run),
				new FileInputStream(bufferFile), meta);
		PutObjectResult result = s3.putObject(put);
		System.out.println("Uploaded to S3 " + result.getVersionId());

		bytesWritten = 0;
		bufferFile = File.createTempFile(prefix, "buffer");
		bufferStream = new FileOutputStream(bufferFile);
	}

	private static String yearMonth(long ts) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
		return format.format(new Date(ts));
	}
}
