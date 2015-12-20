package com.crispy.log;

import java.util.concurrent.ConcurrentHashMap;

import com.crispy.mail.Mail;

import ch.qos.logback.classic.net.SMTPAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.helpers.CyclicBuffer;

public class CrispySMTPAppender extends SMTPAppender {

	private ConcurrentHashMap<String, Long> mSubjectIndex;
	private final long ONE_HOUR = 1L * 3600L * 1000L * 1000L * 1000L;
		
	public CrispySMTPAppender() {
		super();
		mSubjectIndex = new ConcurrentHashMap<String, Long>();
	}
	
	@Override
	protected void sendBuffer(CyclicBuffer<ILoggingEvent> cb, ILoggingEvent lastEventObject) {
        String subjectStr = subjectLayout.doLayout(lastEventObject);
        long lastSent = mSubjectIndex.getOrDefault(subjectStr, 0L);
        long nowTime = System.nanoTime();
        
        if ((nowTime - lastSent) > ONE_HOUR) {
        	if (mSubjectIndex.size() < 1000) { 
        		mSubjectIndex.put(subjectStr, nowTime);
        		super.sendBuffer(cb, lastEventObject);
        	} else {
                System.out.println("SMTP ERROR CAN NOT SEND EMAIL. BUFFER IS FULL");
        	}
        }
	}
}
