/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

/**
 * S3Appender logs directly to S3 once a size threshold is met.
 * 
 * @author Harsh Jain
 */
public class S3Appender extends FileAppender implements Runnable {

	/**
	 * The default maximum file size is 10MB.
	 */
	protected long maxFileSize = 10 * 1024 * 1024;

	private File tmpFolder;
	private String bucket;
	private String folder;
	private LinkedBlockingQueue<String> queue;
	private Thread s3UploaderThread;
	private AtomicBoolean terminateUploaderThread;
	private static AWSCredentials credentials;
	private String producer;

	public S3Appender(Layout layout, File tmpFolder, String bucket,
			String folder, String producer, String accessKey, String secretKey)
			throws IOException {
		super();
		this.bucket = bucket;
		this.folder = folder;
		this.credentials = new BasicAWSCredentials(accessKey, secretKey);
		this.tmpFolder = tmpFolder;
		this.producer = producer;
		queue = new LinkedBlockingQueue<>();
		
		// TODO 
		for (File child : tmpFolder.listFiles()) {
			queue.add(child.getAbsolutePath());
		}
		
		File tmpFile = new File(tmpFolder, UUID.randomUUID().toString());
		this.setTmpFile(tmpFile.getAbsolutePath());
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
		File tmpFile = new File(tmpFolder, UUID.randomUUID().toString());
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
		String fileName = null;
		while ((fileName = queue.poll()) != null) {
			ObjectMetadata metadata = new ObjectMetadata();
			// TODO : Put proper date string.
			PutObjectRequest request = new PutObjectRequest(bucket, folder
					+ "/yyyy-mm-dd/" + producer + "/"
					+ System.currentTimeMillis() + ".log", new File(fileName));
			request.setMetadata(metadata);
			AmazonS3Client s3 = new AmazonS3Client(credentials);
			s3.putObject(request);
			FileUtils.deleteQuietly(new File(fileName));
			if (terminateUploaderThread.get()) {
				break;
			}
		}
	}
}