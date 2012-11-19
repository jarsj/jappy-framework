package com.crispy;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;

public class Log {
	private ConcurrentHashMap<String, Logger> loggers;
	private String logFolder;
	private String smtpTo;
	private boolean standard;
	private Level level;

	public static Log getInstance() {
		return INSTANCE;
	}

	public static void info(String id, String message) {
		INSTANCE.getLogger(id).info(message);
	}

	public static void error(String id, String message) {
		INSTANCE.getLogger(id).error(message);
	}

	public Logger getLogger(String id) {
		if (loggers.containsKey(id)) {
			return loggers.get(id);
		}
		Logger l = Logger.getLogger("jappy." + id);
		try {
			configureAppenders(l);
		} catch (Throwable t) {
		}
		loggers.putIfAbsent(id, l);
		return loggers.get(id);
	}

	private void configureAppenders(Logger l) throws IOException {
		String id = l.getName().substring(l.getName().lastIndexOf('.') + 1);
		l.setLevel(level);
		if (logFolder != null) {
			DailyRollingFileAppender appender = new DailyRollingFileAppender(
					new PatternLayout("%p %d{HH:mm:ss} %c{2}: %m%n"), new File(
							logFolder + "/" + id).getAbsolutePath(),
					"dd-MM-yyyy");
			appender.setThreshold(Priority.DEBUG);
			l.addAppender(appender);
		}
		if (smtpTo != null) {
			EC2SMTPAppender email = new EC2SMTPAppender(smtpTo);
			email.activateOptions();
			email.setThreshold(Priority.ERROR);
			l.addAppender(email);
		}

		if (standard) {
			ConsoleAppender console = new ConsoleAppender();
			console.setLayout(new PatternLayout(
					"%d{ISO8601} [%t] %-5p %c %x - %m%n"));
			console.activateOptions();
			console.setThreshold(Priority.DEBUG);
			l.addAppender(console);
		}
	}

	private Log() {
		loggers = new ConcurrentHashMap<String, Logger>();
		standard = false;
		level = Level.DEBUG;
	}

	private static Log INSTANCE = new Log();

	public void setFolder(String folder) throws IOException {
		File f = new File(folder);
		if (!f.exists()) {
			f.mkdirs();
		}
		if (f.exists() && f.canWrite()) {
			this.logFolder = folder;
			for (Logger l : loggers.values()) {
				l.removeAllAppenders();
				configureAppenders(l);
			}
		}
	}

	public void sendEmailTo(String to) throws IOException {
		this.smtpTo = to;
		for (Logger l : loggers.values()) {
			l.removeAllAppenders();
			configureAppenders(l);
		}
	}

	public void enableDump() throws IOException {
		this.standard = true;
		for (Logger l : loggers.values()) {
			l.removeAllAppenders();
			configureAppenders(l);
		}
	}

	public void setLevel(Level l) {
		for (Logger log : loggers.values()) {
			log.setLevel(l);
		}
		level = l;
	}

	public static void error(String id, String message, Exception e) {
		Logger LOG = Log.getInstance().getLogger(id);
		LOG.error(message, e);
	}
}
