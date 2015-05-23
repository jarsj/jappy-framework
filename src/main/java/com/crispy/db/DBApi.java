package com.crispy.db;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.crispy.db.Table.WhereOp;

@WebServlet("/jappy/db/*")
public class DBApi extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			String path = req.getPathInfo(); 
			if (path.startsWith("_")) {
				doAngular(req, resp);
				return;
			}
			
			String table = req.getPathInfo();
			Table t = Table.get(table);
			boolean isArray = false;
			Enumeration<String> parameters = req.getParameterNames();
			while (parameters.hasMoreElements()) {
				String column = parameters.nextElement();
				if (column.equals("isArray")) {
					isArray = true;
					continue;
				}
				
				String value = req.getParameter(column);
				WhereOp op = WhereOp.EQUALS;
				if (value.startsWith("!"))
					op = WhereOp.NOT_EQUALS;
				if (Objects.equals(value, "NULL")) {
					if (op == WhereOp.EQUALS)
						t.isNull(column);
					else
						t.isNotNull(column);
				} else {
					t.where(column, value, op);
				}
			}

			if (isArray) {
				JSONArray ret = Row.rowsToJSON(t.rows(), new RowTransform() {
					@Override
					public JSONObject transform(JSONObject o) {
						return o;
					}
				});
				resp.getWriter().write(ret.toString());
				resp.getWriter().flush();
			} else {
				JSONObject ret = Row.rowToJSON(t.row());
				resp.getWriter().write(ret.toString());
				resp.getWriter().flush();
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	private void doAngular(HttpServletRequest req, HttpServletResponse resp) {
		StringBuilder sb = new StringBuilder();
		String module = req.getParameter("module");
		// Go over all tables and create a default thingie.
		for (Metadata m : DB.getTables()) {
			sb.append(module + "");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

	}
}
