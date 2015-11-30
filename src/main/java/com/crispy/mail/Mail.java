package com.crispy.mail;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.JSONObject;

/**
 * Mail component.
 * 
 * @author harsh
 */
public class Mail {
	private static Properties props;
	private static String username;
	private static String password;

	private static ScheduledExecutorService background;
	private static ConcurrentHashMap<String, LinkedBlockingQueue<String>> queue;

	public static void init(String credentialsFile) throws Exception {
		Properties cProps = new Properties();
		cProps.load(new FileReader(credentialsFile));

		props = new Properties();
		props.put("mail.smtp.host", cProps.getProperty("smtpServer", "email-smtp.us-east-1.amazonaws.com"));
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", Integer.parseInt(cProps.getProperty("smtpPort", "587")));

		username = cProps.getProperty("smtpUsername");
		password = cProps.getProperty("smtpPassword");
		
		if (username == null || password == null) 
			throw new IllegalStateException("Define smtpUsername or password");

		background = Executors.newSingleThreadScheduledExecutor();
		queue = new ConcurrentHashMap<String, LinkedBlockingQueue<String>>();
		background.scheduleAtFixedRate(sendEmailRunnable, 0, 1, TimeUnit.MINUTES);
	}

	public static void send(String from, String to, String subject, String body) {
		try {
			internalSend(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			}, from, to, subject, body);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void queue(String to, String subject, String body) {
		LinkedBlockingQueue<String> q = queue.get(to);
		if (q == null) {
			q = new LinkedBlockingQueue<String>();
			queue.put(to, q);
		}
		try {
			q.add(new JSONObject().put("to", to).put("subject", subject).put("body", body).toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void internalSend(Authenticator auth, String from, String to, String subject, String body) throws AddressException, MessagingException {
		Session session = Session.getDefaultInstance(props, auth);
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		message.setSubject(subject);
		message.setText(body);
		Transport.send(message);

	}

	private static final Runnable sendEmailRunnable = new Runnable() {

		@Override
		public void run() {
			try {

				Authenticator auth = new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				};

				for (Map.Entry<String, LinkedBlockingQueue<String>> entry : queue.entrySet()) {
					String to = entry.getKey();
					LinkedBlockingQueue<String> q = entry.getValue();

					ArrayList<String> t = new ArrayList<String>();
					q.drainTo(t);

					HashMap<String, StringBuilder> bySubjects = new HashMap<String, StringBuilder>();

					for (String mail : t) {
						JSONObject o = new JSONObject(mail);
						StringBuilder body = bySubjects.get(o.getString("subject"));
						if (body == null) {
							body = new StringBuilder();
							bySubjects.put(o.getString("subject"), body);
						}
						body.append("\n\n");
						body.append(o.getString("body"));
					}

					for (Map.Entry<String, StringBuilder> entry2 : bySubjects.entrySet()) {
						internalSend(auth, "harsh@zopte.com", to, entry2.getKey(), entry2.getValue().toString());
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	};

	public void shutdown() {
		if (background != null) {
			background.shutdown();
		}
	}

	public void shutdownNow() {
		if (background != null) {
			background.shutdownNow();
		}
	}

}
