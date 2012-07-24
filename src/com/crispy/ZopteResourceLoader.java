package com.crispy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import freemarker.cache.TemplateLoader;

/**
 * Zopte resource loader. A resource is either CSS or HTML.
 * 
 * @author harsh
 */
public class ZopteResourceLoader implements TemplateLoader {

	private static final Logger LOG = Logger
			.getLogger(ZopteResourceLoader.class);

	public ZopteResourceLoader() {
	}

	public void closeTemplateSource(Object templateSource) throws IOException {
		LOG.debug("closeTemplateSource " + templateSource);
	}

	public Object findTemplateSource(String name) throws IOException {
		LOG.trace("findTemplateSource " + name);
		if (name.startsWith("class:")) {
			ZopteResource zr = new ZopteResource();
			zr.isClass = true;
			zr.path = "/" + name.substring(name.indexOf(':') + 1);
			zr.lastModified = System.currentTimeMillis();
			return zr;
		} else {
			ZopteResource zr = new ZopteResource();
			zr.isClass = false;
			zr.path = name;
			zr.lastModified = System.currentTimeMillis();
			return zr;
		}
	}

	public long getLastModified(Object templateSource) {
		ZopteResource zr = (ZopteResource) templateSource;
		return zr.lastModified;
	}

	public Reader getReader(Object templateSource, String encoding)
			throws IOException {
		ZopteResource zr = (ZopteResource) templateSource;
		if (zr.isClass)
			return new InputStreamReader(getClass()
					.getResourceAsStream(zr.path));
		else
			return new StringReader(zr.path);
	}
}
