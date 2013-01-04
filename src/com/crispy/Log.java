package com.crispy;

import java.io.File;
import java.util.Enumeration;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;

/**
 * Logging Library. Captures the most common usecases of logging.
 * 
 * @author harsh
 * 
 */
public class Log {

	private static String DEFAULT_FOLDER = "/mnt/logs";
	private static boolean DEFAULT_CONSOLE;
	private static Level DEFAULT_LEVEL;

	private String name;
	private Logger logger;
	private String prefix;

	private Log(String name) {
		this.name = name;
		this.prefix = "";
		this.logger = Logger.getLogger(name);
		if (DEFAULT_CONSOLE) {
			console();
		}
		if (DEFAULT_FOLDER != null) {
			file(DEFAULT_FOLDER);
		}
		if (DEFAULT_LEVEL != null) {
			level(DEFAULT_LEVEL);
		}
	}

	public static Log get(String name) {
		return new Log(name);
	}

	public static void globalFile(String folder) {
		try {
			if (new File(folder).mkdirs()) {
				DEFAULT_FOLDER = folder;
			}
		} catch (Throwable t) {
		}
	}

	public static void globalConsole() {
		DEFAULT_CONSOLE = true;
	}

	public Log file(String folder) {
		return file(folder, Priority.DEBUG);
	}

	public Log file(String folder, Priority p) {
		logger.removeAppender("jappy-file");
		File ff = new File(folder);
		if (!ff.exists()) {
			ff.mkdirs();
		}
		try {
			if (ff.exists() && ff.isDirectory()) {
				DailyRollingFileAppender appender = new DailyRollingFileAppender(
						new PatternLayout("%p %d{HH:mm:ss} %c{2}: %m%n"),
						new File(folder + "/" + name).getAbsolutePath(),
						"dd-MM-yyyy");
				appender.setName("jappy-file");
				appender.setThreshold(p);
				logger.addAppender(appender);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	public Log level(Level l) {
		logger.setLevel(l);
		return this;
	}

	public Log console() {
		return console(Priority.DEBUG);
	}

	public Log console(Priority p) {
		logger.removeAppender("jappy-console");
		ConsoleAppender console = new ConsoleAppender();
		console.setName("jappy-console");
		console.setLayout(new PatternLayout(
				"%d{ISO8601} [%t] %-5p %c %x - %m%n"));
		console.activateOptions();
		console.setThreshold(p);
		logger.addAppender(console);
		return this;
	}

	public Log email(String to, Priority p) {

		EC2SMTPAppender email = new EC2SMTPAppender(to);
		email.setName("jappy-email");
		email.activateOptions();
		email.setThreshold(p);
		logger.addAppender(email);
		return this;
	}

	public Log prefix(String p) {
		this.prefix = p;
		this.prefix += " ";
		return this;
	}

	public void info(String message) {
		logger.info(prefix + message);
	}

	public void error(String message) {
		logger.error(prefix + message);
	}

	public void error(String message, Throwable t) {
		logger.error(prefix + message, t);
	}

	public void trace(String message) {
		logger.trace(prefix + message);
	}

	public void warn(String message) {
		logger.warn(prefix + message);
	}

	public void debug(String message) {
		logger.debug(message);
	}
}
