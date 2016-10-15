package com.crispy.server;

import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpSession;

/**
 * Created by harsh on 6/30/16.
 */
@WebServlet(asyncSupported = true, urlPatterns = { "/basic", "/basic/*" })
public class BasicUnitServlet extends Servlet {

    @GetMethod(path = "/args")
    public JSONObject getArgs(@Param("string") String s,
                              @Param("int") int i,
                              @Param("integer") Integer ii) {
        JSONObject ret = new JSONObject();
        if (s != null) ret.put("string", s);
        ret.put("int", i);
        if (ii != null) ret.put("integer", ii);
        return ret;
    }

    @GetMethod(path = "/session")
    public String readSession(@Session("userid") Long userId) {
        if (userId == null)
            return "";
        return Long.toString(userId);
    }

    @PostMethod(path = "/session")
    public void writeSession(@Param("key") String key, @Param("value") Long value, HttpSession session) {
        session.setAttribute(key, value);
    }

}
