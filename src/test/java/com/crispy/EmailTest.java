package com.crispy;

import com.crispy.mail.Mail;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by harsh on 12/19/15.
 */
public class EmailTest {

    @Test
    public void testSendingEmail() throws InterruptedException {
        Mail.setDefaultCredentialsFile("/Users/harsh/.jappyconfig");
        Mail mail = Mail.get("test");
        try {
            mail.send();
            fail("Should throw exception");
        } catch (Exception e) {
        }

        mail = mail.withFrom("test@crispygames.com").withSubject("This is a test email");
        mail.sendTo("harsh@crispygam.es", "kiran@crispygam.es");
        Thread.sleep(20000);
    }
}
