package com.crispy.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.json.JSONObject;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public class Template {
	private static Template INSTANCE = new Template();

	private Configuration mConfig;
	private ZopteResourceLoader mLoader;

	public static Template getInstance() {
		return INSTANCE;
	}
	
	private Template() {
		try {
			mConfig = new Configuration();
			mLoader = new ZopteResourceLoader();
			mConfig.setTemplateLoader(mLoader);
			mConfig.setLocalizedLookup(false);
			mConfig.setClassicCompatible(false);
			mConfig.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
			mConfig.setWhitespaceStripping(false);
			mConfig.setSetting("url_escaping_charset", "UTF-8");
			mConfig.setDefaultEncoding("UTF-8");
			mConfig.setTemplateUpdateDelay(10);
			mConfig.setStrictSyntaxMode(true);
			mConfig.setObjectWrapper(UWrapper.INSTANCE);
			mConfig.setNumberFormat("number");
		} catch (Throwable t) {

		}
	}
	
		

	public void addFolder(File f) {
		mLoader.addFolder(f);
	}

	public static String expand(String template, JSONObject dict)
			throws IOException {
		try {
			StringWriter sw = new StringWriter();
			freemarker.template.Template t = getInstance().mConfig
					.getTemplate(template);
			t.process(dict, sw, UWrapper.INSTANCE);
			return sw.toString();
		} catch (TemplateException | ParseException t) {
			return "ERROR:" + t.getMessage();
		}
	}
	
	public void clearCache() {
		mConfig.clearTemplateCache();
	}
}
