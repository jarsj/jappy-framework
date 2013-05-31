package com.crispy;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import com.crispy.ZopteResource.ZopteResourceType;

import freemarker.cache.TemplateLoader;

/**
 * Zopte resource loader. A resource is either CSS or HTML.
 * 
 * @author harsh
 */
public class ZopteResourceLoader implements TemplateLoader {

	private CopyOnWriteArrayList<File> searchPaths;
	
	private static final Logger LOG = Logger
			.getLogger(ZopteResourceLoader.class);

	public ZopteResourceLoader() {
		searchPaths = new CopyOnWriteArrayList<File>();
	}

	public void closeTemplateSource(Object templateSource) throws IOException {
		LOG.debug("closeTemplateSource " + templateSource);
	}

	public Object findTemplateSource(String name) throws IOException {
		LOG.trace("findTemplateSource " + name);
		if (name.startsWith("class:")) {
			ZopteResource zr = new ZopteResource();
			zr.type = ZopteResourceType.CLASS;
			zr.path = "/" + name.substring(name.indexOf(':') + 1);
			zr.lastModified = System.currentTimeMillis();
			return zr;
		} else if (name.startsWith("file:")) {
			ZopteResource zr = new ZopteResource();
			zr.type = ZopteResourceType.FILE;
			zr.path = "/" + name.substring(name.indexOf(':') + 1);
			for (File folder : searchPaths) {
				File f = new File(folder, zr.path);
				if (f.exists()) {
					zr.file = f;
					break;
				}
			}
			return zr;
		} else {
			ZopteResource zr = new ZopteResource();
			zr.type = ZopteResourceType.STRING;
			zr.path = name;
			zr.lastModified = System.currentTimeMillis();
			return zr;
		}
	}

	public long getLastModified(Object templateSource) {
		ZopteResource zr = (ZopteResource) templateSource;
		switch (zr.type) {
		case CLASS:
			return zr.lastModified;
		case FILE:
			return zr.file.lastModified();
		case STRING:
			return zr.lastModified;
		default:
			break;
		}
		return zr.lastModified;
	}

	public Reader getReader(Object templateSource, String encoding)
			throws IOException {
		ZopteResource zr = (ZopteResource) templateSource;
		switch (zr.type) {
		case CLASS:
			return new InputStreamReader(getClass()
					.getResourceAsStream(zr.path));
		case FILE:
			return new FileReader(zr.file);
		case STRING:
			return new StringReader(zr.path);
		}
		return null;
	}

	public void addFolder(File f) {
		searchPaths.add(f);
	}
}
