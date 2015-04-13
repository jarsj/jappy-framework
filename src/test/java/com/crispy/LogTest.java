package com.crispy;

import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import com.crispy.cloud.Cloud;
import com.crispy.log.Appender;
import com.crispy.log.Log;

public class LogTest {

	@Test
	public void testConsole() throws Exception {
		Cloud.init("/Users/harsh/.jappyconfig");
		Log log = Log.get("LogTest").appender(Appender.create().console().pattern("%m%n").level(Level.DEBUG));
		
		log.debug("hello {}", null);
		log.debug("hello {}", new JSONObject().put("fuck", "shit"));
		log.trace("hello {} shit {}", new JSONObject().put("fuck", "shit"), new JSONObject().put("holy", "cow"));
	}
	
	public void testEmail() throws InterruptedException {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		//Log log = Log.get("boom").mail("email-smtp.us-east-1.amazonaws.com", 587, "AKIAJDMIFXSB2IWDKEIA",
				//"Akjf6awdeFyXm+Cxziq+QQpE1HzpT4gGu1/gZElDsLHK", true);
		//MDC.put("req.remoteHost", "10.10.10.1");
		//for (int i = 0; i < 9; i++) {
//			log.info("Info " + i);
		//}
		//log.error("Boom");
		//Thread.sleep(1000000);
	}
	
	@Test
	public void testS3() throws Exception {
		Cloud.init("/Users/harsh/.jappyconfig");
		Log log = Log.get("s3logger").appender(Appender.create().folder("/mnt/logs2").s3("crispy-casino", "boom/troom").size("100kb").daily());
		
		for (int i = 0; i < 10000; i++) {
			log.info("What the fuck is going on " + i);
		}
	}

}
