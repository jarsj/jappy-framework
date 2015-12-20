package com.crispy;

import com.crispy.mail.Mail;
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

	public void testConsole() throws Exception {
		Cloud.init("/Users/harsh/.jappyconfig");

		Log log = Log.get("LogTest").appender(Appender.create().console().pattern("%m%n").level(Level.DEBUG));
		
		log.debug("hello {}", null);
		log.debug("hello {}", new JSONObject().put("fuck", "shit"));
		log.trace("hello {} shit {}", new JSONObject().put("fuck", "shit"), new JSONObject().put("holy", "cow"));
	}

    @Test
	public void testEmail() throws Exception {
        Cloud.init("/Users/harsh/.jappyconfig");
        // Mail.setDefaultCredentialsFile("/Users/harsh/.jappyconfig");
        Log log = Log.get("booboo").appender(Appender.create().ec2Mail("us-east-1", "/Users/harsh/.jappyconfig").from
                ("booboo@crispygames.com").to("kiran@crispygam.es"));
        log.error("This message will only be sent once.");
        Thread.sleep(5000);
        log.error("This message will only be sent once.");
        Thread.sleep(10000);
	}
	
	public void testS3() throws Exception {
		Cloud.init("/Users/harsh/.jappyconfig");
		Log log = Log.get("s3logger").appender(Appender.create().folder("/mnt/logs2").s3("crispy-casino", "boom/troom").size("100kb").daily());
		
		for (int i = 0; i < 10000; i++) {
			log.info("What the fuck is going on " + i);
		}
	}

}
