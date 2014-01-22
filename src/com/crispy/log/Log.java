package com.crispy.log;

import java.io.File;
import java.net.URLEncoder;
import java.util.Enumeration;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * Logging Library. Captures the most common usecases of logging.
 * 
 * @author harsh
 * 
 */
public class Log {
	private static String DEFAULT_DAILY_FOLDER;
	private static boolean DEFAULT_CONSOLE;
	private static Level DEFAULT_LEVEL;
	private static String DEFAULT_EMAIL_TO;
	private static Level DEFAULT_EMAIL_LEVEL;
	private static Level DEFAULT_DAILY_LEVEL;
	private static String DEFAULT_SIZE_FOLDER;
	private static Level DEFAULT_SIZE_LEVEL;
	private static Level DEFAULT_CONSOLE_LEVEL;

	private String name;
	private Logger logger;
	private String prefix;
	private boolean async;

	private Log(String name) {
		this(name, false);
	}
	
	private Log(String name, boolean async) {
		this.name = name;
		this.prefix = "";
		this.async = async;
		this.logger = Logger.getLogger(name);
		if (async) {
			async();
		}
		if (DEFAULT_CONSOLE) {
			console(DEFAULT_CONSOLE_LEVEL);
		}
		if (DEFAULT_DAILY_FOLDER != null) {
			daily(DEFAULT_DAILY_FOLDER, DEFAULT_DAILY_LEVEL);
		}
		if (DEFAULT_SIZE_FOLDER != null) {
			size(DEFAULT_SIZE_FOLDER, 64, DEFAULT_SIZE_LEVEL);
		}
		if (DEFAULT_LEVEL != null) {
			level(DEFAULT_LEVEL);
		}
		if (DEFAULT_EMAIL_TO != null) {
			email(DEFAULT_EMAIL_TO, DEFAULT_EMAIL_LEVEL);
		}
	}

	public static Log get(String name) {
		return new Log(name);
	}
	
	public static Log get(String name, boolean async) {
		return new Log(name, async);
	}

	public static void globalDaily(String folder, Level l) {
		try {
			FileUtils.forceMkdir(new File(folder));
			DEFAULT_DAILY_FOLDER = folder;
			DEFAULT_DAILY_LEVEL = l;
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void globalSize(String folder, Level l) {
		try {
			FileUtils.forceMkdir(new File(folder));
			DEFAULT_SIZE_FOLDER = folder;
			DEFAULT_SIZE_LEVEL = l;
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void globalConsole(Level l) {
		DEFAULT_CONSOLE = true;
		DEFAULT_CONSOLE_LEVEL = l;
	}

	public static void globalEmail(String to, Level l) {
		DEFAULT_EMAIL_TO = to;
		DEFAULT_EMAIL_LEVEL = l;
	}

	public Log async() {
		this.async = true;

		AsyncAppender asyncAppender = new AsyncAppender();
		asyncAppender.setName("async-appender");

		Enumeration appenders = logger.getAllAppenders();
		while (appenders.hasMoreElements()) {
			Appender a = (Appender) appenders.nextElement();
			asyncAppender.addAppender(a);
		}
		logger.removeAllAppenders();
		logger.addAppender(asyncAppender);
		return this;
	}

	public Log daily(String folder, Level l) {
		return daily(folder, l, "%p %d{yyyy-MM-dd HH:mm:ss}: %m%n");
	}
	
	public Log daily(String folder, Level l, String pattern) {
		try {
			FileUtils.forceMkdir(new File(folder));

			DailyRollingFileAppender appender = new DailyRollingFileAppender(
					new PatternLayout(pattern),
					new File(folder + "/" + name).getAbsolutePath(),
					"dd-MM-yyyy");
			appender.setName("jappy-daily");
			appender.setThreshold(l);

			if (!async) {
				logger.removeAppender("jappy-daily");
				logger.addAppender(appender);
			} else {
				AsyncAppender aa = (AsyncAppender) logger
						.getAppender("async-appender");
				aa.removeAppender("jappy-daily");
				aa.addAppender(appender);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	public Log size(String folder, int maxSizeInMB, Level l) {
		try {
			FileUtils.forceMkdir(new File(folder));
			RollingFileAppender appender = new RollingFileAppender(
					new PatternLayout("%p %d{yyyy-MM-dd HH:mm:ss}: %m%n"),
					new File(folder + "/" + name).getAbsolutePath());
			appender.setName("jappy-size");
			appender.setMaxFileSize(maxSizeInMB + "MB");
			appender.setThreshold(l);
			appender.setMaxBackupIndex(100);

			if (!async) {
				logger.removeAppender("jappy-size");
				logger.addAppender(appender);
			} else {
				AsyncAppender aa = (AsyncAppender) logger
						.getAppender("async-appender");
				aa.removeAppender("jappy-size");
				aa.addAppender(appender);
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

	public Log console(Level l) {
		ConsoleAppender console = new ConsoleAppender();
		console.setName("jappy-console");
		console.setLayout(new PatternLayout("%p %d{yyyy-MM-dd HH:mm:ss} %m%n"));
		console.setThreshold(l);
		console.activateOptions();

		if (!async) {
			logger.removeAppender("jappy-console");
			logger.addAppender(console);
		} else {
			AsyncAppender aa = (AsyncAppender) logger
					.getAppender("async-appender");
			aa.removeAppender("jappy-console");
			aa.addAppender(console);
		}
		return this;
	}

	public Log email(String to, Level l) {

		EC2SMTPAppender email = new EC2SMTPAppender(to, l);
		email.setName("jappy-email");
		email.setThreshold(l);
		email.activateOptions();

		if (!async) {
			logger.removeAppender("jappy-email");
			logger.addAppender(email);
		} else {
			AsyncAppender aa = (AsyncAppender) logger
					.getAppender("async-appender");
			aa.removeAppender("jappy-email");
			aa.addAppender(email);
		}
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

	public static String safe(String m) {
		return URLEncoder.encode(m);
	}
}