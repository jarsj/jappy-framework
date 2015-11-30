package ch.qos.logback.core.rolling;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.s3.AmazonS3Client;
import com.crispy.cloud.Cloud;
import com.crispy.log.Log;

/*
 * 1. Regular log file rolling as FixedWindowsRollingPolicy does
 * 2. Upload the rolled log file to S3 bucket
 *
 * Also, this policy uploads the active log file on JVM exit. If rollingOnExit is true,
 * another log rolling happens and a rolled log is uploaded. If rollingOnExit is false,
 * the active file is directly uploaded.
 *
 * If rollingOnExit is false and if no rolling happened before JVM exits, this rolling
 * policy uploads the active log file as it is.
 */
public class S3TimeBasedRollingPolicy<E> extends TimeBasedRollingPolicy<E> {
	
	private static final Log LOG = Log.get("jappy.log");
	
	ExecutorService executor = Executors.newFixedThreadPool(1);
	
	String s3BucketName;
	String s3FolderName;
	String uniqueIdentifier = null;
	boolean rollingOnExit = true;
	AmazonS3Client s3Client;

	protected AmazonS3Client getS3Client() {
		if (s3Client == null) {
			s3Client = new AmazonS3Client(Cloud.getCredentials());
		}
		return s3Client;
	}

	@Override
	public void start() {
		super.start();
		// add a hook on JVM shutdown
		if (uniqueIdentifier == null) {
			try {
				uniqueIdentifier = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				uniqueIdentifier = UUID.randomUUID().toString();
			}
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHookRunnable()));
	}

	@Override
	public void rollover() throws RolloverFailure {
		super.rollover();
		try {
			
			String elapsedPeriodsFileName = timeBasedFileNamingAndTriggeringPolicy.getElapsedPeriodsFileName();
			LOG.debug("rollover " + elapsedPeriodsFileName);
			uploadFileToS3Async(elapsedPeriodsFileName);
		} catch (Exception e) {
			throw new RolloverFailure(e.getMessage(), e);
		}
	}

	protected void uploadFileToS3Async(String filename) throws UnknownHostException {
		final File file = new File(filename);
		// if file does not exist or empty, do nothing
		if (!file.exists() || file.length() == 0) {
			LOG.warn("uploadFile Missing " + filename);
			return;
		}
		// add the S3 folder name in front if specified
		final StringBuffer s3ObjectName = new StringBuffer();
		if (getS3FolderName() != null) {
			s3ObjectName.append(getS3FolderName()).append("/");
		}
		s3ObjectName.append(uniqueIdentifier).append("/");
		s3ObjectName.append(file.getName());
		addInfo("Uploading " + filename);
		LOG.info("Uploading " + filename);
		Runnable uploader = new Runnable() {
			@Override
			public void run() {
				try {
					LOG.info("Uploading runnable " + s3ObjectName.toString());
					getS3Client().putObject(getS3BucketName(), s3ObjectName.toString(), file);
				} catch (Exception ex) {
					LOG.warn("error in s3_uploading", ex);
				}
			}
		};
		executor.execute(uploader);
	}

	public void setUniqueIdentifier(String uid) {
		uniqueIdentifier = uid;
	}

	// On JVM exit, upload the current log
	class ShutdownHookRunnable implements Runnable {
		@Override
		public void run() {
			try {
				if (isRollingOnExit())
					// do rolling and upload the rolled file on exit
					rollover();
				else
					// upload the active log file without rolling
					uploadFileToS3Async(getActiveFileName());
				// wait until finishing the upload
				executor.shutdown();
				executor.awaitTermination(10, TimeUnit.MINUTES);
			} catch (Exception ex) {
				addError("Failed to upload a log in S3", ex);
				executor.shutdownNow();
			}
		}
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public void setS3BucketName(String s3BucketName) {
		this.s3BucketName = s3BucketName;
	}

	public String getS3FolderName() {
		return s3FolderName;
	}
	
	public void setS3UniqueId(String s3Id) {
		this.uniqueIdentifier = s3Id;
	}

	public void setS3FolderName(String s3FolderName) {
		if (s3FolderName.startsWith("/"))
			s3FolderName = s3FolderName.substring(1);
		if (s3FolderName.endsWith("/"))
			s3FolderName = s3FolderName.substring(0, s3FolderName.length() - 1);
		this.s3FolderName = s3FolderName;
	}

	public boolean isRollingOnExit() {
		return rollingOnExit;
	}

	public void setRollingOnExit(boolean rollingOnExit) {
		this.rollingOnExit = rollingOnExit;
	}
}