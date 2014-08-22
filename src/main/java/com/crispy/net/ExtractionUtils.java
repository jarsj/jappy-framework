package com.crispy.net;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pojava.datetime.DateTime;

public class ExtractionUtils {
	public static TagNode previous(TagNode node, String tag) {
		TagNode[] children = node.getParent().getChildTags();
		int myIndex = -1;
		for (int i = 0; i < children.length; i++)
			if (children[i] == node)
				myIndex = i;
		while (myIndex > 0) {
			myIndex--;
			if (children[myIndex].getName().equals(tag))
				return children[myIndex];
		}
		return null;
	}

	public static TagNode nextByTag(TagNode node, String tag) {
		TagNode[] children = node.getParent().getChildTags();
		int myIndex = -1;
		for (int i = 0; i < children.length; i++)
			if (children[i] == node)
				myIndex = i;
		while (myIndex < (children.length - 1)) {
			myIndex++;
			if (children[myIndex].getName().equals(tag))
				return children[myIndex];
		}
		return null;
	}

	public static Object sibling(TagNode node, int distance, String command) {
		List children = node.getParent().getChildren();
		int myIndex = -1;
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) == node)
				myIndex = i;
		}
		if (myIndex == -1)
			return null;
		int moved = 0;
		int direction = distance > 0 ? 1 : -1;
		distance = distance * direction;
		while (moved < distance && (myIndex >= 0 && myIndex < children.size())) {
			myIndex = myIndex + direction;
			if (myIndex < 0 || myIndex >= children.size())
				return null;
			Object next = children.get(myIndex);
			if (command.equals("any")) {
				moved++;
			} else if (command.equals("tag") && next instanceof TagNode) {
				moved++;
			} else if (next instanceof TagNode && ((TagNode) next).getName().equals(command)) {
				moved++;
			}
		}
		return children.get(myIndex);
	}

	public static TagNode[] evaluateXPath(TagNode node, String xpath) throws Exception {
		Object[] nodes = node.evaluateXPath(xpath);
		if (nodes == null)
			return new TagNode[0];
		TagNode[] ret = new TagNode[nodes.length];
		System.arraycopy(nodes, 0, ret, 0, nodes.length);
		return ret;
	}

	public static Object first(TagNode node, String xpath) throws Exception {
		Object[] nodes = node.evaluateXPath(xpath);
		if (nodes == null || nodes.length == 0)
			return null;
		return nodes[0];
	}

	public static TagNode next(TagNode node, String xpath) throws Exception {
		Object[] nodes = node.getParent().evaluateXPath(xpath);
		int myIndex = node.getParent().getChildIndex(node);
		if (nodes == null || nodes.length == 0)
			return null;
		for (int i = 0; i < nodes.length; i++) {
			int index = node.getParent().getChildIndex((HtmlNode) nodes[i]);
			if (index > myIndex)
				return (TagNode) nodes[i];
		}
		return null;
	}

	public static long money(TagNode node) throws Exception {
		ExtractionUtils.cleanWikipedia(node);
		String s = node.getText().toString().trim().toLowerCase();
		s = StringUtils.replaceChars(s, "'\":;,", "");
		long multiplier = 1L;
		if (s.contains("crore"))
			multiplier = 10000000L;
		if (s.startsWith("$")) {
			multiplier *= 50L;
			if (s.contains("million"))
				multiplier *= 1000000L;
		}
		try {
			return (long) DecimalFormat.getNumberInstance().parse(s).floatValue() * multiplier;
		} catch (Exception e) {
			return -1;
		}
	}

	public static Calendar date(TagNode node) throws Exception {
		ExtractionUtils.cleanWikipedia(node);
		List<String> lines = ExtractionUtils.lines(node);
		for (String text : lines) {
			try {
				long millis = DateTime.parse(text).toMillis();
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(millis);
				if (c.get(Calendar.YEAR) < 1800)
					return null;
				return c;
			} catch (Exception e) {
			}
		}
		return null;
	}

	public static int duration(TagNode node) throws Exception {
		ExtractionUtils.cleanWikipedia(node);
		String text = node.getText().toString().toLowerCase().trim();
		return DecimalFormat.getIntegerInstance().parse(text).intValue();
	}

	public static String imgSrc(TagNode img) {
		String ret = img.getAttributeByName("src");
		if (ret.startsWith("http:"))
			return ret;
		return "http:" + ret;
	}

	public static JSONArray extractPeople(TagNode td) throws JSONException {
		JSONArray ret = new JSONArray();
		List children = td.getChildren();
		for (Object c : children) {
			if (c instanceof ContentNode) {
				String content = ((ContentNode) c).getContent().toString().trim();
				if (content.length() > 0) {
					for (String cast : StringUtils.split(content, ",/")) {
						if (cast.trim().length() > 0) {
							ret.put(new JSONObject().put("name", cast.trim()));
						}
					}
				}
			} else if (c instanceof TagNode) {
				TagNode tc = (TagNode) c;
				if (tc.getName().equals("br"))
					continue;
				if (tc.getName().equals("a")) {
					ret.put(new JSONObject().put("name", tc.getText().toString().trim()).put("link", ExtractionUtils.fullWikiLink(tc)));
				}
			}
		}
		return ret;
	}

	public static List<String> lines(Object o) {
		ArrayList<String> ret = new ArrayList<String>();
		if (o instanceof ContentNode) {
			String content = ((ContentNode) o).getContent().toString();
			content = content.trim().toLowerCase();
			content = StringEscapeUtils.unescapeHtml(content);
			if (content.length() > 0) {
				ret.add(content);
			}
		} else if (o instanceof TagNode) {
			for (Object child : ((TagNode) o).getChildren()) {
				ret.addAll(lines(child));
			}
		}
		return ret;
	}

	public static String fullWikiLink(TagNode tc) {
		String link = tc.getAttributeByName("href");
		if (link == null)
			return null;
		if (link.startsWith("http"))
			return link;
		return "http://en.wikipedia.org" + link;
	}

	public static void cleanWikipedia(TagNode h2) throws Exception {
		clean(h2, "//span[@class=\"editsection\"]", "//span[@class=\"mw-editsection\"]", "//sup", "//span[@style=\"display:none\"]");
	}

	public static TagNode wikipediaHeadingNode(TagNode document, String... keyword) throws Exception {
		TagNode[] headings = evaluateXPath(document, "//div[@id=\"mw-content-text\"]/h2");
		for (TagNode heading : headings) {
			cleanWikipedia(heading);
			String ch = heading.getText().toString().trim().toLowerCase();
			for (String k : keyword) {
				if (k.equals(ch))
					return heading;
			}
		}
		return null;
	}

	public static void clean(TagNode cast, String... expr) throws Exception {
		for (String e : expr) {
			for (TagNode node : evaluateXPath(cast, e)) {
				node.removeFromTree();
			}
		}
	}

	public static String title(TagNode document) throws Exception {
		Object title = first(document, "//title");
		if (title == null)
			return "";
		if (title instanceof StringBuffer)
			return title.toString();
		if (title instanceof TagNode)
			return ((TagNode) title).getText().toString();
		return null;
	}

	public static Object special(TagNode document, String expr) throws Exception {
		if (expr.startsWith("next")) {
			String command = expr.substring(0, expr.indexOf('('));
			String sibling = command.substring(command.indexOf('-') + 1);
			expr = expr.substring(expr.indexOf('(') + 1);
			String nextExpr = expr.substring(0, expr.lastIndexOf(')'));
			expr = expr.substring(expr.lastIndexOf(')') + 1);

			TagNode nextNode = (TagNode) ExtractionUtils.first(document, nextExpr);
			Object nextNextNode = ExtractionUtils.sibling(nextNode, 1, sibling);
			if (expr.length() > 0)
				return ExtractionUtils.first((TagNode) nextNextNode, expr);
			return nextNextNode;
		}
		return ExtractionUtils.first(document, expr);
	}

	public static String text(Object node) {
		if (node instanceof TagNode)
			return ((TagNode) node).getText().toString();
		else if (node instanceof ContentNode)
			return ((ContentNode) node).getContent().toString();
		return StringEscapeUtils.unescapeHtml(node.toString());
	}

	public static String removeBrackets(String content) {
		int open = content.indexOf('(');
		if (open == -1)
			return content;
		int close = content.indexOf(')', open);
		if (close == -1)
			return content;
		return content.substring(0, open).trim();
	}
}
