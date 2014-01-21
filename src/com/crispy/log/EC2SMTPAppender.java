package com.crispy.log;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.json.JSONObject;

import com.crispy.mail.Mail;

public class EC2SMTPAppender extends ConsoleAppender {

	String to;
	Level l;

	public EC2SMTPAppender(String to, Level l) {
		super();
		this.to = to;
		this.l = l;
	}

	@Override
	public void append(LoggingEvent event) {
		try {
			if (event.getLevel().isGreaterOrEqual(l)) {
				JSONObject mail = new JSONObject();
				mail.put("to", to);
				mail.put("subject", "Error: " + event.getLogger().getName()
						+ ":" + event.getMessage());
				String body = event.getMessage() + "\n" + "Thread Name : "
						+ event.getThreadName() + "\n\n";
				try {
					body = body
							+ EC2SMTPAppender.class.getProtectionDomain()
									.getCodeSource().getLocation().toURI()
									.toASCIIString();
				} catch (Throwable t) {
				}

				if (event.getThrowableInformation() != null) {
					if (event.getThrowableInformation().getThrowable() != null) {
						Throwable t = event.getThrowableInformation()
								.getThrowable();
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						t.printStackTrace(pw);
						pw.flush();
						body = body + sw.toString();
					}
				}

				Mail.getInstance().sendMail(
						to,
						"Error: " + event.getLogger().getName() + ":"
								+ event.getMessage(), body);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
