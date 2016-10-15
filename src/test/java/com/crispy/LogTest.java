package com.crispy;

import ch.qos.logback.classic.Level;
import com.amazonaws.services.opsworks.model.App;
import com.crispy.cloud.Cloud;
import com.crispy.log.Appender;
import com.crispy.log.Log;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class LogTest {

	public void testConsole() throws Exception {
		Cloud.init("/Users/harsh/.jappyconfig");

		Log log = Log.get("LogTest").appender(Appender.create("console").console().pattern("%m%n").level(Level.DEBUG));
		
		log.debug("hello {}", null);
		log.debug("hello {}", new JSONObject().put("fuck", "shit"));
		log.trace("hello {} shit {}", new JSONObject().put("fuck", "shit"), new JSONObject().put("holy", "cow"));
	}

	public void testEmail() throws Exception {
        Cloud.init("/Users/harsh/.jappyconfig");
        // Mail.setDefaultCredentialsFile("/Users/harsh/.jappyconfig");
        Log log = Log.get("booboo").appender(Appender.create("mail").ec2Mail("us-east-1", "/Users/harsh/.jappyconfig")
                .from
                ("booboo@crispygames.com").to("kiran@crispygam.es"));
        log.error("This message will only be sent once.");
        Thread.sleep(5000);
        log.error("This message will only be sent once.");
        Thread.sleep(10000);
	}
	
	public void testS3() throws Exception {
		Cloud.init("/Users/harsh/.jappyconfig");
		Log log = Log.get("s3logger").appender(Appender.create("file").folder("/mnt/logs2").s3("crispy-casino",
                "boom/troom").size("100kb").daily());
		
		for (int i = 0; i < 10000; i++) {
			log.info("What the fuck is going on " + i);
		}
	}

	private void loopTill(int N, Consumer<Integer> callback) {
		for (int i = 0; i < N; i++) {
			callback.accept(i);
		}
	}

    private void runTest(String description, Consumer<Integer> callback) throws InterruptedException {
        Thread t[] = new Thread[100];
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            t[i] = new Thread(() -> loopTill(1000, callback));
            t[i].start();
        }
        for (int i = 0; i < 100; i++) {
            t[i].join();
        }
        long end = System.nanoTime();
        System.out.println(String.format("%s %dms", description, (end - start) / 1000000));
    }

    Log LOG;

	@Test
	public void testManyThreads() throws InterruptedException, IOException {
		// Let's start 100 threads that loop from 1 to 1000
		runTest("Withoout logging", (Integer i) -> {});

        Log.getRoot().clear();

        LOG = Log.get("test");
        runTest("Log no appenders", (Integer i) -> {
            LOG.info("SHIT HAPPENED {}", i);
        });

        File temp = FileUtils.getTempDirectory();
        File logFolder = new File(temp, "log" + System.currentTimeMillis());
        logFolder.mkdir();
        System.out.println(logFolder.getAbsolutePath());
        Log.getRoot().appender(Appender.create("file").folder(logFolder.getAbsolutePath()).level(Level.INFO));
        runTest("File Appender DEBUG", (Integer i) -> {
            LOG.debug("SHIT HAPPENED {}", i);
        });

        runTest("File Appender INFO", (Integer i) -> {
            LOG.info("SHIT HAPPENED {}", i);
        });
        // Now with logging.
        Log.getRoot().clear();
        Log.getRoot().appender(Appender.create("file").folder(logFolder.getAbsolutePath()).level(Level.INFO).async());
        runTest("File Appender ASYNC", (Integer i) -> {
            LOG.info("SHIT HAPPENED {}", i);
        });
        Thread.sleep(10000);

        Log.getRoot().appender(Appender.create("console").console());
        runTest("File+Console Appender INFO", (Integer i) -> {
            LOG.info("SHIT HAPPENED {}", i);
        });
	}

}
