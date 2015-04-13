package com.crispy.log;

import java.net.URLEncoder;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.S3TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Logging Library. Captures the most common usecases of logging.
 * 
 * @author harsh
 * 
 */
public class Log {
	private Logger logger;

	private Log(String name) {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		this.logger = context.getLogger(name);
		this.logger.setAdditive(false);
	}

	public static Log get(String name) {
		return new Log(name);
	}

	public Log appender(Appender appender) {
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(logger.getLoggerContext());
		encoder.setPattern(appender.pattern);
		encoder.start();

		
		

		OutputStreamAppender<ILoggingEvent> ret = null;

		if (appender.console) {
			ret = new ConsoleAppender<ILoggingEvent>();
			ret.setContext(logger.getLoggerContext());
		} else if (appender.s3bucket != null || appender.folder != null) {
			ret = new RollingFileAppender<ILoggingEvent>();
			ret.setContext(logger.getLoggerContext());

			TimeBasedRollingPolicy<ILoggingEvent> policy = null;
			if (appender.s3bucket != null) {
				policy = new S3TimeBasedRollingPolicy<ILoggingEvent>();
				((S3TimeBasedRollingPolicy<ILoggingEvent>) policy).setS3BucketName(appender.s3bucket);
				((S3TimeBasedRollingPolicy<ILoggingEvent>) policy).setS3FolderName(appender.s3folder + "/" + logger.getName());
				((S3TimeBasedRollingPolicy<ILoggingEvent>) policy).setRollingOnExit(true);
			} else {
				policy = new TimeBasedRollingPolicy<ILoggingEvent>();
			}

			if (appender.folder == null) {
				appender.folder = "/tmp";
			}

			policy.setParent((FileAppender) ret);
			policy.setContext(logger.getLoggerContext());
			if (appender.size != null) {
				policy.setFileNamePattern(appender.folder + "/" + logger.getName() + "-%d{yyyy-MM-dd}.%i.log");
			} else {
				policy.setFileNamePattern(appender.folder + "/" + logger.getName() + "-%d{yyyy-MM-dd}.log");
			}

			policy.start();

			if (appender.size != null) {
				SizeAndTimeBasedFNATP<ILoggingEvent> satb = new SizeAndTimeBasedFNATP<ILoggingEvent>();
				satb.setMaxFileSize(appender.size);
				satb.setContext(logger.getLoggerContext());
				satb.setTimeBasedRollingPolicy(policy);
				satb.start();

				policy.setTimeBasedFileNamingAndTriggeringPolicy(satb);
			}

			((RollingFileAppender<ILoggingEvent>) ret).setRollingPolicy(policy);
		}

		if (appender.level != null) {
			ThresholdFilter lf = new ThresholdFilter();
			lf.setLevel(appender.level.toString());
			lf.setContext(logger.getLoggerContext());
			lf.start();
			ret.addFilter(lf);
		}
		
		ret.setEncoder(encoder);
		ret.start();

		logger.addAppender(ret);
		return this;
	}

	/*
	 * public Log mail(String host, int port, String username, String password,
	 * boolean tls) { SMTPAppender appender = new SMTPAppender();
	 * appender.setContext(logger.getLoggerContext());
	 * appender.setSMTPHost(host); appender.setSmtpPort(port);
	 * appender.setUsername(username); appender.setPassword(password);
	 * CyclicBufferTracker<ILoggingEvent> cbTracker = new
	 * CyclicBufferTracker<ILoggingEvent>(); cbTracker.setBufferSize(10);
	 * appender.setCyclicBufferTracker(cbTracker);
	 * appender.setFrom("harsh@redhotcasino.in");
	 * appender.addTo("harsh@crispygam.es");
	 * 
	 * appender.setSTARTTLS(tls);
	 * 
	 * PatternLayout layout = new PatternLayout();
	 * layout.setContext(logger.getLoggerContext()); if (pattern != null) {
	 * layout.setPattern(pattern); } else { layout.setPattern(
	 * "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"); }
	 * 
	 * layout.start(); appender.setLayout(layout);
	 * 
	 * MDCBasedDiscriminator discriminator = new MDCBasedDiscriminator();
	 * discriminator.setContext(logger.getLoggerContext());
	 * discriminator.setKey("req.remoteHost"); discriminator.start();
	 * appender.setDiscriminator(discriminator);
	 * 
	 * appender.start();
	 * 
	 * logger.addAppender(appender); return this; }
	 */

	public void info(String message) {
		logger.info(message);
	}

	public void info(String format, Object message1, Object message2) {
		logger.info(format, message1, message2);
	}

	public void info(String format, Object message) {
		logger.info(format, message);
	}

	public void error(String message) {
		logger.error(message);
	}

	public void error(String format, Object message) {
		logger.error(format, message);
	}

	public void error(String format, Object message1, Object message2) {
		logger.error(format, message1, message2);
	}

	public void error(String message, Throwable t) {
		logger.error(message, t);
	}

	public void trace(String message) {
		logger.trace(message);
	}

	public void warn(String message) {
		logger.warn(message);
	}
	
	public void warn(String format, Object message) {
		logger.warn(format, message);
	}
	
	public void warn(String format, Object message1, Object message2) {
		logger.warn(format, message1, message2);
	}

	public void debug(String message) {
		logger.debug(message);
	}

	public void debug(String format, Object message) {
		logger.debug(format, message);
	}

	public void debug(String format, Object message1, Object message2) {
		logger.debug(format, message1, message2);
	}

	public static String safe(String m) {
		return URLEncoder.encode(m);
	}

	public void trace(String format, Object message) {
		logger.trace(format, message);
	}

	public void trace(String format, Object message1, Object message2) {
		logger.trace(format, message1, message2);
	}
}
