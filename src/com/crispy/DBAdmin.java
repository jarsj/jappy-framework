package com.crispy;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.crispy.Table.WhereOp;


@WebServlet("/dbadmin/*")
public class DBAdmin extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			if (req.getPathInfo() == null) {
				resp.sendRedirect("dbadmin/");
				return;
			}
			
			String path[] = req.getPathInfo().split("/");
			if (path.length == 3 && path[2].equals("delete")) {
				String table = path[1];
				String c = req.getParameter("c");
				String v = req.getParameter("v");
				Table.get(table).where(c, v).delete();
				resp.sendRedirect(req.getContextPath() + "/dbadmin/" + table);
				return;
			}

			if (path.length == 3 && path[2].equals("lookup")) {
				String table = path[1];
				String c = req.getParameter("c");
				Metadata m = DB.getMetadata(table);
				
				if (m.getDisplay() == null) {
					throw new IllegalStateException("This table has no display configured. Don't lookup");
				}

				JSONArray ret = new JSONArray();
				List<Row> rows = Table
						.get(table)
						.columns(c, m.getDisplay())
						.where(m.getDisplay(), "%" + req.getParameter("term") + "%",
								WhereOp.LIKE).limit(10).rows();
				for (Row r : rows) {
					String display = r.display();
					ret.put(new JSONObject().put("id", r.columnAsString(c))
							.put("label", display).put("value", display));
				}
				resp.getWriter().write(ret.toString());
				resp.getWriter().flush();
				return;
			}

			if (path.length == 3 && path[2].equals("fetch")) {
				resp.getWriter().write(fetch(path[1], req).toString());
				resp.getWriter().flush();
				return;
			}

			JSONObject data = new JSONObject();
			data.put("root", req.getContextPath());
			addMetadata(data);
			PrintWriter out = resp.getWriter();
			out.write(Template.expand("class:header.tpl", data));
			if (path.length >= 2) {
				String table = path[1];
				addTable(data, table);
				Metadata m = DB.getMetadata(table);
				String columns[] = m.columnNames();

				JSONArray columnsJSON = new JSONArray();
				for (Column c : m.getColumns()) {
					if (c.isAutoIncrement())
						continue;
					if (c.getType().equals("TIMESTAMP") && c.getDefault() != null)
						continue;
					SimpleType type = c.simpleType(m);

					JSONObject columnJSON = new JSONObject();
					columnJSON.put("name", c.getName());
					columnJSON.put("type", type.toString());
					if (type == SimpleType.REFERENCE) {
						Constraint cons = m.getConstraint(c.getName());
						columnJSON.put("destTable", cons.getDestTable());
						columnJSON.put("destColumn", cons.getDestColumn());
					}
					columnsJSON.put(columnJSON);
				}
				data.put("columns", columnsJSON);

				if (path.length == 2) {
					data.put("primaryColumn", m.getPrimary().getColumn(0));
					data.put("preview",
							Utils.arrayToJSON(Arrays.asList(columns(m, 6))));
					out.write(Template.expand("class:table.tpl", data));
				} else if (path.length == 3) {
					String c = req.getParameter("c");
					String v = req.getParameter("v");

					if (path[2].equals("edit")) {

						data.put("primaryColumn", c);
						data.put("primaryValue", v);
						Row row = Table.get(table).where(c, v).row();
						JSONObject rowJSON = new JSONObject();
						JSONObject remoteJSON = new JSONObject();
						for (Column col : m.getColumns()) {
							if (col.isAutoIncrement())
								continue;
							if (m.getPrimary().hasColumn(col.getName()))
								continue;
							switch (col.simpleType(m)) {
							case TEXT:
							case LONGTEXT:
								rowJSON.put(col.getName(), StringEscapeUtils
										.escapeHtml(row.columnAsString(col
												.getName())));
								break;
							case INTEGER:
								rowJSON.put(col.getName(),
										row.columnAsString(col.getName()));
								break;
							case DATETIME:
								rowJSON.put(col.getName(), 
										row.dateAsString(col.getName(), "yyyy-MM-dd HH:mm:ss"));
								break;
							case DATE:
								rowJSON.put(col.getName(), row.dateAsString(
										col.getName(), "yyyy-MM-dd"));
								break;
							case REFERENCE:
								rowJSON.put(col.getName(),
										row.columnAsString(col.getName()));
								Constraint cons = m
										.getConstraint(col.getName());
								Metadata dstMeta = DB.getMetadata(cons.getDestTable());
								Row remote = Table
										.get(cons.getDestTable())
										.where(cons.getDestColumn(),
												row.column(col.getName()))
										.columns(dstMeta.getDisplay()).row();
								if (remote == null) {
									remoteJSON.put(col.getName(), "");
								} else {
									remoteJSON.put(col.getName(), StringEscapeUtils
											.escapeHtml(remote.display()));
								}
								break;
							}
						}
						data.put("row", rowJSON);
						data.put("remote", remoteJSON);

						out.write(Template.expand("class:edit.tpl", data));
					}

				}
			}

			out.write(Template.expand("class:footer.tpl", data));
		} catch (Throwable t) {
			t.printStackTrace();
			throw new ServletException(t);
		}
	}

	private String[] columns(Metadata m, int N) {
		ArrayList<String> columns = new ArrayList<String>();
		columns.add(m.getPrimary().getColumn(0));
		String[] temp = m.columnNames();
		for (int i = 0; i < temp.length && columns.size() < N; i++) {
			if (temp[i].equals(m.getPrimary().getColumn(0)))
				continue;
			columns.add(temp[i]);
		}
		return columns.toArray(new String[] {});
	}

	private JSONObject fetch(String table, HttpServletRequest req)
			throws NumberFormatException, JSONException, SQLException {
		Metadata m = DB.getMetadata(table);
		int iColumns = Integer.parseInt(req.getParameter("iColumns"));
		String sSearch = req.getParameter("sSearch");
		if (sSearch != null && sSearch.trim().length() == 0)
			sSearch = null;

		String[] previewColumns = columns(m, iColumns - 1);

		Table t = Table.get(table).columns(previewColumns)
				.start(Integer.parseInt(req.getParameter("iDisplayStart")))
				.limit(Integer.parseInt(req.getParameter("iDisplayLength")));
		int iSortingCols = Integer.parseInt(req.getParameter("iSortingCols"));
		if (iSortingCols >= 1) {
			int sortColumn = Integer.parseInt(req.getParameter("iSortCol_0"));
			String direction = req.getParameter("sSortDir_0");
			if (direction.equals("asc")) {
				t = t.ascending(previewColumns[sortColumn]);
			} else {
				t = t.descending(previewColumns[sortColumn]);
			}
		}

		/**
		 * Note we can only search display columns. If no display column is configured, we 
		 * can't search. Ideally show this in the UI.
		 */
		if (sSearch != null && m.getDisplay() != null) {
			t = t.where(m.getDisplay(), sSearch + "%", WhereOp.LIKE);
		}

		List<Row> rows = t.rows();

		JSONObject ret = new JSONObject();
		ret.put("sEcho", Integer.parseInt(req.getParameter("sEcho")));
		ret.put("iTotalRecords", Table.get(table).count());
		ret.put("iTotalDisplayRecords", rows.size());

		JSONArray data = new JSONArray();
		
		for (Row r : rows) {
			JSONArray rowJSON = new JSONArray();
			for (String column : previewColumns) {
				if (m.getConstraint(column) != null) {
					Constraint cons = m.getConstraint(column);
					Row toShow = Table.get(cons.getDestTable())
							.where(cons.getDestColumn(), r.column(column))
							.row();
					if (toShow == null) {
						rowJSON.put("NULL");
					} else {
						rowJSON.put(toShow.display());
					}
				} else {
					rowJSON.put(Utils.trim(r.columnAsString(column, ""), 30));
				}

			}
			StringBuilder action = new StringBuilder();
			action.append(String
					.format("<a href='../dbadmin/%s/edit?c=%s&v=%s'><i class='icon-pencil'></i></a>",
							table, m.getPrimary().getColumn(0),
							r.columnAsString(m.getPrimary().getColumn(0))));
			action.append(String
					.format("<a href='../dbadmin/%s/delete?c=%s&v=%s'><i class='icon-trash'></i></a>",
							table, m.getPrimary().getColumn(0),
							r.columnAsString(m.getPrimary().getColumn(0))));
			rowJSON.put(action.toString());
			data.put(rowJSON);
		}
		ret.put("aaData", data);
		return ret;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		for (String key : req.getParameterMap().keySet()) {
			System.out.println(key + "=" + req.getParameter(key));
		}
		if (req.getPathInfo() == null) {
			resp.sendError(500);
			return;
		}
		String path[] = req.getPathInfo().split("/");
		if (path.length != 3) {
			resp.sendError(500);
			return;
		}
		String table = path[1];
		Metadata m = DB.getMetadata(table);

		if (path[2].equals("add")) {
			ArrayList<String> columns = Collections.list(req
					.getParameterNames());
			Iterator<String> iter = columns.iterator();
			while (iter.hasNext()) {
				if (m.getColumn(iter.next()) == null)
					iter.remove();
			}
			String[] values = new String[columns.size()];
			for (int i = 0; i < values.length; i++) {
				values[i] = req.getParameter(columns.get(i));
			}

			try {
				Table.get(table).columns(columns.toArray(new String[] {}))
						.values(values).add();
				resp.sendRedirect(req.getContextPath() + "/dbadmin/" + table);
				return;
			} catch (Exception e) {
				e.printStackTrace();
				resp.sendRedirect(req.getContextPath() + "/dbadmin/" + table
						+ "?error=" + e.getMessage());
				return;
			}
		} else if (path[2].equals("edit")) {
			String c = req.getParameter("_c");
			String v = req.getParameter("_v");

			ArrayList<String> columns = Collections.list(req
					.getParameterNames());
			Iterator<String> iter = columns.iterator();
			while (iter.hasNext()) {
				if (m.getColumn(iter.next()) == null)
					iter.remove();
			}

			String[] values = new String[columns.size()];
			for (int i = 0; i < values.length; i++) {
				values[i] = req.getParameter(columns.get(i));
			}

			try {
				Table.get(table).columns(columns.toArray(new String[] {}))
						.values(values).where(c, v).update();
				resp.sendRedirect(req.getContextPath() + "/dbadmin/" + table
						+ "/edit?c=" + c + "&v=" + v);
				return;
			} catch (Exception e) {
				e.printStackTrace();
				resp.sendRedirect(req.getContextPath() + "/dbadmin/" + table
						+ "/edit?c=" + c + "&v=" + v + "&error="
						+ e.getMessage());
				return;
			}
		}
	}

	private void addMetadata(JSONObject data) throws JSONException {
		JSONArray tables = new JSONArray();
		for (Metadata m : DB.getTables()) {
			if (m.isSystem())
				continue;
			tables.put(new JSONObject().put("name", m.getTableName()));
		}
		data.put("tables", tables);
	}

	private void addTable(JSONObject data, String table) throws JSONException {
		Metadata m = DB.getMetadata(table);
		data.put("table", m.toJSONObject());
	}

}
