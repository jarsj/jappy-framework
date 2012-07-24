/*
Copyright (c) 2010 tgerm.com
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, 
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.crispy;
	
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.net.SMTPAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Extension of Log4j {@link SMTPAppender} for Gmail support
 * 
 * @author abhinav@tgerm.com
 * @see <a href="http://code.google.com/p/log4j-gmail-smtp-appender/">Google Code Project</a> <br/>
 *      <a href="http://www.tgerm.com">My Blog</a>
 */
public class EC2SMTPAppender extends ConsoleAppender {
	
	String to;
	
	public EC2SMTPAppender(String to) {
		super();
		this.to = to;
	}
	
	@Override
	public void append(LoggingEvent event) {
		try {
			Mail.getInstance().sendMail(new JSONObject().put("to", to).put("subject", "Error: " + event.getLogger().getName() + ":" + event.getMessage()));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	

}
