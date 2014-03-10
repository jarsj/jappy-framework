package com.crispy.log;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.CountingQuietWriter;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LoggingEvent;

import com.crispy.cloud.Cloud;

/**
 * S3Appender logs directly to S3 once a size threshold is met.
 * 
 * @author Harsh Jain
 */
public class S3Appender extends FileAppender implements Runnable {

	/**
	 * The default maximum file size is 1MB.
	 */
	protected long maxFileSize = 10 * 1024 * 1024;

	private File localFolder;
	private LinkedBlockingQueue<String> queue;
	private String bucket;
	private String s3Folder;
	private Thread s3UploaderThread;
	private AtomicBoolean terminateUploaderThread;

	public S3Appender(Layout layout, File localFolder, String bucket,
			String s3Folder) throws IOException {
		super();
		this.terminateUploaderThread = new AtomicBoolean();
		queue = new LinkedBlockingQueue<String>();
		this.bucket = bucket;
		this.s3Folder = s3Folder;
		this.localFolder = localFolder;
		if (localFolder.exists()) {
			for (File child : localFolder.listFiles()) {
				queue.add(child.getAbsolutePath());
			}
		} else {
			localFolder.mkdirs();
		}
		File tmpFile = new File(localFolder, UUID.randomUUID().toString());
		this.setTmpFile(tmpFile.getAbsolutePath());
		super.layout = layout;
		s3UploaderThread = new Thread(this);
		terminateUploaderThread.set(false);
		s3UploaderThread.start();
	}

	/**
	 * Get the maximum size that the output file is allowed to reach before
	 * being rolled over to backup files.
	 * 
	 * @since 1.1
	 */
	public long getMaximumFileSize() {
		return maxFileSize;
	}

	public void rollOver() {
		queue.add(this.getFile());
		File tmpFile = new File(localFolder, UUID.randomUUID().toString());
		try {
			this.setTmpFile(tmpFile.getAbsolutePath());
		} catch (IOException e) {
		}
	}

	public synchronized void setTmpFile(String fileName) throws IOException {
		super.setFile(fileName, false, this.bufferedIO, this.bufferSize);
	}

	public void setMaximumFileSize(long maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public void setMaxFileSize(String value) {
		maxFileSize = OptionConverter.toFileSize(value, maxFileSize + 1);
	}

	protected void setQWForFiles(Writer writer) {
		this.qw = new CountingQuietWriter(writer, errorHandler);
	}

	/**
	 * We need to rollover on both size and date!
	 */
	protected void subAppend(LoggingEvent event) {
		super.subAppend(event);
		if (fileName != null && qw != null) {
			long size = ((CountingQuietWriter) qw).getCount();
			if (size >= maxFileSize) {
				rollOver();
			}
		}
	}

	@Override
	public synchronized void close() {
		super.close();
		terminateUploaderThread.set(true);
		try {
			s3UploaderThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true) {
			String fileName = queue.poll();
			if (fileName != null) {
				try {
					File f = new File(fileName);
					Cloud.s3(bucket).upload(s3Folder + "/" + f.getName(), f);
					FileUtils.deleteQuietly(f);
				} catch (Exception ex) {
					queue.add(fileName);
				}
			}

			if (terminateUploaderThread.get()) {
				break;
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}