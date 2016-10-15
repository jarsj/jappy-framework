package com.crispy.log;

import ch.qos.logback.classic.Level;

import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * @author harsh
 */
public class Appender {
	String name;
	boolean console;
	String folder;
	String s3bucket;
	String s3folder;
	String s3UniqueId;
	boolean daily;
	String size;
	String pattern;
	Level level;
	int history;
	//
	String mailTo;
	String mailFrom;
	String smtpHost;
	String smtpUsername;
	String smtpPassword;

	int smtpPort;
	boolean smtpTls;
	boolean smtpSsl;
	boolean hourly;
	boolean async;

	private Appender(String name) {
		this.name = name;
		console = false;
		daily = false;
		hourly = false;
        async = false;
		history = 0;
		pattern = "%c{2} %-12date{YYYY-MM-dd HH:mm:ss.SSS} %-5level - %msg%n";
	}

	public static Appender create(String name) {
		return new Appender(name);
	}

	public Appender console() {
		this.console = true;
		return this;
	}

	public Appender size(String size) {
		this.size = size;
		return this;
	}

	public Appender folder(String folder) {
		this.folder = folder;
		return this;
	}

	public Appender hourly() {
		hourly = true;
		return this;
	}
	
	public Appender daily() {
		daily = true;
		return this;
	}

	public Appender history(int n) {
		history = n;
		return this;
	}

	public Appender level(Level level) {
		this.level = level;
		return this;
	}

	public Appender s3(String s3uri, String uniqueId) {
		try {
			URI uri = new URI(s3uri);
			s3bucket = uri.getHost();
			s3folder = uri.getPath();
			s3UniqueId = uniqueId;
			
			if (s3bucket == null || s3folder == null) {
				throw new IllegalArgumentException("Can not initialize s3 appender with " + s3uri);
			}
			return this;
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Couldn't parse URI", e);
		}
	}

	public Appender s3(String bucket, String folder, String uniqueId) {
		s3bucket = bucket;
		s3folder = folder;
		s3UniqueId = uniqueId;
		return this;
	}
	
	public Appender pattern(String pattern) {
		this.pattern = pattern;
		return this;
	}

	public Appender ec2Mail(String zone, String authFile) {
		try {
			mail("email-smtp." + zone + ".amazonaws.com", 587);
			Properties propFile = new Properties();
			propFile.load(new FileReader(authFile));
			auth(propFile.getProperty("smtpUsername"), propFile.getProperty("smtpPassword"));
			useTLS();
		} catch (Throwable t) {
			throw new IllegalStateException("Could not configure ec2mail", t);
		}
		return this;
	}

	public Appender mail(String host, int port) {
		this.smtpHost = host;
		this.smtpPort = port;
		return this;
	}

	public Appender auth(String username, String password) {
		if (username == null || password == null) 
			throw new IllegalArgumentException("Missing username or password in appender");
		this.smtpUsername = username;
		this.smtpPassword = password;
		return this;
	}

	public Appender useSsl() {
		this.smtpSsl = true;
		return this;
	}

	public Appender useTLS() {
		this.smtpTls = true;
		return this;
	}

	public Appender from(String from) {
		this.mailFrom = from;
		return this;
	}

	public Appender to(String to) {
		this.mailTo = to;
		return this;
	}

	public Appender async() {
		this.async = true;
		return this;
	}
}
