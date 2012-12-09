package com.crispy;

import java.io.File;

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

	private String name;
	private Logger logger;
	private String prefix;

	private Log(String name) {
		this.name = name;
		this.prefix = "";
		this.logger = Logger.getLogger(name);
	}

	public static Log get(String name) {
		return new Log(name);
	}
	
	public Log file(String folder) {
		return file(folder, Priority.DEBUG);
	}

	public Log file(String folder, Priority p) {
		if (logger.getAppender("jappy-file") != null) {
			return this;
		}
		File ff = new File(folder);
		if (!ff.exists()) {
			ff.mkdir();
		}
		try {
			DailyRollingFileAppender appender = new DailyRollingFileAppender(
					new PatternLayout("%p %d{HH:mm:ss} %c{2}: %m%n"), new File(
							folder + "/" + name).getAbsolutePath(),
					"dd-MM-yyyy");
			appender.setName("jappy-file");
			appender.setThreshold(p);
			logger.addAppender(appender);
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
		if (logger.getAppender("jappy-console") != null) {
			return this;
		}
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
