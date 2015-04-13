package com.crispy.log;

import java.net.URI;
import java.net.URISyntaxException;

import ch.qos.logback.classic.Level;

/**
 * 
 * 
 * 
 * @author harsh
 */
public class Appender {
	boolean console;
	String folder;
	String s3bucket;
	String s3folder;
	boolean daily;
	String size;
	String pattern;
	Level level;
	
	private Appender() {
		console = false;
		daily = false;
		pattern = "%-12date{YYYY-MM-dd HH:mm:ss.SSS} %-5level - %msg%n";
	}
	
	public static Appender create() {
		return new Appender();
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
	
	public Appender daily() {
		daily = true;
		return this;
	}
	
	public Appender level(Level level) {
		this.level = level;
		return this;
	}
	
	public Appender s3(String s3uri) {
		try {
			URI uri = new URI(s3uri);
			s3bucket = uri.getHost();
			s3folder = uri.getPath();
			return this;
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Couldn't parse URI", e);
		}
	}
	
	public Appender s3(String bucket, String folder) {
		s3bucket = bucket;
		s3folder = folder;
		return this;
	}
	
	public Appender pattern(String pattern) {
		this.pattern = pattern;
		return this;
	}
}
