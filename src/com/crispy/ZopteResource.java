package com.crispy;

import java.io.File;

public class ZopteResource {
	enum ZopteResourceType {
		CLASS,
		FILE,
		STRING
	}
	
	ZopteResourceType type;
	File file;
	String path;
	long lastModified;
}
