package com.crispy;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class Mail implements Runnable {
	private static Mail INSTANCE = new Mail();
	private Properties props;
	private String username;
	private String password;
	private ScheduledExecutorService background;

	public Mail() {

	}

	public void start(String host, String username, String password) {
		props = new Properties();
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.auth", "true");

		this.username = username;
		this.password = password;

		background = Executors.newSingleThreadScheduledExecutor();
		queue = new LinkedBlockingQueue<JSONObject>();
		background.scheduleAtFixedRate(this, 1, 1, TimeUnit.MINUTES);
	}

	public static Mail getInstance() {
		return INSTANCE;
	}

	public void sendMail(String to, String subject, String body) {
		try {
			sendMail(new JSONObject().put("to", to).put("subject", subject).put("body", body));
		} catch (JSONException e) {
		}
	}
	
	public void sendMail(JSONObject mail) {
		if (queue.size() > 100)
			return;
		if (!(mail.has("to") && mail.has("subject") && mail.has("body"))) {
			throw new IllegalArgumentException(
					"Mail JSON misses to, subject or body");
		}
		queue.add(mail);
	}

	@Override
	public void run() {
		try {
			JSONObject mail = queue.poll();
			if (mail != null) {
				Authenticator auth = new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				};
				Session session = Session.getDefaultInstance(props, auth);
				MimeMessage message = new MimeMessage(session);
				message.setFrom(new InternetAddress("harsh@zopte.com"));
				message.addRecipient(Message.RecipientType.TO,
						new InternetAddress(mail.getString("to")));
				message.setSubject(Utils.trim(mail.getString("subject"), 50));
				message.setText(mail.getString("body"));
				Transport.send(message);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void shutdown() {
		if (background != null) {
			background.shutdownNow();
		}
	}

	private LinkedBlockingQueue<JSONObject> queue;
}
