package com.crispy.log;

import java.io.File;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

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
	private static String LOG_FOLDER = "/mnt/logs";

	private String name;
	private Logger logger;
	private String prefix;
	private boolean async;

	private Log(String name) {
		this.name = name;
		this.prefix = "";
		this.logger = Logger.getLogger(name);
		this.async = false;
	}

	public static Log get(String name) {
		return new Log(name);
	}

	public Log async(boolean async) {
		if (this.async == async)
			return this;
		this.async = async;
		if (this.async) {
			AsyncAppender asyncAppender = new AsyncAppender();
			asyncAppender.setName(name + "-async-appender");
			Enumeration appenders = logger.getAllAppenders();
			while (appenders.hasMoreElements()) {
				Appender a = (Appender) appenders.nextElement();
				asyncAppender.addAppender(a);
			}
			logger.removeAllAppenders();
			logger.addAppender(asyncAppender);
		} else {
			AsyncAppender asyncAppender = (AsyncAppender) logger
					.getAppender(name + "-async-appender");
			logger.removeAllAppenders();
			Enumeration appenders = asyncAppender.getAllAppenders();
			while (appenders.hasMoreElements()) {
				Appender a = (Appender) appenders.nextElement();
				logger.addAppender(a);
			}
		}
		return this;
	}

	public Log daily(Level l) {
		return daily(l, "%p %d{yyyy-MM-dd HH:mm:ss}: %m%n");
	}

	public Log s3(Level l, String bucket, String folder, String pattern,
			String maxSize, long duration, TimeUnit period) {
		try {
			S3Appender appender = new S3Appender(new PatternLayout(pattern),
					new File("/mnt/tmp"), bucket, folder);
			appender.setName("jappy-s3");
			appender.setMaxFileSize(maxSize);
			appender.setThreshold(l);
			appender.setRolloverTime(duration, period);

			if (!async) {
				logger.removeAppender("jappy-s3");
				logger.addAppender(appender);
			} else {
				AsyncAppender aa = (AsyncAppender) logger.getAppender(name
						+ "-async-appender");
				aa.removeAppender("jappy-s3");
				aa.addAppender(appender);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	public Log s3(Level l, String bucket, String folder, String pattern,
			String maxSize) {
		return s3(l, bucket, folder, pattern, maxSize, Long.MAX_VALUE,
				TimeUnit.MINUTES);
	}

	public Log daily(Level l, String pattern) {
		try {
			DailyRollingFileAppender appender = new DailyRollingFileAppender(
					new PatternLayout(pattern),
					new File(LOG_FOLDER + "/" + name).getAbsolutePath(),
					"dd-MM-yyyy");
			appender.setName("jappy-daily");
			appender.setThreshold(l);

			if (!async) {
				logger.removeAppender("jappy-daily");
				logger.addAppender(appender);
			} else {
				AsyncAppender aa = (AsyncAppender) logger.getAppender(name
						+ "-async-appender");
				aa.removeAppender("jappy-daily");
				aa.addAppender(appender);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	public Log size(int maxSizeInMB, Level l) {
		try {
			RollingFileAppender appender = new RollingFileAppender(
					new PatternLayout("%p %d{yyyy-MM-dd HH:mm:ss}: %m%n"),
					new File(LOG_FOLDER + "/" + name).getAbsolutePath());
			appender.setName("jappy-size");
			appender.setMaxFileSize(maxSizeInMB + "MB");
			appender.setThreshold(l);
			appender.setMaxBackupIndex(100);

			if (!async) {
				logger.removeAppender("jappy-size");
				logger.addAppender(appender);
			} else {
				AsyncAppender aa = (AsyncAppender) logger.getAppender(name
						+ "-async-appender");
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
			AsyncAppender aa = (AsyncAppender) logger.getAppender(name
					+ "-async-appender");
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
			AsyncAppender aa = (AsyncAppender) logger.getAppender(name
					+ "-async-appender");
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
