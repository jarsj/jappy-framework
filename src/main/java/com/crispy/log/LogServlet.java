package com.crispy.log;

import ch.qos.logback.classic.Level;
import com.crispy.server.GetMethod;
import com.crispy.server.Param;
import com.crispy.server.Servlet;

import javax.servlet.annotation.WebServlet;

/**
 * Created by harsh on 5/26/16.
 */
@WebServlet(urlPatterns = "/jappy/log/*")
public class LogServlet extends Servlet {

    @GetMethod(path = "/change")
    public void changeLevel(@Param("name") String name,
                            @Param("appender") String appender,
                            @Param("level") String level) {
        if (name.equals("")) {
            Log.getRoot().changeLevel(appender, Level.toLevel(level.toUpperCase()));
        } else {
            Log.get(name).changeLevel(appender, Level.toLevel(level.toUpperCase()));
        }
    }
}
