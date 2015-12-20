package com.crispy.mail;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
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

    private static int maxMessagesPerMinute = 600;
    private static ScheduledExecutorService background;
    private static ConcurrentHashMap<String, Mail> sharedInstances;
    private static String defaultCredentialsFile;
    private static LinkedBlockingQueue<SendEmailRunnable> queue;

    private static final Runnable sendEmailRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                List<SendEmailRunnable> tasks = new ArrayList<>();
                queue.drainTo(tasks, maxMessagesPerMinute / 6);
                for (SendEmailRunnable task : tasks) {
                    task.run();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    };

    static {
        background = Executors.newSingleThreadScheduledExecutor();
        sharedInstances = new ConcurrentHashMap<>();
        queue = new LinkedBlockingQueue<>();
        background.scheduleAtFixedRate(sendEmailRunnable, 0, 10, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                background.shutdown();
                try {
                    background.awaitTermination(2, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    private Properties props;
    private Authenticator auth;
    private String from;
    private String[] to;
    private String subject;
    private String body;
    private boolean html;

    private Mail() {
        html = false;
    }

    private Mail(Mail m) {
        auth = m.auth;
        props = m.props;
        from = m.from;
        if (m.to != null) {
            to = new String[m.to.length];
            System.arraycopy(m.to, 0, to, 0, m.to.length);
        }
        subject = m.subject;
        body = m.body;
        html = m.html;
    }

    public static void setDefaultCredentialsFile(String file) {
        defaultCredentialsFile = file;
    }

    public static void setMaxMessagesPerMinute(int m) {
        if (m < 1) throw new IllegalArgumentException("Can't set max messages per minute to < 1");
        maxMessagesPerMinute = m;
    }

    public static Mail get(String type) {
        return sharedInstances.computeIfAbsent(type, k -> {
            Mail m = new Mail();
            if (defaultCredentialsFile != null) {
                try {
                    m.setCredentials(defaultCredentialsFile);
                } catch (IOException e) {
                    return null;
                }
            }
            return m;
        });
    }

    public Mail setCredentials(String file) throws IOException {
        Properties cProps = new Properties();
        cProps.load(new FileReader(file));

        props = new Properties();
        props.put("mail.smtp.host", cProps.getProperty("smtpServer", "email-smtp.us-east-1.amazonaws.com"));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", Integer.parseInt(cProps.getProperty("smtpPort", "587")));

        String username = cProps.getProperty("smtpUsername");
        String password = cProps.getProperty("smtpPassword");

        if (username == null || password == null)
            throw new IllegalStateException("Define smtpUsername or password");

        auth = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };

        return this;
    }

    public Mail withFrom(String from) {
        Mail m = new Mail(this);
        m.from = from;
        return m;
    }

    public Mail withTo(String... to) {
        Mail m = new Mail(this);
        m.to = to;
        return m;
    }

    public Mail withSubject(String subject) {
        Mail m = new Mail(this);
        m.subject = subject;
        return m;
    }

    public Mail enableHtml() {
        html = true;
        return this;
    }

    public Mail withBody(String body) {
        Mail m = new Mail(this);
        m.body = body;
        return m;
    }

    public void send() {
        send(from, subject, body, to);
    }

    public void sendTo(String... to) {
        send(from, subject, body, to);
    }

    public void sendFrom(String from) {
        send(from, subject, body, to);
    }

    public void send(String subject, String body) {
        send(from, subject, body, to);
    }

    public void sendToWithSubject(String subject, String ... to) {
        send(from, subject, body, to);
    }

    public void sendToWithSubjectAndBody(String subject, String body, String ... to) {
        send(from, subject, body, to);
    }

    public void send(String from, String subject, String body, String... to) {
        if (auth == null || props == null)
            throw new IllegalStateException("Mail doesn't have credentials");
        if (from == null)
            throw new IllegalStateException("Can't send email without a from");
        if (to == null || to.length == 0)
            throw new IllegalStateException("Can't send email without a destination");
        if (subject == null)
            subject = "";
        if (body == null)
            body = "";

        SendEmailRunnable ser = new SendEmailRunnable();
        ser.auth = auth;
        ser.props = props;
        ser.from = from;
        ser.subject = subject;
        ser.body = body;
        ser.tos = to;
        queue.offer(ser);
    }

    private class SendEmailRunnable implements Runnable {
        Properties props;
        Authenticator auth;
        String from;
        String[] tos;
        String subject;
        String body;


        @Override
        public void run() {
            try {
                Session session = Session.getDefaultInstance(props, auth);
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                for (String to : tos) {
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
                }
                message.setSubject(subject);
                if (html) {
                    message.setContent(body, "text/html; charset=utf-8");
                } else {
                    message.setText(body);
                }

                Transport.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
