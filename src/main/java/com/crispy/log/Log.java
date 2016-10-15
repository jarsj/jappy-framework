package com.crispy.log;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import ch.qos.logback.classic.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.sift.MDCBasedDiscriminator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.S3TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.CyclicBufferTracker;

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
	}

	public static Log get(String name) {
		return new Log(name);
	}

	public static Log getRoot() {
		return new Log(Logger.ROOT_LOGGER_NAME);
	}

	public Log inherit(boolean inherit) {
		this.logger.setAdditive(inherit);
		return this;
	}

    public Log clear() {
        logger.setLevel(Level.ALL);
        logger.detachAndStopAllAppenders();
        return this;
    }

	void changeLevel(String appenderName, Level l) {
		ch.qos.logback.core.Appender<ILoggingEvent> appender = logger.getAppender(appenderName);
		if (appenderName == null) {
			throw new IllegalArgumentException("No appender " + appenderName + " in logger " + logger.getName());
		}
		appender.clearAllFilters();

		ThresholdFilter lf = new ThresholdFilter();
		lf.setLevel(l.toString());
		lf.setContext(logger.getLoggerContext());
		lf.start();

		appender.addFilter(lf);
	}

	public Log appender(Appender appender) {
        logger.detachAppender(appender.name);

		if (appender.smtpHost != null) {
			return smtpAppender(appender);
		}

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
				S3TimeBasedRollingPolicy<ILoggingEvent> s3Policy = (S3TimeBasedRollingPolicy<ILoggingEvent>) policy;
				s3Policy.setS3BucketName(appender.s3bucket);
				s3Policy.setS3FolderName(appender.s3folder + "/" + logger.getName());
				s3Policy.setS3UniqueId(appender.s3UniqueId);
				s3Policy.setRollingOnExit(true);
			} else {
				policy = new TimeBasedRollingPolicy<ILoggingEvent>();
			}

			policy.setMaxHistory(appender.history);

			if (appender.folder == null) {
				appender.folder = "/tmp";
			}
			try {
				FileUtils.forceMkdir(new File(appender.folder));
			} catch (IOException e) {
				throw new IllegalStateException("Can not initialize logging.", e);
			}

			policy.setParent((FileAppender) ret);
			policy.setContext(logger.getLoggerContext());
			// Rolling happens based on the filename pattern. Took a while to
			// discover this.

			if (appender.size != null) {
				if (appender.hourly) {
					policy.setFileNamePattern(appender.folder + "/" + logger.getName() + "-%d{yyyy-MM-dd-HH}.%i.log");
				} else {
					policy.setFileNamePattern(appender.folder + "/" + logger.getName() + "-%d{yyyy-MM-dd}.%i.log");
				}
			} else {
				if (appender.hourly) {
					policy.setFileNamePattern(appender.folder + "/" + logger.getName() + "-%d{yyyy-MM-dd-HH}.log");
				} else {
					policy.setFileNamePattern(appender.folder + "/" + logger.getName() + "-%d{yyyy-MM-dd}.log");
				}
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

        ret.setEncoder(encoder);

        if (appender.async) {
            ret.start();

            AsyncAppender aRet = new AsyncAppender();
            aRet.setContext(logger.getLoggerContext());
            aRet.addAppender(ret);
            aRet.setName(appender.name);
            aRet.setQueueSize(1000);

            if (appender.level != null) {
                ThresholdFilter lf = new ThresholdFilter();
                lf.setLevel(appender.level.toString());
                lf.setContext(logger.getLoggerContext());
                lf.start();
                aRet.addFilter(lf);
            }
            aRet.start();


            logger.addAppender(aRet);
        } else {
            ret.setName(appender.name);

            if (appender.level != null) {
                ThresholdFilter lf = new ThresholdFilter();
                lf.setLevel(appender.level.toString());
                lf.setContext(logger.getLoggerContext());
                lf.start();
                ret.addFilter(lf);
            }
            ret.start();

            logger.addAppender(ret);
        }

		return this;
	}

	private Log smtpAppender(Appender appender) {
		CrispySMTPAppender ret = new CrispySMTPAppender();
		ret.setContext(logger.getLoggerContext());
		ret.setSMTPHost(appender.smtpHost);
		ret.setSmtpPort(appender.smtpPort);
		ret.setUsername(appender.smtpUsername);
		ret.setPassword(appender.smtpPassword);

		CyclicBufferTracker<ILoggingEvent> cbTracker = new CyclicBufferTracker<ILoggingEvent>();
		cbTracker.setBufferSize(10);
		ret.setCyclicBufferTracker(cbTracker);
		ret.setFrom(appender.mailFrom);
		ret.addTo(appender.mailTo);

		ret.setSTARTTLS(appender.smtpTls);
		ret.setSSL(appender.smtpSsl);

		PatternLayout layout = new PatternLayout();
		layout.setContext(logger.getLoggerContext());
		layout.setPattern(appender.pattern);
		layout.start();
		ret.setLayout(layout);

		MDCBasedDiscriminator discriminator = new MDCBasedDiscriminator();
		discriminator.setContext(logger.getLoggerContext());
		discriminator.setKey("req.remoteHost");
		discriminator.start();
		ret.setDiscriminator(discriminator);

		ret.start();

		logger.addAppender(ret);
		return this;
	}

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

	public void errorFormat(String format, Object message) {
		logger.error(format, message);
	}

	public void errorFormat(String format, Object message1, Object message2) {
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

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}
}
